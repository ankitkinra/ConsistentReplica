package org.umn.distributed.consistent.common;

import org.umn.distributed.consistent.server.ReplicaServer;
import org.umn.distributed.consistent.server.sequential.SequentialServer;

public class ConsistentReplica {

	public static void main(String[] args) {
		// TODO: add actual command line parameters
		Props.loadProperties();
		ReplicaServer replicaServer = new SequentialServer("localhost",
				Props.COORDINATOR_PORT);
	}

}
