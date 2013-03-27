package org.umn.distributed.consistent.server.coordinator;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.umn.distributed.consistent.common.Machine;
import org.umn.distributed.consistent.common.Props;
import org.umn.distributed.consistent.common.TCPClient;
import org.umn.distributed.consistent.common.Utils;
import org.umn.distributed.consistent.server.AbstractServer;
import org.umn.distributed.consistent.server.quorum.CommandCentral;
import org.umn.distributed.consistent.server.quorum.CommandCentral.COORDINATOR_CALLS;

public class CoordinatorClientCallFormatter {

	private static final String FAILED_MACHINES_MSG_PLACEHOLDER = "%%FAILED_MACHINES%%";
	private static final String SUCCESS_MACHINES_MSG_PLACEHOLDER = "%%SUCCESS_MACHINES%%";
	private static final String ARTICLE_ID_MSG_PLACEHOLDER = "%%ARTICLE_ID%%";
	private static final byte[] GET_ARTICLE_ID = Utils.stringToByte(
			"fetchArticleId", Props.ENCODING);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public static int gettingArticleId(Machine coordinatorMachine)
			throws IOException {
		// TODO this will start a TCPClient and send the Coodinartor a request

		byte[] id = TCPClient.sendData(coordinatorMachine, GET_ARTICLE_ID);
		String idStr = Utils.byteToString(id, Props.ENCODING);

		return Integer.parseInt(idStr);
	}

	/**
	 * <pre>
	 * return write quorum along with the article-id when required == 
	 * GET_WRITE_QUORUM_COMMAND-M=1-A=i-S=id:a.b.c.d:i1|id:a.b.c.d:i2-F=id:a.b.c.d:i3
	 * 
	 * @param coordinatorMachine
	 * @param articleId
	 * @param successMachines
	 * @param failedMachines
	 * @return
	 * @throws IOException
	 */
	public static int getArticleIdWithWriteQuorum(Machine ownMachine,
			Machine coordinatorMachine, Integer articleId,
			Set<Machine> successMachines, Set<Machine> failedMachines)
			throws IOException {
		StringBuilder writeQuorumMessage = new StringBuilder(
				CommandCentral.COORDINATOR_CALLS.GET_WRITE_QUORUM.name());
		writeQuorumMessage.append(AbstractServer.COMMAND_PARAM_SEPARATOR)
				.append("M=").append(ownMachine.getId());
		writeQuorumMessage.append(AbstractServer.COMMAND_PARAM_SEPARATOR)
				.append("A=").append(articleId);
		writeQuorumMessage.append(AbstractServer.COMMAND_PARAM_SEPARATOR)
				.append("S=").append(getMachinesToSendFormat(successMachines));
		writeQuorumMessage.append(AbstractServer.COMMAND_PARAM_SEPARATOR)
				.append("F=").append(getMachinesToSendFormat(failedMachines));

		byte[] awqReturn = TCPClient.sendData(coordinatorMachine, Utils
				.stringToByte(writeQuorumMessage.toString(), Props.ENCODING));
		// we will modify the variables sent to us
		String awqStr = Utils.byteToString(awqReturn, Props.ENCODING);
		// return expected as "WMQ-aid=<id>-F=<machine1>;<machine2>..."
		String[] brokenOnCommandSeparator = awqStr
				.split(AbstractServer.COMMAND_PARAM_SEPARATOR);
		for (int i = 1; i < brokenOnCommandSeparator.length; i++) {

			String[] brokenOnEqual = brokenOnCommandSeparator[i]
					.split(AbstractServer.COMMAND_VALUE_SEPARATOR);
			switch (i) {
			case 1:
				// this is the Aid
				articleId = Integer.parseInt(brokenOnEqual[1]);
				break;
			case 2:
				if (brokenOnEqual.length > 1) {
					parseAndSetMachines(failedMachines, brokenOnEqual[1]);
				} else {
					failedMachines.clear(); // done
				}

				break;
			default:
				break;

			}
		}

		return articleId;
	}

	/**
	 * Server sends back the pipeSeparated new machines
	 * 
	 * @param failedMachines
	 * @param machineSeparatedBySemiColon
	 *            with id Example = 1:111.43.24.1:5431|3:111.43.24.1:5432
	 */
	private static void parseAndSetMachines(Set<Machine> machineSetPut,
			String machineSeparatedBySemiColon) {
		List<Machine> machines = new LinkedList<Machine>();
		String[] semiColonSeparated = machineSeparatedBySemiColon
				.split(AbstractServer.LIST_SEPARATOR);
		for (String server : semiColonSeparated) {
			String[] serverAdd = server.split(":");
			machines.add(new Machine(Integer.parseInt(serverAdd[0]),
					serverAdd[1], Integer.parseInt(serverAdd[2])));

		}
		// if all well
		machineSetPut.clear();
		machineSetPut.addAll(machines);
	}

	private static String getMachinesToSendFormat(Set<Machine> machineSet) {

		StringBuilder sb = new StringBuilder("");
		for (Machine server : machineSet) {

			sb.append(server.getId()).append(":").append(server.getIP())
					.append(":").append(server.getPort()).append("|"); // TODO
																		// convert
																		// this
																		// to
																		// list-separator

		}
		return sb.toString();
	}

	/**
	 * <pre>
	 * Calls expected as :
	 * 1) return read quorum == GET_READ_QUORUM_COMMAND-M=1-S=id:a.b.c.d:i1|id:a.b.c.d:i2-F=id:a.b.c.d:i3
	 * 2) return write quorum along with the article-id when required == WQ-M=1-A=i-S=id:a.b.c.d:i1|id:a.b.c.d:i2-F=id:a.b.c.d:i3
	 * @param coordinatorMachine
	 * @param successMachines
	 * @param failedMachines
	 * @throws IOException
	 */
	public static void getReadQuorum(Machine ownMachine,
			Machine coordinatorMachine, Set<Machine> successMachines,
			Set<Machine> failedMachines) throws IOException {
		StringBuilder readMessage = new StringBuilder(
				COORDINATOR_CALLS.GET_READ_QUORUM.name());
		readMessage.append(AbstractServer.COMMAND_PARAM_SEPARATOR).append("M=")
				.append(ownMachine.getId());
		readMessage.append(AbstractServer.COMMAND_PARAM_SEPARATOR).append("S=")
				.append(getMachinesToSendFormat(successMachines));
		readMessage.append(AbstractServer.COMMAND_PARAM_SEPARATOR).append("F=")
				.append(getMachinesToSendFormat(failedMachines));
		byte[] rqReturn = TCPClient.sendData(coordinatorMachine,
				Utils.stringToByte(readMessage.toString(), Props.ENCODING));
		// we will modify the variables sent to us
		/**
		 * response = RMQ-F=<machine1>;<machine2>
		 */
		String rqStr = Utils.byteToString(rqReturn, Props.ENCODING);
		String[] rqStrBrokenOnCommandSeparator = rqStr
				.split(AbstractServer.COMMAND_PARAM_SEPARATOR);
		/*
		 * String[] rqStrBrokenOnValueSeparator =
		 * rqStr.split(AbstractServer.COMMAND_VALUE_SEPARATOR); String[]
		 * brokenOnSemiColon =
		 * rqStrBrokenOnValueSeparator[1].split(AbstractServer.LIST_SEPARATOR);
		 */
		for (int i = 1; i < rqStrBrokenOnCommandSeparator.length; i++) {

			String[] brokenOnEqual = rqStrBrokenOnCommandSeparator[i]
					.split(AbstractServer.COMMAND_VALUE_SEPARATOR);
			switch (i) {
			case 1:
				if (brokenOnEqual.length > 1) {
					parseAndSetMachines(failedMachines, brokenOnEqual[1]);
				} else {
					// if no data in the failed servers, it means we are done
					failedMachines.clear();
				}
				break;
			default:
				break;

			}
		}

	}

}
