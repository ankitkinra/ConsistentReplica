package org.umn.distributed.consistent.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;
import org.umn.distributed.consistent.common.Machine;
import org.umn.distributed.consistent.common.Utils;

public abstract class AbstractServer implements TcpServerDelegate {
	protected Logger logger = Logger.getLogger(this.getClass());

	public enum STRATEGY {
		SEQUENTIAL, QUORUM
	}

	protected static final String REGISTER_COMMAND = "REGISTER";
	protected static final String INVALID_COMMAND = "INVCOM";

	protected static final String HEARTBEAT_COMMAND = "PING";
	protected static final String COMMAND_SUCCESS = "SUCCESS";
	protected static final String COMMAND_FAILED = "FAILED";
	
	protected static final String READ_QUORUM_COMMAND = "RQ";
	protected static final String WRITE_QUORUM_COMMAND = "WQ";

	protected static final String COMMAND_PARAM_SEPARATOR = "-";
	private TCPServer tcpServer;
	protected int port;
	protected Machine myInfo;
	protected STRATEGY strategy;
	// TODO think if we can change this to list sorted in increasing order
	protected TreeMap<Integer, Machine> knownClients = new TreeMap<Integer, Machine>();
	private ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
	protected final Lock readL = rwl.readLock();
	protected final Lock writeL = rwl.writeLock();

	protected AbstractServer(STRATEGY strategy, int port, int numTreads) {
		this.strategy = strategy;
		this.port = port;
		this.tcpServer = new TCPServer(this, numTreads);
	}

	public void start() throws Exception {
		logger.debug("starting server");
		try {
			this.port = this.tcpServer.startListening(this.port);
			// TODO: Add id once you get it form server
			myInfo = new Machine(Utils.getLocalServerIp(), this.port);
			startSpecific();
		} catch (IOException ioe) {
			logger.debug("Error starting server. Stopping now");
			this.stop();
			logger.error("Error starting tcp server", ioe);
			throw ioe;
		}
	}

	public abstract void startSpecific() throws Exception;

	protected Machine addMachine(Machine machine) {
		writeL.lock();
		try {
			return this.knownClients.put(machine.getId(), machine);
		} finally {
			writeL.unlock();
		}
	}

	protected Machine removeMachine(int id) {
		writeL.lock();
		try {
			return this.knownClients.remove(id);
		} finally {
			writeL.unlock();
		}
	}

	protected Set<Machine> getMachineList() {
		Set<Machine> machineSet = new HashSet<Machine>();
		readL.lock();
		try {
			Collection<Machine> machines = this.knownClients.values();
			Iterator<Machine> it = machines.iterator();
			while (it.hasNext()) {
				machineSet.add(it.next());
			}
		} finally {
			readL.unlock();
		}
		return machineSet;
	}

	protected List<Machine> getTailList(int id) {
		List<Machine> list = new ArrayList<Machine>();
		readL.lock();
		try {
			SortedMap<Integer, Machine> tailMap = this.knownClients.tailMap(id);
			Iterator<Integer> it = tailMap.keySet().iterator();
			while (it.hasNext()) {
				Integer key = it.next();
				list.add(tailMap.get(key));
			}
		} catch (IllegalArgumentException iae) {
			logger.error(
					"Id :" + id + " outside its range: "
							+ this.knownClients.firstKey() + "-"
							+ this.knownClients.lastKey(), iae);
		} finally {
			readL.unlock();
		}
		return list;
	}

	public int getInternalPort() {
		return this.port;
	}

	public void stop() {
		this.tcpServer.stop();
	}

}
