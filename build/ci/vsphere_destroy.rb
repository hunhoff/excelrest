# encoding: UTF-8
#
# Destroy script:
# Use it with presto: presto build/utils/cf_destroy.rb
# More info at: https://rndwiki2.atlanta.hp.com/confluence/display/CDO/Presto
#
# Copyright 2015, Hewlett-Packard
#
# All rights reserved - Do Not Redistribute
#
require File.expand_path(File.join(*%w[ .. lib helper.rb ]), File.dirname(__FILE__))

begin
  config = configuration do
    provider 'vSphere'
  end

  prefix = 'panda-system-test'

  # get a list of nodes to destroy
  nodes  = CD::Helper.get_nodes_by_name(config, prefix)

  # Destroy the environment which also destroys the nodes and servers
  nodes.each do |node|
    Presto.log.info "Deleting VM #{node[:name]}"

    node node[:name] do
      action :destroy
    end
  end


rescue Exception => e
  # Log error message but don't re-raise the exception
  Presto.log.error "#{e.class}: #{e}"
end
