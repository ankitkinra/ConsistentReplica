package org.umn.distributed.consistent.server;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Level;
import org.umn.distributed.consistent.common.BulletinBoard;
import org.umn.distributed.consistent.common.LoggingUtils;
import org.umn.distributed.consistent.common.Machine;
import org.umn.distributed.consistent.common.Props;
import org.umn.distributed.consistent.common.TCPClient;
import org.umn.distributed.consistent.common.Utils;
import org.umn.distributed.consistent.server.coordinator.Coordinator;

public abstract class ReplicaServer extends AbstractServer {

	protected static final String INTERNAL_WRITE_COMMAND = "INWRITE";
	public static final String WRITE_COMMAND = "WRITE";
	public static final String READ_COMMAND = "READ"; //READ from client
	public static final String READITEM_COMMAND = "RDITEM";

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
			logger.debug("############coordinatorIP=" + coordinatorIP);
			coordinatorPort = Props.COORDINATOR_PORT;
		}
		// TODO: set id
		this.coordinatorMachine = new Machine(coordinatorIP, coordinatorPort);
	}

	protected void startCoordinator() throws Exception {
		logger.debug("Starting coordinator");
		try {
			this.coordinatorServer = createCoordinator();
			logger.debug("****************Starting Coordinator****************");
			this.coordinatorServer.start();
			int port = coordinatorServer.getInternalPort();
			this.coordinatorMachine.setPort(port);
			logger.info("****************Coordinator "
					+ this.coordinatorMachine + " started");
		} catch (Exception e) {
			logger.error("Unable to start coordinator. Stopping coordinator", e);
			this.coordinatorServer.stop();
			throw e;
		}
	}

	protected abstract Coordinator createCoordinator();

	protected String createRegisterMessage() {
		StringBuilder builder = new StringBuilder();
		builder.append(REGISTER_COMMAND).append(COMMAND_PARAM_SEPARATOR);
		builder.append(myInfo);
		return builder.toString();
	}

	@Override
	public void startSpecific() throws Exception {
		logger.debug("Starting replica server");
		try {
			preRegister();
			register();
			postRegister();
		} catch (Exception e) {
			logger.error("Failed to register " + myInfo + " to "
					+ coordinatorMachine, e);
			// TODO: fix it
			// stop();
			throw e;
		}
		logger.info("******************************** Replica started ************************************");
		logger.info("Server IP: " + this.myInfo.getIP() + ", Server Port: " + this.myInfo.getExternalPort());
		logger.info("*************************************************************************************");
	}

	protected void preRegister() throws Exception {
		logger.debug("Pre registering");
		if (this.coordinator) {
			startCoordinator();
		}
		logger.debug("Starting external listener for " + this.myInfo);
		int port = this.externalTcpServer
				.startListening(Props.SERVER_EXTERNAL_PORT);
		this.myInfo.setExternalPort(port);
		logger.info("External listener started at port " + port + " for "
				+ this.myInfo);
	}

	protected void register() throws Exception {
		logger.info("Registering " + this.myInfo + " to coordinator "
				+ this.coordinatorMachine);
		String registerMessage = createRegisterMessage();
		byte resp[] = TCPClient.sendData(coordinatorMachine,
				Utils.stringToByte(registerMessage));
		String respStr = Utils.byteToString(resp);
		if (!respStr.startsWith(COMMAND_SUCCESS)) {
			logger.error("Error getting serverId from the coordinator "
					+ this.coordinatorMachine);
			throw new Exception("Coordinator rejected to register the replica");
		} else {
			String respParams[] = respStr.split(COMMAND_PARAM_SEPARATOR);
			this.myInfo.setid(Integer.parseInt(respParams[1]));
			logger.info("Updated " + this.myInfo + " with new id");
		}
		LoggingUtils.addSpecificFileAppender(getClass(), "logs\\",
				this.myInfo.getId() + "", Level.DEBUG);
		logger.info(myInfo + " registered to coordinator " + coordinatorMachine);
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
	public abstract String post(String req);

	/*
	 * Read all the posts with ids
	 */
	public abstract String readItemList(String req);

	/*
	 * Show details for one post
	 */
	public abstract String readItem(String req);

	/*
	 * Actually writes the content. Implementation depends on the type of server
	 * (Primary, coordinator, normal server)
	 */
	public abstract String write(String req);

	public byte[] handleRequest(byte[] request) {
		try {
			String req = Utils.byteToString(request);
			if (req.startsWith(HEARTBEAT_COMMAND)) {
				req = req
						.substring((HEARTBEAT_COMMAND + COMMAND_PARAM_SEPARATOR)
								.length());
				logger.info(myInfo
						+ " recieved heartbeat ping with server list " + req);
				List<Machine> machinesToAdd = Machine.parseList(req);
				Set<Machine> currentMachines = getMachineList();
				for (Machine machine : machinesToAdd) {
					if (!currentMachines.contains(machine)) {
						this.addMachine(machine);
						logger.info(this.myInfo + " added " + machine
								+ " to known server list");
					} else {
						currentMachines.remove(machine);
					}
				}
				for (Machine machine : currentMachines) {
					this.removeMachine(machine.getId());
					logger.info(this.myInfo + " removed " + machine
							+ " from known server list");
				}
				return Utils.stringToByte(COMMAND_SUCCESS);
			} else if (req.startsWith(START_ELECTION_COMMAND)) {
				// TODO: handle election. SHould block all writes;
				return null;
			} else if (req.startsWith(END_ELECTION_COMMAND)) {
				// TODO: handle election ends. start new coordinator and start
				// acceptign reqs after coordinator
				return null;
			}
			return handleSpecificRequest(req);
		} catch (Exception e) {
			logger.error("Exception handling request in replica server", e);
			return Utils.stringToByte(COMMAND_FAILED + COMMAND_PARAM_SEPARATOR
					+ e.getMessage());
		}
	}

	@Override
	public void stop() {
		super.stop();
		if (this.coordinator) {
			this.coordinatorServer.stop();
		}
	}

	public abstract byte[] handleSpecificRequest(String request);
	

	public void startElection() {
		/**
		 * <pre>
		 * 1. Need to ask to all the clients in the knownClients.ceilingMap(own.machineId()) to start
		 * election 
		 * 2. if none of the machines respond then, I win election and send the new message to the knownClients.tailMap()
		 * 3. If a startElection message is received from a client
		 * a) From lower id
		 * </pre>
		 */
	}

}

