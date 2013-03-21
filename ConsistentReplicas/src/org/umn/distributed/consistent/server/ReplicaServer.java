package org.umn.distributed.consistent.server;

import java.io.IOException;

import org.umn.distributed.consistent.common.BulletinBoard;
import org.umn.distributed.consistent.common.Machine;
import org.umn.distributed.consistent.common.Props;

public abstract class ReplicaServer extends AbstractServer {
	public enum STRATEGY {
		SEQUENTIAL, QUORUM,
	}

	private boolean coordinator;
	private Machine coordinatorMachine;
	private STRATEGY strategy;
	private TCPServer externalTcpServer;
	private int externalPort;
	//Need to access bb using syncronized methods
	protected BulletinBoard bb = new BulletinBoard();

	protected ReplicaServer(STRATEGY strategy, String coordinatorIP,
			int coordinatorPort) {
		super(Props.SERVER_INTERNAL_PORT, Props.INTERNAL_SERVER_THREADS);
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
	public String write(String message) {
		return null;
	}
	
	public byte[] handleRequest(byte[] request){
		//TODO handle and if not handled
		boolean notHandled = true;
		if(notHandled){
			return handleSpecificRequest("");
		}
		return null;
	}
	
	public abstract byte[] handleSpecificRequest(String request);
}
