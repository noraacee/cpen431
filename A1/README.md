Assignment 1

Name: Aaron Chan
Sending ID: 26643114
Secret Code Length: 16
Secret Code: 68D9C29AF61EE097913EF21AD8761A05

Instructions:
On windows, you can run the jar through CMD with the command (assuming you are in the directory): 
	java -Xmx1024m -jar A1.jar (arguments)

The command accepts up to four arguments and are outlined as follow:
	1 argument  - student number
	2 arguments - student number, timeout duration (in ms)
	3 arguments - IP, port, student number
	4 arguments - IP, port, student number, timeout duration

For example if you wish to send the ID 26643114 to IP 162.219.6.226:5627 with a timeout of 1 second:
	java -Xmx1024m -jar A1.jar 162.219.6.226 5627 26643114 1000

For arguments that are not specified, the program defaults to IP 162.219.6.226, port 5627 and timeout
duration 100ms. Note that you cannot have 0 arguments.