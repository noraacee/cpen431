#!/usr/bin/env python

import sys

directory = 'test/'
tests_list = 'tests.list'
nodes_list = 'nodes.list'


test_number = -1
if len(sys.argv) > 1:
    test_number = int(sys.argv[1])

if test_number == -1:
    tests = []
    tests_file = open(directory + tests_list, 'r')
    for line in tests_file:
        arguments = line.split(":")
        tests.append(arguments[1].rstrip('\n'))

    print "Available tests:"
    for i, test in enumerate(tests):
        print "[{0}] {1}".format(i, test)

    test_number = int(raw_input("\nPick a test: "))


