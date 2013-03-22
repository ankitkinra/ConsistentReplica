package org.umn.distributed.consistent.common;

import org.umn.distributed.consistent.server.ReplicaServer;
import org.umn.distributed.consistent.server.sequential.SequentialServer;

public class ConsistentReplica {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//TODO: add actual command line parameters
		Props.loadProperties();
		Coordinator coordinator = new SequentialCoordinator()
		ReplicaServer replicaServer = new SequentialServer("localhost", Props.COORDINATOR_PORT);

	}

}
