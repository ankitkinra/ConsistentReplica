package org.umn.distributed.consistent.common.client.testfrmwk;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.umn.distributed.consistent.common.Article;
import org.umn.distributed.consistent.common.LoggingUtils;
import org.umn.distributed.consistent.common.Machine;
import org.umn.distributed.consistent.common.TCPClient;
import org.umn.distributed.consistent.common.Utils;
import org.umn.distributed.consistent.server.AbstractServer;
import org.umn.distributed.consistent.server.ReplicaServer;
import org.umn.distributed.consistent.server.quorum.CommandCentral.CLIENT_REQUEST;

import com.thoughtworks.xstream.XStream;

public class TestClient {
	private static final String READ_ITEM_COMMAND_NAME = "readItem";
	private static final String READ_ITEMS_COMMAND_NAME = "readItems";
	private static final String POST_OPERATION_NAME = "post";
	private String xmlTestCasePath = null;
	private Logger logger = Logger.getLogger(TestClient.class);
	private ArrayList<Round> rounds = new ArrayList<Round>();
	/**
	 * TreeMap gives us the sorted view of the order the articles were published
	 * at
	 */
	private TreeMap<Integer, ArticlesPublished> articlesPublishedRecord = new TreeMap<Integer, ArticlesPublished>();
	private Machine myCoord;
	private HashMap<Integer, RoundSummary> roundSummaries = new HashMap<Integer, RoundSummary>();
	private Random r = new Random();

	public TestClient(String xmlFilePath, String coordinatorIp,
			int coordinatorPort) {
		this.xmlTestCasePath = xmlFilePath;
		Round newRound = getParsedRound(xmlFilePath);
		this.rounds.add(newRound);
		this.myCoord = new Machine(coordinatorIp, coordinatorPort);

	}

	public static void main(String[] args) {
		String xmlTestFile = args[0];
		String coopIP = args[1];
		int coopPort = Integer.parseInt(args[2]);
		TestClient tc = new TestClient(xmlTestFile, coopIP, coopPort);
		System.out.println(tc.rounds);
		tc.startTest();
	}

	private List<Machine> getReplicaServers() throws Exception {
		List<Machine> avlblReplicaList = null;
		byte[] resp = TCPClient.sendData(this.myCoord,
				Utils.stringToByte(AbstractServer.GET_REGISTERED_COMMAND));

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
				avlblReplicaList = Machine.parseList(replicaStr);
			}
		}
		return avlblReplicaList;
	}

	private void startTest() {
		for (int i = 0; i < this.rounds.size(); i++) {
			// init new summary
			Round r = this.rounds.get(i);
			RoundSummary rs = new RoundSummary(r);
			this.roundSummaries.put(i, rs);
			logger.info("In the testClient starting round =" + r.name);
			for (Operation op : r.operations) {
				/**
				 * Before each operation we need to start a timer and get the
				 * servers where we need to send the operation to
				 */
				if (op.name.equals(POST_OPERATION_NAME)) {
					String rootArticlesToPostStr = op.params.get("root");
					String childArticlesToPostStr = op.params.get("child");
					int rootArticlesToPost = 0, repliesToPost = 0;
					if (!Utils.isEmpty(rootArticlesToPostStr)) {
						rootArticlesToPost = Integer
								.parseInt(rootArticlesToPostStr);
					}
					if (!Utils.isEmpty(childArticlesToPostStr)) {
						repliesToPost = Integer
								.parseInt(childArticlesToPostStr);
					}
					try {
						postArticles(rs, rootArticlesToPost, repliesToPost);
					} catch (Exception e) {
						logger.error("Error in posting articles", e);
					}
				} else if (op.name.equals(READ_ITEMS_COMMAND_NAME)) {
					/**
					 * Need to get a random server and read its content and
					 * record the time After that we can give an analysis which
					 * says that do we have all the articles that we have posted
					 * in this server or not
					 */

					try {
						readArticlesAndCompareOwnPublishedArticles(rs);
					} catch (Exception e) {
						logger.error("Error in readArticles articles", e);
					}

				} else if (op.name.equals(READ_ITEM_COMMAND_NAME)) {

					String relativeArticleIdStr = op.params.get("id");
					int relativeArticleId = 0;
					if (!Utils.isEmpty(relativeArticleIdStr)) {
						relativeArticleId = Integer
								.parseInt(relativeArticleIdStr);
					}
					try {
						readSelfPublishedArticle(rs, relativeArticleId);
					} catch (Exception e) {
						logger.error("Error in reading articleId = "
								+ relativeArticleId, e);
					}
				}
			}
		}

	}

	private void readArticlesAndCompareOwnPublishedArticles(RoundSummary rs)
			throws Exception {

		List<Machine> avlblMachines = getReplicaServers();
		if (avlblMachines.size() > 0) {
			long readTimer = System.currentTimeMillis();
			// startTimer
			Collections.shuffle(avlblMachines);
			Machine machineToRead = avlblMachines.get(0);
			List<Article> articlesReturned = getArticleList(machineToRead);
			// endTimer
			long totalTimeToRead = System.currentTimeMillis() - readTimer;
			// TODO add logs
			rs.addOperationDetail(READ_ITEMS_COMMAND_NAME, totalTimeToRead);
			LoggingUtils
					.logInfo(
							logger,
							"Article List = %s from Server =%s, retrieved in time = %s",
							articlesReturned, machineToRead, totalTimeToRead);
			/**
			 * Check if any article is missing
			 * 
			 */

			Set<Integer> returnedArticleIdSet = new HashSet<Integer>();
			for (Article a : articlesReturned) {
				returnedArticleIdSet.add(a.getId());
			}
			Set<Integer> publishedArtSet = articlesPublishedRecord.keySet();
			publishedArtSet.removeAll(returnedArticleIdSet);
			if (publishedArtSet.size() > 0) {
				// missing articles
				logger.info("Following published articles were not found in the server ="
						+ machineToRead);
				for (Integer missedArt : publishedArtSet) {
					logger.info("Missed Article =" + missedArt);
				}

			}
		}
	}

	private void readSelfPublishedArticle(RoundSummary rs, int relativeArticleId)
			throws Exception {
		if (relativeArticleId > articlesPublishedRecord.size()
				|| relativeArticleId < 0) {
			throw new IllegalArgumentException(
					"relativeArticleId cannot be more than the number of articles published");
		}
		Article articleToRead = null;
		int counter = 0;
		for (Entry<Integer, ArticlesPublished> entry : articlesPublishedRecord
				.entrySet()) {
			if (counter == relativeArticleId) {
				// exit loop
				articleToRead = entry.getValue().getArticle();
				break;
			}
			counter++;
		}
		if (articleToRead != null) {
			List<Machine> avlblMachines = getReplicaServers();
			if (avlblMachines.size() > 0) {
				long readTimer = System.currentTimeMillis();
				// startTimer
				Collections.shuffle(avlblMachines);
				Machine machineToRead = avlblMachines.get(0);
				Article returnedFromMachine = getArticle(machineToRead,
						articleToRead.getId() + "");
				// endTimer
				long totalTimeToRead = System.currentTimeMillis() - readTimer;
				// TODO add logs
				rs.addOperationDetail(READ_ITEM_COMMAND_NAME, totalTimeToRead);
				LoggingUtils.logInfo(logger,
						"Article = %s from Server =%s, retrieved in time = %s",
						returnedFromMachine, machineToRead, totalTimeToRead);
			} else {
				LoggingUtils
						.logInfo(
								logger,
								"Article = %s was not found on the selected server =%s",
								articleToRead);
			}

		}
	}

	private void postArticles(RoundSummary rs, int rootArticlesToPost,
			int repliesToPost) throws Exception {
		// first posting roots
		if (rootArticlesToPost > 0) {
			List<Machine> avlblMachines = getReplicaServers();
			if (avlblMachines.size() > 0) {
				long timer = 0;
				for (int i = 0; i < rootArticlesToPost; i++) {
					timer = System.currentTimeMillis();
					// startTimer
					Collections.shuffle(avlblMachines);
					Machine toPost = avlblMachines.get(0);
					Article randomArticle = getArticle();
					postArticle(randomArticle, toPost);
					// endTimer
					long totalTimeToPublish = System.currentTimeMillis()
							- timer;

					articlesPublishedRecord.put(randomArticle.getId(),
							new ArticlesPublished(i, randomArticle, toPost,
									totalTimeToPublish));
					rs.addOperationDetail(POST_OPERATION_NAME,
							totalTimeToPublish);

				}
			}
		}

		if (repliesToPost > 0) {
			List<Machine> avlblMachines = getReplicaServers();
			List<Machine> avlblMachinCopy = new ArrayList<Machine>();
			avlblMachinCopy.addAll(avlblMachines);
			Article randomParentArticle = getRandomParentArticle(avlblMachinCopy);
			if (avlblMachines.size() > 0) {
				long timer = 0;
				for (int i = 0; i < repliesToPost; i++) {
					timer = System.currentTimeMillis();
					// startTimer
					Collections.shuffle(avlblMachines);
					Machine toPost = avlblMachines.get(0);
					Article randomArticle = createReplyArticle(randomParentArticle);
					postArticle(randomArticle, toPost);
					// endTimer
					long totalTimeToPublish = System.currentTimeMillis()
							- timer;
					// TODO add logs
					articlesPublishedRecord.put(randomArticle.getId(),
							new ArticlesPublished(i, randomArticle, toPost,
									totalTimeToPublish));
					rs.addOperationDetail(POST_OPERATION_NAME,
							totalTimeToPublish);

				}
			}
		}
	}

	private Article createReplyArticle(Article randomParentArticle) {
		return new Article(0, randomParentArticle.getId(), "TestReplyArticle_"
				+ System.currentTimeMillis(),
				"TestReplyCONTENT........................");
	}

	// need to get a random article from this bb so I need to read first the
	// entire bb, but do not record this time
	// as the time should be recorded for the posting only
	/*
	 * We can get this article in two ways 1) If we have posted some article in
	 * this test run then we make it the parent 2) Else, we go to a random
	 * server and pick up an article
	 */
	private Article getRandomParentArticle(List<Machine> avlblMachines) {
		Article randomArticleToReturn = null;
		if (articlesPublishedRecord.size() > 0) {
			// we have published an article in this run, pick one
			randomArticleToReturn = articlesPublishedRecord.firstEntry()
					.getValue().getArticle();
		} else {
			if (avlblMachines.size() > 0) {
				while (randomArticleToReturn == null) {
					Collections.shuffle(avlblMachines);
					Machine m = avlblMachines.get(0);
					List<Article> aList = getArticleList(m);
					if (aList.size() > 0) {
						randomArticleToReturn = aList.get(0);
					} else {
						avlblMachines.remove(0);
					}
				}
			}

		}
		return randomArticleToReturn;
	}

	private Article getArticle() {
		return new Article(0, 0, "TestArticle_" + System.currentTimeMillis(),
				"TestCONTENT........................");
	}

	private void postArticle(Article article, Machine machine) {
		try {
			String command = CLIENT_REQUEST.POST.name()
					+ AbstractServer.COMMAND_PARAM_SEPARATOR + article;
			byte resp[] = TCPClient.sendData(machine,
					Utils.stringToByte(command));
			String response = Utils.byteToString(resp);
			if (response.startsWith(AbstractServer.COMMAND_SUCCESS)) {
				int articleIdAssigned = Integer
						.parseInt(response
								.substring((AbstractServer.COMMAND_SUCCESS + AbstractServer.COMMAND_PARAM_SEPARATOR)
										.length()));
				System.out.println("Article written with id "
						+ articleIdAssigned);
				article.setId(articleIdAssigned);
			} else {
				System.out.println("Error writing article to " + machine);
			}
		} catch (IOException e) {
			logger.error("Error adding article", e);
		}
	}

	private List<Article> getArticleList(Machine machine) {
		List<Article> articles = null;
		try {
			// Start with id 1. This can be changed later to handle only
			// specific list reads
			String command = CLIENT_REQUEST.READ_ITEMS.name()
					+ ReplicaServer.COMMAND_PARAM_SEPARATOR + 1;
			byte resp[] = TCPClient.sendData(machine,
					Utils.stringToByte(command));
			String response = Utils.byteToString(resp);
			if (response.startsWith(AbstractServer.COMMAND_SUCCESS)) {

				response = response
						.substring((AbstractServer.COMMAND_SUCCESS + AbstractServer.COMMAND_PARAM_SEPARATOR)
								.length());
				articles = parseArticleList(response);
			} else {
				System.out
						.println("Error reading article list from " + machine);
			}
		} catch (IOException e) {
			logger.error("Error adding article", e);
		}
		return articles;
	}

	private Article getArticle(Machine machine, String id) {
		Article readArticle = null;
		try {
			String command = CLIENT_REQUEST.READ_ITEM.name()
					+ ReplicaServer.COMMAND_PARAM_SEPARATOR + id;
			byte resp[] = TCPClient.sendData(machine,
					Utils.stringToByte(command));
			String response = Utils.byteToString(resp);
			if (response.startsWith(AbstractServer.COMMAND_SUCCESS)) {
				response = response
						.substring((AbstractServer.COMMAND_SUCCESS + AbstractServer.COMMAND_PARAM_SEPARATOR)
								.length());
				readArticle = Article.parseArticle(response);
			} else {
				System.out.println("Error reading article from " + machine);
			}
		} catch (IOException e) {
			logger.error("Error adding article", e);
		}
		return readArticle;

	}

	public static List<Article> parseArticleList(String req) {
		List<Article> articles = new LinkedList<Article>();
		int index = -1;
		int start = 0;
		while ((index = req.indexOf("]", start)) > -1) {
			Article a = Article.parseArticle(req.substring(start, index + 1));
			articles.add(a);
			start = index + 1;

		}
		return articles;
	}

	private static Round getParsedRound(String filePath) {
		BufferedReader in = null;
		StringBuilder sb = new StringBuilder();
		try {
			in = new BufferedReader(new FileReader(filePath));
			String str = null;
			while ((str = in.readLine()) != null) {
				sb.append(str);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {

				}
			}
		}
		XStream xstream = new XStream();
		xstream.alias("round", Round.class);
		xstream.alias("operation", Operation.class);

		Round newRound = (Round) xstream.fromXML(sb.toString());

		return newRound;
	}

}
