#
# Panda system test script
# Use it with presto: presto build/create_vm.rb
# More info at https://rndwiki2.atlanta.hp.com/confluence/display/CDO/Presto
#
# Copyright 2014, Hewlett-Packard
#
# All rights reserved - Do Not Redistribute
#

require File.expand_path(File.join(*%w[ .. lib helper.rb ]), File.dirname(__FILE__))

config = configuration do
  provider 'vSphere'
end

prefix = 'panda-system-test'
number = Time.now.strftime('%Y%m%d%H%M%S')

if ENV['BUILD_NUMBER'] != nil
  # Running from Jenkins
  number = ENV['BUILD_NUMBER']
end

config.node_name = "#{prefix}-#{number}"

# We're going to use the dev-vm environment because it's continually updated with the latest version of everything
config.environment_name = 'dev-vm'

begin
  node_server = CD::Helper.get_server(config, config.node_name)
  if node_server.nil?
    node config.node_name do
      environment config.environment_name
      secret_file config.chef_databag_secret_key
      bootstrap :chef
      run_list ['recipe[1c-keys::default]']

      action :provision
    end
    node_server = CD::Helper.get_server(config, config.node_name)
  end

  CD::Helper.node_chef_client_run(
    config,
    config.node_name,
    config.environment_name,
    node_server.public_ip,
    [
      'recipe[1c-keys::default]'
    ]
  )

  CD::Helper.node_chef_client_run(
    config,
    config.node_name,
    config.environment_name,
    node_server.public_ip,
    %w(recipe[genesis_cdo::local_db] recipe[genesis_cdo::default] recipe[minitest-handler::default])
  )

  config.ws_ips   = [node_server.public_ip]
  config.chef_ips = [node_server.public_ip]

rescue Exception => e
  # Log error message and re-raise the exception
  Presto.log.fatal "#{e.class}: #{e}"
  raise e
ensure
  # Copy web server specific log files...
  # CD::Helper.copy_ws_logs(config)
end
