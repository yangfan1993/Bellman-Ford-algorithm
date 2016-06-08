1. Project documentation
The project have three layers, so it would have three files. The bfclient.java is the upper one, the routeTable.java is the middle one, and the distanceVec.java is the basic one. 
The distancevec store what the bfclient would send to its neighbors, it has two hashtables, referring the link cost and next-hop. It is also the message that bfclients communicate with each other. When we use SHOWRT, the information shown is also associated with the distancevec. We also have functions to change it to bytes and recover it from bytes. It is the core of all of our works.
The routeTable store most of the information to implement the routeTable, except the timeouts(These are implemented in the bfclient). It has several distancevecs, the one of the bfclient itself and the distancevecs from its neighbors. We use the hittable neighbor to store these distances received. Another three hashtable are nearby, offline and dead. To mark the state of the neighbors. nearby means they are still in the nearby, offline means they are linkdowned, but could be linked again. And the dead means they are timeout or close, we no longer need them. The dead hashtable could be removed, I use it just wanna  solve all the count to infinity problem, but failed at last, only able to handle the ones related to Poison reverse.
The bfclient is the main one, to handle the instructions typed in by the users, periodically generating and broadcasting packets, and receive the packets from its neighbors. So there are two main threads existing, one to receive distanceVec from the neighbors, if the routeTable changed after this receive, it would also broadcast the neighbors,  and one to send periodically updates. Also the timeout of the neighbors are implemented here. Because it is more relevant to the listening thread.


2. Program features and using scenario
The program is a distributed bellman-ford program, it is used to realize the function of testing  
There are four existing functions here. The CLOSE, LINKUP, LINKDOWN, and SHOWRT.
To invoke the bfclient, first make, then java bfclient localport timeout [ipaddress1 port1 weight1 ...]
An example is java bfclient 4115 3 128.59.196.2 4116 5.0 128.59.196.2 4118 30.0. 
SHOWRT only need you to invoke using SHOWRT. The output format is the same as the one in the assignment requirement.
LINKUP have one parameter, the address:port. 
An example is LINKUP 209.2.224.152:4116. Make sure that this link used to exist, if not, it would not be workable.
LINKDOWN have one parameter, the address:port. 
An example is LINKDOWN 209.2.224.152:4114. Make sure there exist a link between the two bfclients, or it would be not workable also.
CLOSE is simple, just type in the CLOSE. Remind here, CLOSE is not the same as CTRL+C, if you use CTRL+C to exit, the other bfclients would know these after 3*TIMEOUTS, however, if you use CLOSE to exit, they would be immediately informed to change the routetable.


3. The protocol utilized to exchange information
What the bfclient send to their neighbors are just the distancevec. But the distancevec are actually hashtables, so we need to translate them to bytes, and recover them in the receiving bfclient. Also we need a header for these all, and I use a tailer also. The tailer is implemented to help complete the second additional feature.
It have a 48 byte length head. The head is covers three things, the receiver’s address use the first 20 byte, the sender’s address use the 28-48 byte. In the middle part, it could be divide into three parts. 22-24 is the length of the message, it means how many pairs the distancevec have. Because at most we have five bfclients in our program, so typically length is less than five. The 24-28 is the checksum, as the TCP also have. The 20-21 marks what kind of message it is. If it is 0, then it is normal update message, if it is 1, then it is linkdown message, if it is 2, then it is bfclient close message. As the latter two kind of message, the length of the message should be set to zero. And it do not need to have the tail 8 bytes also.
And each of the pair in the distancevec also have a length of 48 byte. Because the IP address and port would need 28 bytes at most, like 255.255.255.255:4118, it is translated to 20 bytes. The link cost is a double number, so it is 8 bytes. The next-hop is definitely also an address, so translated also 20 bytes. The sequence is node name, link cost, next-hop.
At last, we have a 8 byte tail storing the direct link weight between the sender and the receiver, with it, we do not need to type in all the parameters of neighbors at first.


4. The additional features
Although not directly implement the Poison reverse in my program, but I utilize the same method to handle some of the count to infinity problem. When the link between two nodes are link down, the bfclient would find the next smallest cost link in his routeTable to update its distancevec. But the link chosen should not also use the current bfclient as its next-hop. The principle is just the same as the Poison reverse. So it could handle the  cases that Poison reverse could work. Take the example of assignment requirement, without Poisoned reverse, if the link between 4115 and 4116 are linkdowned, the count to infinity would happen without Poisoned reverse, but now it would have correct output. 

Also, my program do not need you to type all the parameters of neighbors when invoked. Take the example of the assignment requirement, invoke the program like this is also ok. The link only need to be appear in one side of the bfclient, don’t need in both sides. Thus make it more like the real implementation, where full topology, even between the neighbors is not needed.
java bfclient 4115 3 128.59.196.2 4116 5.0 128.59.196.2 4118 30.0
java bfclient 4118 3 128.59.196.2 4116 5.0
java bfclient 4116 3 128.59.196.2 4118 5.0 128.59.196.2 4117 10.0

5. Something else
As illustrated above, if the specified count to infinity problem could not be handled by the typical Poison reverse, then it could not be solved by my program also, it is beyond my ability.
Here the port number + IP address could not have more than 20 characters. The 255.255.255.255:9999 is the largest accepted one. The 255.255.255.25:65535 is also OK but not 255.255.255.255:65535.
Also, when the terminal displayed the “Broadcasting”, “Receiving packets”, it means that the bfclients are exchanging their distancevec, so at this time, please pause the LINKDOWN and LINKUP tests, or CLOSE commands, till they have completed this procedure. If not, maybe some unintended things might happened.
