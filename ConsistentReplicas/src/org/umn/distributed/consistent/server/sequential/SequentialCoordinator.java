package org.umn.distributed.consistent.server.sequential;

import java.io.IOException;

import org.umn.distributed.consistent.common.Article;
import org.umn.distributed.consistent.common.Machine;
import org.umn.distributed.consistent.common.Props;
import org.umn.distributed.consistent.common.TCPClient;
import org.umn.distributed.consistent.common.Utils;
import org.umn.distributed.consistent.server.coordinator.Coordinator;

public class SequentialCoordinator extends Coordinator {

	public static final String COMMAND_WRITE = "CWRITE";

	public SequentialCoordinator() {
		super(STRATEGY.SEQUENTIAL);
	}

	@Override
	public byte[] handleRequest(byte[] request) {
		String reqStr = Utils.byteToString(request, Props.ENCODING);
		if (reqStr.startsWith(COMMAND_WRITE)) {
			String[] req = reqStr.split("-");
			Article article = Article.parseArticle(req[1]);
			// TODO: write this to local and other replicas
		}
		return handleSpecificRequest(reqStr);
	}

	@Override
	public byte[] handleSpecificRequest(String str) {
		return null;
	}

	private class WriteService implements Runnable {
		private Machine replica;
		private Article article;

		WriteService(Machine replica, Article article) {
			this.replica = replica;
			this.article = article;
		}

		@Override
		public void run() {
			try {
				String command = WRITE_COMMAND + "-" + article.toString();
				byte dataRead[] = TCPClient.sendData(this.replica,
						Utils.stringToByte(command, Props.ENCODING));
				String resp = Utils.byteToString(dataRead, Props.ENCODING);
				if (resp.startsWith(COMMAND_SUCCESS)) {

				}
			} catch (IOException e) {
				logger.error("WriteServiceError", e);
			}
			// logger.info(String.format("dataRead =%s from server = %s",
			// resp, serverToWrite));
			// latchToDecrement.countDown();
		}
	}
}
