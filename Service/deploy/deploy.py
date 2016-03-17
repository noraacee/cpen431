#!/usr/bin/env python

import os
import util
from scp import SCPClient


directory = 'deploy'
server = 'Server.jar'
list_nodes = '../nodes.list'
log_output = 'output.log'

command_kill = "pkill -f %s" % server
command_check = "ps aux | grep %s | grep -v grep" % server
command_start = "nohup java -jar -Xmx64m %s &" % server


def main():
    os.chdir(directory)

    print "Modes:"
    print "[1] Kill all nodes"
    print "[2] Restart all nodess"
    print "[3] Deploy all nodess"
    print "[4] Check all nodess"
    print "[5] Kill specific node(s)"
    print "[6] Restart specific node(s)"
    print "[7] Deploy sepcific node(s)"
    print "[8] Check specific node(s)"

    mode = int(raw_input("\nSelect a mode: "))
    print ''

    nodes_list = []
    with open(list_nodes, 'r') as nodes_file:
        for line in nodes_file:
            if "#" not in line:
                nodes_list.append(line.split(':')[0])

    nodes = []
    if mode >= 5:
        mode -= 4
        print "Nodes:"
        for i, node in enumerate(nodes_list):
            print "[%d] %s" % (i + 1, node)

        servers = raw_input("\nSelect node(s) separated by a comma: ")
        servers = servers.strip().split(",")
        print ''

        for index in servers:
            nodes.append(nodes_list[int(index) - 1])
    else:
        nodes = nodes_list

    with open(log_output, 'w') as log:
        for node in nodes:
            log.write("[" + node + "]\n")
            if mode == 1:
                print "killing node: " + node
            elif mode == 2:
                print "restarting node: " + node
            elif mode == 3:
                print "deploying node: " + node
            elif mode == 4:
                print "checking node: " + node

            with util.connect(node) as connection:
                if connection is not None:
                    if mode == 4:
                        stdin, stdout, stderr = connection.exec_command(command_check)
                        if stdout.channel.recv_exit_status() == 1:
                            log.write("offline\n")
                        else:
                            log.write("online\n")
                        continue

                    if mode == 3:
                        with SCPClient(connection.get_transport()) as scp:
                            scp.put(server, server)
                            log.write("server deployed\n")

                            scp.put(list_nodes, list_nodes.split('/')[-1])
                            log.write("nodes list deployed\n")

                    while True:
                        stdin, stdout, stderr = connection.exec_command(command_kill)
                        while not stdout.channel.exit_status_ready():
                            if stdout.channel.exit_status_ready():
                                break

                        stdin, stdout, stderr = connection.exec_command(command_check)
                        if stdout.channel.recv_exit_status() == 1:
                            log.write("current node killed\n")
                            break

                    if mode != 1:
                        connection.exec_command(command_start)
                        log.write("new node started\n")

                else:
                    log.write("failed to connect\n")
                    print "failed to connect to node: " + node

                log.write('\n\n')

    os.startfile(log_output)
    print "\ndone"
