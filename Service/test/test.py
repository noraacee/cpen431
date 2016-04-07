#!/usr/bin/env python

import os
from subprocess import call


directory = 'test'
tests_list = 'tests.list'
nodes_list = '../nodes.list'
output = 'log.txt'
secret = '9833401'

command_test = "java -jar %s %s "


def main():
    os.chdir(directory)

    tests = []
    with open(tests_list, 'r') as tests_file:
        for line in tests_file:
            arguments = line.split(":")
            tests.append([arguments[1], arguments[2], arguments[3].strip()])

    print "Available tests:"
    for i, test in enumerate(tests):
        print "[%d] %s" % (i + 1, test[0])

    test_number = int(raw_input("\nPick a test: ")) - 1

    if tests[test_number][2] == 'y':
        submit = raw_input("\nSubmit results? (y/n): ")
    else:
        submit = 'n'

    with open(output, 'w') as log:
        print "\nstarting test: " + tests[test_number][0]

        global command_test
        if submit == 'y':
            command_test += secret

        if tests[test_number][1] == 'y':
            call(command_test % (tests[test_number][0], nodes_list), stdout=log, stderr=log)
        else:
            call(command_test % (tests[test_number][0], nodes_list))

    # os.startfile(output)

    print "\ndone"
