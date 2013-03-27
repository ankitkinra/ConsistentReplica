package org.umn.distributed.consistent.server.quorum;

public class CommandCentral {
	/**
	 * <pre>
	 * a) Calls from the external client and their responses
	 * 		i) ReadItemList == returns the local bb appended latest Article retrieved from Quorum
	 * 		ii) ReadItem == returns the exact article requested by the client, as ReadItemList would be invoked 
	 * 						before this and we will have all the articles needed by the client which include the local replica 
	 * 						articles along with the max written article.
	 * 		iii) Post == when the client gives an article to be posted, need to assemble the write quorum [after asking coordinator] and get this written out to the quorum
	 * </pre>
	 * 
	 */
	private static final String RESPONSE_SUFFIX = "_RESPONSE";

	/*
	 * TODO add the syntax here possibly
	 */
	public static enum CLIENT_REQUEST {
		READ_ITEMS, READ_ITEM, POST;
		public String RESPONSE() {
			return this.name() + RESPONSE_SUFFIX;
		}
	};

	/**
	 * <pre>
	 * 	b) Requests TO and FROM [ as the send format is the same as received format, as practically I an receiving my call] the internal replica servers and their responses
	 * 		i) ReadLatestItem -- Call from an internal server which just wants the latest article to be returned
	 * 		ii) ReadItemsFromID -- Call from an internal server with an articleId and we need to return all the articles after this number
	 * 		iii) WriteArticle -- Write an article in local BB, this will be a well formed article with an Id and everything 
	 * 		iv) Election calls TODO
	 * </pre>
	 */
	public static enum REQUEST_INTERNAL_SERVER {
		READ_LATEST_ITEM, READ_ITEMS_FROM_ID, WRITE_ARTICLE;
		public String RESPONSE() {
			return this.name() + RESPONSE_SUFFIX;
		}
	};
	public static final String ARTICLE_ID_PARAMETER_KEY = "AID";


	/**
	 * <pre>
	 * 	d) Calls to Quorum Coordinator and their responses
	 * 		i) GetReadQuorum
	 * 		ii) GetWriteQuorumAndArticleId
	 * 		iii) Register
	 * </pre>
	 */
	public static enum COORDINATOR_CALLS {
		GET_READ_QUORUM, GET_WRITE_QUORUM, REGISTER;
		public String RESPONSE() {
			return this.name() + RESPONSE_SUFFIX;
		}
	};

	/**
	 * <pre>
	 *  * e) Requests from Quorum Coordinator
	 * 		i) Heartbeat
	 * </pre>
	 */
	public static enum COORDINATOR_REQUESTS {
		HEARTBEAT;
		public String RESPONSE() {
			return this.name() + RESPONSE_SUFFIX;
		}
	};

	public static void main(String[] args) {
		CLIENT_REQUEST rq = CLIENT_REQUEST.READ_ITEM;
		System.out.println(rq);
		System.out.println(rq.RESPONSE());
	}
}
