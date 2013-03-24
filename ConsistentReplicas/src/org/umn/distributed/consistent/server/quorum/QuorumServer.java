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

public class QuorumServer extends ReplicaServer {
	
	private static final String READ_BB_COMMAND_COMPLETE = READ_QUORUM_COMMAND
			+ "-FROM_ID=%%ARTICLE_ID%%";
	private Integer lastSyncedId;
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
		 * with merged bb : shadow copying in a syncronized method
		 * 
		 */
		HashMap<Machine, String> responseMap = getBBFromReadQuorum();
		// merge stage
		mergeResponsesWithLocal(responseMap);
		// doing this instead of calling sync as we want to wait on this
		// operation
		lastSyncedId = this.bb.getMaxId();
	}

	private void mergeResponsesWithLocal(HashMap<Machine, String> responseMap) {
		BulletinBoard mergedBulletinBoard = null;
		boolean first = true;
		for (Map.Entry<Machine, String> machineBBStr : responseMap.entrySet()) {
			// get BB from string
			if (first) {
				mergedBulletinBoard = BulletinBoard
						.parseBBFromArticleList(machineBBStr.getValue());
				first = false;
			} else {
				BulletinBoard bbThis = BulletinBoard
						.parseBBFromArticleList(machineBBStr.getValue());
				// now merge
				mergedBulletinBoard = BulletinBoard.mergeBB(
						mergedBulletinBoard, bbThis);
			}

		}
		// once the bulletinBoard is merged, we can just replace
		this.bb.mergeWithMe(mergedBulletinBoard);

	}

	private void validateParameters() {

	}

	public String write(String message) {
		// this is local write for me
		Article a = Article.parseArticle(message);
		if (a != null) {
			if (a.isRoot()) {
				this.bb.addArticle(a);
			} else {
				this.bb.addArticleReply(a);
			}
		}
		return null;
	}

	@Override
	public String post(String message) {
		Article aToWrite = Article.parseArticle(message);
		// need to get the article Id from the coordinator
		HashSet<Machine> successfulServers = new HashSet<Machine>();
		HashSet<Machine> failedServers = new HashSet<Machine>();
		HashMap<Machine, Boolean> writeStatus = new HashMap<Machine, Boolean>();
		int articleId = 0;
		do {
			try {
				articleId = populatWriteQuorum(articleId, successfulServers,
						failedServers);
				aToWrite.setId(articleId);
				executeWriteRequestOnWriteQuorum(writeStatus,
						successfulServers, failedServers, aToWrite);
			} catch (IOException e) {
				logger.error(
						"Error in populating the quorum, no option but to try again",
						e);
			}

		} while (failedServers.size() > 0);

		// once write is done just return
		return aToWrite.toString();

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
			if(failedServers.contains(myInfo)){
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
					if (str.equals(COMMAND_SUCCESS)) {
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

	private int populatWriteQuorum(Integer articleId,
			HashSet<Machine> successfulServers, HashSet<Machine> failedServers)
			throws IOException {
		return CoordinatorClientCallFormatter.getArticleIdWithWriteQuorum(
				this.myInfo, this.coordinatorMachine, articleId,
				successfulServers, failedServers);
	}

	@Override
	public String readItemList(String req) {

		HashMap<Machine, String> responseMap = getBBFromReadQuorum();

		// once we have populated and know that required quorum was achieved

		mergeResponsesWithLocal(responseMap);
		syncThread.resetTimer();
		return bb.toString();
	}

	private HashMap<Machine, String> getBBFromReadQuorum() {
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
				populatReadQuorum(successfulServers, failedServers);
			} catch (IOException e) {
				logger.error("quorum popualtion failed", e);
			}
			executeReadRequestOnReadQuorum(responseMap, successfulServers,
					failedServers);
		} while (failedServers.size() > 0);
		return responseMap;
	}

	private void executeReadRequestOnReadQuorum(
			HashMap<Machine, String> responseMap,
			HashSet<Machine> successfulServers, HashSet<Machine> failedServers) {

		if (failedServers != null) {
			List<ReadService> threadsToRead = new ArrayList<ReadService>(
					failedServers.size());
			final CountDownLatch readQuorumlatch = new CountDownLatch(
					failedServers.size());

			for (Machine server : failedServers) {
				ReadService t = new ReadService(server, readQuorumlatch);
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

	private void populatReadQuorum(HashSet<Machine> successfulServers,
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
		/*
		 * Assemble the quorum and then
		 */

		return this.bb.getArticle(Integer.parseInt(id)).toString();
	}

	private class ReadService extends Thread {
		Machine serverToRead;
		CountDownLatch latchToDecrement;
		byte[] dataRead;

		ReadService(Machine serverToRead, CountDownLatch latchToDecrement) {
			this.serverToRead = serverToRead;
			this.latchToDecrement = latchToDecrement;
		}

		@Override
		public void run() {
			try {
				dataRead = TCPClient
						.sendData(this.serverToRead, Utils
								.stringToByte(READ_BB_COMMAND_COMPLETE
										.replaceAll("%%ARTICLE_ID%%",
												String.valueOf(lastSyncedId))));
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
				String command = WRITE_QUORUM_COMMAND + "-"
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
		String[] req = request.split(COMMAND_PARAM_SEPARATOR);
		if (request.startsWith(READ_QUORUM_COMMAND)) {
			// need to get bb converted to string from a specific id
			int lastSyncArticleId = 0;
			String[] arrStr = req[1].split("=");
			lastSyncArticleId = Integer.parseInt(arrStr[1]);
			List<Article> articles = this.bb.getArticlesFrom(lastSyncArticleId);
			return parseBytesFromArticleList(articles);
		} else if (request.startsWith(WRITE_QUORUM_COMMAND)) {
			write(req[1]);
			return Utils.stringToByte(COMMAND_SUCCESS);
			// write local

		}
		return null;

	}

	private byte[] parseBytesFromArticleList(List<Article> articles) {
		StringBuilder sb = new StringBuilder();
		for (Article a : articles) {
			sb.append(a.toString()).append(";");
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
			this.shutDownInvoked = true;
		}

		public void run() {
			while (!shutDownInvoked) {
				try {
					synchronized (this.lockObj) {
						this.lockObj.wait(this.totalTimeToSleep);
					}
					if (syncTookPlace) {
						syncTookPlace = false;
						continue;
					} else {
						syncBB();
					}
				} catch (InterruptedException e) {
					logger.error("Error in syncBB", e);
				}
			}
		}
	}

	protected void postRegister() {
		// start the sync thread
		syncThread.start();
	}

	protected void postUnRegister() {
		this.syncThread.invokeShutdown();
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
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Article a = new Article(0, 0, "t1", "c1");
		qs.post(a.toString());
	}
}
