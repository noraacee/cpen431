#!/usr/bin/env python

import os
import util
import sys
import xmlrpclib
from scp import SCPClient, SCPException

server = 'https://www.planet-lab.org/PLCAPI/'
host = '52.53.244.22'  # util.get_ip()
username = 'chan.aaron@alumni.ubc.ca'
password = 'cpen431'
slice_name = 'ubc_cpen431_8'
port = '12664'

load_threshold = 100

directory = 'monitor'
output = 'monitor.log'
nodes_list = 'nodes.list'
test = 'test.txt'

command_ping = "ping -i 0.2 -c 5 %s | grep 'rtt'" % host
command_uptime = "uptime"
command_remove = "rm %s"
command_java = "java -version"
command_java_get = "wget -qO jre.rpm http://javadl.sun.com/webapps/download/AutoDL?BundleId=101397"
command_java_install = "sudo rpm -ivh jre.rpm"


class Node:
    def __init__(self, node_name, node_ip, node_ping, node_load):
        self.name = node_name
        self.ip = node_ip
        self.ping = float(node_ping)
        self.load = float(node_load)

    def __cmp__(self, other):
        if hasattr(other, 'ping') and hasattr(other, 'load'):
            if self.ping == other.ping:
                return self.load - other.load
            else:
                return self.ping - other.ping


def main():
    os.chdir(directory)

    print "Modes:"
    print "[1] Analyze nodes"
    print "[2] Generate node list"

    mode = int(raw_input("\nSelect a mode: "))

    if mode == 1:
        auth = {'AuthMethod': 'password', 'Username': username, 'AuthString': password}
        api_server = xmlrpclib.ServerProxy(server, allow_none=True)

        if not api_server.AuthCheck(auth):
            with open(output, 'w') as log:
                log.write("authentication failed\n")
                sys.exit()
        print "\nauthentication successful"

        node_slices = api_server.GetSlices(auth, slice_name, ['node_ids'])
        print "slices acquired"

        node_ids = []
        for node_slice in node_slices:
            for node_id in node_slice['node_ids']:
                node_ids.append(node_id)

        node_filter = {'boot_state': 'boot', 'node_id': node_ids}
        node_return_fields = ['hostname']
        node_nodes = api_server.GetNodes(auth, node_filter, node_return_fields)
        print "nodes acquired"

        nodes = []
        test_file = open(test, 'w')
        test_file.close()
        for node in node_nodes:
            hostname = node['hostname']
            print "analyzing node: " + hostname

            connection = util.connect(hostname)
            if connection is not None:
                with SCPClient(connection.get_transport()) as scp:
                    try:
                        scp.put(test, test)
                    except SCPException:
                        print "skipping node %s because of failed scp" % hostname
                        continue
                connection.exec_command(command_remove % test)

                stdin, stdout, stderr = connection.exec_command(command_ping)
                stdout.channel.recv_exit_status()
                ping = stdout.readline().split('=')[1].split('/')[1]

                stdin, stdout, stderr = connection.exec_command(command_uptime)
                stdout.channel.recv_exit_status()
                uptime = stdout.readline().split(',')
                load = uptime[len(uptime) - 1].strip()
                connection.close()

                if float(load) > load_threshold != -1:
                    print "skipping node %s because it does not meet load threshold" % hostname
                    continue

                nodes.append(Node(hostname, util.get_ip(hostname), ping, load))
            else:
                print "unable to connect to node: " + hostname

        nodes.sort()

        with open(output, 'w') as log:
            for node in nodes:
                log.write(node.name + ":" + node.ip + ":" + str(node.ping) + ":" + str(node.load) + '\n')

        os.remove(test)
        os.startfile(output)
    elif mode == 2:
        number = int(raw_input("Number of nodes to use: "))
        print "\nGenerating nodes list"

        test_file = open(test, 'w')
        test_file.close()
        with open(output, 'r') as log:
            with open('../' + nodes_list, 'w') as nodes:
                for i, line in enumerate(log):
                    if i < number:
                        info = line.split(":")
                        hostname = info[0]
                        ip = info[1]
                        print "loading node %s:%s" % (hostname, ip)

                        with util.connect(hostname) as connection:
                            if connection is not None:
                                stdin, stdout, stderr = connection.exec_command(command_java)
                                if stdout.channel.recv_exit_status() != 0:
                                    stdin, stdout, stderr = connection.exec_command(command_remove % 're.rpm')
                                    stdout.channel.recv_exit_status()

                                    print "downloading java"
                                    stdin, stdout, stderr = connection.exec_command(command_java_get)
                                    stdout.channel.recv_exit_status()

                                    if stdout.channel.recv_exit_status() == 0:
                                        print "installing java"
                                        stdin, stdout, stderr = connection.exec_command(command_java_install)
                                        while not stdout.channel.exit_status_ready():
                                            if stdout.channel.exit_status_ready():
                                                break

                                        stdin, stdout, stderr = connection.exec_command(command_java)
                                        if stdout.channel.recv_exit_status() == 0:
                                            print "java installed"
                                        else:
                                            print "skipping node %s because java cannot be installed" % hostname
                                            number += 1
                                            continue
                                    else:
                                        print "skipping node %s because java cannot be downloaded" % hostname
                                        number += 1
                                        continue
                            else:
                                print "skipping node %s because of failed ssh" % hostname
                                number += 1
                                continue

                        nodes.write(ip + ":" + port + "\n")
                    else:
                        break

        os.remove(test)
        os.chdir('..')
        os.startfile(nodes_list)

    print "\ndone"
