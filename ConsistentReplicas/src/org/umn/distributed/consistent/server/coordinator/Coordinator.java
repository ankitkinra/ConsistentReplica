package org.umn.distributed.consistent.server.coordinator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.umn.distributed.consistent.common.Machine;
import org.umn.distributed.consistent.common.Props;
import org.umn.distributed.consistent.common.TCPClient;
import org.umn.distributed.consistent.common.Utils;
import org.umn.distributed.consistent.server.AbstractServer;

public abstract class Coordinator extends AbstractServer {

	protected AtomicInteger articleID = new AtomicInteger(1);
	protected AtomicInteger knownMachineID = new AtomicInteger(1);
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
			Machine machineToAdd = Machine.parse(reqStr
					.substring((REGISTER_COMMAND + COMMAND_PARAM_SEPARATOR)
							.length()));
			machineToAdd.setid(knownMachineID.getAndIncrement());
			logger.info(machineToAdd
					+ " trying to register with the coordinator");
			this.addMachine(machineToAdd);
			List<UpdaterThread> threads = new ArrayList<UpdaterThread>();
			Set<Machine> machineSetToUpdateWithNewServer = getMachineList();
			machineSetToUpdateWithNewServer.remove(machineToAdd); // cannot send
																	// this
																	// machine
																	// all the
																	// machines

			CountDownLatch latch = new CountDownLatch(
					machineSetToUpdateWithNewServer.size() + 1);
			UpdaterThread thread = new UpdaterThread(machineToAdd,
					machineSetToUpdateWithNewServer, latch, true);
			threads.add(thread);
			thread.start();
			Set<Machine> setWithOnlyNewMachine = new HashSet<Machine>();
			setWithOnlyNewMachine.add(machineToAdd);

			for (Machine currMachine : machineSetToUpdateWithNewServer) {
				// this is already added to the set once
				thread = new UpdaterThread(currMachine, setWithOnlyNewMachine,
						latch, true);
				threads.add(thread);
				thread.start();
			}
			try {
				latch.await(Props.NETWORK_TIMEOUT, TimeUnit.MILLISECONDS);
				for (UpdaterThread t : threads) {
					if (!Utils.byteToString(t.dataRead).startsWith(
							COMMAND_SUCCESS)) {
						logger.error("Unable to update registered list on machine "
								+ t.serverToUpdate);
						return Utils
								.stringToByte(COMMAND_FAILED
										+ COMMAND_PARAM_SEPARATOR
										+ "Unable to update registered server list on all replicas");
					}
				}
			} catch (InterruptedException ie) {
				logger.error("Updater latch interrupted", ie);
			}
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

	protected class UpdaterThread extends Thread {
		Machine serverToUpdate;
		Set<Machine> update;
		CountDownLatch latchToDecrement;
		byte[] dataRead;
		boolean add;

		UpdaterThread(Machine serverToUpdate, Set<Machine> update,
				CountDownLatch latchToDecrement, boolean add) {
			this.serverToUpdate = serverToUpdate;
			this.update = update;
			this.latchToDecrement = latchToDecrement;
			this.add = add;
		}

		@Override
		public void run() {
			try {
				StringBuilder builder = new StringBuilder();
				if (add) {
					builder.append(ADD_SERVER_COMMAND).append(
							COMMAND_PARAM_SEPARATOR);
					for (Machine machine : update) {
						builder.append(machine);
					}
				} else {
					builder.append(REMOVE_SERVER_COMMAND).append(
							COMMAND_PARAM_SEPARATOR);
					for (Machine machine : update) {
						builder.append(machine);
					}
				}
				logger.info("UpdaterThread builder for update operation == "
						+ builder);
				dataRead = TCPClient.sendData(this.serverToUpdate,
						Utils.stringToByte(builder.toString()));
				logger.info("Updated server " + serverToUpdate
						+ " with the latest list; server response = "
						+ dataRead);
			} catch (IOException e) {
				logger.error("Cannot write to " + serverToUpdate, e);
			} finally {
				latchToDecrement.countDown();
			}

		}
	}
}
