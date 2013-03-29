package org.umn.distributed.consistent.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Random;

import org.apache.log4j.Logger;

public class TCPClient {
	protected static Logger logger = Logger.getLogger(TCPClient.class);
	private static Random randomDelay = new Random();

	public static byte[] sendData(Machine remoteMachine, byte[] data)
			throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("Send " + Utils.byteToString(data) + " to "
					+ remoteMachine);
		}
		// Adding a random delay
		int maxDelay = Props.maxPseudoNetworkDelay;
		if (maxDelay < 1) {
			maxDelay = ClientProps.maxPseudoNetworkDelay;
		}
		long delay = randomDelay.nextInt(maxDelay);
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
			LoggingUtils.logError(logger, e,
					"Error while waiting by thread = %s", Thread
							.currentThread().getName());
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
			// TODO: add a timeout here to handle the writer thread waiting in
			// the execution
			clientSocket.getOutputStream().write(data);
			clientSocket.getOutputStream().flush();
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			is = clientSocket.getInputStream();

			while ((count = is.read(buffer)) > -1) {
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
		if (logger.isDebugEnabled()) {
			logger.debug("Data received at client "
					+ Utils.byteToString(buffer));
		}
		return buffer;
	}
}
