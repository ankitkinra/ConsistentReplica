package org.umn.distributed.consistent.common;

public class Machine {
	private int id;
	private String IP;
	private int port;
	private int extPort;

	public Machine(int id, String iP, int port, int extPort) {
		this.id = id;
		this.IP = iP;
		this.port = port;
		this.extPort = extPort;
	}

	public Machine(String iP, int port) {
		this(0, iP, port, 0);
	}

	public Machine(String iP, int port, int extPort) {
		this(0, iP, port, extPort);
	}

	public Machine(int id, String iP, int port) {
		this(id, iP, port, 0);
	}

	public int getId() {
		return id;
	}

	public void setid(int id) {
		this.id = id;
	}

	public String getIP() {
		return IP;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getExternalPort() {
		return extPort;
	}

	public void setExternalPort(int extPort) {
		this.extPort = extPort;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((IP == null) ? 0 : IP.hashCode());
		result = prime * result + id;
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
		if (id != other.id)
			return false;
		if (port != other.port)
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Machine [id=").append(id).append(", IP=").append(IP)
				.append(", port=").append(port).append("]");
		return builder.toString();
	}

}
