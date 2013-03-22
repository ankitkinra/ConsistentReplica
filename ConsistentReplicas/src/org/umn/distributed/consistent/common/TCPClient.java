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
		int count = 0;
		InputStream is = null;
		byte[] buffer = new byte[buffSize];
		try {
			clientSocket = new Socket(remoteMachine.getIP(),
					remoteMachine.getPort());
			clientSocket.getOutputStream().write(data);
			clientSocket.
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			is = clientSocket.getInputStream();
			while (is.available() > 0 && (count = is.read(buffer)) > -1) {
				bos.write(buffer, 0, count);
			}
			bos.flush();
			is.close();
			buffer = bos.toByteArray();
			bos.close();

		} catch (IOException e) {
			throw e;
		} finally {
			try {
				if (clientSocket != null) {
					clientSocket.close();
				}
			} catch (IOException ios) {
				throw ios;
			}
		}

		return buffer;
	}
}
