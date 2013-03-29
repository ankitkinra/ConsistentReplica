package org.umn.distributed.consistent.server.ryw;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.umn.distributed.consistent.common.LoggingUtils;
import org.umn.distributed.consistent.common.Machine;
import org.umn.distributed.consistent.common.Props;
import org.umn.distributed.consistent.common.TCPClient;
import org.umn.distributed.consistent.common.Utils;
import org.umn.distributed.consistent.server.coordinator.Coordinator;
import org.umn.distributed.consistent.server.quorum.CommandCentral.COORDINATOR_CALLS;
import org.umn.distributed.consistent.server.quorum.CommandCentral.COORDINATOR_REQUESTS;

public class ReadYourWritesCoordinator extends Coordinator {

	private static final int MAX_NUMBER_OF_BACKUP_REPLICAS_TO_MAINTAIN = 1;
	protected TreeMap<Integer, Machine> knownBackups = new TreeMap<Integer, Machine>();
	private ReentrantReadWriteLock rwlKnownBackups = new ReentrantReadWriteLock();
	protected final Lock readLKnownBackups = rwlKnownBackups.readLock();
	protected final Lock writeLKnownBackups = rwlKnownBackups.writeLock();

	public ReadYourWritesCoordinator() {
		super(STRATEGY.READ_YOUR_WRITES);
	}

	/**
	 * <pre>
	 * will handle the following requests
	 * M == own id with which it registered
	 * S = Success Servers list in following format = id,ip,port
	 * F = Failed Servers list in following format = id,ip,port
	 * A = Article id, pass 0 if id needed
	 * 
	 * </pre>
	 */
	@Override
	public byte[] handleSpecificRequest(String request) {
		String[] req = request.split(COMMAND_PARAM_SEPARATOR);
		if (request.startsWith(COORDINATOR_CALLS.GET_READ_QUORUM.name())) {
			/**
			 * req[1] == represents SuccessServers req[2] == represents
			 * FailedServers; just return all the backup servers
			 */
			int i = 1;
			String[] splitArr = req[i].split("=");
			int sentMachineId = Integer.parseInt(splitArr[1]); // to keep
																// consistent
			splitArr = req[++i].split("=");
			Set<Machine> successServers = parseServers(splitArr, 1);
			splitArr = req[++i].split("=");
			Set<Machine> failedServers = null;
			failedServers = parseServers(splitArr, 1);
			getReadQuorum(successServers, failedServers);
			return getReadQuorumMessage(failedServers);
		} else if (request
				.startsWith(COORDINATOR_CALLS.GET_WRITE_QUORUM.name())) {
			/**
			 * req[1] == articleId req[2] == represents SuccessServers req[3] ==
			 * represents FailedServers Need to return in the write quorum all
			 * the knownBackupMachines if failed is populated, that means that
			 * we need a new backup server as one of them has failed, need to
			 * initiate the actOnBackupFailed
			 */
			logger.info("GET_WRITE_QUORUM_COMMAND;request at RYW_C=" + request);
			int i = 1;
			String[] arrStr = req[i].split("=");
			int sentMachineId = Integer.parseInt(arrStr[1]);
			arrStr = req[++i].split("=");
			int localArticleId = Integer.parseInt(arrStr[1]);
			arrStr = req[++i].split("=");
			Set<Machine> successServers = parseServers(arrStr, 1);
			arrStr = req[++i].split("=");
			Set<Machine> failedServers = parseServers(arrStr, 1);
			if (localArticleId <= 0) {
				localArticleId = articleID.getAndIncrement();
			}
			getWriteQuorum(successServers, failedServers);
			return getWriteQuorumReturnMessage(localArticleId, failedServers);
		}
		return Utils.stringToByte(INVALID_COMMAND);
	}

	protected void registerMachineAction(Machine machineToAdd) {
		super.registerMachineAction(machineToAdd);
		// if the number of servers in the backup list are less than
		// required_backups
		// we need to add this machine to that set as well
		writeLKnownBackups.lock();
		try {
			if (MAX_NUMBER_OF_BACKUP_REPLICAS_TO_MAINTAIN > knownBackups.size()) {
				if (syncMachineWithBackups(machineToAdd)) {
					knownBackups.put(machineToAdd.getId(), machineToAdd);
				}
			}
		} finally {
			writeLKnownBackups.unlock();
		}
	}

	private Set<Machine> parseServers(String[] serversArr, int index) {
		Set<Machine> machines = new HashSet<Machine>();
		if (serversArr != null && serversArr.length >= index + 1) {
			String[] serverArr = serversArr[index].split("\\|");
			for (String oneServer : serverArr) {
				String[] splitAdd = oneServer.split(":");
				machines.add(new Machine(Integer.parseInt(splitAdd[0]),
						splitAdd[1], Integer.parseInt(splitAdd[2])));
			}
		}
		return machines;
	}

	/**
	 * As machine which has failed can occur in both the machine sets as a m1
	 * can be in knownBackups and in knownClients, hence we check the machine
	 * first in knownBackups and then proceed to recover or promote some other
	 * machine as backup.
	 */
	protected void actOnFailedMachine(Machine m) {
		if (knownBackupContains(m)) {
			actOnBackupFailed(m);
		} else {
			// only other possible scenario is that the machine was in the
			// knownClientSet
			super.actOnFailedMachine(m);
		}
	}

	private void actOnBackupFailed(Machine m) {
		/**
		 * TODO
		 * 
		 * <pre>
		 * 1) need to get a server from the knownClients - knownBackups
		 * 2) Then ask this server to sync with the remainingKnownBackups
		 * 3) add it to the knownBackups
		 * </pre>
		 * 
		 * Taking write lock on both the list, as soon there is going to be a
		 * change so we do not want coordinator to give any stale detail
		 */
		Set<Machine> knownList = getMachineList();
		Set<Machine> backupList = getKnownBackupMachineSet();
		knownList.removeAll(backupList);
		Machine toTransfer = null;
		for (Machine mt : knownList) {
			if (syncMachineWithBackups(mt)) {
				toTransfer = mt;
				break;
			}
		}

		writeLKnownBackups.lock();
		try {
			knownBackups.remove(m.getId());
			knownBackups.put(toTransfer.getId(), toTransfer);
		} finally {

			writeLKnownBackups.unlock();

		}

	}

	private boolean syncMachineWithBackups(Machine mt) {
		/**
		 * We need to ask this machine to sync with the backup servers
		 * 
		 */
		StringBuilder command = new StringBuilder(
				COORDINATOR_REQUESTS.SYNC.name());
		command.append(COMMAND_PARAM_SEPARATOR);
		Set<Machine> backUpMachines = getKnownBackupMachineSet();
		for (Machine m : backUpMachines) {
			command.append(m);
		}
		byte[] dataRead = null;
		try {
			dataRead = TCPClient.sendData(mt,
					Utils.stringToByte(command.toString()));
		} catch (IOException e) {
			logger.error("Error while syncing", e);
			return false;
		}

		if (Utils.byteToString(dataRead).startsWith(COMMAND_SUCCESS)) {
			return true;
		} else {
			return false;
		}

	}

	private boolean knownBackupContains(Machine m) {
		readLKnownBackups.lock();
		try {
			return this.knownBackups.containsKey(m.getId());
		} finally {
			readLKnownBackups.unlock();
		}

	}

	/**
	 * Need to add both the machine sets to this method
	 */
	protected Set<Machine> getHeartbeatMachineList() {
		Set<Machine> heartbeatMachines = new HashSet<Machine>();
		Set<Machine> knownSet = getMachineList();
		Set<Machine> knownBackupSet = getKnownBackupMachineSet();
		heartbeatMachines.addAll(knownSet);
		heartbeatMachines.addAll(knownBackupSet);
		LoggingUtils
				.logInfo(
						logger,
						"Starting heartbeat for ReadYourWrites Coordinator; knownSet=%s; knownBackupSet=%s",
						knownSet, knownBackupSet);
		return heartbeatMachines;
	}

	private byte[] getReadQuorumMessage(Set<Machine> failedMachines) {
		String prefix = "RMQ" + COMMAND_PARAM_SEPARATOR;
		return getQuorumReturnMessage(prefix, failedMachines);
	}

	private void getReadQuorum(Set<Machine> successMachines,
			Set<Machine> failedMachines) {
		getQuorum(true, successMachines, failedMachines);
	}

	private void getWriteQuorum(Set<Machine> successMachines,
			Set<Machine> failedMachines) {
		getQuorum(false, successMachines, failedMachines);
	}

	/**
	 * <pre>
	 * when calling this method pass a new articleId if existing 
	 *  articleId == -1 only
	 * </pre>
	 * 
	 * @param articleId
	 * @param failedMachines
	 * @return
	 */
	private byte[] getWriteQuorumReturnMessage(Integer articleId,
			Set<Machine> failedMachines) {
		String prefix = "WMQ" + COMMAND_PARAM_SEPARATOR + "aid="
				+ (articleId > -1 ? articleId : 0) + COMMAND_PARAM_SEPARATOR;
		return getQuorumReturnMessage(prefix, failedMachines);
	}

	private byte[] getQuorumReturnMessage(String prefix,
			Set<Machine> failedMachines) {
		StringBuilder quorumResponse = new StringBuilder(prefix).append("F=");
		for (Machine m : failedMachines) {
			quorumResponse.append(m.getId()).append(":").append(m.getIP())
					.append(":").append(m.getPort()).append(LIST_SEPARATOR);
		}
		return Utils.stringToByte(quorumResponse.toString());
	}

	private void getQuorum(boolean readQuorum, Set<Machine> successMachines,
			Set<Machine> failedMachines) {
		logger.debug(String.format(
				"getQuorum;readQ=%s,successMachines=%s, failedMachines=%s",
				readQuorum, successMachines, failedMachines));
		/**
		 * <pre>
		 * calculate readQNumber and writeQNumber from totalQNumber 
		 * then qNumnberToReturn = readQuorum?readQNumber:writeQNumber
		 * then the machines to put in failedSet==0 ? qNumnberToReturn : (qNumnberToReturn - successMachines.size()) 
		 * get the Set<Machine> knownMap and remove from this (successMachines + failedMachines)
		 * convert the remaining set to a list as we need get random servers
		 * </pre>
		 */
		int backupSize = 0;
		readLKnownBackups.lock();
		try {
			logger.debug("Current knownMachine =" + knownBackups.toString());
			backupSize = knownBackups.size();
		} finally {
			readLKnownBackups.unlock();
		}
		int quorumSizeToReturn = 0;
		if (readQuorum) {
			quorumSizeToReturn = 1;
		} else {
			quorumSizeToReturn = backupSize;
			boolean quorumFormed = false;
			while (!quorumFormed) {
				readLKnownBackups.lock();
				readL.lock();
				if (MAX_NUMBER_OF_BACKUP_REPLICAS_TO_MAINTAIN <= knownClients
						.size()) {
					// then if the quorumSizeToReturn <
					// MAX_NUMBER_OF_BACKUP_REPLICAS_TO_MAINTAIN
					if (quorumSizeToReturn < MAX_NUMBER_OF_BACKUP_REPLICAS_TO_MAINTAIN) {
						// cannot create appropriate quorum need to
						// sleep,
						// till we have a server promoted
						readLKnownBackups.unlock();
						readL.unlock();

						try {
							Thread.sleep(Props.HEARTBEAT_INTERVAL);
						} catch (InterruptedException e) {
							logger.error("Error while quorum creation", e);
						}
					} else {
						quorumFormed = true; // break out of loop
					}
				}
				readLKnownBackups.unlock();
				readL.unlock();
			}
			readLKnownBackups.lock();
			try {
				quorumSizeToReturn = knownBackups.size();
			} finally {
				readLKnownBackups.unlock();
			}

		}

		// now we have the appropriate quorum size

		if (failedMachines.size() > 0) {
			// initiate failedProtocol and send the new machine
			for (Machine m : failedMachines) {
				actOnBackupFailed(m);
			}
		}
		// get set of knownMachines and remove successMachines and
		int machinesToReturn = successMachines.size() + failedMachines.size() == 0 ? quorumSizeToReturn
				: (quorumSizeToReturn - successMachines.size());
		logger.debug(String.format(
				"getQuorum; machinesToReturn = %s; quorumSizeToReturn=%s",
				machinesToReturn, quorumSizeToReturn));
		if (machinesToReturn > 0) {
			Set<Machine> knownBackupMachineSet = getKnownBackupMachineSet();
			knownBackupMachineSet.removeAll(successMachines);
			/*
			 * the failedmachines will be removed by now from the backup machine
			 * set fail over processing
			 */

			if (knownBackupMachineSet.size() > 0) {
				failedMachines.clear();
				if (!readQuorum) {
					// sent the remaining knownBackupMachineSet
					failedMachines.addAll(knownBackupMachineSet);
				} else {
					// if readQuorum then we need to send back just one
					// backup
					LinkedList<Machine> machineList = new LinkedList<Machine>();
					machineList.addAll(knownBackupMachineSet);
					Collections.shuffle(machineList); // to even out
														// requests
					failedMachines.add(machineList.poll());

				}

			} else {
				/**
				 * this means that after removing the machine from backup we do
				 * not have enough clients to create as backup, so we cannot
				 * continue with the operation !!!!
				 */
				// TODO unsafe situation, alarm
			}
		} else {
			// if no more machines to send then we need to clear out
			// failedSet
			// as read or write requirement is complete
			failedMachines.clear();
		}

	}

	protected Set<Machine> getKnownBackupMachineSet() {
		readLKnownBackups.lock();
		try {
			Set<Machine> machineSet = new HashSet<Machine>();
			machineSet.addAll(knownBackups.values());
			return machineSet;
		} finally {
			readLKnownBackups.unlock();
		}

	}

	public static void main(String[] args) {
		Props.loadProperties("C:\\Users\\akinra\\git\\ConsistentReplica\\ConsistentReplicas\\config.properties");
		System.out.println(Props.ENCODING);
		ReadYourWritesCoordinator qc = new ReadYourWritesCoordinator();
		for (int i = 0; i < 10; i++) {
			Machine m = new Machine(i, i + "." + i + "." + i + "." + i, i);
			qc.addMachine(m);
		}

		String readQrqst = GET_READ_QUORUM_COMMAND
				+ "-M=4-S=1:1.1.1.1:1|2:2.2.2.2:2|-F=";
		String writeQrqst = GET_WRITE_QUORUM_COMMAND
				+ "-M=4-A=0-S=1:1.1.1.1:1|2:2.2.2.2:2|-F=";
		System.out.println(Utils.byteToString(qc
				.handleSpecificRequest(readQrqst)));
		System.out.println(Utils.byteToString(qc
				.handleSpecificRequest(writeQrqst)));
	}
}
