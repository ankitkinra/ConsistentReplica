package org.umn.distributed.consistent.common.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.umn.distributed.consistent.common.Article;
import org.umn.distributed.consistent.common.Machine;
import org.umn.distributed.consistent.common.Props;
import org.umn.distributed.consistent.common.TCPClient;
import org.umn.distributed.consistent.common.Utils;
import org.umn.distributed.consistent.server.AbstractServer;
import org.umn.distributed.consistent.server.ReplicaServer;
import org.umn.distributed.consistent.server.quorum.CommandCentral.CLIENT_REQUEST;

public class Client {
	public static final String PROPERTIES_FILE = "client.properties";
	public static final String COMMAND_READ_LIST = "readlist";
	public static final String COMMAND_READ = "read";
	public static final String COMMAND_POST = "post";
	public static final String COMMAND_REPLY = "postreply";
	public static final String COMMAND_NEXT = "n";
	public static final String COMMAND_PREVIOUS = "p";
	public static final String COMMAND_QUIT = "q";
	public static final String COMMAND_STOP = "stop";
	private Machine coordinator;
	private HashMap<Integer, Machine> replicaServerMap = new HashMap<Integer, Machine>();
	private List<String> articleList;
	private static int perPage = 10;
	private BufferedReader reader;

	private Client(String coordinatorIp, int coordinatorPort) {
		this.coordinator = new Machine(coordinatorIp, coordinatorPort);
		this.reader = new BufferedReader(new InputStreamReader(System.in));
	}

	private String readLine() throws IOException {
		return reader.readLine();
	}

	private void refreshReplicaServers() throws Exception {
		byte resp[] = TCPClient.sendData(coordinator,
				Utils.stringToByte(AbstractServer.GET_REGISTERED_COMMAND));
		String respStr = Utils.byteToString(resp);
		if (!respStr.startsWith(AbstractServer.COMMAND_SUCCESS)) {
			throw new Exception(
					"Error getting the replica server list from coordinator");
		} else {
			String replicaStr = respStr
					.substring((AbstractServer.COMMAND_SUCCESS + AbstractServer.COMMAND_PARAM_SEPARATOR)
							.length());
			if (replicaStr.length() > 0) {
				List<Machine> toAdd = Machine.parseList(replicaStr);
				this.replicaServerMap.clear();
				for (Machine m : toAdd) {
					m.setPort(m.getExternalPort());
					m.setExternalPort(0);
					this.replicaServerMap.put(m.getId(), m);
				}
			}
		}
	}

	private void postArticle(Article article, Machine machine) {
		try {
			String command = CLIENT_REQUEST.POST.name()
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
			e.printStackTrace();
		}
	}

	private void printIndentated(String str) {
		boolean stopped = false;
		articleList = Utils.getIndentedArticleList(str);
		int start = 0;
		while (!stopped) {
			int curr = 0;
			while (curr < perPage && (curr + start) < articleList.size()) {
				System.out.println(articleList.get(start + curr));
				curr++;
			}
			if ((start + perPage) < articleList.size()) {
				System.out.print("Next (n), ");
			}
			if (start == 0) {
				System.out.println("Previous (p), ");

			}
			System.out.println("Quit (q):");
			try {
				String command = readLine();
				if (command.equals("n")) {
					start += perPage;
				} else if (command.equals("p")) {
					start -= perPage;
				} else if (command.equals("q")) {
					stopped = true;
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
				stopped = true;
			}
		}
	}

	private void readArticleList(Machine machine) {
		try {
			// Start with id 1. This can be changed later to handle only
			// specific list reads
			String command = CLIENT_REQUEST.READ_ITEMS.name()
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
				System.out
						.println("Error reading article list from " + machine);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void readArticle(Machine machine, int id) {
		try {
			String command = CLIENT_REQUEST.READ_ITEM.name()
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
			e.printStackTrace();
		}

	}

	public Machine parseAndGetMachine(String idStr) {
		int id = 0;
		try {
			if (!Utils.isEmpty(idStr)) {
				id = Integer.parseInt(idStr);
			}
		} catch (NumberFormatException nfe) {
			System.out.println("Invalid machine id format");
			return null;
		}
		Integer key = id;
		if (id == 0) {
			id = (int) Math.random() * (this.replicaServerMap.size() - 1);
			Set<Integer> keys = this.replicaServerMap.keySet();
			for (Integer k : keys) {
				key = k;
				if (id == 0) {
					break;
				}
				id--;
			}
		}
		return this.replicaServerMap.get(key);
	}

	public static void showStartUsage() {
		System.out.println("Usage:");
		System.out
				.println("Start replica: ./startclient.sh <coordinatorIp> <coordinatorPort> [<config file path>]");
	}

	public static void showUsage() {
		System.out.println("\n\nUsage:");
		System.out.println("Post: " + COMMAND_POST
				+ " |<post title>|<post content>| [<replica id>]");
		System.out.println("Read List: " + COMMAND_READ_LIST
				+ " [<replica id>]");
		System.out.println("Read Details: " + COMMAND_READ
				+ " <post id> [<replica id>]");
		System.out
				.println("Reply: "
						+ COMMAND_REPLY
						+ " <post id to reply> |<reply title>|<reply content>| [<replica id>]");
		System.out.println("Stop Client:" + COMMAND_STOP);
		System.out.println("eg.: " + COMMAND_POST
				+ " |New news title|New news Content|");
	}

	public static void main(String[] args) {
		int port = 0;
		if (args.length == 2 || args.length == 3) {
			try {
				port = Integer.parseInt(args[1]);
				if (!Utils.isValidPort(port)) {
					System.out.println("Invalid port");
					showStartUsage();
					return;
				}
				if (args.length == 2) {
					Props.loadProperties(PROPERTIES_FILE);
				} else {
					Props.loadProperties(args[2]);
				}
			} catch (NumberFormatException nfe) {
				System.out.println("Invalid port");
				showStartUsage();
				return;
			}
		} else {
			showStartUsage();
			return;
		}
		Client client = new Client(args[0], port);
		try {
			client.refreshReplicaServers();
			boolean stopped = false;
			while (!stopped) {
				showUsage();
				String command = client.readLine();
				if (command.startsWith(COMMAND_READ_LIST)) {
					String idStr = command
							.substring(COMMAND_READ_LIST.length()).trim();
					Machine machine = client.parseAndGetMachine(idStr);
					if (machine == null) {
						System.out
								.println("Machine not found in list. Will update the replica list now");
						client.refreshReplicaServers();
					} else {
						client.readArticleList(machine);
					}
				} else if (command.startsWith(COMMAND_READ)) {
					command = command.substring(COMMAND_READ.length()).trim();
					int index = command.indexOf(" ");
					try {
						int id = Integer.parseInt(command.substring(0, index));
						String idStr = command.substring(index).trim();
						Machine machine = client.parseAndGetMachine(idStr);
						if (machine == null) {
							System.out
									.println("Machine not found in list. Will update the replica list now");
							client.refreshReplicaServers();
						} else {
							client.readArticle(machine, id);
						}
					} catch (NumberFormatException nfe) {
						System.out.println("Invalid article id format");
					}
				} else if (command.startsWith(COMMAND_POST)) {
					command = command.substring(COMMAND_POST.length()).trim();
					if (command.startsWith("\"")) {
						int index = command.indexOf("\"", 1);
						String articleTitle = command.substring(1, index);
						command = command.substring(index + 1).trim();
						index = command.indexOf("\"", 1);
						String articleContent = command.substring(1, index);
						String idStr = command.substring(index + 1).trim();
						Machine machine = client.parseAndGetMachine(idStr);
						if (machine == null) {
							System.out
									.println("Machine not found in list. Will update the replica list now");
							client.refreshReplicaServers();
						} else {
							client.postArticle(new Article(0, 0, articleTitle,
									articleContent), machine);
						}
					} else {
						System.out
								.println("Unable to parse post title and post content.");
					}
				} else if (command.startsWith(COMMAND_REPLY)) {
					command = command.substring(COMMAND_REPLY.length()).trim();
					int index = command.indexOf(" ");
					try {
						int id = Integer.parseInt(command.substring(0, index));
						command = command.substring(index).trim();
						if (command.startsWith("\"")) {
							index = command.indexOf("\"", 1);
							String articleTitle = command.substring(1, index);
							command = command.substring(index + 1).trim();
							index = command.indexOf("\"", 1);
							String articleContent = command.substring(1, index);
							String idStr = command.substring(index + 1).trim();
							Machine machine = client.parseAndGetMachine(idStr);
							if (machine == null) {
								System.out
										.println("Machine not found in list. Will update the replica list now");
								client.refreshReplicaServers();
							} else {
								client.postArticle(new Article(0, id,
										articleTitle, articleContent), machine);
							}
						} else {
							System.out
									.println("Unable to parse post title and post content.");
						}
					} catch (NumberFormatException nfe) {
						System.out.println("Invalid article id format");
					}
				} else if (command.startsWith(COMMAND_STOP)) {
					stopped = true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
