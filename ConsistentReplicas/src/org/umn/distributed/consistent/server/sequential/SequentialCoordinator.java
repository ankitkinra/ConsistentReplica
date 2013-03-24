package org.umn.distributed.consistent.server.sequential;

import org.umn.distributed.consistent.common.Article;
import org.umn.distributed.consistent.common.Utils;
import org.umn.distributed.consistent.server.coordinator.Coordinator;

public class SequentialCoordinator extends Coordinator {

	public static final String WRITE_COMMAND = "CWRITE";
	private SequentialServer server;

	public SequentialCoordinator() {
		super(STRATEGY.SEQUENTIAL);
	}

	public void setLocalReplica(SequentialServer server) {
		this.server = server;
	}

	public int getId() {
		return articleID.getAndIncrement();
	}
	
	@Override
	public byte[] handleSpecificRequest(String reqStr) {
		if (reqStr.startsWith(WRITE_COMMAND)) {
			String[] req = reqStr.split("-");
			Article article = Article.parseArticle(req[1]);
			int id = getId();
			article.setId(id);
			return Utils.stringToByte(this.server.writeAsPrimary(article));
		}
		return Utils.stringToByte(INVALID_COMMAND);
	}
}
