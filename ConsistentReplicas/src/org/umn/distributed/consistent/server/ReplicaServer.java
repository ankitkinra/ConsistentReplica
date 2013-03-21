package org.umn.distributed.consistent.server;

import java.util.TreeMap;

import org.umn.distributed.consistent.common.Machine;
import org.umn.distributed.consistent.common.Props;

public abstract class ReplicaServer extends AbstractServer {
	public enum STRATEGY {
		SEQUENTIAL, QUORUM,
	}

	private boolean coordinator;
	private String coordinatorIP;
	private int coordinatorPort;
	private STRATEGY strategy;
	
	// TODO think if we can change this to list sorted in increasing order
	private TreeMap<Integer, Machine> knownClients = new TreeMap<Integer, Machine>();
	
	private ReplicaServer(STRATEGY strategy,
			String coordinatorIP, int coordinatorPort) {
		super(Props.SERVER_INTERNAL_PORT, Props.REPLICA_SERVER_THREADS);
		this.strategy = strategy;
		this.coordinatorIP = coordinatorIP;
		this.coordinatorPort = coordinatorPort;
	}
	
	@Override
	public void start() throws Exception{
		super.start();
		preRegister();
		register();
		postRegister();
	}

	protected void preRegister(){
		
	}

	protected void register() {

	}

	protected void postRegister(){
		
	}

	protected void shutdown() {
		preUnRegister();
		unRegister();
		postUnRegister();
	}

	protected void preUnRegister(){
		
	}

	protected void unRegister() {

	}

	protected void postUnRegister(){
		
	}

	protected void initCoordinator() {
		//TODO
		/**
		 * this will start a listener on some port which listens to 
		 * other servers request
		 * Also we need to heartbeat all the known servers
		 */
	}
	
	
	/**TCP Operations
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
	public boolean write() {
		return true;
	}
}
