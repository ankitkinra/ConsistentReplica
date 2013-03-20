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

	public TCPServer(TcpServerDelegate delegate, int numThreads) {
		this.delegate = delegate;
		this.numThreads = numThreads;
	}

	public int startListening(int port) throws IOException {
		port = Utils.findFreePort(port);
		serverSocket = new ServerSocket(port);
		this.executerService = Executors.newFixedThreadPool(numThreads);
		return port;
	}

	@Override
	public void run() {
		while (true) {
			try {
				executerService.execute(new Handler(serverSocket.accept()));
			} catch (IOException e) {
				logger.error("Error accepting connection from client", e);
			}
		}
	}

	void shutdownAndAwaitTermination(ExecutorService pool) {
		pool.shutdown();
		try {
			if (!pool.awaitTermination(STOP_TIMEOUT, TimeUnit.MILLISECONDS)) {
				pool.shutdownNow();
				if (!pool.awaitTermination(STOP_TIMEOUT, TimeUnit.SECONDS)) {
					logger.error("Thread pool did not terminate");
				}
			}
		} catch (InterruptedException ie) {
			pool.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	private class Handler implements Runnable {
		private Socket socket;

		Handler(Socket socket) {
			this.socket = socket;
		}

		@Override
		public void run() {
			InputStream is;
			int buffSize = 1024;
			byte buffer[] = new byte[buffSize];
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			int start = 0;
			int count;
			try {
				is = socket.getInputStream();
				count = is.read(buffer, start, buffSize);
				while(count > -1) {
					bos.write(buffer, start, count);
					start += count;
					count = is.read(buffer, start, buffSize);
				}
				buffer = delegate.handleRequest(bos.toByteArray());
				socket.getOutputStream().write(buffer);
			} catch (IOException e) {
				logger.error("Error communicating with client", e);
			}
			finally {
				try {
					//TODO: Check do we really want to close it or not.
					this.socket.close();
				}
				catch(IOException ios) {
					logger.warn("Error closing socket", ios);
				}
			}
		}
	}
}
