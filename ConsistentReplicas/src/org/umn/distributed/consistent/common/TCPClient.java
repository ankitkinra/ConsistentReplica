package org.umn.distributed.consistent.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import org.apache.log4j.Logger;

public class TCPClient {
	protected static Logger logger = Logger.getLogger(TCPClient.class);

	public static byte[] sendData(Machine remoteMachine, byte[] data)
			throws IOException {
		if(logger.isDebugEnabled()) {
			logger.debug("Send " + Utils.byteToString(data) + " to " + remoteMachine);
		}
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
			//TODO: add a timeout here to handle the writer thread waiting in the execution
			clientSocket.getOutputStream().write(data);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			is = clientSocket.getInputStream();
			
			while((count = is.read(buffer)) > -1) {
				bos.write(buffer, 0, count);
			}
			bos.flush();
			buffer = bos.toByteArray();
			bos.close();
		} catch (IOException ioe) {
			logger.error("Error connecting to " + remoteMachine, ioe);
			throw ioe;
		} finally {
			try {
				if (clientSocket != null) {
					clientSocket.close();
				}
			} catch (IOException ios) {
				throw ios;
			}
		}
		if(logger.isDebugEnabled()) {
			logger.debug("Data received at client " + Utils.byteToString(buffer));
		}
		return buffer;
	}
}
