#!/usr/bin/env python

import os
import util
from datetime import datetime
from scp import SCPClient

datetime_format = '%Y-%m-%d %H:%M:%S'

directory = 'log'
log_server = 'server.log'
log_node = 'node_%s.log'
list_nodes = '../nodes.list'


def main():
    os.chdir(directory)

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

    with open(log_server, 'w') as log:
        log.write(datetime.now().strftime(datetime_format) + '\n\n')

        for node in nodes:
            print "retrieving node log: " + node
            with open(log_node % node, 'w') as node_log:
                connection = util.connect(node)

                if connection is not None:
                    with util.get_scp(connection) as scp:
                        scp.get(log_server, 'tmp_' + log_server)

                    with open('tmp_' + log_server, 'r') as node_log_file:
                        for line in node_log_file:
                            node_log.write(line)
                            log.write(line)

                    log.write('\n\n')
                    connection.close()
                else:
                    node_log.write("failed to connect")
                    log.write("failed to connect to node: " + node + '\n\n')
                    print "failed to connect to node: " + node

    os.remove('tmp_' + log_server)
    os.startfile(log_server)

    print "\ndone"
