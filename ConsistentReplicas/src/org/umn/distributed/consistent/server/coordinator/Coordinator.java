package org.umn.distributed.consistent.server.coordinator;

import java.io.IOException;
import java.util.ArrayList;
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

	private HeartBeat heartBeatThread = new HeartBeat();
	private Object heartBeatMonitorObj = new Object();
	private boolean wasHeartBeatSignaled = true;

	protected Coordinator(STRATEGY strategy) {
		super(strategy, Props.COORDINATOR_PORT,
				Props.COORDINATOR_SERVER_THREADS);
	}

	@Override
	public void startSpecific() throws Exception {
		try {
			heartBeatThread.start();
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public void showInfo() {
		logger.info("******************************* Coordinator info ************************************");
		logger.info("IP:" + this.myInfo.getIP() + ", Port:"
				+ this.myInfo.getPort());
		logger.info("*************************************************************************************");

	}

	@Override
	public byte[] handleRequest(byte[] request) {
		try {
			String reqStr = Utils.byteToString(request);
			if (reqStr.startsWith(REGISTER_COMMAND)) {
				StringBuilder builder = new StringBuilder();
				Machine machineToAdd = Machine.parse(reqStr
						.substring((REGISTER_COMMAND + COMMAND_PARAM_SEPARATOR)
								.length()));
				machineToAdd.setid(knownMachineID.getAndIncrement());
				builder.append(COMMAND_SUCCESS).append(COMMAND_PARAM_SEPARATOR)
						.append(machineToAdd.getId());
				registerMachineAction(machineToAdd);
				return Utils.stringToByte(builder.toString());
			} else if (reqStr.startsWith(GET_REGISTERED_COMMAND)) {
				logger.debug("Client requested the registered server list");
				StringBuilder builder = new StringBuilder();
				builder.append(COMMAND_SUCCESS).append(COMMAND_PARAM_SEPARATOR);
				Set<Machine> machineSetToUpdateWithNewServer = getMachineList();
				for (Machine currMachine : machineSetToUpdateWithNewServer) {
					builder.append(currMachine);
				}
				return Utils.stringToByte(builder.toString());
			}
			return handleSpecificRequest(reqStr);
		} catch (Exception e) {
			logger.error("Exception handling request in replica server", e);
			return Utils.stringToByte(COMMAND_FAILED + COMMAND_PARAM_SEPARATOR
					+ e.getMessage());
		}
	}

	protected void registerMachineAction(Machine machineToAdd) {
		this.addMachine(machineToAdd);
		synchronized (heartBeatMonitorObj) {
			wasHeartBeatSignaled = true;
			heartBeatMonitorObj.notify();
		}
		logger.info(machineToAdd
				+ " added by coordinator to known replica list");
	}

	/**
	 * Extracting this method so that child Coordinators can add custom machine
	 * operation which a server fails the ping test
	 * 
	 * @param m
	 */
	protected void actOnFailedMachine(Machine m) {
		removeMachine(m.getId());
	}

	protected Set<Machine> getHeartbeatMachineList() {
		return getMachineList();
	}

	public abstract byte[] handleSpecificRequest(String str);

	@Override
	public final void stop() {
		super.stop();
		heartBeatThread.interrupt();
	}

	protected class HeartBeat extends Thread {
		@Override
		public void run() {
			List<PingThread> threads = null;
			Set<Machine> currentMachines = null;
			try {
				while (true) {
					synchronized (heartBeatMonitorObj) {
						if (!wasHeartBeatSignaled) {
							heartBeatMonitorObj.wait(Props.HEARTBEAT_INTERVAL);
						}
						wasHeartBeatSignaled = false;
					}
					threads = new ArrayList<PingThread>();
					currentMachines = getHeartbeatMachineList();
					CountDownLatch latch = new CountDownLatch(
							currentMachines.size());
					for (Machine currMachine : currentMachines) {
						PingThread thread = new PingThread(currMachine,
								currentMachines, latch);
						threads.add(thread);
						thread.start();
					}
					latch.await(Props.NETWORK_TIMEOUT, TimeUnit.MILLISECONDS);
					for (PingThread t : threads) {
						if (t.isAlive()) {
							t.interrupt();
						}
						if (t.dataRead == null
								|| !Utils.byteToString(t.dataRead).startsWith(
										COMMAND_SUCCESS)) {
							logger.error("Unable to update known servers on machine "
									+ t.serverToUpdate
									+ ". Removing from known server list");
							actOnFailedMachine(t.serverToUpdate);
						}
					}
				}
			} catch (InterruptedException ie) {
				logger.error("Heartbeat thread interrupted", ie);
				if (threads != null) {
					for (PingThread thread : threads) {
						thread.interrupt();
					}
				}
			}
		}
	}

	protected class PingThread extends Thread {
		Machine serverToUpdate;
		Set<Machine> machines;
		CountDownLatch latchToDecrement;
		byte dataRead[];

		PingThread(Machine serverToUpdate, Set<Machine> machines,
				CountDownLatch latchToDecrement) {
			this.serverToUpdate = serverToUpdate;
			this.machines = machines;
			this.latchToDecrement = latchToDecrement;
		}

		@Override
		public void run() {
			try {
				logger.debug("Updating known server list on "
						+ this.serverToUpdate);
				StringBuilder builder = new StringBuilder();
				builder.append(HEARTBEAT_COMMAND).append(
						COMMAND_PARAM_SEPARATOR);
				for (Machine machine : machines) {
					if (!machine.equals(serverToUpdate)) {
						builder.append(machine);
					}
				}
				dataRead = TCPClient.sendData(this.serverToUpdate,
						Utils.stringToByte(builder.toString()));
				String response = Utils.byteToString(dataRead);
				if (response.startsWith(COMMAND_FAILED)) {
					removeMachine(serverToUpdate.getId());
					logger.info(serverToUpdate + " responded with "
							+ COMMAND_FAILED
							+ " to heartbeat. Removed from known servers list");
				}
			} catch (IOException e) {
				logger.error("Error updating server " + serverToUpdate
						+ " with known server list", e);
			} finally {
				latchToDecrement.countDown();
			}
		}
	}
}
