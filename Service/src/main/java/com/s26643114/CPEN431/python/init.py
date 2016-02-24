#!/usr/bin/env python

import argparse
import sys
import xmlrpclib

# Argument parser that accepts username, password and slice name
parser = argparse.ArgumentParser(description='Retrieves nodes to monitor')
parser.add_argument('-u', default='m3p8@ugrad.cs.ubc.ca', dest='username', help="Username for PlanetLab", metavar='username')
parser.add_argument('-p', default='password1', dest='password', help="Password for PlanetLab", metavar='password')
parser.add_argument('-s', default='ubc_cpen431_8', dest='slicename', help="Slice to retreive nodes from", metavar='slice name')
args = parser.parse_args()

# Create an empty dictionary (XML-RPC struct)
auth = {}

# Specify password authentication
auth['AuthMethod'] = 'password'

# Username and password
auth['Username'] = args.username
auth['AuthString'] = args.password

api_server = xmlrpclib.ServerProxy('https://www.planet-lab.org/PLCAPI/', allow_none=True)
	
# Check authentication
if not api_server.AuthCheck(auth):
    with open('error.log', 'a') as log:
        log.write("authentication failed\n")	
	sys.exit("")
print "authentication sucessful"

# Query Planet Lab API for host names
node_ids = api_server.GetSlices(auth, args.slicename, ['node_ids'])[0]['node_ids']
node_filter = { 'boot_state': 'boot' , 'node_id': node_ids}
node_return_fields = ['hostname', 'node_id']
nodes = api_server.GetNodes(auth, node_filter, node_return_fields)

print "Got nodes"
with open('nodes.txt', 'w') as n:
    for node in nodes:
        n.write(node['hostname'] + ":" + str(node['node_id']) + "\n")