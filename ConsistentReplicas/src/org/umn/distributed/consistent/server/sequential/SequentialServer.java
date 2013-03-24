package org.umn.distributed.consistent.server.sequential;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.umn.distributed.consistent.common.Article;
import org.umn.distributed.consistent.common.Machine;
import org.umn.distributed.consistent.common.Props;
import org.umn.distributed.consistent.common.TCPClient;
import org.umn.distributed.consistent.common.Utils;
import org.umn.distributed.consistent.server.ReplicaServer;
import org.umn.distributed.consistent.server.coordinator.Coordinator;

public class SequentialServer extends ReplicaServer {

	public SequentialServer(boolean isCoordinator, String coordinatorIP,
			int coordinatorPort) {
		super(STRATEGY.SEQUENTIAL, isCoordinator, coordinatorIP,
				coordinatorPort);
	}

	@Override
	public String post(String req) {
		try {
			if (this.coordinator) {
				int id = ((SequentialCoordinator) this.coordinatorServer)
						.getId();
				Article article = Article.parseArticle(req);
				article.setId(id);
				return writeAsPrimary(article);
			} else {
				String command = SequentialCoordinator.WRITE_COMMAND + "-" + req.toString();
				byte[] dataRead = TCPClient.sendData(this.coordinatorMachine,
						Utils.stringToByte(command));
				return Utils.byteToString(dataRead);
			}
		} catch (IOException ioe) {
			logger.error("Coordinator not able to write", ioe);
			return COMMAND_FAILED + COMMAND_PARAM_SEPARATOR
					+ "Coordinator returned error: " + ioe;
		}
	}

	//TODO: no parameter required because we always pass all the articles
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

	public String writeAsPrimary(Article article) {
		HashMap<Machine, Boolean> writeStatus = new HashMap<Machine, Boolean>();
		Set<Machine> set = getMachineList();
		set.remove(myInfo);
		if(writeToReplicas(writeStatus, set, article)) {
			return COMMAND_SUCCESS
					+ COMMAND_PARAM_SEPARATOR + article.getId();
		}
		return COMMAND_FAILED + "-" + "Failed writing the article to replicas";
	}

	@Override
	public String write(String req) {
		logger.debug("Request to write article " + req + " on " + this.myInfo);
		Article article = Article.parseArticle(req);
		boolean result = false;
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

	private boolean writeToReplicas(HashMap<Machine, Boolean> writeStatus,
			Set<Machine> failedServers, Article article) {
		final CountDownLatch writelatch = new CountDownLatch(
				failedServers.size());
		Set<Machine> serverList = getMachineList();
		List<WriterThread> threadsToWrite = new ArrayList<WriterThread>(
				failedServers.size());
		Iterator<Machine> it = failedServers.iterator();
		while (it.hasNext()) {
			Machine machine = it.next();
			if (serverList.contains(machine)) {
				WriterThread writer = new WriterThread(machine, article,
						writelatch);
				threadsToWrite.add(writer);
				writer.start();
			}
		}
		try {
			writelatch.await(Props.NETWORK_TIMEOUT, TimeUnit.MILLISECONDS);
			for (WriterThread wr : threadsToWrite) {
				if (wr.isAlive()) {
					wr.interrupt();
				}
				if (wr.dataRead != null) {
					String str = Utils
							.byteToString(wr.dataRead, Props.ENCODING);
					if (str.equals(COMMAND_SUCCESS)) {
						writeStatus.put(wr.serverToWrite, true);
						failedServers.remove(wr.serverToWrite);
					}
					// TODO:handle this condition. If you are not able to write
					// to all the threads then this hangs
					// and keeps trying until it can write to all the threads.
					// else {
					// wr.dataRead = null;
					// }
				}
			}
		} catch (InterruptedException ie) {
			logger.error("Error waiting in writer latch", ie);
			// interrupt all other threads
			// TODO other servers should kill
		}
		if (failedServers.size() > 0) {
			writeToReplicas(writeStatus, failedServers, article);
		}
		// TODO: Don't know how to process the error and do the error handling.
		// We assume
		// it will never fail.
		return true;
	}

	@Override
	public byte[] handleSpecificRequest(String request) {
		if (request.startsWith(INTERNAL_WRITE_COMMAND)) {
			return Utils
					.stringToByte(
							write(request
									.substring((INTERNAL_WRITE_COMMAND + COMMAND_PARAM_SEPARATOR)
											.length())));
		} else if (request.startsWith(WRITE_COMMAND)) {
			return Utils.stringToByte(post(request
					.substring((WRITE_COMMAND + COMMAND_PARAM_SEPARATOR)
							.length())));
		} else if (request.startsWith(READ_COMMAND)) {
			//TODO: not needed because no id is being passed now
			return Utils.stringToByte(readItemList(request
					.substring((READ_COMMAND + COMMAND_PARAM_SEPARATOR)
							.length())));
		} else if (request.startsWith(READITEM_COMMAND)) {
			return Utils.stringToByte(readItem(request
					.substring((READITEM_COMMAND + COMMAND_PARAM_SEPARATOR)
							.length())));
		}
		return Utils.stringToByte(INVALID_COMMAND, Props.ENCODING);
	}

	@Override
	protected Coordinator createCoordinator() {
		return new SequentialCoordinator();
	}

	private class WriterThread extends Thread {
		Machine serverToWrite;
		Article articleToWrite = null;
		CountDownLatch latchToDecrement;
		byte[] dataRead;

		WriterThread(Machine serverToWrite, Article aToWrite,
				CountDownLatch latchToDecrement) {
			this.serverToWrite = serverToWrite;
			this.latchToDecrement = latchToDecrement;
			this.articleToWrite = aToWrite;
		}

		@Override
		public void run() {
			try {
				String command = INTERNAL_WRITE_COMMAND + "-"
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
