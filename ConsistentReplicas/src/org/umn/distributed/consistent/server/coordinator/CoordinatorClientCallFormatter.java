package org.umn.distributed.consistent.server.coordinator;

import java.io.IOException;

import org.umn.distributed.consistent.common.Machine;
import org.umn.distributed.consistent.common.TCPClient;

public class CoordinatorClientCallFormatter {

	private static final byte[] GET_ARTICLE_ID = null;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public static byte[] gettingArticleId(Machine coordinatorMachine)
			throws IOException {
		// TODO this will start a TCPClient and send the Coodinartor a request

		byte[] id = TCPClient.sendData(coordinatorMachine, GET_ARTICLE_ID);
		return id;
	}

}
