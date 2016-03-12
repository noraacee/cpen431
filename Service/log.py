#!/usr/bin/env python

import os
import paramiko
from datetime import datetime
from scp import SCPClient

datetime_format = '%Y-%m-%d %H:%M:%S'

username = 'ubc_cpen431_8'
password = 'CPEN431'
key = '../Key/id_rsa'

directory = 'log/'
log = 'server.log'
nodes_list = 'nodes.list'


def connect_ssh_client(hostname):
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    try:
        ssh.connect(hostname, username=username, password=password, key_filename=key)
        return ssh
    except (paramiko.BadHostKeyException, paramiko.AuthenticationException, paramiko.SSHException):
        return None


nodes = []
nodes_file = open(nodes_list, 'r')
for line in nodes_file:
    if "#" not in line:
        nodes.append(line.split(":")[0])
nodes_file.close()

node_log = open(directory + log, 'w')
node_log.write(datetime.now().strftime(datetime_format) + '\n\n')

for node in nodes:
    print "retrieving node log: " + node
    connection = connect_ssh_client(node)
    if connection is not None:
        with SCPClient(connection.get_transport()) as scp:
            scp.get(log, directory + node + '_' + log)
            scp.close()
        connection.close()

        node_log_file = open(directory + node + '_' + log, 'r')
        for line in node_log_file:
            node_log.write(line)
        node_log_file.close()

        os.remove(directory + node + "_" + log)
        node_log.write('\n')
node_log.close()

os.chdir(directory)
os.startfile(log)

print "done"
