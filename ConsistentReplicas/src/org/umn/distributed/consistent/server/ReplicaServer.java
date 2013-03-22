package org.umn.distributed.consistent.server;

import java.io.IOException;

import org.umn.distributed.consistent.common.BulletinBoard;
import org.umn.distributed.consistent.common.Machine;
import org.umn.distributed.consistent.common.Props;
import org.umn.distributed.consistent.common.Utils;

public abstract class ReplicaServer extends AbstractServer {

	protected static final String WRITE_COMMAND = "WRITE";
	protected static final String READ_COMMAND = "READ";
	protected static final String READITEM_COMMAND = "RDITEM";
	protected static final String INVALID_COMMAND = "INVCOM";
	
	private static final String HEARTBEAT_COMMAND = "PING";
	private static final String START_ELECTION_COMMAND = "STRTELEC";
	private static final String END_ELECTION_COMMAND = "ENDELEC";
	protected static final String COMMAND_SUCCESS = "SUCCESS";
	protected static final String COMMAND_FAILED = "FAILED";
	
	
	private boolean coordinator;
	private Machine coordinatorMachine;
	private TCPServer externalTcpServer;
	private int externalPort;
	//Need to access bb using syncronized methods
	protected BulletinBoard bb = new BulletinBoard();

	protected ReplicaServer(STRATEGY strategy, String coordinatorIP,
			int coordinatorPort) {
		super(strategy, Props.SERVER_INTERNAL_PORT, Props.INTERNAL_SERVER_THREADS);
		this.externalTcpServer = new TCPServer(this,
				Props.EXTERNAL_SERVER_THREADS);
		this.strategy = strategy;
	}

	@Override
	public void start() throws Exception {
		try {
			super.start();
			preRegister();
			register();
			postRegister();
		}
		catch (Exception e) {
			stop();
			throw e;
		}
	}

	protected void preRegister() {

	}

	protected void register() {
		// this.coordinatorMachine = new Machine(id, iP, coordinatorPort);
	}

	protected void postRegister() throws IOException{
		this.externalPort = this.externalTcpServer.startListening(this.externalPort);
	}

	protected void shutdown() {
		preUnRegister();
		unRegister();
		postUnRegister();
	}

	protected void preUnRegister() {

	}

	protected void unRegister() {

	}

	protected void postUnRegister() {

	}

	protected void initCoordinator() {
		// TODO
		/**
		 * this will start a listener on some port which listens to other
		 * servers request Also we need to heartbeat all the known servers
		 */
	}

	/**
	 * TCP Operations
	 * 
	 */

	/**
	 * Client Operations
	 */

	/*
	 * Post and read details. implementation will vary based on the protocol
	 */
	public abstract String post(String message);

	/*
	 * Read all the posts with ids
	 */
	public abstract String readItemList();

	/*
	 * Show details for one post
	 */
	public abstract String readItem(String id);

	/*
	 * Actually writes the content. Implementation depends on the type of server
	 * (Primary, coordinator, normal server)
	 */
	public abstract String write(String message);
	
	public byte[] handleRequest(byte[] request){
		String req = Utils.byteToString(request, Props.ENCODING);
		if(req.startsWith(HEARTBEAT_COMMAND)) {
			return Utils.stringToByte(COMMAND_SUCCESS, Props.ENCODING);
		}
		else if(req.startsWith(START_ELECTION_COMMAND)) {
			//TODO: handle election. SHould block all writes;
			return null;
		}
		else if(req.startsWith(END_ELECTION_COMMAND)) {
			//TODO: handle election ends. start new coordinator and start acceptign reqs after coordinator
			return null;
		}
		return handleSpecificRequest("");
	}
	
	public abstract byte[] handleSpecificRequest(String request);
}
