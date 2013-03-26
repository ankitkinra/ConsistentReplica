package org.umn.distributed.consistent.common.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
	private HashMap<Integer, Machine> replicaServerMap = new HashMap<Integer, Machine>();
	private static final String POST_START = BulletinBoard.FORMAT_START
			+ Article.FORMAT_START;
	private static int format_indent = 2;

	private Client(String coordinatorIp, int coordinatorPort) {
		this.coordinator = new Machine(coordinatorIp, coordinatorPort);
	}

	private void startClient() throws Exception {
		byte resp[] = TCPClient.sendData(coordinator,
				Utils.stringToByte(AbstractServer.GET_REGISTERED_COMMAND));
		logger.info("resp=" + resp);
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
					this.replicaServerMap.put(m.getId(), m);
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

	private void printArticleIndented(String str, int indent) {
		for (int i = format_indent; i < indent; i++) {
			System.out.print(" ");
		}
		System.out.println(str);
	}

	private String printIndentated(String str) {
		int indent = 0;
		while (str.length() > 0) {
			if (str.startsWith(POST_START)) {
				indent += format_indent;
				int index = str.indexOf(Article.FORMAT_END);
				if (index < -1) {
					System.out.println("Article list format error");
					break;
				}
				printArticleIndented(str.substring(1, index + 1), indent);
				str = str.substring(index + 1);
			} else if (str.startsWith(BulletinBoard.FORMAT_ENDS)) {
				indent -= format_indent;
				if (indent < 0) {
					System.out.println("Article list format error");
					break;
				}
				str = str.substring(1);
			} else {
				System.out.println("Article list format error");
				break;
			}
		}
		return str;
	}

	private void readArticleList(Machine machine) {
		try {
			// Start with id 1. This can be changed later to handle only
			// specific list reads
			String command = ReplicaServer.READ_COMMAND
					+ ReplicaServer.COMMAND_PARAM_SEPARATOR + 1;
			byte resp[] = TCPClient.sendData(machine,
					Utils.stringToByte(command));
			String response = Utils.byteToString(resp);
			if (response.startsWith(AbstractServer.COMMAND_SUCCESS)) {
				System.out.println("********Read articles********");
				response = response
						.substring((AbstractServer.COMMAND_SUCCESS + AbstractServer.COMMAND_PARAM_SEPARATOR)
								.length());
				printIndentated(response);
				System.out
						.println("****************************************************");
			} else {
				System.out.println("Error reading article list from " + machine);
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
				System.out.println("Error reading article from " + machine);
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
			BufferedReader br = new BufferedReader(new InputStreamReader(
					System.in));
			while (true) {
				System.out
						.println("Command (r, rl <5 to move ahead>, w <article>):");
				String command = br.readLine();
				System.out.println("ID:");
				int id = Integer.parseInt(br.readLine());
				Machine machine = client.replicaServerMap.get(id);
				if (command.startsWith("rl")) {
					client.readArticleList(machine);
				} else if (command.startsWith("r")) {
					client.readArticle(machine, command.substring(2));
				} else if (command.startsWith("w")) {
					String articleStr = command.substring(2);
					Article article = Article.parseArticle(articleStr);
					client.postArticle(article, machine);
//					String articleParams[] = articleStr.split("\\|");
//					if (articleParams.length != 3) {
//						System.out.println("Illegal articl format");
//					} else {
//						int parentId = 0;
//						try {
//							parentId = Integer.parseInt(articleParams[0]);
//						} catch (NumberFormatException nfe) {
//							System.out.println("Invalid parentId");
//						}
//						client.postArticle(new Article(0, parentId,
//								articleParams[1], articleParams[2]), machine);
//					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
