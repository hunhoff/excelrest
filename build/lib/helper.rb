require 'fileutils'
require 'aws-sdk'
require 'timeout'
require 'rest_client'
require 'chef/environment'
require 'chef/node'
require 'chef/knife/node_run_list_set'
require 'presto/dsl/environment'
require File.expand_path(File.join(*%w[ .. lib bastion.rb ]), File.dirname(__FILE__))

module CD
  class Helper

    # Sometimes an error might occur while trying to retrieve objects from chef.
    # These constants are used to control the number of retries and the interval between retries.
    MAX_ATTEMPTS   = 10
    SLEEP_INTERVAL = 2

    def self.convert_dots_to_underscores(string)
      return string.gsub('.', '_')
    end

    def self.convert_dots_to_dashes(string)
      return string.gsub('.', '-')
    end

    def self.convert_underscores_to_dashes(string)
      return string.gsub('_', '-')
    end

    def self.copy_chef_logs(config)
      begin
        config.chef_ips.each do |ws_ip|
          # Zip and copy chef log files
          Presto.log.info 'Transferring chef logs...'
          self.ssh_sudo_cmd(config, ws_ip, "tar -czf /tmp/chef-logs.tgz #{config.chef_log_dir}")
          self.scp(config, ws_ip, '/tmp/chef-logs.tgz', "./chef-logs-#{ws_ip}.tgz")
          self.ssh_sudo_cmd(config, ws_ip, 'rm -f /tmp/chef-logs.tgz')
        end
      rescue Exception => e
        Presto.log.error 'Error copying chef logs...'
        Presto.log.error "#{e.class}: #{e}"
      end
    end

    # Zip and copy web server related log files, only stopping tomcat once
    # TODO: Put a hash in the config to denote whether these options are available...
    def self.copy_ws_logs(config)
      begin
        config.ws_ips.each do |ws_ip|
          self.ssh_sudo_cmd(config, ws_ip, '/etc/init.d/apache2 stop')
          self.ssh_sudo_cmd(config, ws_ip, '/etc/init.d/tomcat-genesis stop')
          self.ssh_sudo_cmd(config, ws_ip, '/etc/init.d/tomcat-indigo stop')
          self.ssh_sudo_cmd(config, ws_ip, '/etc/init.d/tomcat-skywalker stop')

          Presto.log.info 'Transferring apache2 logs...'
          self.ssh_sudo_cmd(config, ws_ip, "tar -czf /tmp/apache2-logs.tgz #{config.ws_hs_log_dir}")
          self.scp(config, ws_ip, '/tmp/apache2-logs.tgz', "./apache2-logs-#{ws_ip}.tgz")

          Presto.log.info 'Transferring tomcat logs...'
          self.ssh_sudo_cmd(config, ws_ip, "tar -czf /tmp/tomcat-logs.tgz #{config.ws_tc_log_dir}")
          self.scp(config, ws_ip, '/tmp/tomcat-logs.tgz', "./tomcat-logs-#{ws_ip}.tgz")

          # TODO: Uncomment when jacoco is enabled
          # Presto.log.info 'Transferring jacoco logs...'
          #sleep(30)
          #self.scp(config, ws_ip, '/usr/share/tomcat/agents/jacoco/data/jacocoagent.exec', "./jacoco-#{web_server}.exec")

          self.ssh_sudo_cmd(config, ws_ip, '/etc/init.d/tomcat-genesis start')
          self.ssh_sudo_cmd(config, ws_ip, '/etc/init.d/tomcat-indigo start')
          self.ssh_sudo_cmd(config, ws_ip, '/etc/init.d/tomcat-skywalker start')
          self.ssh_sudo_cmd(config, ws_ip, '/etc/init.d/apache2 start')

          self.ssh_sudo_cmd(config, ws_ip, 'rm -f /tmp/*-logs.tgz')
        end
      rescue Exception => e
        Presto.log.error 'Error copying tomcat logs...'
        Presto.log.error "#{e.class}: #{e}"
      end
    end

    def self.get_chef_config(environment_name, chef_server_url, chef_validation_client_name)
      chef_config = "current_dir = File.dirname(__FILE__)
      log_level                :warn
      log_location             STDOUT
      no_lazy_load             true
      client_key               \"/etc/chef/client.pem\"
      validation_client_name   \"#{chef_validation_client_name}\"
      validation_key           \"/etc/chef/validation.pem\"
      chef_server_url          \"#{chef_server_url}\"
      cache_type               'BasicFile'
      cache_options( :path => \"#{ENV['HOME']}/.chef/checksums\" )
      cookbook_path            [\"/root/chef-repo/cookbooks\"]
      environment             \"#{environment_name}\"
      "
      return chef_config
    end

    def self.get_first_run(environment_name, run_list = nil)
      first_run = "{\"cluster_name\" : \"#{environment_name}\"}"
      first_run = "{\"run_list\" : #{run_list}, \"cluster_name\" : \"#{environment_name}\"}" unless run_list.nil?
      return first_run
    end

    def self.get_nodes_by_name(config, node_name_prefix)
      # Search for nodes starting with node_name.  This will get used as a regex in the search
      nodes = config._cloud.retrieve_nodes(:name => "/^#{node_name_prefix}/")

      Presto.log.info "Filtered list of nodes: [#{nodes}]"

      nodes.map do |node|
        {
          :name      => node.name,
          :fqdn      => node.data['fqdn'],
          :public_ip => node.data['ipaddress']
        }
      end
    end

    def self.get_node_addresses(config, environment_name)
      nodes = self.get_nodes(config, environment_name).map { |node| node['ipaddress'] }
      # Return the list of ip adresses
      Presto.log.info "Nodes addresses: [#{nodes}]"

      nodes
    end

    def self.get_integration_number(environment_list)
      # Looking for environments that match the following names:
      # - idm-CD-step2-functional-test-1119095021-1-7-2100-lb
      # - IDM-berrac-1119095021-lb
      # Returns only the portion of the environment that represents the integration number
      environment_list.find { |e| /\d{10}/ =~ e }.match(/\d{10}/)
    end

    def self.get_server(config, server_name)
      config._cloud.server(server_name)
    end

    def self.node_chef_client_run(config, node_name, environment_name, server_public_ip, run_list)
      node_run_list           = Chef::Knife::NodeRunListSet.new
      node_run_list.name_args = [node_name, run_list.join(',')]
      node_run_list.run

      self.ssh_sudo_cmd(config, server_public_ip, "chef-client -E #{environment_name}")
    end

    def self.print_node_information(config, environment_name)
      # Get nodes
      nodes = self.get_nodes(config, environment_name)
      Presto.log.info '.'
      Presto.log.info "Environment: #{environment_name}"
      Presto.log.info '=' * 60

      nodes.each do |node|
        Presto.log.info "FQDN: #{node[:fqdn]}"
        Presto.log.info "Public IP: #{node['ipaddress']}"
        Presto.log.info '.'
      end

      Presto.log.info '=' * 60 + "\n"
    end

    def self.print_license_plate_url(config)
      Presto.log.info '.'
      Presto.log.info 'License Plate URL'
      Presto.log.info '=' * 60
      Presto.log.info "https://#{config.license_plate_url}"
      Presto.log.info '=' * 60 + "\n"
    end

    def self.scp(config, ip, remote_file, local_file)
      Presto.log.info(Presto::Core::Executor.exec_command(
        "scp -o StrictHostKeyChecking=no -i #{config.ssh_key} #{config.ssh_user}@#{ip}:#{remote_file} #{local_file}",
        :retry => 5, :echo => true
      ).stdout
      )
    end

    def self.ssh_sudo_cmd(config, ip, cmd)
      Presto.log.info(Presto::Core::Executor.exec_command(
        "ssh -o StrictHostKeyChecking=no -i #{config.ssh_key} #{config.ssh_user}@#{ip} 'sudo #{cmd}'",
        :retry => 5, :echo => true
      ).stdout
      )
    end

    def self.load_config_file(repo_path)
      # Expecting to receive the YAML config file via envvars
      # e.g. version=1.6.5000 configuration=smoke.yaml presto build/cf/provisioner.rb
      config_file = ENV['configuration'] || raise('Could not find the cloud formation configuration file.')
      cnf         = YAML::load_file("#{repo_path}/build/cf/config/#{config_file}")
      return cnf
    end

    def self.destroy_vsphere_vm(config, node)
      rescued_exceptions = []

      # # Destroy the vsphere object.
      begin
        Presto.log.info "Destroying the vSphere VM #{node[:name]}..."
        node node[:name] do
          action :destroy
        end
      rescue Exception => e
        rescued_exceptions << e
      end

      # Log error as warning messages
      rescued_exceptions.each { |e| Presto.log.error("#{e.class}: #{e}") } unless rescued_exceptions.empty?
    end

    def self.get_license_plate_url(config, environment_name)
      environment = Presto::DSL::Environment.new(config, environment_name)
      nodes       = config._cloud.retrieve_nodes(:environment => environment)
      url         = nodes[0].data['haproxy_hp']['dns_update']['dns_addr']
      return url
    end

    #
    # Converts a expanded w.x.y.z version into a condensed w.x.y version...
    #
    # REQUIRED expanded                  > expanded version (w.x.y.z)
    # RETURN the condensed version (w.x.y)
    #
    def self.condense_version(expanded)
      major = minor = build = patch = 0
      if expanded =~ /^(\d+)\.?(\d+)?\.?(\d+)?\.?(\d+)?$/
        major = $1.to_i
        minor = $2.to_i
        build = $3.to_i
        patch = $4.to_i
      else
        raise "Not a valid expanded version number: '#{expanded}'"
      end
      raise 'Invalid patch number: limited to 99' if patch > 99
      return "#{major}.#{minor}.#{100*build + patch}"
    end

    #
    # Converts a condensed w.x.y version into a expanded w.x.y.z version...
    #
    # REQUIRED condensed                 > condensed version (w.x.y)
    # RETURN the expanded version (w.x.y.z)
    #
    def self.expand_version(condensed)
      major = minor = build = 0
      if condensed =~ /^(\d+)\.?(\d+)?\.?(\d+)?$/
        major = $1.to_i
        minor = $2.to_i
        build = $3.to_i
      else
        raise "Not a valid condensed version number: '#{expanded}'"
      end
      exp_build = build / 100
      patch     = build % 100
      return "#{major}.#{minor}.#{exp_build}" if patch == 0
      return "#{major}.#{minor}.#{exp_build}.#{patch}"
    end
  end
end

