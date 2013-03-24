package org.umn.distributed.consistent.server.coordinator;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.umn.distributed.consistent.common.Machine;
import org.umn.distributed.consistent.common.Props;
import org.umn.distributed.consistent.common.TCPClient;
import org.umn.distributed.consistent.common.Utils;

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

	public static int getArticleIdWithWriteQuorum(Machine coordinatorMachine,
			Integer articleId, Set<Machine> successMachines,
			Set<Machine> failedMachines) throws IOException {
		String message = "AWQ;aid=" + ARTICLE_ID_MSG_PLACEHOLDER + ";sm="
				+ SUCCESS_MACHINES_MSG_PLACEHOLDER + ";fm="
				+ FAILED_MACHINES_MSG_PLACEHOLDER;

		String successMachineStr = getMachinesToSendFormat(successMachines);
		String failedMachineStr = getMachinesToSendFormat(failedMachines);

		message = message.replaceAll(ARTICLE_ID_MSG_PLACEHOLDER,
				String.valueOf(articleId));
		message = message.replaceAll(SUCCESS_MACHINES_MSG_PLACEHOLDER,
				successMachineStr);
		message = message.replaceAll(FAILED_MACHINES_MSG_PLACEHOLDER,
				failedMachineStr);
		byte[] awqReturn = TCPClient.sendData(coordinatorMachine,
				Utils.stringToByte(message, Props.ENCODING));
		// we will modify the variables sent to us
		String awqStr = Utils.byteToString(awqReturn, Props.ENCODING);

		String[] brokenOnSemiColon = awqStr.split(";");
		for (int i = 1; i < brokenOnSemiColon.length; i++) {

			String[] brokenOnEqual = brokenOnSemiColon[i].split("=");
			switch (i) {
			case 1:
				// this is the Aid
				articleId = Integer.parseInt(brokenOnEqual[1]);
				break;
			case 2:
				parseAndSetMachines(failedMachines, brokenOnEqual[1]);
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
	 * @param machineSeparatedByPipe
	 *            Example = 111.43.24.1:5431|111.43.24.1:5432
	 */
	private static void parseAndSetMachines(Set<Machine> machineSetPut,
			String machineSeparatedByPipe) {
		List<Machine> machines = new LinkedList<Machine>();
		String[] pipeSeparated = machineSeparatedByPipe.split("|");
		for (String server : pipeSeparated) {
			String[] serverAdd = server.split(":");
			machines.add(new Machine(serverAdd[0], Integer
					.parseInt(serverAdd[1])));

		}
		// if all well
		machineSetPut.clear();
		machineSetPut.addAll(machines);
	}

	private static String getMachinesToSendFormat(Set<Machine> machineSet) {

		StringBuilder sb = new StringBuilder("");
		for (Machine server : machineSet) {

			sb.append(server.getIP()).append(":").append(server.getPort())
					.append("|");

		}
		return sb.toString();
	}

	public static void getReadQuorum(Machine coordinatorMachine,
			Set<Machine> successMachines, Set<Machine> failedMachines)
			throws IOException {
		String message = "RQ;" + "sm=" + SUCCESS_MACHINES_MSG_PLACEHOLDER
				+ ";fm=" + FAILED_MACHINES_MSG_PLACEHOLDER;

		String successMachineStr = getMachinesToSendFormat(successMachines);
		String failedMachineStr = getMachinesToSendFormat(failedMachines);

		message = message.replaceAll(SUCCESS_MACHINES_MSG_PLACEHOLDER,
				successMachineStr);
		message = message.replaceAll(FAILED_MACHINES_MSG_PLACEHOLDER,
				failedMachineStr);
		byte[] rqReturn = TCPClient.sendData(coordinatorMachine,
				Utils.stringToByte(message, Props.ENCODING));
		// we will modify the variables sent to us
		String rqStr = Utils.byteToString(rqReturn, Props.ENCODING);

		String[] brokenOnSemiColon = rqStr.split(";");
		for (int i = 1; i < brokenOnSemiColon.length; i++) {

			String[] brokenOnEqual = brokenOnSemiColon[i].split("=");
			switch (i) {
			case 1:
				parseAndSetMachines(failedMachines, brokenOnEqual[1]);
				break;
			default:
				break;

			}
		}

	}

}
