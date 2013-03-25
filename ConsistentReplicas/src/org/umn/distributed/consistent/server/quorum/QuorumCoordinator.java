package org.umn.distributed.consistent.server.quorum;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.umn.distributed.consistent.common.Machine;
import org.umn.distributed.consistent.common.Props;
import org.umn.distributed.consistent.common.Utils;
import org.umn.distributed.consistent.server.coordinator.Coordinator;

public class QuorumCoordinator extends Coordinator {

	private boolean sendOwnMachineAsQuorum = false;

	public QuorumCoordinator() {
		super(STRATEGY.QUORUM);
	}

	/**
	 * <pre>
	 * will handle the following requests
	 * M == own id with which it registered
	 * S = Success Servers list in following format = id,ip,port
	 * F = Failed Servers list in following format = id,ip,port
	 * A = Article id, pass 0 if id needed
	 * 1) return read quorum == RQ-M=1-S=id:a.b.c.d:i1|id:a.b.c.d:i2-F=id:a.b.c.d:i3
	 * 2) return write quorum along with the article-id when required == WQ-M=1-A=i-S=id:a.b.c.d:i1|id:a.b.c.d:i2-F=id:a.b.c.d:i3
	 * </pre>
	 */
	@Override
	public byte[] handleSpecificRequest(String request) {
		String[] req = request.split(COMMAND_PARAM_SEPARATOR);
		if (request.startsWith(GET_READ_QUORUM_COMMAND)) {
			/**
			 * req[1] == represents SuccessServers req[2] == represents
			 * FailedServers
			 */
			int i = 1;
			String[] splitArr = req[i].split("=");
			int sentMachineId = Integer.parseInt(splitArr[1]);
			splitArr = req[++i].split("=");
			Set<Machine> successServers = parseServers(splitArr, 1);
			splitArr = req[++i].split("=");
			Set<Machine> failedServers = null;
			failedServers = parseServers(splitArr, 1);
			getReadQuorum(sentMachineId, successServers, failedServers);
			return getReadQuorumMessage(failedServers);
		} else if (request.startsWith(GET_WRITE_QUORUM_COMMAND)) {
			/**
			 * req[1] == articleId req[2] == represents SuccessServers req[3] ==
			 * represents FailedServers
			 */
			logger.info("GET_WRITE_QUORUM_COMMAND;request at QC=" + request);
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
			getWriteQuorum(sentMachineId, successServers, failedServers);
			return getWriteQuorumReturnMessage(localArticleId, failedServers);
		}
		return null;
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

	private byte[] getReadQuorumMessage(Set<Machine> failedMachines) {
		String prefix = "RMQ" + COMMAND_PARAM_SEPARATOR;
		return getQuorumReturnMessage(prefix, failedMachines);
	}

	private void getReadQuorum(int ownMachineId, Set<Machine> successMachines,
			Set<Machine> failedMachines) {
		getQuorum(true, ownMachineId, successMachines, failedMachines);
	}

	private void getWriteQuorum(int ownMachineId, Set<Machine> successMachines,
			Set<Machine> failedMachines) {
		getQuorum(false, ownMachineId, successMachines, failedMachines);
	}

	/**
	 * <pre>
	 * TODO when calling this method pass a new articleId if existing 
	 * articleId == -1 only
	 * </pre>
	 * 
	 * @param articleId
	 * @param failedMachines
	 * @return
	 */
	private byte[] getWriteQuorumReturnMessage(Integer articleId,
			Set<Machine> failedMachines) {
		String prefix = "WMQ" + COMMAND_PARAM_SEPARATOR + "aid=" + articleId
				+ COMMAND_PARAM_SEPARATOR;
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

	private void getQuorum(boolean readQuorum, int ownMachineId,
			Set<Machine> successMachines, Set<Machine> failedMachines) {
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
		readL.lock();
		try {
			logger.debug("Current knownMachine =" + knownClients.toString());
			Machine sendMachine = knownClients.get(ownMachineId);
			int totalQSize = knownClients.size();
			int[] rwQSize = getReadWriteQSize(totalQSize);
			int quorumSizeToReturn = 0;
			if (readQuorum) {
				quorumSizeToReturn = rwQSize[0];
			} else {
				quorumSizeToReturn = rwQSize[1];
			}

			int machinesToReturn = successMachines.size()
					+ failedMachines.size() == 0 ? quorumSizeToReturn
					: (quorumSizeToReturn - successMachines.size());
			// get set of knownMachines and remove successMachines and
			// failedMachines
			logger.debug(String.format(
					"getQuorum; machinesToReturn = %s; quorumSizeToReturn=%s",
					machinesToReturn, quorumSizeToReturn));
			if (machinesToReturn > 0) {
				Set<Machine> knownMachineSet = getKnownMachineSet();
				knownMachineSet.removeAll(successMachines);
				knownMachineSet.removeAll(failedMachines);

				if (knownMachineSet.size() > 0) {
					failedMachines.clear();
					if (!readQuorum) {
						if (sendOwnMachineAsQuorum) {
							if (knownMachineSet.contains(sendMachine)) {
								// if knownMachineSet still contains
								// sendMachine,
								// then include it to final list
								failedMachines.add(sendMachine);
								knownMachineSet.remove(sendMachine);
								machinesToReturn--;
							}
						}
					} else {
						// if readQuorum then do not send own machine back, let
						// readQuorum be empty if needed
						knownMachineSet.remove(sendMachine);
						// as we can always merge local and respond to the
						// client
					}
					LinkedList<Machine> machineList = new LinkedList<Machine>();
					machineList.addAll(knownMachineSet);
					for (int i = 0; machineList.size() > 0
							&& i < machinesToReturn; i++) {
						Collections.shuffle(machineList);
						failedMachines.add(machineList.poll());
					}
				} else {
					/**
					 * this means that after removing the failed set we are not
					 * able to support the read/write request, this would
					 * typically mean that replica has found out that a
					 * particular server is not responding but our heartbeat
					 * mechanism has still to figure that out The best way to
					 * handle that is to block this client for sometime so that
					 * the heartbeat might get activated, or allow the client to
					 * come back with the same failed Server
					 * 
					 */
					try {
						logger.info("Sleeping for HEARTBEAT interval = "
								+ Props.HEARTBEAT_INTERVAL);
						Thread.sleep(Props.HEARTBEAT_INTERVAL);
					} catch (InterruptedException e) {
						logger.error("Sleep interrupted", e);
					}
				}
			} else {
				// if no more machines to send then we need to clear out
				// failedSet
				// as read or write requirement is complete
				failedMachines.clear();
			}

		} finally {
			readL.unlock();
		}

	}

	private Set<Machine> getKnownMachineSet() {
		readL.lock();
		try {
			Set<Machine> machineSet = new HashSet<Machine>();
			machineSet.addAll(knownClients.values());
			return machineSet;
		} finally {
			readL.unlock();
		}

	}

	private int[] getReadWriteQSize(int totalQSize) {
		int[] arr = new int[2];
		if (totalQSize > 0) {
			arr[1] = totalQSize / 2 + 1;
			arr[0] = totalQSize - arr[1] + 1;
		} else {
			arr[1] = 0;
			arr[0] = 0;
		}
		return arr;
	}

	public static void main(String[] args) {
		Props.loadProperties("C:\\Users\\akinra\\git\\ConsistentReplica\\ConsistentReplicas\\config.properties");
		System.out.println(Props.ENCODING);
		QuorumCoordinator qc = new QuorumCoordinator();
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
