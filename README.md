ConsistentReplica
=================
This project revolves around demonstration of Distributed Consistency protocols in a simulated environment.

We have the following protocols implemented:
a) Sequential Consistency
b) Quorum Consistency
c) Read your own writes consistency

Each Replica server has a bulletin board which has a series of posts and reply of the posts. 

The cluster group has one Coordinator which at the moment needs to stay in order to keep system running.

Future scope:
1) Coordinator election: The basic election can be incorporated in the system with less effort.
2) Make the Bulletin Board persistent on the disk so that replicas can recover quickly.
