package org.umn.distributed.consistent.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.umn.distributed.consistent.common.Utils;

public class TCPServer implements Runnable {
	private Logger logger = Logger.getLogger(this.getClass());
	private static long STOP_TIMEOUT = 1000;
	private TcpServerDelegate delegate;
	private int numThreads = 0;
	private ExecutorService executerService;
	private ServerSocket serverSocket;
	private Thread thread;

	public TCPServer(TcpServerDelegate delegate, int numThreads) {
		this.delegate = delegate;
		this.numThreads = numThreads;
	}

	public int startListening(int port) throws IOException {
		logger.debug("Starting the tcp listener");
		port = Utils.findFreePort(port);
		this.serverSocket = new ServerSocket(port);
		this.executerService = Executors.newFixedThreadPool(numThreads);
		this.thread = new Thread(this);
		this.thread.start();
		return port;
	}

	@Override
	public void run() {
		logger.debug("Started tcpServer on port:"
				+ this.serverSocket.getLocalPort());
		while (true) {
			try {
				this.executerService
						.execute(new Handler(serverSocket.accept()));
			} catch (IOException e) {
				logger.error("Error accepting connection from client", e);
			}
		}
	}

	public void stop() {
		logger.debug("Stopping TcpServer on port:"
				+ this.serverSocket.getLocalPort());
		try {
			this.serverSocket.close();
		} catch (IOException ioe) {
			logger.debug("Interrupted ServerSocket in while listening", ioe);
		}
		logger.debug("ServerSocket closed");
		this.executerService.shutdown();
		try {
			if (!this.executerService.awaitTermination(STOP_TIMEOUT,
					TimeUnit.MILLISECONDS)) {
				this.executerService.shutdownNow();
				if (!this.executerService.awaitTermination(STOP_TIMEOUT,
						TimeUnit.SECONDS)) {
					logger.error("Thread pool did not terminate");
				}
			}
		} catch (InterruptedException ie) {
			this.executerService.shutdownNow();
			Thread.currentThread().interrupt();
		}
		logger.debug("ExecutorService stopped");
		this.thread.interrupt();
		logger.debug("TCPServer thread stopped");
	}

	private class Handler implements Runnable {
		private Socket socket;

		Handler(Socket socket) {
			this.socket = socket;
		}

		@Override
		public void run() {
			InputStream is = null;
			int buffSize = 1024;
			byte buffer[] = new byte[buffSize];
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			int count = 0;
			try {
				is = socket.getInputStream();
				while (is.available() > 0 && (count = is.read(buffer)) > -1) {
					bos.write(buffer, 0, count);
				}
				bos.flush();
				buffer = bos.toByteArray();
				logger.debug("A client connected to server;socket="+socket);
				if(logger.isDebugEnabled()) {
					logger.debug("Data received at server: " + Utils.byteToString(buffer));
				}
				buffer = delegate.handleRequest(buffer);
				if(logger.isDebugEnabled()) {
					logger.debug("Data returned to client :" + Utils.byteToString(buffer));
				}
				socket.getOutputStream().write(buffer);
				is.close();
				bos.close();

				// TODO:add specific handling for different exceptions types
				// based on what exception is thrown when the remote client
				// closes the
				// socket
			} catch (IOException e) {
				logger.error("Error communicating with client", e);
			} finally {
				try {
					// TODO: Check do we really want to close it or not.
					this.socket.close();
				} catch (IOException ios) {
					logger.warn("Error closing socket", ios);
				}
			}
		}
	}
}
