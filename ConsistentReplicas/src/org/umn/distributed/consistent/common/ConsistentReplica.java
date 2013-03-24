package org.umn.distributed.consistent.common;

import org.umn.distributed.consistent.server.ReplicaServer;
import org.umn.distributed.consistent.server.sequential.SequentialServer;

public class ConsistentReplica {

	public static void main(String[] args) {
		// TODO: add actual command line parameters
		Props.loadProperties(args[0]);
		ReplicaServer replicaServer = new SequentialServer(true, null, 0);
		try {
			replicaServer.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
