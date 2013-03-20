package org.umn.distributed.consistent.server;

public interface TcpServerDelegate {
	public byte[] handleRequest(byte[] request);
}
