Group ID 8A

Aaron Chan (26643114) Kerby Chang (36916112) Shaun Chang (33820119) Stephen Tai (35015114)

Shutdown - Request.java:line 123
Server list - Service/nodes.list
Tester - Amazon EC2-Micro Ubuntu

The server receives requests and parses them in separate threads taken from a thread pool. 
The client itself that holds the requests is also taken from a client pool. When a request
is parsed, consistent hashing is used to determine which node to store the data, based on the
key given in the request. If routing is required, the ip and port are appened to the request
before sending it to an appropiate node. The receiving node will configure the packet to reply
to the original sender before parsing the packet normally. A heartbeat gossiping protocol is 
used to check for node liveliness. Each node keeps a list of a heartbeat count for all the 
other nodes. At a set interval, each node will increase their own heartbeat and send their own 
version of the list to one other node. When a list of heartbeats are received, the node will 
update their own list with the highest heartbeat count. If a node's heartbeat has not been
incremented for a certain period of time, the node will be considered dead and removed from
the node list. 