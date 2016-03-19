#!/usr/bin/env python

import os
import util


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
    print "[0] Deploy nodes list"
    print "[1] Deploy server"
    print "[2] Restart"
    print "[3] Kill"
    print "[4] Check"

    mode = int(raw_input("\nSelect a mode: "))
    print ''

    nodes_list = []
    with open(list_nodes, 'r') as nodes_file:
        for line in nodes_file:
            if "#" not in line:
                nodes_list.append(line.split(':')[0])

    nodes = []
    print "Nodes:"
    print "[0] All"
    for i, node in enumerate(nodes_list):
        print "[%d] %s" % (i + 1, node)

    servers = raw_input("\nSelect node(s) separated by a comma: ")
    servers = servers.strip().split(",")
    print ''

    if '0' in servers:
        nodes = nodes_list
    else:
        for index in servers:
            nodes.append(nodes_list[int(index) - 1])

    with open(log_output, 'w') as log:
        for node in nodes:
            log.write("[" + node + "]\n")
            if mode == 0:
                print "deploy nodes list: " + node
            if mode == 1:
                print "deploying node: " + node
            elif mode == 2:
                print "restarting node: " + node
            elif mode == 3:
                print "killing node: " + node
            elif mode == 4:
                print "checking node: " + node

            connection = util.connect(node)

            if connection is not None:
                if mode == 4:
                    stdin, stdout, stderr = connection.exec_command(command_check)
                    if stdout.channel.recv_exit_status() == 1:
                        log.write("offline\n")
                    else:
                        log.write("online\n")
                    continue

                if mode <= 1:
                    with util.get_scp(connection) as scp:
                        if mode == 1:
                            scp.put(server, server)
                            log.write("server deployed\n")

                        scp.put(list_nodes, list_nodes.split('/')[-1])
                        log.write("nodes list deployed\n")

                while True:
                    stdin, stdout, stderr = connection.exec_command(command_kill)
                    stdout.channel.recv_exit_status()

                    stdin, stdout, stderr = connection.exec_command(command_check)
                    if stdout.channel.recv_exit_status() == 1:
                        log.write("current node killed\n")
                        break

                if mode != 3:
                    connection.exec_command(command_start)

                    while True:
                        stdin, stdout, stderr = connection.exec_command(command_check)
                        if stdout.channel.recv_exit_status() == 0:
                            log.write("new node started\n")
                            break

                connection.close()
            else:
                log.write("failed to connect\n")
                print "failed to connect to node: " + node

            log.write('\n\n')

    # os.startfile(log_output)
    print "\ndone"
