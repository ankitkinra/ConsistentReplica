package org.umn.distributed.consistent.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.umn.distributed.consistent.server.ReplicaServer;
import org.umn.distributed.consistent.server.sequential.SequentialServer;

public class ConsistentReplica {

	public static final String COORDINATOR_PARAM = "coordinator";
	public static final String COMMAND_SHOWINFO = "showinfo";
	public static final String COMMAND_STOP = "stop";

	private static void showUsage() {
		System.out.println("Usage:");
		System.out.println("Start coordinator: ./startreplica.sh "
				+ COORDINATOR_PARAM + " <config file path>");
		System.out
				.println("Start replica: ./startreplica.sh <Coordinator Ip> <Coordinator Port> <config file path>");
	}

	public static void main(String[] args) {
		// TODO: add actual command line parameters
		ReplicaServer replicaServer = null;
		if (args.length == 2 || args.length == 3) {
			if (args[0].equals(COORDINATOR_PARAM) && args.length == 2) {
				Props.loadProperties(args[1]);
				replicaServer = new SequentialServer(true, null, 0);
			} else if (args.length == 3) {
				try {
					int port = Integer.parseInt(args[1]);
					if (!Utils.isValidPort(port)) {
						System.out.println("Invalid port");
						showUsage();
						return;
					}
					Props.loadProperties(args[2]);
					replicaServer = new SequentialServer(false, args[0], port);
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
