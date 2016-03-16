#!/usr/bin/env python

import os
import util
from scp import SCPClient


directory = 'deploy'
server = 'Server.jar'
list_nodes = '../nodes.list'
log_output = 'output.log'

command_kill = "pkill -f %s" % server
command_start = "nohup java -jar -Xmx64m %s &" % server

kill = raw_input("Kill servers? (y/n)") == 'y'
if not kill:
    deploy_server = raw_input("Deploy server? (y/n): ") == 'y'
    deploy_nodes_list = raw_input("Deploy nodes list? (y/n): ") == 'y'
else:
    deploy_server = False
    deploy_nodes_list = False
print ''

os.chdir(directory)
nodes = []
with open(list_nodes, 'r') as nodes_file:
    for line in nodes_file:
        if "#" not in line:
            nodes.append(line.split(':')[0])

with open(log_output, 'w') as log:
    for node in nodes:
        log.write("[" + node + "]\n")
        if kill:
            print "killing node: " + node
        else:
            print "starting node: " + node

        with util.connect(node) as connection:
            if connection is not None:
                if not kill:
                    with SCPClient(connection.get_transport()) as scp:
                        if deploy_server:
                            scp.put(server, server)

                        if deploy_nodes_list:
                            scp.put(list_nodes, list_nodes.split('/')[-1])

                stdin, stdout, stderr = connection.exec_command(command_kill)
                if stdout.channel.recv_exit_status() == 0:
                    log.write("current node killed\n")
                else:
                    log.write("no current node to kill\n")

                if not kill:
                    connection.exec_command(command_start)
                    log.write("new node started\n\n\n")

            else:
                log.write("failed to connect\n\n")
                print "failed to connect to node: " + node

os.startfile(log_output)
print "\ndone"
