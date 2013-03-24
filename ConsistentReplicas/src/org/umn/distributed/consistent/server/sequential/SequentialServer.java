package org.umn.distributed.consistent.server.sequential;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.umn.distributed.consistent.common.Article;
import org.umn.distributed.consistent.common.Machine;
import org.umn.distributed.consistent.common.Props;
import org.umn.distributed.consistent.common.TCPClient;
import org.umn.distributed.consistent.common.Utils;
import org.umn.distributed.consistent.server.ReplicaServer;
import org.umn.distributed.consistent.server.TCPServer.Handler;
import org.umn.distributed.consistent.server.coordinator.Coordinator;

public class SequentialServer extends ReplicaServer {

	private ExecutorService executerService = Executors.newFixedThreadPool(Props.WRITER_SERVER_THREADS);

	public SequentialServer(boolean isCoordinator, String coordinatorIP,
			int coordinatorPort) {
		super(STRATEGY.SEQUENTIAL, isCoordinator, coordinatorIP,
				coordinatorPort);
	}
	
	@Override
	public String post(String req) {
		String command = INTERNAL_WRITE_COMMAND + "-" + req.toString();
		try {
			byte[] dataRead = TCPClient.sendData(this.coordinatorMachine,
					Utils.stringToByte(command, Props.ENCODING));
			return Utils.byteToString(dataRead, Props.ENCODING);
		} catch (IOException ioe) {
			logger.error("Coordinator not able to write", ioe);
			return COMMAND_FAILED + COMMAND_PARAM_SEPARATOR
					+ "Coordinator returned error: " + ioe;
		}
	}

	@Override
	public String readItemList(String req) {
		return COMMAND_SUCCESS + COMMAND_PARAM_SEPARATOR
				+ this.bb.toShortString();
	}

	@Override
	public String readItem(String id) {
		try {
			int articleId = Integer.parseInt(id);
			Article article = this.bb.getArticle(articleId);
			if (article != null) {
				return COMMAND_SUCCESS + COMMAND_PARAM_SEPARATOR
						+ article.toString();
			}
		} catch (NumberFormatException nfe) {
			logger.error("Invalid article id format: " + id, nfe);
		}
		return COMMAND_FAILED + COMMAND_PARAM_SEPARATOR + "Invalid article id";
	}

	@Override
	public String write(String req) {
		Article article = Article.parseArticle(req);
		boolean result = false;

		if (this.coordinator) {
			HashMap<Machine, Boolean> writeStatus = new HashMap<Machine, Boolean>();
			writeToReplicas(writeStatus, getMachineList(), article);
		}
		if (article.isRoot()) {
			result = this.bb.addArticle(article);
		} else {
			result = this.bb.addArticleReply(article);
		}
		if (result) {
			return COMMAND_SUCCESS;
		}
		return COMMAND_FAILED + "-" + "Failed writing the article";
	}

	private boolean writeToReplicas(HashMap<Machine, Boolean> writeStatus, List<Machine> failedServers, Article article) {
		final CountDownLatch writelatch = new CountDownLatch(
				failedServers.size());
		List<Machine> serverList = getMachineList();
		for (Machine server : failedServers) {
			WriteService t = new WriteService(server, aToWrite,
					writelatch);
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

	@Override
	public byte[] handleSpecificRequest(String request) {
		if (request.startsWith(INTERNAL_WRITE_COMMAND)) {
			return Utils
					.stringToByte(
							write(request
									.substring((INTERNAL_WRITE_COMMAND + COMMAND_PARAM_SEPARATOR)
											.length())), Props.ENCODING);
		} else if (request.startsWith(WRITE_COMMAND)) {
			return Utils.stringToByte(post(request
					.substring((WRITE_COMMAND + COMMAND_PARAM_SEPARATOR)
							.length())), Props.ENCODING);
		} else if (request.startsWith(READ_COMMAND)) {
			return Utils.stringToByte(write(request
					.substring((READ_COMMAND + COMMAND_PARAM_SEPARATOR)
							.length())), Props.ENCODING);
		} else if (request.startsWith(READITEM_COMMAND)) {
			return Utils.stringToByte(write(request
					.substring((READITEM_COMMAND + COMMAND_PARAM_SEPARATOR)
							.length())), Props.ENCODING);
		}
		return Utils.stringToByte(INVALID_COMMAND, Props.ENCODING);
	}

	@Override
	protected Coordinator createCoordinator() {
		return new SequentialCoordinator();
	}

	private class WriteService extends Thread {
		Machine serverToWrite;
		Article articleToWrite = null;
		CountDownLatch latchToDecrement;
		byte[] dataRead;

		WriteService(Machine serverToWrite, Article aToWrite,
				CountDownLatch latchToDecrement) {
			this.serverToWrite = serverToWrite;
			this.latchToDecrement = latchToDecrement;
			this.articleToWrite = aToWrite;
		}

		@Override
		public void run() {
			try {
				String command = WRITE_COMMAND + "-"
						+ articleToWrite.toString();
				dataRead = TCPClient.sendData(this.serverToWrite,
						Utils.stringToByte(command, Props.ENCODING));
			} catch (IOException e) {
				logger.error("Cannot write to " + serverToWrite.toString(), e);
			}
			logger.info(String.format("dataRead =%s from server = %s",
					dataRead, serverToWrite));
			latchToDecrement.countDown();
		}
	}
}
