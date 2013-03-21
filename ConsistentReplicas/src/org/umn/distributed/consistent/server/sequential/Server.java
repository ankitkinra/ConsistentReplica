package org.umn.distributed.consistent.server.sequential;

import org.umn.distributed.consistent.server.AbstractServer;

public class Server extends AbstractServer {

	public Server(String iP, int port, STRATEGY strategy, String coordinatorIP,
			int coordinatorPort) {
		super(iP, port, strategy, coordinatorIP, coordinatorPort);
		// TODO Auto-generated constructor stub
	}

	/*
	 * (non-Javadoc)
	 * @see org.umn.distributed.consistent.common.AbstractServer#write()
	 * Redirect to primary server for the write.
	 */
	@Override
	public boolean write() {
		return false;
	}

	@Override
	public String post(String message, String parentId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String readItemList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String readItem(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] handleRequest(byte[] request) {
		// TODO Auto-generated method stub
		return null;
	}
}
