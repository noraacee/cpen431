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


os.chdir(directory)

nodes = []
with open(list_nodes, 'r') as nodes_file:
    for line in nodes_file:
        if "#" not in line:
            nodes.append(line.split(':')[0])


with open(log_server, 'w') as log:
    log.write(datetime.now().strftime(datetime_format) + '\n\n')

    for node in nodes:
        print "retrieving node log: " + node
        with open(log_node % node, 'w') as node_log:
            with util.connect(node) as connection:
                if connection is not None:
                    with SCPClient(connection.get_transport()) as scp:
                        scp.get(log_server, 'tmp_' + log_server)

                    with open('tmp_' + log_server, 'r') as node_log_file:
                        for line in node_log_file:
                            node_log.write(line)
                            log.write(line)

                    log.write('\n\n')
                else:
                    node_log.write("failed to connect")
                    log.write("failed to connect to node: " + node + '\n\n')
                    print "failed to connect to node: " + node

os.remove('tmp_' + log_server)
os.startfile(log_server)

print "\ndone"
