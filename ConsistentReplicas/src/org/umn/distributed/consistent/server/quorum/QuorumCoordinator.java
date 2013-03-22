package org.umn.distributed.consistent.server.quorum;

import org.umn.distributed.consistent.server.coordinator.Coordinator;

public class QuorumCoordinator extends Coordinator {

	public QuorumCoordinator() {
		super(STRATEGY.QUORUM);
	}

	@Override
	public byte[] handleRequest(byte[] request) {
		return null;
	}

	@Override
	public byte[] handleSpecificRequest(String str) {
		// TODO Auto-generated method stub
		return null;
	}
}
