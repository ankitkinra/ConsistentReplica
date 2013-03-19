package org.umn.distributed.consistent.server.quorum;

import org.umn.distributed.consistent.server.AbstractServer;

public class QuorumServer extends AbstractServer {
	private final int TOTAL_QUORUM_STRENGTH;
	private final int READ_QUORUM_STRENGTH;
	private final int WRITE_QUORUM_STRENGTH;

	public QuorumServer(String iP, int port, STRATEGY strategy,
			String coordinatorIP, int coordinatorPort, int totalQuorum,
			int readQuorum, int writeQuorum) {
		super(iP, port, strategy, coordinatorIP, coordinatorPort);
		this.TOTAL_QUORUM_STRENGTH = totalQuorum;
		this.READ_QUORUM_STRENGTH = readQuorum;
		this.WRITE_QUORUM_STRENGTH = writeQuorum;
		validateParameters();
	}

	private void validateParameters() {
		if (WRITE_QUORUM_STRENGTH < TOTAL_QUORUM_STRENGTH / 2) {
			throw new IllegalArgumentException(
					String.format(
							"WIRTE-Quorum %s needs to be more than TOTAL-Quorum/2 = %s",
							WRITE_QUORUM_STRENGTH, (TOTAL_QUORUM_STRENGTH / 2)));
		}

		if (WRITE_QUORUM_STRENGTH + READ_QUORUM_STRENGTH < TOTAL_QUORUM_STRENGTH) {
			throw new IllegalArgumentException(
					String.format(
							"WIRTE-Quorum %s + READ-Quorum = %s needs to be more than TOTAL-Quorum = %s",
							WRITE_QUORUM_STRENGTH, READ_QUORUM_STRENGTH,
							TOTAL_QUORUM_STRENGTH));
		}

	}

	/*
	 * (non-Javadoc) This server will contact the coordinator to get the latest
	 * id
	 * 
	 * @see org.umn.distributed.consistent.common.AbstractServer#write()
	 */
	@Override
	public boolean write() {
		// TODO Auto-generated method stub
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

}
