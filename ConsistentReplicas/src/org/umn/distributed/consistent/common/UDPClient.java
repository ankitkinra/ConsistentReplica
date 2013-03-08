package org.umn.distributed.consistent.common;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

public abstract class UDPClient {
	boolean shutdownInvoked = false;
	private int senderPort;
	protected DatagramSocket writerSocket;
	private UDPListener listener;
	/**
	 * A continuous reader will be spawned on the listenerPort and as soon as we
	 * have something read the subclas method to process the packet will get
	 * invoked
	 */
	private int listenerPort;
	protected Logger logger = Logger.getLogger(this.getClass());

	/**
	 * This method creates and instance of UDPClient but doesn't start the
	 * listener.
	 * 
	 * @param writerPort
	 * @throws SocketException
	 */
	public UDPClient(int senderPort) throws SocketException {
		this(senderPort, 0);
	}

	/**
	 * This method creates an instance of UDPClient and starts the listener.
	 * 
	 * @param writerPort
	 * @param listenerPort
	 * @throws SocketException
	 */
	public UDPClient(int senderPort, int listenerPort) throws SocketException {
		if (senderPort > 0) {
			initWriterPort(senderPort);
		}
		if (listenerPort > 0) {
			initAndStartListener(listenerPort);
		}

	}

	private void initAndStartListener(int listenerPort2) throws SocketException {
		this.listenerPort = listenerPort2;
		listener = new UDPListener("UDPListener", listenerPort2);
		listener.start();
	}

	private void initWriterPort(int senderPort2) throws SocketException {
		this.senderPort = senderPort2;
		this.writerSocket = new DatagramSocket(senderPort2);
	}

	public boolean isShutdownInvoked() {
		return shutdownInvoked;
	}

	public int getSenderPort() {
		return senderPort;
	}

	/**
	 * Returns the listener port if UDPClient is started with listener port
	 * also. Returns zero if its the send only UDPClient.
	 * 
	 * @return
	 */
	public int getListenerPort() {
		return listenerPort;
	}

	public boolean shutdown() {
		// remove all the bindings and exit
		this.shutdownInvoked = true;
		if (this.writerSocket != null) {
			this.writerSocket.close();
		}
		if (this.listener != null) {
			this.listener.interrupt();
		}
		return true;
	}

	protected boolean sendData(String data, String hostname, int port) {
		logger.debug("send udp data data:" + data + ",hostname:" + hostname + ",port:" + port);
		if (port < 1 || port > 65535) {
			logger.error("invalid port number:" + port);
			return false;
		}

		try {
			InetAddress ip = InetAddress.getByName(hostname);
			byte[] byteData = data.getBytes();
			int length = byteData.length;
			DatagramPacket packet = new DatagramPacket(byteData, length, ip,
					port);
			writerSocket.send(packet);
			return true;
		} catch (SocketException e) {
			logger.error(e.getMessage(), e);
		} catch (UnknownHostException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return false;
	}

	protected boolean sendDataAndReceive(String data, String hostname, int port) {
		logger.debug("send udp data data:" + data + ",hostname:" + hostname + ",port:" + port);
		if (port < 1 || port > 65535) {
			logger.error("invalid port number:" + port);
			return false;
		}

		try {
			InetAddress ip = InetAddress.getByName(hostname);
			byte[] byteData = data.getBytes();
			DatagramPacket packet = new DatagramPacket(byteData, byteData.length, ip,
					port);
			writerSocket.send(packet);
			byte[] recievedData = new byte[1024];
			DatagramPacket recPacket = new DatagramPacket(recievedData, recievedData.length);
			writerSocket.receive(recPacket);
			processRecievedPacket(recPacket);
			return true;
		} catch (SocketException e) {
			logger.error(e.getMessage(), e);
		} catch (UnknownHostException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return false;
	}

	protected abstract void processRecievedPacket(DatagramPacket packet);

	protected abstract void handleRecieved(DatagramPacket packet, DatagramSocket socket) throws IOException;

	public class UDPListener extends Thread {
		protected DatagramSocket socket = null;

		public UDPListener(String name, int port) throws SocketException {
			super(name);
			socket = new DatagramSocket(port);
		}

		@Override
		public void interrupt() {
			this.socket.close();
			super.interrupt();
		}

		public void run() {
			logger.debug("started listener thread started");
			byte[] buf = null;
			DatagramPacket packet = null;
			while (!UDPClient.this.shutdownInvoked) {
				buf = new byte[1024];
				packet = new DatagramPacket(buf, buf.length);
				try {
					socket.receive(packet);
					UDPClient.this.handleRecieved(packet,socket);
				} catch (SocketException e) {
					logger.error(e.getMessage(), e);
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			}
			socket.close();
		}
	}
}
