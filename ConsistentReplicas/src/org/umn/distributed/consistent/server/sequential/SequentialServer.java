package org.umn.distributed.consistent.server.sequential;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.umn.distributed.consistent.common.Article;
import org.umn.distributed.consistent.common.Machine;
import org.umn.distributed.consistent.common.Props;
import org.umn.distributed.consistent.common.TCPClient;
import org.umn.distributed.consistent.common.Utils;
import org.umn.distributed.consistent.server.ReplicaServer;
import org.umn.distributed.consistent.server.coordinator.Coordinator;

public class SequentialServer extends ReplicaServer {
	public SequentialServer(String coordinatorIP, int coordinatorPort) {
		super(STRATEGY.SEQUENTIAL, coordinatorIP, coordinatorPort);
	}

	@Override
	public String post(String message) {
		return null;
	}

	@Override
	public String readItemList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String readItem(String id) {
		return null;
	}

	@Override
	public String write(String message) {
		String req[] = message.split("-");
		Article article = Article.parseArticle(req[1]);
		boolean result = false;
		if (article.isRoot()) {
			result = this.bb.addArticle(article);
		} else {
			result = this.bb.addArticleReply(article);
		}
		if (result) {
			return COMMAND_SUCCESS;
		}
		return COMMAND_FAILED + "-" + "FAILED WRITING THE ARTICLE";
	}

	@Override
	public byte[] handleSpecificRequest(String request) {
		if (request.startsWith(WRITE_COMMAND)) {
			return Utils.stringToByte(
					write(request.substring((WRITE_COMMAND + "-").length())),
					Props.ENCODING);
		} else if (request.startsWith(READ_COMMAND)) {

		} else if (request.startsWith(READITEM_COMMAND)) {

		}
		return Utils.stringToByte(INVALID_COMMAND, Props.ENCODING);
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

	@Override
	protected Coordinator createCoordinator() {
		return new SequentialCoordinator();
	}
}
