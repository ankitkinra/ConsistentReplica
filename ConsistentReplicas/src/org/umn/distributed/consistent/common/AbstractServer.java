package org.umn.distributed.consistent.common;

import java.util.TreeMap;

public abstract class AbstractServer {

	public enum STRATEGY {
		SEQUENTIAL, QUORUM,
	}

	protected IReadStrategy readStrategy;
	protected IWriteStrategy writeStrategy;

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

	protected abstract void preRegister();

	protected void register() {

	}

	protected abstract void postRegister();

	protected void shutdown() {
		preUnRegister();
		unRegister();
		postUnRegister();
	}

	protected abstract void preUnRegister();

	protected void unRegister() {

	}

	protected abstract void postUnRegister();

	protected void initCoordinator() {
		//TODO
		/**
		 * this will start a listener on some port which listens to 
		 * other servers request
		 * Also we need to heartbeat all the known servers
		 */
	}

	/**
	 * Client Operations
	 */

	/*
	 * Post and read details. implementation will vary based on the protocol
	 */
	public String post(String message, String parentId) {
		return writeStrategy.write(message, parentId);
	}

	/*
	 * Read all the posts with ids
	 */
	public String readItemList() {
		return readStrategy.readItemList();
	}

	/*
	 * Show details for one post
	 */
	public String readItem(String id) {
		return readStrategy.readItem(id);
	}

	/*
	 * Actually writes the content. Implementation depends on the type of server
	 * (Primary, coordinator, normal server)
	 */
	public boolean write() {
		return true;
	}

}
