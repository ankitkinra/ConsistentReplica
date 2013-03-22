package org.umn.distributed.consistent.server.coordinator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.umn.distributed.consistent.common.Machine;
import org.umn.distributed.consistent.common.Props;
import org.umn.distributed.consistent.common.TCPClient;
import org.umn.distributed.consistent.common.Utils;
import org.umn.distributed.consistent.server.AbstractServer;

public abstract class Coordinator extends AbstractServer {

	private volatile int articleID = 1;
	protected HashMap<Integer, Integer> toRemoveList = new HashMap<Integer, Integer>();

	protected Coordinator(STRATEGY strategy) {
		super(strategy, Props.COORDINATOR_PORT,
				Props.COORDINATOR_SERVER_THREADS);
	}

	@Override
	public void startSpecific() throws Exception {
		try {

		} catch (Exception e) {
			throw e;
		}
	}

	protected synchronized int getArticleId() {
		articleID = articleID + 1;
		return articleID;
	}

	protected boolean checkIsAlive(Machine machine) {
		try {
			byte[] data = TCPClient.sendData(machine,
					Utils.stringToByte(HEARTBEAT_COMMAND, Props.ENCODING));
			String response = Utils.byteToString(data, Props.ENCODING);
			if (response.startsWith(COMMAND_SUCCESS)) {
				logger.info(machine.toString()
						+ " sent negative response to heartbeat");
				return true;
			}
		} catch (IOException e) {
			logger.info(machine.toString() + " did not respond to heartbeat");
		}
		return false;
	}

	@Override
	public byte[] handleRequest(byte[] request) {
		String reqStr = Utils.byteToString(request, Props.ENCODING);
		if (reqStr.startsWith(REGISTER_COMMAND)) {
			String req[] = reqStr.split(COMMAND_PARAM_SEPARATOR);
			Machine machine = new Machine(Integer.parseInt(req[1]), req[2],
					Integer.parseInt(req[3]), Integer.parseInt(req[4]));
			this.knownClients.put(machine.getId(), machine);
			return Utils.stringToByte(COMMAND_SUCCESS, Props.ENCODING);
		}
		return handleSpecificRequest(reqStr);
	}

	public abstract byte[] handleSpecificRequest(String str);

	protected class HeartBeat implements Runnable {

		@Override
		public void run() {
			while (true) {
				try {
					wait(Props.HEARTBEAT_INTERVAL);
					Iterator<Integer> it = knownClients.keySet().iterator();
					while (it.hasNext()) {
						Machine machine = knownClients.get(it.next());
						// This is to make sure that machine has not been
						// removed
						if (machine != null) {
							if (!checkIsAlive(machine)) {
								if (toRemoveList.containsKey(machine.getId())) {
									toRemoveList
											.put(machine.getId(), toRemoveList
													.get(machine.getId()) + 1);
								}
							}
						}
					}

					it = toRemoveList.keySet().iterator();
					while (it.hasNext()) {
						Integer id = it.next();
						if (toRemoveList.get(id) >= Props.REMOVE_INTERVAL) {
							// TODO: check this code
							knownClients.remove(id);
							it.remove();
						}
					}
				} catch (InterruptedException e) {
					logger.error("Heartbeat thread interrupted", e);
				}
			}
		}

	}
}
