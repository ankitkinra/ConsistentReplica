package org.umn.distributed.consistent.common;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

public class Machine {
	protected Logger logger = Logger.getLogger(this.getClass());

	public static final String FORMAT_START = "[";
	public static final String FORMAT_END = "]";

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
		builder.append(FORMAT_START).append(id).append("|").append(IP)
				.append("|").append(port).append("|")
				.append(extPort).append(FORMAT_END);
		return builder.toString();
	}

	public static Machine parse(String machineStr)
			throws IllegalArgumentException {
		System.out.println("machine" + machineStr);
		if (!machineStr.startsWith(FORMAT_START)
				|| !machineStr.endsWith(FORMAT_END)) {
			throw new IllegalArgumentException("Invalid machine format");
		}
		machineStr = machineStr.substring(1, machineStr.length() - 1);
		String machineParams[] = machineStr.split("\\|");
		if (machineParams.length != 4) {
			throw new IllegalArgumentException(
					"Invalid machine parameter number");
		}
		int id = 0;
		int internalPort = 0;
		int externalPort = 0;
		try {
			id = Integer.parseInt(machineParams[0]);
			internalPort = Integer.parseInt(machineParams[2]);
			externalPort = Integer.parseInt(machineParams[3]);
			return new Machine(id, machineParams[1], internalPort, externalPort);
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException("Invalid article id/parentId");
		}
	}

	public static List<Machine> parseList(String req) {
		List<Machine> listMachines = new LinkedList<Machine>();
		int index = -1;
		int start = 0;
		while((index = req.indexOf("]", start)) > -1) {
			Machine machine = Machine.parse(req.substring(start, index + 1));
			listMachines.add(machine);
			start = index + 1;
			
		}
		return listMachines;
	}
}
