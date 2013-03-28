package org.umn.distributed.consistent.server.quorum;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.umn.distributed.consistent.common.Article;
import org.umn.distributed.consistent.common.BulletinBoard;
import org.umn.distributed.consistent.common.Machine;
import org.umn.distributed.consistent.common.Props;
import org.umn.distributed.consistent.common.TCPClient;
import org.umn.distributed.consistent.common.Utils;
import org.umn.distributed.consistent.server.ReplicaServer;
import org.umn.distributed.consistent.server.coordinator.Coordinator;
import org.umn.distributed.consistent.server.coordinator.CoordinatorClientCallFormatter;
import org.umn.distributed.consistent.server.quorum.CommandCentral.CLIENT_REQUEST;
import org.umn.distributed.consistent.server.quorum.CommandCentral.REQUEST_INTERNAL_SERVER;

public class QuorumServer extends ReplicaServer {
	/**
	 * <pre>
	 * a) Calls from the external client and their responses
	 * 		i) ReadItemList == returns the local bb appended latest Article retrieved from Quorum
	 * 		ii) ReadItem == returns the exact article requested by the client, as ReadItemList would be invoked 
	 * 						before this and we will have all the articles needed by the client which include the local replica 
	 * 						articles along with the max written article.
	 * 		iii) Post == when the client gives an article to be posted, need to assemble the write quorum [after asking coordinator] and get this written out to the quorum
	 * b) Calls from the internal replica servers and their responses
	 * 		i) ReadLatestItem -- Call from an internal server which just wants the latest article to be returned
	 * 		ii) ReadItemsFromID -- Call from an internal server with an articleId and we need to return all the articles after this number
	 * 		iii) WriteArticle -- Write an article in local BB, this will be a well formed article with an Id and everything 
	 * 		iv) Election calls TODO
	 * d) Calls to Quorum Coordinator and their responses
	 * 		i) GetReadQuorum
	 * 		ii) GetWriteQuorumAndArticleId
	 * 		iii) Register
	 * e) Call from Quorum Coordinator
	 * 		i) Heartbeat
	 * 
	 * </pre>
	 */

	private static final String READ_BB_COMMAND_COMPLETE = READ_QUORUM_COMMAND
			+ "-FROM_ID=%%ARTICLE_ID%%";
	private Integer lastSyncedId = 0;
	private Object syncLock = new Object();
	private SyncThread syncThread = new SyncThread(syncLock,
			Props.QUORUM_SYNC_TIME_MILLIS);

	public QuorumServer(boolean isCoordinator, String coordinatorIP,
			int coordinatorPort) {
		super(STRATEGY.QUORUM, isCoordinator, coordinatorIP, coordinatorPort);
		validateParameters();
	}

	private void syncBB() {
		/**
		 * assemble read quorum, which can contain at least one server (in a one
		 * cluster environment) Now merge the BB of read quorum replace own.bb
		 * with merged bb : shadow copying in a synchronized method
		 * 
		 */
		HashMap<Machine, String> responseMap;
		try {
			responseMap = getBBFromReadQuorum(lastSyncedId);
			// merge stage
			logger.info("from syncBB lastSyncedId=" + lastSyncedId
					+ ";response=" + responseMap);
			mergeResponsesWithLocal(responseMap);
			// doing this instead of calling sync as we want to wait on this
			// operation

			lastSyncedId = this.bb.getMaxId();
			logger.info("$$$$$$$After Sync bb=" + this.bb.toString()
					+ ";\nlastSyncedId=" + lastSyncedId);

		} catch (IOException e) {
			logger.error("Maybe the coordinator has died", e);
			this.stop();
		} catch (RuntimeException e) {
			logger.error("Unknown exception caught in sync", e);
			throw e;
		}

	}

	private void mergeResponsesWithLocal(HashMap<Machine, String> responseMap) {
		BulletinBoard mergedBulletinBoard = new BulletinBoard();
		boolean first = true;
		for (Map.Entry<Machine, String> machineBBStr : responseMap.entrySet()) {
			// get BB from string
			String bbStr = machineBBStr.getValue().substring(
					COMMAND_SUCCESS.length()
							+ COMMAND_PARAM_SEPARATOR.length());
			// TODO: I am not sure about this. looks like you always have
			// READ_QUORUM_RESPONSE + COMMAND_PARAM_SEPARATOR
			// in the response. and still you are checking isEmpty. I think it
			// should be parsed and checked.
			if (Utils.isEmpty(bbStr)) {
				logger.warn("Skipping BB String as it is Empty; " + bbStr);
				continue;
			}
			BulletinBoard
					.parseAndAddBBEntriesIntoBB(mergedBulletinBoard, bbStr);
			// if (first) {
			//
			// String articles = machineBBStr.getValue().substring(
			// READ_QUORUM_RESPONSE.length()
			// + COMMAND_PARAM_SEPARATOR.length());
			// logger.info("Sending for parse = " + articles);
			// mergedBulletinBoard = BulletinBoard
			// .parseBBFromArticleList(articles);
			// logger.info(String
			// .format("After parsing articles =%s, mergedBulletinBoard  = %s",
			// articles, mergedBulletinBoard));
			// first = false;
			//
			// } else {
			//
			// BulletinBoard bbThis = BulletinBoard
			// .parseBBFromArticleList(articles);
			// logger.info(String
			// .format("After parsing articles =%s, mergedBulletinBoard  = %s",
			// articles, bbThis));
			// // now merge
			// logger.info("Two bbs to merge = " + mergedBulletinBoard
			// + "\nTwo=" + bbThis);
			// mergedBulletinBoard = BulletinBoard.mergeBB(
			// mergedBulletinBoard, bbThis);
			// }
		}
		// once the bulletinBoard is merged, we can just replace
		if (mergedBulletinBoard != null) {
			this.bb.mergeWithMe(mergedBulletinBoard);
		}

	}

	private void validateParameters() {

	}

	public String write(String message) {
		// this is local write for me
		Article a = null;
		try {
			a = Article.parseArticle(message);
			if (a != null && a.getId() > 0) {
				if (a.isRoot()) {
					this.bb.addArticle(a);
				} else {
					this.bb.addArticleReply(a);
				}
			}
			return COMMAND_SUCCESS + COMMAND_PARAM_SEPARATOR + a.toString();
		} catch (IllegalArgumentException e) {
			logger.error("Article Parse error, message = " + message, e);
		}

		return COMMAND_FAILED + COMMAND_PARAM_SEPARATOR;
	}

	@Override
	public String post(String message) {
		Article aToWrite = null;
		try {
			aToWrite = Article.parseArticle(message);
			if (aToWrite != null) {
				// need to get the article Id from the coordinator
				HashSet<Machine> successfulServers = new HashSet<Machine>();
				HashSet<Machine> failedServers = new HashSet<Machine>();
				HashMap<Machine, Boolean> writeStatus = new HashMap<Machine, Boolean>();
				int articleId = 0;
				do {
					try {
						articleId = populateWriteQuorum(articleId,
								successfulServers, failedServers);
						aToWrite.setId(articleId);
						executeWriteRequestOnWriteQuorum(writeStatus,
								successfulServers, failedServers, aToWrite);
					} catch (IOException e) {
						logger.error(
								"Error in population of writeQuorum , Maybe the coordinator has died",
								e);
						this.stop();
					}
					waitIfFailedServers(failedServers);
				} while (failedServers.size() > 0);

				// once write is done just return
				return COMMAND_SUCCESS + COMMAND_PARAM_SEPARATOR
						+ aToWrite.toString();
			}

		} catch (IllegalArgumentException e1) {

			logger.error("article parse error, message = " + message, e1);
		}
		return COMMAND_FAILED + COMMAND_PARAM_SEPARATOR;

	}

	private void executeWriteRequestOnWriteQuorum(
			HashMap<Machine, Boolean> writeStatus,
			HashSet<Machine> successfulServers, HashSet<Machine> failedServers,
			Article aToWrite) {
		/**
		 * to execute a write on the group of servers we again need to start a
		 * countDownlatch with writer service
		 */

		if (failedServers != null) {
			if (failedServers.contains(myInfo)) {
				// need to write local
				write(aToWrite.toString());
				successfulServers.add(myInfo);
				failedServers.remove(myInfo);
			}
			List<WriteService> threadsToWrite = new ArrayList<WriteService>(
					failedServers.size());
			final CountDownLatch writeQuorumlatch = new CountDownLatch(
					failedServers.size());

			for (Machine server : failedServers) {
				WriteService t = new WriteService(server, aToWrite,
						writeQuorumlatch);
				threadsToWrite.add(t);
				t.start();
			}
			try {
				if (!writeQuorumlatch.await(Props.NETWORK_TIMEOUT,
						TimeUnit.MILLISECONDS)) {
					// as nw has timed out I need to interrupt all other threads
					interruptWriteThreads(threadsToWrite);
				}
				for (WriteService wr : threadsToWrite) {
					String str = null;
					// try {
					str = Utils.byteToString(wr.dataRead, Props.ENCODING);
					if (str.startsWith(COMMAND_SUCCESS)) {
						writeStatus.put(wr.serverToWrite, true);
					} else {
						// basically failed
						wr.dataRead = null;
					}

				}
			} catch (InterruptedException ie) {
				logger.error("Error", ie);
				// interrupt all other threads
				// TODO other servers should kill
				interruptWriteThreads(threadsToWrite);
			} finally {
				// if not thread has some value add it to the success servers
				// else let
				// it be in failed set
				for (WriteService rs : threadsToWrite) {
					if (rs.dataRead != null) {
						successfulServers.add(rs.serverToWrite);
						failedServers.remove(rs.serverToWrite);
					}
				}
			}

		}

	}

	private void interruptWriteThreads(List<WriteService> threadsToWrite) {
		for (WriteService wr : threadsToWrite) {
			if (wr.dataRead == null) {
				wr.interrupt();
			}
		}
	}

	private int populateWriteQuorum(Integer articleId,
			HashSet<Machine> successfulServers, HashSet<Machine> failedServers)
			throws IOException {
		return CoordinatorClientCallFormatter.getArticleIdWithWriteQuorum(
				this.myInfo, this.coordinatorMachine, articleId,
				successfulServers, failedServers);
	}

	/**
	 * TODO should return the local view of the system and not invoke the read
	 * quorum
	 */

	@Override
	public String readItemList(String articleReadFrom) {

		HashMap<Machine, String> responseMap;
		try {
			responseMap = getBBFromReadQuorum(Integer.parseInt(articleReadFrom));
			mergeResponsesWithLocal(responseMap);
			syncThread.resetTimer();
			return bb.toString();
		} catch (NumberFormatException e) {
			/**
			 * Article writing failed, maybe coordinator has failed
			 */
			logger.error("Maybe the coordinator has died", e);
			this.stop();
		} catch (IOException e) {
			logger.error("Maybe the coordinator has died", e);
			this.stop();
		}
		return null;

		// once we have populated and know that required quorum was achieved

	}

	private HashMap<Machine, String> getBBFromReadQuorum(int articleReadFrom)
			throws IOException {
		/**
		 * Any request can be modelled as <code>
		 * 
		 * do{
		 * 	populate failedServers from the coordinator
		 * 	once we have the servers, we need to invoke the operation and keep track of all the servers which failed so that 
		 * 	we can send this to the coordinator again	
		 * }while(failedServers.size == 0)
		 * </code>
		 */

		HashSet<Machine> successfulServers = new HashSet<Machine>();
		HashSet<Machine> failedServers = new HashSet<Machine>();
		HashMap<Machine, String> responseMap = new HashMap<Machine, String>();
		do {
			try {
				populateReadQuorum(successfulServers, failedServers);
				executeReadRequestOnReadQuorum(responseMap, successfulServers,
						failedServers, articleReadFrom);
			} catch (IOException e) {
				logger.error("quorum popualtion failed", e);
				throw e;
			}
			waitIfFailedServers(failedServers);
		} while (failedServers.size() > 0);
		return responseMap;
	}

	private void waitIfFailedServers(HashSet<Machine> failedServers) {
		if (failedServers.size() > 0) {
			/**
			 * introduce a timeout, so that the coordinator gets a chance to
			 * remove the server via the heartbeat
			 */
			logger.info("Thread will sleep = "
					+ Thread.currentThread().getName());
			try {
				Thread.sleep(Props.HEARTBEAT_INTERVAL);
			} catch (InterruptedException e) {
				logger.warn("Got interrupted, will continue");
			}
		}
	}

	private void executeReadRequestOnReadQuorum(
			HashMap<Machine, String> responseMap,
			HashSet<Machine> successfulServers, HashSet<Machine> failedServers,
			int articleReadFrom) {

		if (failedServers != null) {
			if (failedServers.contains(myInfo)) {
				/*
				 * as we have already implemented local read in the final step
				 * of merge of bbs, no need to read now.
				 */

				successfulServers.add(myInfo);
				failedServers.remove(myInfo);
				logger.info("Removed from local read;myInfo=" + myInfo);
			}
			List<ReadService> threadsToRead = new ArrayList<ReadService>(
					failedServers.size());
			final CountDownLatch readQuorumlatch = new CountDownLatch(
					failedServers.size());

			for (Machine server : failedServers) {
				ReadService t = new ReadService(server, readQuorumlatch,
						articleReadFrom);
				threadsToRead.add(t);
				t.start();
			}
			try {
				if (!readQuorumlatch.await(Props.NETWORK_TIMEOUT,
						TimeUnit.MILLISECONDS)) {
					interruptReadThreads(threadsToRead);
				}

				for (ReadService rs : threadsToRead) {
					String str = null;

					str = Utils.byteToString(rs.dataRead, Props.ENCODING);
					responseMap.put(rs.serverToRead, str);

				}
			} catch (InterruptedException ie) {
				logger.error("Error", ie);
				// interrupt all other threads
				// TODO other servers should kill
				interruptReadThreads(threadsToRead);
			} finally {
				// if not thread has some value add it to the success servers
				// else let
				// it be in failed set
				for (ReadService rs : threadsToRead) {
					if (rs.dataRead != null) {
						successfulServers.add(rs.serverToRead);
						failedServers.remove(rs.serverToRead);
					}
				}
			}

		}
	}

	private void interruptReadThreads(List<ReadService> threadsToRead) {
		for (ReadService rs : threadsToRead) {
			if (rs.dataRead == null) {
				rs.interrupt();
			}
		}
	}

	private void populateReadQuorum(HashSet<Machine> successfulServers,
			HashSet<Machine> failedServers) throws IOException {
		CoordinatorClientCallFormatter.getReadQuorum(this.myInfo,
				coordinatorMachine, successfulServers, failedServers);

	}

	private BulletinBoard getMaxIdBulletinBoard(
			List<BulletinBoard> boardsFromReadQuorum) {
		BulletinBoard maxIdBB = null;
		if (boardsFromReadQuorum != null) {
			for (BulletinBoard bb : boardsFromReadQuorum) {
				if (maxIdBB == null) {
					maxIdBB = bb;
				} else {
					if (maxIdBB.getMaxId() < bb.getMaxId()) {
						maxIdBB = bb;
					}
				}
			}
		}
		return maxIdBB;
	}

	@Override
	public String readItem(String id) {
		return this.bb.getArticle(Integer.parseInt(id)).toString();
	}

	private class ReadService extends Thread {

		Machine serverToRead;
		CountDownLatch latchToDecrement;
		byte[] dataRead;
		int articlesToReadFrom;

		ReadService(Machine serverToRead, CountDownLatch latchToDecrement,
				int articlesToReadFrom) {
			this.serverToRead = serverToRead;
			this.latchToDecrement = latchToDecrement;
			this.articlesToReadFrom = articlesToReadFrom;
		}

		@Override
		public void run() {
			try {
				String command = null;
				if (articlesToReadFrom == -1) {
					// cannot append -1 in the command as it messes with the
					// command parameter separator logic
					// read latest
					command = REQUEST_INTERNAL_SERVER.READ_LATEST_ITEM.name();

				} else {
					// read from articleId
					StringBuilder commandBuilder = new StringBuilder(
							REQUEST_INTERNAL_SERVER.READ_ITEMS_FROM_ID.name());

					commandBuilder.append(COMMAND_PARAM_SEPARATOR);
					commandBuilder.append(articlesToReadFrom);
					command = commandBuilder.toString();

				}
				logger.info("command being sent from the ReadService is "
						+ command);
				dataRead = TCPClient.sendData(this.serverToRead,
						Utils.stringToByte(command));
			} catch (IOException e) {
				logger.error("ReadServiceError", e);
			}
			logger.info(String.format("dataRead =%s from server = %s",
					Utils.byteToString(dataRead), serverToRead));
			latchToDecrement.countDown();
		}

	}

	private class WriteService extends Thread {

		Machine serverToWrite;
		Article articleToWrite = null;
		CountDownLatch latchToDecrement;
		byte[] dataRead;

		WriteService(Machine serverToRead, Article aToWrite,
				CountDownLatch latchToDecrement) {
			this.serverToWrite = serverToRead;
			this.latchToDecrement = latchToDecrement;
			this.articleToWrite = aToWrite;
		}

		@Override
		public void run() {
			try {
				String command = REQUEST_INTERNAL_SERVER.WRITE_ARTICLE + "-"
						+ articleToWrite.toString();
				dataRead = TCPClient.sendData(this.serverToWrite,
						Utils.stringToByte(command, Props.ENCODING));
			} catch (IOException e) {
				logger.error("WriteServiceError", e);
			}
			logger.info(String.format("dataRead =%s from server = %s",
					Utils.byteToString(dataRead), serverToWrite));
			latchToDecrement.countDown();
		}
	}

	/**
	 * Requests that I can answer to
	 * 
	 * <pre>
	 * 1. read: Need to return my bb state after the mentioned id : GET_BB-FROM_ID=<id> 
	 * 2. write: Need to write the article to my BB and return response : WRITE_COMMAND-<articleToString>
	 * </pre>
	 */
	@Override
	public byte[] handleSpecificRequest(String request) {
		String[] reqBrokenOnCommandParamSeparator = request
				.split(COMMAND_PARAM_SEPARATOR);
		logger.info("$$$$$$$$$$$$Message received at quorumServer"
				+ reqBrokenOnCommandParamSeparator);
		if (request.startsWith(CLIENT_REQUEST.READ_ITEMS.name())) {
			/**
			 * Need to return to the client the local bb which is appended with
			 * the latest article written across the read quorum
			 */
			mergeLatestArticleFromQuorum();
			return parseBytesFromArticleListWithCommandPrefix(
					COMMAND_SUCCESS,
					this.bb.getAllArticles());
		} else if (request.startsWith(CLIENT_REQUEST.READ_ITEM.name())) {
			/**
			 * FORMAT: READ_ITEM-<int>
			 */
			String intStr = request
					.substring((CLIENT_REQUEST.READ_ITEM.name() + COMMAND_PARAM_SEPARATOR)
							.length());
			int articleToRead = Integer.parseInt(intStr);

			Article aRead = this.bb.getArticle(articleToRead);

			StringBuilder response = new StringBuilder(
					COMMAND_SUCCESS);
			response.append(COMMAND_PARAM_SEPARATOR).append(
					aRead != null ? aRead : "");
			return Utils.stringToByte(response.toString());
		} else if (request.startsWith(CLIENT_REQUEST.POST.name())) {
			/**
			 * FORMAT: POST-AID=<int>
			 */
			return Utils
					.stringToByte(post(request.substring((CLIENT_REQUEST.POST
							.name() + COMMAND_PARAM_SEPARATOR).length())));
		}
		// internal server request
		else if (request.startsWith(REQUEST_INTERNAL_SERVER.WRITE_ARTICLE
				.name())) {
			// write local
			return Utils.stringToByte(write(reqBrokenOnCommandParamSeparator[1]));
		} else if (request.startsWith(REQUEST_INTERNAL_SERVER.READ_LATEST_ITEM
				.name())) {
			return Utils.stringToByte(COMMAND_SUCCESS + COMMAND_PARAM_SEPARATOR
					+ this.bb.getMaxArticle());
		} else if (request
				.startsWith(REQUEST_INTERNAL_SERVER.READ_ITEMS_FROM_ID.name())) {
			/**
			 * FORMAT: READ_ITEMS_FROM_ID-<int>
			 */
			String articlesToReadFromStr = request
					.substring((REQUEST_INTERNAL_SERVER.READ_ITEMS_FROM_ID
							.name() + COMMAND_PARAM_SEPARATOR).length());
			int articlesToReadFrom = Integer.parseInt(articlesToReadFromStr);

			return parseBytesFromArticleListWithCommandPrefix(COMMAND_SUCCESS,
					this.bb.getArticlesFrom(articlesToReadFrom));

		}
		return Utils.stringToByte(INVALID_COMMAND);

	}

	public void mergeLatestArticleFromQuorum() {
		try {
			HashMap<Machine, String> responseMap = getLatestArticleFromReadQuorum();
			mergeResponsesWithLocal(responseMap);
		} catch (IOException e) {
			logger.error("Maybe the coordinator has died", e);
			this.stop();
		}

	}

	private HashMap<Machine, String> getLatestArticleFromReadQuorum()
			throws IOException {
		HashSet<Machine> successfulServers = new HashSet<Machine>();
		HashSet<Machine> failedServers = new HashSet<Machine>();
		HashMap<Machine, String> responseMap = new HashMap<Machine, String>();
		do {
			try {
				populateReadQuorum(successfulServers, failedServers);
				executeReadRequestOnReadQuorum(responseMap, successfulServers,
						failedServers, -1);
			} catch (IOException e) {
				logger.error("quorum population failed", e);
				throw e;
			}
			waitIfFailedServers(failedServers);
		} while (failedServers.size() > 0);
		return responseMap;
	}

	private byte[] parseBytesFromArticleListWithCommandPrefix(
			String commandPrefix, List<Article> articles) {

		StringBuilder sb = new StringBuilder(commandPrefix);
		sb.append(COMMAND_PARAM_SEPARATOR);
		for (Article a : articles) {
			sb.append(a.toString()).append(LIST_SEPARATOR);
		}
		return Utils.stringToByte(sb.toString());
	}

	@Override
	protected Coordinator createCoordinator() {
		return new QuorumCoordinator();
	}

	private class SyncThread extends Thread {
		boolean shutDownInvoked = false;
		Object lockObj = null;
		long totalTimeToSleep = 0;
		boolean syncTookPlace = false;

		public SyncThread(Object lock, long timeToSleep) {
			this.lockObj = lock;
			this.totalTimeToSleep = timeToSleep;
		}

		public void resetTimer() {
			syncTookPlace = true;
		}

		void invokeShutdown() {
			logger.info("shutdown invoked");
			this.shutDownInvoked = true;
		}

		public void run() {
			while (!shutDownInvoked) {
				try {
					synchronized (this.lockObj) {
						this.lockObj.wait(this.totalTimeToSleep);
					}
					logger.info(String
							.format("Woke up to sync;syncTookPlace=%s,shutDownInvoked=%s,",
									syncTookPlace, shutDownInvoked));
					if (syncTookPlace) {
						syncTookPlace = false;
						continue;
					} else if (shutDownInvoked) {
						break;
					} else {
						syncBB();
					}
				} catch (InterruptedException e) {
					if (!shutDownInvoked) {
						logger.error("Error in syncBB", e);
					}
				}
			}
		}
	}

	protected void preRegister() throws Exception {
		super.preRegister();
		logger.info("Pre-registered and now syncing");
		syncBB();
	}
	protected void postRegister() throws IOException {
		super.postRegister();
		// start the sync thread
		//syncBB();
		/**
		 * before we are ready to take requests we need to get upto with all the
		 * known clients hence we need to initiate the sync, but we need to use
		 * the knownClients
		 */
		syncThread.start();

	}

	protected void postUnRegister() {
		// need to shutdown the TCPServer / TCP Client
		super.postUnRegister();
		stop();
	}

	public static void main(String[] args) {
		/**
		 * What all paramters are needed
		 * 
		 */
		Props.loadProperties(args[0]);
		boolean isCoordinator = Boolean.parseBoolean(args[1]);
		String coordinatorIP = args[2];
		int coordinatorPort = Integer.parseInt(args[3]);
		QuorumServer qs = new QuorumServer(isCoordinator, coordinatorIP,
				coordinatorPort);
		try {
			qs.start();
			/*
			 * Article a = new Article(0, 0, "t1", "c1"); qs.post(a.toString());
			 */

			/**
			 * Try to post articles which are in the the config file
			 */
			qs.testPostArticles(Props.TEST_ARTICLES_TO_POPULATE);
			// System.out.println(qs.readItemList("0"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void testPostArticles(String articlesToPublish) {
		if (!Utils.isEmpty(articlesToPublish)) {
			/**
			 * need to parse articlesToPublish and get list of articles to
			 * publish
			 */
			String[] brokenArticleStrings = articlesToPublish
					.split(COMMAND_PARAM_SEPARATOR);
			for (String articleStr : brokenArticleStrings) {
				// this is publishing to the quorum
				logger.info("$$$$$$$$$$$$$$$$$Test article publishing to the quorum; article = "
						+ articleStr);
				this.post(articleStr);
			}
		}
	}

	public void stop() {
		super.stop();
		this.externalTcpServer.stop();
		this.syncThread.invokeShutdown();
		this.syncThread.interrupt();
	}
}
