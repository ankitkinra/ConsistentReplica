package org.umn.distributed.consistent.common;

public abstract class AbstractServer {

	IReadStrategy readStrategy;
	IWriteStrategy writeStrategy;

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
	
	
	protected void initCoordinator(){
		
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
