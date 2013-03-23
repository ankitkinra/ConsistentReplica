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
	private static final String READ_LIST_COMMAND = "GET_BB|FROM_ID=%%ARTICLE_ID%%";
	private Integer lastSyncedId;

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
		lastSyncedId = this.bb.getMaxId();
	}

	private void mergeResponsesWithLocal(HashMap<Machine, String> responseMap) {
		BulletinBoard mergedBulletinBoard = null;
		boolean first = true;
		for (Map.Entry<Machine, String> machineBBStr : responseMap.entrySet()) {
			// get BB from string
			if (first) {
				mergedBulletinBoard = BulletinBoard
						.parseBulletinBoard(machineBBStr.getValue());
				first = false;
			} else {
				BulletinBoard bbThis = BulletinBoard
						.parseBulletinBoard(machineBBStr.getValue());
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
			this.bb.addArticle(a);
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
		Integer articleId = -1;
		do {
			try {
				populatWriteQuorum(articleId, successfulServers, failedServers);
			} catch (IOException e) {
				logger.error(
						"Error in populating the quorum, no option but to try again",
						e);
			}
			executeWriteRequestOnWriteQuorum(writeStatus, successfulServers,
					failedServers, aToWrite);
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
				writeQuorumlatch.await(Props.NETWORK_TIMEOUT, TimeUnit.MILLISECONDS);
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
				for (WriteService wr : threadsToWrite) {
					if (wr.dataRead == null) {
						wr.interrupt();
					}
				}
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

	private void populatWriteQuorum(Integer articleId,
			HashSet<Machine> successfulServers, HashSet<Machine> failedServers)
			throws IOException {
		CoordinatorClientCallFormatter.getArticleIdWithWriteQuorum(
				this.coordinatorMachine, articleId, successfulServers,
				failedServers);
	}

	/**
	 * TODO make readItemList in a separate thread as we need to wait for a lot
	 * of servers to return results.
	 */
	@Override
	public String readItemList(String req) {

		HashMap<Machine, String> responseMap = getBBFromReadQuorum();

		// once we have populated and know that required quorum was achieved

		mergeResponsesWithLocal(responseMap);
		// TODO reset the thread which does sync as sync just happened
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
				readQuorumlatch.await(Props.NETWORK_TIMEOUT, TimeUnit.MILLISECONDS);
				// TODO add a timeout and then fail the operation
				for (ReadService rs : threadsToRead) {
					String str = null;

					str = Utils.byteToString(rs.dataRead, Props.ENCODING);
					responseMap.put(rs.serverToRead, str);

				}
			} catch (InterruptedException ie) {
				logger.error("Error", ie);
				// interrupt all other threads
				// TODO other servers should kill
				for (ReadService rs : threadsToRead) {
					if (rs.dataRead == null) {
						rs.interrupt();
					}
				}
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

	private void populatReadQuorum(HashSet<Machine> successfulServers,
			HashSet<Machine> failedServers) throws IOException {
		CoordinatorClientCallFormatter.getReadQuorum(coordinatorMachine,
				successfulServers, failedServers);

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
								.stringToByte(READ_LIST_COMMAND.replaceAll(
										"%%ARTICLE_ID%%",
										String.valueOf(lastSyncedId))));
			} catch (IOException e) {
				logger.error("ReadServiceError", e);
			}
			logger.info(String.format("dataRead =%s from server = %s",
					dataRead, serverToRead));
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
			// TODO Auto-generated method stub
			try {
				String command = WRITE_COMMAND + "-"
						+ articleToWrite.toString();
				dataRead = TCPClient.sendData(this.serverToWrite,
						Utils.stringToByte(command, Props.ENCODING));
			} catch (IOException e) {
				logger.error("WriteServiceError", e);
			}
			logger.info(String.format("dataRead =%s from server = %s",
					dataRead, serverToWrite));
			latchToDecrement.countDown();
		}
	}

	@Override
	public byte[] handleSpecificRequest(String request) {
		return null;
	}

	@Override
	protected Coordinator createCoordinator() {
		return new QuorumCoordinator();
	}
}
