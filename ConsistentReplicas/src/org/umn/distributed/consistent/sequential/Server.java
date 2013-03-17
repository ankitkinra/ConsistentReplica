package org.umn.distributed.consistent.sequential;

import org.umn.distributed.consistent.common.AbstractServer;

public class Server extends AbstractServer {

	/*
	 * (non-Javadoc)
	 * @see org.umn.distributed.consistent.common.AbstractServer#write()
	 * Redirect to primary server for the write.
	 */
	@Override
	public boolean write() {
		return false;
	}
}
