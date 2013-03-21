package org.umn.distributed.consistent.server.quorum;

import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.umn.distributed.consistent.common.Article;
import org.umn.distributed.consistent.common.BulletinBoard;
import org.umn.distributed.consistent.common.Machine;
import org.umn.distributed.consistent.common.TCPClient;
import org.umn.distributed.consistent.common.Utils;
import org.umn.distributed.consistent.server.AbstractServer;
import org.umn.distributed.consistent.server.ReplicaServer;
import org.umn.distributed.consistent.server.ReplicaServer.STRATEGY;

public class QuorumServer extends ReplicaServer {
	private static final byte[] READ_LIST_COMMAND = null;
	private static final int NETWORK_TIMEOUT = 100;
	private Logger logger = Logger.getLogger(this.getClass());
	private static final String WRITE_COMMAND = "%%ARTICLE%%";
	private static final String ENCODING = "UTF8";

	public QuorumServer(STRATEGY strategy, String coordinatorIP,
			int coordinatorPort) {
		super(strategy, coordinatorIP, coordinatorPort);
		validateParameters();
	}

	private void validateParameters() {

	}

	/*
	 * (non-Javadoc) This server will contact the coordinator to get the latest
	 * id
	 * 
	 * @see org.umn.distributed.consistent.common.AbstractServer#write()
	 */
	@Override
	public boolean write() {
		// TODO Auto-generated method stub
		return false;
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
			populatWriteQuorum(articleId, successfulServers, failedServers);
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
				writeQuorumlatch.await(NETWORK_TIMEOUT, TimeUnit.SECONDS);
				// TODO add a timeout and then fail the operation
				for (WriteService rs : threadsToWrite) {
					String str = null;
					try {
						str = Utils.convertByteToString(rs.dataRead,
								rs.dataRead.length, 0, "UTF8");
						if (str.equals(WRITE_SUCCESS)) {
							writeStatus.put(rs.serverToRead, true);
						} else {
							// basically failed
							rs.dataRead = null;
						}

					} catch (UnsupportedEncodingException e) {
						logger.error("str null due to encoding exception", e);
						rs.dataRead = null;
						// this will not allow this server to be removed from
						// the failedservers

					}

				}
			} catch (InterruptedException ie) {
				logger.error("Error", ie);
				// interrupt all other threads
				// TODO other servers should kill
				for (ReadService rs : threadsToWrite) {
					if (rs.dataRead == null) {
						rs.interrupt();
					}
				}
			} finally {
				// if not thread has some value add it to the success servers
				// else let
				// it be in failed set
				for (ReadService rs : threadsToWrite) {
					if (rs.dataRead != null) {
						successfulServers.add(rs.serverToRead);
						failedServers.remove(rs.serverToRead);
					}
				}
			}

		}

	}

	private void populatWriteQuorum(Integer articleId,
			HashSet<Machine> successfulServers, HashSet<Machine> failedServers) {
		// TODO Auto-generated method stub

	}

	/**
	 * TODO make readItemList in a separate thread as we need to wait for a lot
	 * of servers to return results.
	 */
	@Override
	public String readItemList() {

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
			populatReadQuorum(successfulServers, failedServers);
			executeReadRequestOnReadQuorum(responseMap, successfulServers,
					failedServers);
		} while (failedServers.size() > 0);

		// once we have populated and know that required quorum was achieved

		// parse and create BB from each response

		List<BulletinBoard> boardsFromReadQuorum = convertResponseToBB(responseMap);

		// find out the maximum id replica and replace own BB with that, as we
		// need to show client
		// consistent view.

		BulletinBoard bb = getMaxIdBulletinBoard(boardsFromReadQuorum);

		// replace/merge own BB, race conditions
		/*
		 * If we wrote something just now, and we got a stale copy from the
		 * maximum server then we can loose some latest write and in effect make
		 * quorum inconsistent, hence we cannot ideally replace we need to
		 * merge.
		 */

		return bb.toString();
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
				readQuorumlatch.await(NETWORK_TIMEOUT, TimeUnit.SECONDS);
				// TODO add a timeout and then fail the operation
				for (ReadService rs : threadsToRead) {
					String str = null;
					try {
						str = Utils.convertByteToString(rs.dataRead,
								rs.dataRead.length, 0, "UTF8");
						responseMap.put(rs.serverToRead, str);
					} catch (UnsupportedEncodingException e) {
						logger.error("str null due to encoding exception", e);
						rs.dataRead = null;
						// this will not allow this server to be removed from
						// the failedservers

					}

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
			HashSet<Machine> failedServers) {
		// TODO Auto-generated method stub

	}

	private BulletinBoard getMaxIdBulletinBoard(
			List<BulletinBoard> boardsFromReadQuorum) {
		// TODO Auto-generated method stub
		return null;
	}

	private List<BulletinBoard> convertResponseToBB(
			HashMap<Machine, String> responseMap) {
		// TODO Auto-generated method stub
		return null;
	}

	private Socket getSocket(Machine server) {
		// TODO Auto-generated method stub
		return null;
	}

	private List<Machine> getReadQuorum(int numberOfServers) {
		return getQuorum(true, numberOfServers);
	}

	private List<Machine> getQuorum(boolean readQuorum, int numberOfServers) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String readItem(String id) {
		// TODO Auto-generated method stub
		return null;
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
			// TODO Auto-generated method stub
			dataRead = TCPClient.sendData(getSocket(this.serverToRead),
					READ_LIST_COMMAND);
			logger.info(String.format("dataRead =%s from server = %s",
					dataRead, serverToRead));
			latchToDecrement.countDown();
		}

	}

	private class WriteService extends Thread {

		Machine serverToRead;
		Article articleToWrite = null;
		CountDownLatch latchToDecrement;
		byte[] dataRead;

		WriteService(Machine serverToRead, Article aToWrite,
				CountDownLatch latchToDecrement) {
			this.serverToRead = serverToRead;
			this.latchToDecrement = latchToDecrement;
			this.articleToWrite = aToWrite;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			dataRead = TCPClient.sendData(
					getSocket(this.serverToRead),
					Utils.stringToByte(
							WRITE_COMMAND.format("%%ARTICLE%%",
									articleToWrite.toString()), ENCODING));
			logger.info(String.format("dataRead =%s from server = %s",
					dataRead, serverToRead));
			latchToDecrement.countDown();
		}

	}
}
