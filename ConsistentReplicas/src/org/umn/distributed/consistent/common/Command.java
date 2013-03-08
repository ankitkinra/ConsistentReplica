package org.umn.distributed.consistent.common;

import java.util.ArrayList;
import java.util.HashMap;

import org.umn.distributed.consistent.common.Parameter.PARA_TYPE;



public class Command {
	private String _commandName;
	private ArrayList<Parameter> _requiredParameters;
	private ArrayList<Parameter> _optionalParameters;
	private String _usage;
	private static HashMap<String, Command> KNOWN_COMMANDS = new HashMap<String, Command>();

	/**
	 * TODO we have the constructor get the basic validations on the required
	 * parameters
	 * 
	 * @param commandName
	 * @param requiredParameters
	 * @param _usage
	 */
	public Command(String commandName, Parameter[] parameters) {
		this._commandName = commandName;
		this._optionalParameters = new ArrayList<Parameter>();
		this._requiredParameters = new ArrayList<Parameter>();
		if (parameters != null) {
			for (Parameter p : parameters) {
				if (p.getType() == PARA_TYPE.OPTIONAL) {
					this._optionalParameters.add(p);
				} else {
					this._requiredParameters.add(p);
				}
			}
		}
		this._usage = getUsage(commandName, parameters);
	}

	private static String getUsage(String commandName, Parameter[] parameters) {
		StringBuilder sb = new StringBuilder("Command:");
		sb.append(commandName);
		sb.append("has the following list of parameters.");
		if (parameters != null) {
			for (Parameter p : parameters) {
				sb.append(p.toString());
			}
		} else {
			sb.append("No Parameters");
		}
		return sb.toString();
	}

	public static void addCommand(String commandName, Parameter[] parameters) {
		Command c = new Command(commandName, parameters);
		KNOWN_COMMANDS.put(commandName, c);
	}

	/**
	 * This will parse the user command and return the user the map
	 * 
	 * @param userCommand
	 * @return
	 */
	public static ParsedCommand parseCommand(String userCommand)
			throws CommandNotParsedException {
		HashMap<String, String> parsedCommandWithParameterValues = new HashMap<String, String>();
		String[] commandSplit = userCommand.split("\\s+");
		// the first token is command name
		if (commandSplit.length == 0) {
			throw new CommandNotParsedException("Command not provided",
					getKnownCommands());
		}
		if (!KNOWN_COMMANDS.containsKey(commandSplit[0])) {
			throw new CommandNotParsedException("Command not found",
					getKnownCommands());
		}
		Command c = KNOWN_COMMANDS.get(commandSplit[0]);
		if (commandSplit.length < c._requiredParameters.size() + 1) {
			throw new CommandNotParsedException(
					"Required Parameters not passed", c._usage);
		}

		// first the required parameters
		// parseParameter(parsedCommandWithParameterValues, commandSplit, c,
		// true);
		int i = 0;
		for (; i < c._requiredParameters.size(); i++) {
			parseParameter(parsedCommandWithParameterValues,
					commandSplit[i + 1], c, c._requiredParameters.get(i));
		}
		// i = requiredParameters + 1
		// then the optional parameters
		for (int j = 0; i < c._requiredParameters.size()
				+ c._optionalParameters.size(); j++, i++) {
			if (i + 1< commandSplit.length) {
				parseParameter(parsedCommandWithParameterValues,
						commandSplit[i + 1], c, c._optionalParameters.get(j));
			} else {
				parsedCommandWithParameterValues.put(
						c._optionalParameters.get(j).getParameterName(),
						c._optionalParameters.get(j).getDefaultValue());
			}
		}
		return new ParsedCommand(c, parsedCommandWithParameterValues);
	}

	private static void parseParameter(
			HashMap<String, String> parsedCommandWithParameterValues,
			String paraValue, Command comm, Parameter p)
			throws CommandNotParsedException {
		if (paraValue.length() == 0) {
			if (p.isRequired()) {
				throw new CommandNotParsedException("parameter " + p
						+ " is empty", comm._usage);
			} else {
				paraValue = p.getDefaultValue();
			}

		}

		try {
			parsedCommandWithParameterValues.put(p.getParameterName(),
					p.validate(paraValue));
		} catch (IllegalArgumentException e) {
			throw new CommandNotParsedException(
					"Parameter validation failed for parameter :"
							+ p.getParameterName(), comm._usage);
		}

	}

	private static String getKnownCommands() {
		StringBuilder sb = new StringBuilder(
				"The following commands are known to this system");
		sb.append(KNOWN_COMMANDS.toString());
		return sb.toString();
	}

	public String getCommandName() {
		return _commandName;
	}

	public ArrayList<Parameter> getRequiredParameters() {
		return _requiredParameters;
	}

	public ArrayList<Parameter> getOptionalParameters() {
		return _optionalParameters;
	}

	public String getUsage() {
		return _usage;
	}

}
