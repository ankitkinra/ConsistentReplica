package org.umn.distributed.consistent.server.common;

public interface TcpServerDelegate {
	public byte[] handleRequest(byte[] request);
}
