package org.umn.distributed.consistent.common;

public class Machine {
	private int id;
	private String IP;
	private int port;

	public String getIP() {
		return IP;
	}

	public int getPort() {
		return port;
	}
	
	public Machine(String iP, int port) {
		super();
		IP = iP;
		this.port = port;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((IP == null) ? 0 : IP.hashCode());
		result = prime * result + port;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Machine other = (Machine) obj;
		if (IP == null) {
			if (other.IP != null)
				return false;
		} else if (!IP.equals(other.IP))
			return false;
		if (port != other.port)
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Machine [IP=");
		builder.append(IP);
		builder.append(", port=");
		builder.append(port);
		builder.append("]");
		return builder.toString();
	}

	
}
