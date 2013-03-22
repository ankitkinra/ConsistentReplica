package org.umn.distributed.consistent.server.coordinator;

import org.umn.distributed.consistent.common.Props;
import org.umn.distributed.consistent.common.Utils;
import org.umn.distributed.consistent.server.AbstractServer;

public abstract class Coordinator extends AbstractServer {

	private volatile int articleID = 1;
	protected Coordinator(STRATEGY strategy) {
		super(strategy, Props.COORDINATOR_PORT, Props.COORDINATOR_SERVER_THREADS);
	}

	@Override
	public byte[] handleRequest(byte[] request) {
		String req = Utils.byteToString(request, Props.ENCODING);
		if(req.startsWith(prefix))
		return handleSpecificRequest(req);
	}
	
	public abstract byte[] handleSpecificRequest(String request);
	
	
}
