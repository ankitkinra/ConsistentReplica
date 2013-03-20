package org.umn.distributed.consistent.server;

import java.util.TreeMap;

import org.umn.distributed.consistent.common.Machine;

public abstract class AbstractServer implements TcpServerDelegate{

	public enum STRATEGY {
		SEQUENTIAL, QUORUM,
	}

	private Machine myInfo;
	private boolean coordinator;
	private String IP;
	private int port;
	private STRATEGY strategy;
	private String coordinatorIP;
	private int coordinatorPort;
	
	// TODO think if we can change this to list sorted in increasing order
	private TreeMap<Integer, Machine> knownClients = new TreeMap<Integer, Machine>();
	
	public AbstractServer(String iP, int port, STRATEGY strategy,
			String coordinatorIP, int coordinatorPort) {
		super();
		this.IP = iP;
		this.port = port;
		this.strategy = strategy;
		this.coordinatorIP = coordinatorIP;
		this.coordinatorPort = coordinatorPort;
	}

	protected void start() {
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
	public abstract String post(String message, String parentId);

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
	
	@Override
	public byte[] handleRequest(byte[] request) {
		
	}

}
