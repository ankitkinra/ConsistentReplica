package org.umn.distributed.consistent.common;

import java.util.HashMap;

public class ParsedCommand {
	private Command comm = null;
	private HashMap<String, String> parsedParametersValue = new HashMap<String, String>();
	public ParsedCommand(Command comm,
			HashMap<String, String> parsedParametersValue) {
		
		this.comm = comm;
		this.parsedParametersValue = parsedParametersValue;
	}
	public Command getComm() {
		return comm;
	}
	public HashMap<String, String> getParsedParametersValue() {
		return parsedParametersValue;
	}
	
	
}
