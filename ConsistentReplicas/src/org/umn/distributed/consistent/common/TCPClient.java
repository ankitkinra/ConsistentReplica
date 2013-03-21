package org.umn.distributed.consistent.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class TCPClient {

	public static byte[] sendData(Machine remoteMachine, byte[] data)
			throws IOException {
		/**
		 * This will open a local socket and send the data to the remoteMachine
		 */
		Socket clientSocket = null;
		int buffSize = 1024;
		InputStream is = null;
		byte[] returnMessage = new byte[1024];
		try {
			clientSocket = new Socket(remoteMachine.getIP(),
					remoteMachine.getPort());
			clientSocket.getOutputStream().write(data);
			
			int count = 0;
			int start = 0;

			ByteArrayOutputStream bos = new ByteArrayOutputStream();

			is = clientSocket.getInputStream();
			count = is.read(returnMessage, start, buffSize);
			while (count > -1) {
				bos.write(returnMessage, start, count);
				start += count;
				count = is.read(returnMessage, start, buffSize);
			}
			is.close();
			returnMessage = bos.toByteArray();
			bos.close();

		} catch (IOException e) {
			throw e;
		} finally {
			try {
				clientSocket.close();
			} catch (IOException ios) {
				throw ios;
			}
		}

		return returnMessage;
	}
}
