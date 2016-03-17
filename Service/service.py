#!/usr/bin/env python

import deploy
import log
import monitor
import test

print "Modes:"
print "[1] Deploy"
print "[2] Test"
print "[3] Log"
print "[4] Monitor"

mode = int(raw_input("\nSelect a mode: "))
print ''

if mode == 1:
    deploy.main()
elif mode == 2:
    test.main()
elif mode == 3:
    log.main()
elif mode == 4:
    monitor.main()
