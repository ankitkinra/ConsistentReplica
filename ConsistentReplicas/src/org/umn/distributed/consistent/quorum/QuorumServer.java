package org.umn.distributed.consistent.quorum;

import org.umn.distributed.consistent.common.AbstractServer;

public class QuorumServer extends AbstractServer {

	public QuorumServer(String iP, int port, STRATEGY strategy,
			String coordinatorIP, int coordinatorPort) {
		super(iP, port, strategy, coordinatorIP, coordinatorPort);
	}

	@Override
	protected void preRegister() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void postRegister() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void preUnRegister() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void postUnRegister() {
		// TODO Auto-generated method stub
		
	}
	
	/*
	 * (non-Javadoc)
	 * This server will contact the coordinator to get the latest id
	 * @see org.umn.distributed.consistent.common.AbstractServer#write()
	 */
	@Override
	public boolean write() {
		// TODO Auto-generated method stub
		return false;
	}

}
