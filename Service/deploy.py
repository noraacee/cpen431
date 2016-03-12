#!/usr/bin/env python

import paramiko
from scp import SCPClient


username = 'ubc_cpen431_8'
password = 'CPEN431'
key = '../Key/id_rsa'

directory = 'deploy/'
server = 'Server.jar'
nodes_list = 'nodes.list'

command_start = "nohup java -jar -Xmx64m {0} &".format(server)
command_kill = "pgrep -f {0} | awk '{print \"kill -9 \" $1}' | sh".format(server)


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

for node in nodes:
    print "starting node: " + node
    connection = connect_ssh_client(node)
    if connection is not None:
        with SCPClient(connection.get_transport()) as scp:
            scp.put(directory + server, server)
            scp.put(nodes_list, nodes_list)
            scp.close()
        connection.exec_command(command_kill)
        connection.exec_command(command_start)
        connection.close()

print "done"
