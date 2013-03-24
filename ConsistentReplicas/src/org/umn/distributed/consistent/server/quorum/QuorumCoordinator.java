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

	public QuorumCoordinator() {
		super(STRATEGY.QUORUM);
	}

	@Override
	public byte[] handleRequest(byte[] request) {
		return null;
	}

	/**
	 * <pre>
	 * will handle the following requests
	 * 1) return read quorum == RQ-S=a.b.c.d:i1|a.b.c.d:i2-F=a.b.c.d:i3
	 * 2) return write quorum along with the article-id when required == WQ-A=i-S=a.b.c.d:i1|a.b.c.d:i2-F=a.b.c.d:i3
	 * </pre>
	 */
	@Override
	public byte[] handleSpecificRequest(String request) {
		/**
		 * TODO I need the server who initiated the request so that I can add
		 * the server that initiated the request to the read/write quorum if
		 * needed
		 */
		String[] req = request.split(COMMAND_PARAM_SEPARATOR);
		if (request.startsWith(READ_QUORUM_COMMAND)) {
			/**
			 * req[1] == represents SuccessServers
			 * req[2] == represents FailedServers
			 */
			String[] serversStr = req[1].split("=");
			Set<Machine> successServers = parseServers(serversStr[1]);
			serversStr = req[2].split("=");
			Set<Machine> failedServers = parseServers(serversStr[1]);
			getReadQuorum(successServers, failedServers);
			return getReadQuorumMessage(failedServers);
		} else if (request.startsWith(WRITE_QUORUM_COMMAND)){
			/**
			 * req[1] == articleId
			 * req[2] == represents SuccessServers
			 * req[3] == represents FailedServers
			 */
			String[] arrStr = req[1].split("=");
			int localArticleId = Integer.parseInt(arrStr[1]);
			arrStr = req[2].split("=");
			Set<Machine> successServers = parseServers(arrStr[1]);
			arrStr = req[2].split("=");
			Set<Machine> failedServers = parseServers(arrStr[1]);
			if(localArticleId <= 0){
				localArticleId = articleID.getAndIncrement(); 
			}
			getWriteQuorum(successServers, failedServers);
			return getWriteQuorumReturnMessage(localArticleId, failedServers);
		}
		return null;
	}

	private Set<Machine> parseServers(String string) {
		// TODO Auto-generated method stub
		return null;
	}

	private byte[] getReadQuorumMessage(Set<Machine> failedMachines) {
		String prefix = "RMQ;";
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
	 * TODO when calling this method pass a new articleId if existing 
	 * articleId == -1 only
	 * </pre>
	 * @param articleId
	 * @param failedMachines
	 * @return
	 */
	private byte[] getWriteQuorumReturnMessage(Integer articleId,
			Set<Machine> failedMachines) {
		String prefix = "WMQ;aid="+articleId+";";
		return getQuorumReturnMessage(prefix, failedMachines);
	}
	
	private byte[] getQuorumReturnMessage(String prefix,
			Set<Machine> failedMachines) {
		StringBuilder quorumResponse = new StringBuilder(prefix);
		for (Machine m : failedMachines) {
			quorumResponse.append(m.getIP()).append(":").append(m.getPort())
					.append(";");
		}
		return Utils.stringToByte(quorumResponse.toString());
	}

	private void getQuorum(boolean readQuorum, Set<Machine> successMachines,
			Set<Machine> failedMachines) {
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
			int totalQSize = knownClients.size();
			int[] rwQSize = getReadWriteQSize(totalQSize);
			int quorumSizeToReturn = 0;
			if(readQuorum){
				quorumSizeToReturn = rwQSize[0];
			}else{
				quorumSizeToReturn = rwQSize[1];
			}
			

			int machinesToReturn = failedMachines.size() == 0 ? quorumSizeToReturn
					: (quorumSizeToReturn - successMachines.size());
			// get set of knownMachines and remove successMachines and
			// failedMachines
			if (machinesToReturn > 0) {
				Set<Machine> knownMachineSet = getKnownMachineSet();
				knownMachineSet.removeAll(successMachines);
				knownMachineSet.removeAll(failedMachines);
				if (knownMachineSet.size() > 0) {
					failedMachines.clear();
					LinkedList<Machine> machineList = new LinkedList<Machine>();
					machineList.addAll(knownMachineSet);
					for (int i = 0; i < machinesToReturn; i++) {
						Collections.shuffle(machineList);
						failedMachines.add(machineList.poll());
					}
				}
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
		// TODO Auto-generated method stub
		return null;
	}
}
