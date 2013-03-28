package org.umn.distributed.consistent.server.sequential;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.umn.distributed.consistent.server.quorum.CommandCentral.CLIENT_REQUEST;

public class SequentialServer extends ReplicaServer {
	private Object obj = new Object();

	public SequentialServer(boolean isCoordinator, String coordinatorIP,
			int coordinatorPort) {
		super(STRATEGY.SEQUENTIAL, isCoordinator, coordinatorIP,
				coordinatorPort);
	}

	@Override
	protected void postRegister() throws IOException {
		super.postRegister();
		if (this.coordinator) {
			((SequentialCoordinator) this.coordinatorServer)
					.setLocalReplica(this);
		}
	}

	@Override
	public String post(String req) {
		try {
			if (this.coordinator) {
				logger.debug(this.myInfo + " handling posted message " + req
						+ " as primary");
				int id = ((SequentialCoordinator) this.coordinatorServer)
						.getId();
				Article article = Article.parseArticle(req);
				article.setId(id);
				return writeAsPrimary(article);
			} else {
				logger.debug(this.myInfo + " sending posted message " + req
						+ " to primary " + this.coordinatorMachine);
				String command = SequentialCoordinator.WRITE_COMMAND + "-"
						+ req.toString();
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

	// TODO: no parameter required because we always pass all the articles
	@Override
	public String readItemList(String req) {
		logger.debug("Read itemlist starting with " + req + " on "
				+ this.myInfo);
		return COMMAND_SUCCESS + COMMAND_PARAM_SEPARATOR
				+ this.bb.toShortString();
	}

	@Override
	public String readItem(String id) {
		logger.debug("Read item with " + id + " on " + this.myInfo);
		try {
			int articleId = Integer.parseInt(id);
			Article article = this.bb.getArticle(articleId);
			if (article != null) {
				return COMMAND_SUCCESS + COMMAND_PARAM_SEPARATOR + article;
			}
		} catch (NumberFormatException nfe) {
			logger.error("Invalid article id format: " + id, nfe);
		}
		return COMMAND_FAILED + COMMAND_PARAM_SEPARATOR + "Invalid article id";
	}

	public String writeAsPrimary(Article article) {
		logger.debug(this.myInfo + " writing " + article + " as primary");
		HashMap<Machine, Boolean> writeStatus = new HashMap<Machine, Boolean>();
		Set<Machine> set = getMachineList();
		if (writeToReplicas(writeStatus, set, article)) {
			localWrite(article);
			return COMMAND_SUCCESS + COMMAND_PARAM_SEPARATOR + article.getId();
		}
		return COMMAND_FAILED + "-" + "Failed writing the article to replicas";
	}

	public String localWrite(Article article) {
		boolean result = false;
		synchronized (obj) {
			while ((this.bb.getMaxId() + 1) < article.getId()) {
				try {
					logger.debug("Server will wait for the turn of "
							+ article.getId());
					obj.wait();
					logger.debug("Server will now commit id: "
							+ article.getId());
				} catch (InterruptedException e) {
					logger.error("Writer thread interrupted", e);
				}
			}
			// Write only if the last entry written has the id one less than
			// this
			if (this.bb.getMaxId() + 1 == article.getId()) {
				if (article.isRoot()) {
					result = this.bb.addArticle(article);
				} else {
					result = this.bb.addArticleReply(article);
				}
			}
			obj.notifyAll();
		}
		if (result) {
			return COMMAND_SUCCESS + COMMAND_PARAM_SEPARATOR + article.getId()
					+ " written";
		}
		return COMMAND_FAILED + "-" + "Failed writing the article";
	}

	@Override
	public String write(String req) {
		logger.debug("Request to write article " + req + " on " + this.myInfo);
		Article article = Article.parseArticle(req);
		return localWrite(article);
	}

	private boolean writeToReplicas(HashMap<Machine, Boolean> writeStatus,
			Set<Machine> failedServers, Article article) {
		logger.debug(this.myInfo + " writing " + article + " to replicas");
		if (failedServers.size() == 0) {
			logger.info("No replica to write. Primary will write local only");
		}
		final CountDownLatch writelatch = new CountDownLatch(
				failedServers.size());
		Set<Machine> serverSet = getMachineList();
		for (Machine machine : failedServers) {
			if (!serverSet.contains(machine)) {
				logger.info(machine
						+ " won't be used for writing. Not found in known server list");
				failedServers.remove(machine);
			}
		}
		List<WriterThread> threadsToWrite = new ArrayList<WriterThread>(
				failedServers.size());
		logger.debug(this.myInfo + " will write " + article + " to "
				+ failedServers.size() + " replicas");
		for (Machine machine : failedServers) {
			logger.debug("Trying to write " + article + " to " + machine);
			WriterThread writer = new WriterThread(machine, article, writelatch);
			threadsToWrite.add(writer);
			writer.start();
		}
		try {
			logger.debug("Start waiting on latch for threads to write");
			if (writelatch.await(Props.NETWORK_TIMEOUT, TimeUnit.MILLISECONDS)) {
				logger.info("Finished writing to all the threads");
			} else {
				logger.info("Latch timed out while writing to replicas");
			}
			for (WriterThread wr : threadsToWrite) {
				if (wr.isAlive()) {
					wr.interrupt();
				}
				if (wr.dataRead != null) {
					String str = Utils.byteToString(wr.dataRead);
					if (str.startsWith(COMMAND_SUCCESS)) {
						logger.info("Written " + wr.articleToWrite + " to "
								+ wr.serverToWrite);
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
			if (threadsToWrite != null) {
				for (WriterThread thread : threadsToWrite) {
					thread.interrupt();
				}
			}
		}
		if (failedServers.size() > 0) {
			logger.info(failedServers + " failed to write " + article
					+ ". Will again for this servers");
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
					.stringToByte(write(request
							.substring((INTERNAL_WRITE_COMMAND + COMMAND_PARAM_SEPARATOR)
									.length())));
		} else if (request.startsWith(CLIENT_REQUEST.POST.name())) {
			return Utils
					.stringToByte(post(request.substring((CLIENT_REQUEST.POST
							.name() + COMMAND_PARAM_SEPARATOR).length())));
		} else if (request.startsWith(CLIENT_REQUEST.READ_ITEMS.name())) {
			// TODO: not needed because no id is being passed now
			return Utils
					.stringToByte(readItemList(request
							.substring((CLIENT_REQUEST.READ_ITEMS.name() + COMMAND_PARAM_SEPARATOR)
									.length())));
		} else if (request.startsWith(CLIENT_REQUEST.READ_ITEM.name())) {
			return Utils
					.stringToByte(readItem(request
							.substring((CLIENT_REQUEST.READ_ITEM.name() + COMMAND_PARAM_SEPARATOR)
									.length())));
		}
		return Utils.stringToByte(INVALID_COMMAND, Props.ENCODING);
	}

	@Override
	protected Coordinator createCoordinator() {
		return new SequentialCoordinator();
	}

	public void stop() {
		super.stop();
		this.externalTcpServer.stop();
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
						Utils.stringToByte(command));
			} catch (IOException e) {
				logger.error("Cannot write to " + serverToWrite, e);
			}
			latchToDecrement.countDown();
		}
	}
}
