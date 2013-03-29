package org.umn.distributed.consistent.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.umn.distributed.consistent.server.ReplicaServer;
import org.umn.distributed.consistent.server.quorum.QuorumServer;
import org.umn.distributed.consistent.server.ryw.ReadYourWritesServer;
import org.umn.distributed.consistent.server.sequential.SequentialServer;

public class ConsistentReplica {

	public static final String COORDINATOR_PARAM = "coordinator";
	public static final String SEQUENTIAL = "s";
	public static final String QUORUM = "q";
	public static final String RYW = "r";
	public static final String COMMAND_SHOWINFO = "showinfo";
	public static final String COMMAND_STOP = "stop";

	private static void showUsage() {
		System.out.println("Usage:");
		System.out.println("Start coordinator: ./startreplica.sh "
				+ COORDINATOR_PARAM + " [" + SEQUENTIAL + "|" + QUORUM + "|"
				+ RYW + "] <config file path>");
		System.out.println("Start replica: ./startreplica.sh [" + SEQUENTIAL
				+ "|" + QUORUM + "|" + RYW 	
				+ "] <Coordinator Ip> <Coordinator Port> <config file path>");
		System.out.println("s        :Use Sequential consistency");
		System.out.println("q        :Use Quorum consistency");
		System.out.println("r        :Use Read-Your-Write consistency");
	}

	public static ReplicaServer getServerInstance(String str,
			boolean isCoordinator, String ip, int port) {
		if (str.equals(SEQUENTIAL)) {
			return new SequentialServer(isCoordinator, ip, port);
		} else if (str.equals(QUORUM)) {
			return new QuorumServer(isCoordinator, ip, port);
		} else if (str.equals(RYW)) {
			return new ReadYourWritesServer(isCoordinator, ip, port);
		}
		return null;
	}

	public static void main(String[] args) {
		// TODO: add actual command line parameters
		ReplicaServer replicaServer = null;
		if (args.length == 3 || args.length == 4) {
			if (args[0].equals(COORDINATOR_PARAM) && args.length == 3) {
				Props.loadProperties(args[2]);
				replicaServer = getServerInstance(args[1], true, null, 0);
			} else if (args.length == 4) {
				try {
					int port = Integer.parseInt(args[2]);
					if (!Utils.isValidPort(port)) {
						System.out.println("Invalid port");
						showUsage();
						return;
					}
					Props.loadProperties(args[3]);
					replicaServer = getServerInstance(args[0], false, args[1],
							port);
				} catch (NumberFormatException nfe) {
					System.out.println("Invalid port");
					showUsage();
					return;
				}
			} else {
				showUsage();
				return;
			}
			try {
				if (replicaServer == null) {
					showUsage();
					return;
				}
				replicaServer.start();
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		} else {
			showUsage();
			return;
		}
		boolean stopped = false;
		while (!stopped) {
			BufferedReader br = new BufferedReader(new InputStreamReader(
					System.in));
			while (true) {
				System.out.println("Usage:");
				System.out.println("Command (" + COMMAND_SHOWINFO + ", "
						+ COMMAND_STOP + "):");
				try {
					String command = br.readLine();
					if (command.startsWith(COMMAND_SHOWINFO)) {
						replicaServer.showInfo();
					} else if (command.startsWith(COMMAND_STOP)) {
						replicaServer.stop();
					}
				} catch (IOException e) {
					System.out.println("Error reading from command line");
				}
			}
		}
	}

}
