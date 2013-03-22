package org.umn.distributed.consistent.server;

import java.io.IOException;

import org.umn.distributed.consistent.common.BulletinBoard;
import org.umn.distributed.consistent.common.Machine;
import org.umn.distributed.consistent.common.Props;
import org.umn.distributed.consistent.common.TCPClient;
import org.umn.distributed.consistent.common.Utils;
import org.umn.distributed.consistent.server.coordinator.Coordinator;

public abstract class ReplicaServer extends AbstractServer {

	private static final String START_ELECTION_COMMAND = "STRTELEC";
	private static final String END_ELECTION_COMMAND = "ENDELEC";

	protected boolean coordinator = false;
	protected Machine coordinatorMachine;
	protected TCPServer externalTcpServer;
	protected Coordinator coordinatorServer;
	// Need to access bb using syncronized methods
	protected BulletinBoard bb = new BulletinBoard();

	protected ReplicaServer(STRATEGY strategy, boolean isCoordinator,
			String coordinatorIP, int coordinatorPort) {
		super(strategy, Props.SERVER_INTERNAL_PORT,
				Props.INTERNAL_SERVER_THREADS);
		this.externalTcpServer = new TCPServer(this,
				Props.EXTERNAL_SERVER_THREADS);
		this.strategy = strategy;
		this.coordinator = isCoordinator;
		if (this.coordinator) {
			coordinatorIP = Utils.getLocalServerIp();
			coordinatorPort = Props.COORDINATOR_PORT;
		}
		// TODO: set id
		this.coordinatorMachine = new Machine(coordinatorIP, coordinatorPort);
	}

	protected void startCoordinator() throws Exception {
		logger.debug("Starting coordinator");
		try {
			this.coordinatorServer = createCoordinator();
			this.coordinatorServer.start();
			int port = coordinatorServer.getInternalPort();
			this.coordinatorMachine.setPort(port);
			logger.debug("Coordinator started");
		} catch (Exception e) {
			logger.error("Unable to start coordinator", e);
			this.coordinatorServer.stop();
			throw e;
		}
	}

	protected abstract Coordinator createCoordinator();

	protected String createRegisterMessage() {
		StringBuilder builder = new StringBuilder();
		builder.append(REGISTER_COMMAND).append(COMMAND_PARAM_SEPARATOR);
		builder.append(myInfo.getId()).append(COMMAND_PARAM_SEPARATOR);
		builder.append(myInfo.getIP()).append(COMMAND_PARAM_SEPARATOR);
		builder.append(myInfo.getPort()).append(COMMAND_PARAM_SEPARATOR);
		builder.append(myInfo.getExternalPort());
		return builder.toString();
	}

	@Override
	public void startSpecific() throws Exception {
		logger.debug("Start replica server");
		try {
			preRegister();
			register();
			postRegister();
		} catch (Exception e) {
			logger.error("Failed to registerer to " + coordinatorMachine, e);
			// TODO: fix it
			// stop();
			throw e;
		}
	}

	protected void preRegister() throws Exception {
		logger.debug("Pre registering");
		if (this.coordinator) {
			startCoordinator();
		}
		int port = this.externalTcpServer
				.startListening(Props.SERVER_EXTERNAL_PORT);
		this.myInfo.setExternalPort(port);
	}

	protected void register() throws Exception {
		logger.debug("Registering machine to coordinator");
		String registerMessage = createRegisterMessage();
		byte resp[] = TCPClient.sendData(coordinatorMachine,
				Utils.stringToByte(registerMessage, Props.ENCODING));
		String respStr = Utils.byteToString(resp, Props.ENCODING);
		if (!respStr.startsWith(COMMAND_SUCCESS)) {
			throw new Exception("Coordinator rejected to register the replica");
		}
		logger.info("Registered to coordinator " + coordinatorMachine);
	}

	protected void postRegister() throws IOException {
		logger.debug("Post registering");
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

	public byte[] handleRequest(byte[] request) {
		String req = Utils.byteToString(request, Props.ENCODING);
		if (req.startsWith(HEARTBEAT_COMMAND)) {
			return Utils.stringToByte(COMMAND_SUCCESS, Props.ENCODING);
		} else if (req.startsWith(START_ELECTION_COMMAND)) {
			// TODO: handle election. SHould block all writes;
			return null;
		} else if (req.startsWith(END_ELECTION_COMMAND)) {
			// TODO: handle election ends. start new coordinator and start
			// acceptign reqs after coordinator
			return null;
		}
		return handleSpecificRequest("");
	}

	public abstract byte[] handleSpecificRequest(String request);
}
