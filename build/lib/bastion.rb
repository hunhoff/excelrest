# encoding: UTF-8
#

require 'aws-sdk'
require 'fileutils'
require 'rest_client'

module CD
  module Help
    # Collection of useful methods for interacting with CDO bastion hosts.
    class Bastion

      attr_accessor :rmq_host, :pxc_host

      def initialize(config)
        # Validating requirements
        fail('Missing nexus_int_host config setting.') if config.nexus_int_host.nil?
        fail('Missing nexus_int_port config setting.') if config.nexus_int_port.nil?
        fail('Missing pxc_host config setting.') if config.pxc_host.nil?
        fail('Missing rmq_host config setting.') if config.rmq_host.nil?
        fail('Missing bastion_host config setting.') if config.bastion_host.nil?
        fail('Missing bastion_ssh_user config setting.') if config.bastion_ssh_user.nil?
        fail('Missing bastion_ssh_key config setting.') if config.bastion_ssh_key.nil?

# Preparing connection information.
        @connection = {
          host:     config.bastion_host,
          ssh_user: config.bastion_ssh_user,
          ssh_key:  config.bastion_ssh_key
        }

# Preparing servers information.
        @pxc_host   = config.pxc_host
        @rmq_host   = config.rmq_host
        @nexus_url  = "#{config.nexus_int_host}:#{config.nexus_int_port}/nexus/service/local/artifact/maven/redirect"
      end

      def create_database(options)
        conn = "-i #{@connection[:ssh_key]} #{@connection[:ssh_user]}@#{@connection[:host]}"

        Presto.log.info "Creating database #{options[:database]} at #{@pxc_host}..."
        command = "./mysql_create_database #{@pxc_host} #{options[:database]}"
        Presto.log.info Presto::Core::Executor.exec_command("ssh -oStrictHostKeyChecking=no #{conn} #{command}", :echo => true).stdout

        Presto.log.info "Creating the user #{options[:user]} and granting access to #{options[:database]}..."
        command = "./mysql_create_user #{@pxc_host} #{options[:user]} #{options[:password]} #{options[:user_privileges]} #{options[:database]}"
        Presto.log.info Presto::Core::Executor.exec_command("ssh -oStrictHostKeyChecking=no #{conn} #{command}", :echo => true).stdout
      end

      def destroy_database(rescued_exceptions, options)
        conn = "-i #{@connection[:ssh_key]} #{@connection[:ssh_user]}@#{@connection[:host]}"
        if options[:user]
          begin
            Presto.log.info "Dropping the user #{options[:user]}..."
            command = "./mysql_drop_user #{@pxc_host} #{options[:user]}"
            Presto.log.info Presto::Core::Executor.exec_command("ssh -oStrictHostKeyChecking=no #{conn} #{command}", :echo => true).stdout
          rescue Exception => e
            rescued_exceptions << e
          end
        end
        if options[:database]
          begin
            Presto.log.info "Dropping the database #{options[:database]}..."
            command = "./mysql_drop_database #{@pxc_host} #{options[:database]}"
            Presto.log.info Presto::Core::Executor.exec_command("ssh -oStrictHostKeyChecking=no #{conn} #{command}", :echo => true).stdout
          rescue Exception => e
            rescued_exceptions << e
          end
        end
      end

      def create_vhost(options)
        conn = " -i #{@connection[:ssh_key]} #{@connection[:ssh_user]}@#{@connection[:host]}"

        Presto.log.info "Creating virtual host #{options[:vhost]} at #{@rmq_host}..."
        command = "./rabbit_create_vhost #{@rmq_host} #{options[:port]} #{options[:vhost]}"
        Presto.log.info Presto::Core::Executor.exec_command("ssh -oStrictHostKeyChecking=no #{conn} #{command}", :echo => true).stdout

        Presto.log.info "Creating the user #{options[:user]} and granting access to #{options[:vhost]}..."
        command = "./rabbit_create_user #{@rmq_host} #{options[:port]} #{options[:user]} #{options[:password]} administrator #{options[:vhost]} '.\\*' '.\\*' '.\\*'"
        Presto.log.info Presto::Core::Executor.exec_command("ssh -oStrictHostKeyChecking=no #{conn} #{command}", :echo => true).stdout
      end

      def destroy_vhost(rescued_exceptions, options)
        conn = "#{@connection[:ssh_user]}@#{@connection[:host]} -i #{@connection[:ssh_key]}"
        if options[:user]
          begin
            Presto.log.info "Removing the user #{options[:user]}..."
            command = "./rabbit_delete_user #{@rmq_host} #{options[:port]} #{options[:user]}"
            Presto.log.info Presto::Core::Executor.exec_command("ssh -oStrictHostKeyChecking=no #{conn} #{command}", :echo => true).stdout
          rescue Exception => e
            rescued_exceptions << e
          end
        end
        if options[:vhost]
          begin
            Presto.log.info "Destroying the Virtual Host #{options[:vhost]}..."
            command = "./rabbit_delete_vhost #{@rmq_host} #{options[:port]} #{options[:vhost]}"
            Presto.log.info Presto::Core::Executor.exec_command("ssh -oStrictHostKeyChecking=no #{conn} #{command}", :echo => true).stdout
          rescue Exception => e
            rescued_exceptions << e
          end
        end
      end

      def migrate_database(options)
        database   = options[:database]
        basename   = options[:basename]
        repository = options[:repository]
        type       = options[:type]
        classifier = options[:classifier]
        pom        = options[:pom]
		service = options[:service]

        version     = pom[:version]
        group_id    = pom[:group_id]
        artifact_id = pom[:artifact_id]
        source_url  = "#{@nexus_url}?r=#{repository}\\&g=#{group_id}\\&a=#{artifact_id}\\&v=#{version}\\&p=#{type}\\&c=#{classifier}"

        download_info = {
          target_dir:  "/tmp/flyway_files/#{version}",
          target_name: "#{basename}.#{type}",
          source_url:  source_url
        }
		if options[:migration_type] == 'flyway'
          conn    = "#{@connection[:ssh_user]}@#{@connection[:host]} -i #{@connection[:ssh_key]}"
          command = "./flyway_run_migrate #{download_info[:target_name]} 3.0 #{@pxc_host} #{database} --useUnicode --characterEncoding=UTF-8"

          Presto.log.info "Transfering migration files of DB #{database}..."
          transfer_migration_scripts(download_info, options[:flyway_properties])

          Presto.log.info "Running flyway to migrate DB #{database}..."
          Presto.log.info Presto::Core::Executor.exec_command("ssh -oStrictHostKeyChecking=no #{conn} #{command}", :echo => true).stdout

		else
          conn    = "#{@connection[:ssh_user]}@#{@connection[:host]} -i #{@connection[:ssh_key]}"
          command = "./java_migrate.rb #{download_info[:target_name]} #{@pxc_host} #{database} #{service}"

          Presto.log.info "Transfering migration jar of DB #{database}..."
          transfer_migration_jar(download_info)

          Presto.log.info "Running java migrate to migrate DB #{database}..."
          Presto.log.info Presto::Core::Executor.exec_command("ssh -oStrictHostKeyChecking=no #{conn} #{command}", :echo => true).stdout
                              
        end

      end

	  def transfer_migration_jar(download_info)
        destination = "#{@connection[:ssh_user]}@#{@connection[:host]}:~"
        FileUtils.mkdir_p download_info[:target_dir]

        Dir.chdir download_info[:target_dir] do
          `wget --progress=dot:mega -O #{download_info[:target_name]} #{download_info[:source_url]}`          
          Presto.log.info Presto::Core::Executor.exec_command("scp -oStrictHostKeyChecking=no -i #{@connection[:ssh_key]} #{download_info[:target_name]} #{destination}", :echo => true).stdout
        end
      end 
	  
      def transfer_migration_scripts(download_info, flyway_properties)
        destination = "#{@connection[:ssh_user]}@#{@connection[:host]}:~"
        FileUtils.mkdir_p download_info[:target_dir]

        Dir.chdir download_info[:target_dir] do
          `wget --progress=dot:mega -O #{download_info[:target_name]} #{download_info[:source_url]}`

          unless flyway_properties.nil?
            tar_name = download_info[:target_name]
            sql_dir  = 'sql'
            `tar -xvf #{tar_name}`
            File.delete("#{sql_dir}/flyway.properties")
            File.rename("#{sql_dir}/#{flyway_properties}", "#{sql_dir}/flyway.properties")
            `tar cvf #{tar_name} #{sql_dir}/`
            FileUtils.rm_rf(sql_dir)
          end

          Presto.log.info Presto::Core::Executor.exec_command("scp -oStrictHostKeyChecking=no -i #{@connection[:ssh_key]} #{download_info[:target_name]} #{destination}", :echo => true).stdout
        end
      end
    end
  end
end
