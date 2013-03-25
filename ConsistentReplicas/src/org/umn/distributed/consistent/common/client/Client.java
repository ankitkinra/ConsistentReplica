package org.umn.distributed.consistent.common.client;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.umn.distributed.consistent.common.Article;
import org.umn.distributed.consistent.common.BulletinBoard;
import org.umn.distributed.consistent.common.Machine;
import org.umn.distributed.consistent.common.Props;
import org.umn.distributed.consistent.common.TCPClient;
import org.umn.distributed.consistent.common.Utils;
import org.umn.distributed.consistent.server.AbstractServer;
import org.umn.distributed.consistent.server.ReplicaServer;

public class Client {
	private Logger logger = Logger.getLogger(this.getClass());

	private Machine coordinator;
	private HashMap<Integer,Machine> replicaServerMap = new HashMap<Integer,Machine>();

	private Client(String coordinatorIp, int coordinatorPort) {
		this.coordinator = new Machine(coordinatorIp, coordinatorPort);
	}

	private void startClient() throws Exception {
		byte resp[] = TCPClient.sendData(coordinator,
				Utils.stringToByte(AbstractServer.GET_REGISTERED_COMMAND));
		logger.info("resp="+resp);
		String respStr = Utils.byteToString(resp);
		if (!respStr.startsWith(AbstractServer.COMMAND_SUCCESS)) {
			logger.error("Error getting the replica server list from coordinator");
			throw new Exception(
					"Error getting the replica server list from coordinator");
		} else {
			String replicaStr = respStr
					.substring((AbstractServer.COMMAND_SUCCESS + AbstractServer.COMMAND_PARAM_SEPARATOR)
							.length());
			if (replicaStr.length() > 0) {
				List<Machine> toAdd = Machine.parseList(replicaStr);
				for (Machine m : toAdd) {
					this.replicaServerMap.put(m.getId(),m);
					logger.debug("Added replica " + m + " to client");
				}
			}
		}
	}

	private void postArticle(Article article, Machine machine) {
		try {
			String command = ReplicaServer.WRITE_COMMAND
					+ AbstractServer.COMMAND_PARAM_SEPARATOR + article;
			byte resp[] = TCPClient.sendData(machine,
					Utils.stringToByte(command));
			String response = Utils.byteToString(resp);
			if (response.startsWith(AbstractServer.COMMAND_SUCCESS)) {
				System.out
						.println("Article written with id "
								+ response
										.substring((AbstractServer.COMMAND_SUCCESS + AbstractServer.COMMAND_PARAM_SEPARATOR)
												.length()));
			} else {
				System.out.println("Error writing article to " + machine);
			}
		} catch (IOException e) {
			logger.error("Error adding article", e);
		}
	}

	private void printIndentated(String str, String alignment) {
		int index = -1;
		int start = 1;
		while((index = str.indexOf(Article.FORMAT_END, start)) > -1) {
			System.out.print(alignment);
			System.out.println(str.substring(start, index + 1));
			str = str.substring(index + 2);
			
		}
		if (str.startsWith(BulletinBoard.FORMAT_START)) {
			printIndentated(str.substring(1), alignment + "  ");
		}
	}

	private void readArticleList(Machine machine) {
		try {
			String command = ReplicaServer.READ_COMMAND;
			byte resp[] = TCPClient.sendData(machine,
					Utils.stringToByte(command));
			String response = Utils.byteToString(resp);
			if (response.startsWith(AbstractServer.COMMAND_SUCCESS)) {
				System.out.println("********Read articles********");
				printIndentated(
						response.substring((AbstractServer.COMMAND_SUCCESS + AbstractServer.COMMAND_PARAM_SEPARATOR)
								.length()), "");
				System.out
						.println("****************************************************");
			} else {
				System.out.println("Error writing article to " + machine);
			}
		} catch (IOException e) {
			logger.error("Error adding article", e);
		}
	}

	private void readArticle(Machine machine, String id) {
		try {
			String command = ReplicaServer.READITEM_COMMAND
					+ ReplicaServer.COMMAND_PARAM_SEPARATOR + id;
			byte resp[] = TCPClient.sendData(machine,
					Utils.stringToByte(command));
			String response = Utils.byteToString(resp);
			if (response.startsWith(AbstractServer.COMMAND_SUCCESS)) {
				System.out.println("********Read article********");
				System.out
						.println(response
								.substring((AbstractServer.COMMAND_SUCCESS + AbstractServer.COMMAND_PARAM_SEPARATOR)
										.length()));
				System.out
						.println("****************************************************");
			} else {
				System.out.println("Error writing article to " + machine);
			}
		} catch (IOException e) {
			logger.error("Error adding article", e);
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Props.loadProperties(args[2]);
		Client client = new Client(args[0], Integer.parseInt(args[1]));
		try {
			client.startClient();
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			while (true) {
				System.out.println("Command (r, rl <5 to move ahead>, w <article>):");
				
				String command = br.readLine();
				System.out.println("ID:");
				int id = Integer.parseInt(br.readLine());
				Machine machine = client.replicaServerMap.get(id);
				if (command.startsWith("rl")) {
					client.readArticleList(machine);
				} else if (command.startsWith("r")) {
					client.readArticle(machine, command.substring(2));
				} else if (command.startsWith("w")) {
					try {
						command = command.substring(2);
						client.postArticle(Article.parseArticle(command),
								machine);
					} catch (IllegalArgumentException ex) {
						System.out.print("IllegalArgumentException: " + command
								+ ". Try again");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
