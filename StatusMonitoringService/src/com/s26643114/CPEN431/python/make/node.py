#!/usr/bin/env python

import argparse
import json
import os
import paramiko
import sys
from scp import SCPClient
from subprocess import call

# Argument parser that accepts username, password and slice name
parser = argparse.ArgumentParser(description='Retrieves nodes to monitor')
parser.add_argument('hostname', help="Name of host to ssh into")
parser.add_argument('id', help="Id of node")
parser.add_argument('-u', default='ubc_cpen431_8', dest='username', help="Username for SSH", metavar='username')
parser.add_argument('-k', default='id_rsa', dest='key', help="File name of private key", metavar='key')
parser.add_argument('-p', default='CPEN431', dest='password', help="Password for private key", metavar='password')
args = parser.parse_args()

# Create an SSH connection
def connectSSHClient():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    try:
        ssh.connect(args.hostname, username=args.username, password=args.password, key_filename=args.key)
        return ssh
    except (paramiko.BadHostKeyException, paramiko.AuthenticationException, paramiko.SSHException):
        return None

# Class to store node information for json
class Node:
	def __init__(self, name, id, online, login, disk, uptime):
		self.name = name
		self.id = id
		self.login = login
		self.disk = disk
		self.online = online
		self.uptime = uptime


node = ""

# Reads disk space
disk = ""
try :
    call(['du', '-s'], stdout=disk)
except (Exception):
    disk = "Unable to read"

# Checks uptime and current load
uptime = ""
try:
    call(['uptime'], stdout=uptime)
except (Exception):
    uptime = "Unable to read"
		
nodeInfo = Node(args.hostname, args.id, "Yes", "Yes", disk, uptime)
node = nodeInfo.__dict__

# Writes nodeList to json
open('data.json','w').close();
with open('data.json', 'a') as data:
	json.dump(node, data, indent=4, sort_keys=True)
	
#ssh = connectSSHClient()
#if ssh is not None:
    #with SCPClient(ssh.get_transport()) as scp:
        #scp.put(sys.argv[0], sys.argv[0])
    #scp.close()
    #ssh.close()
	
# Deletes itself
#os.remove(sys.argv[0])