package org.umn.distributed.consistent.common.client.testfrmwk;

import java.util.HashMap;

/**
 * <pre>
 * 1) Need to record how many operations of each type took place
 * 2) What was the cumulative time of the operations
 * 3) What was the max and the min time of each type of operation
 * </pre>
 * 
 * @author akinra
 * 
 */
public class RoundSummary {
	HashMap<String, OperationSummary> opSummaries = new HashMap<String, RoundSummary.OperationSummary>();
	private Round summaryOfRound;

	public RoundSummary(Round r) {
		this.summaryOfRound = r;
	}

	public void addOperationDetail(String uniqueOpName, long timeTaken) {
		OperationSummary summary = null;
		if (opSummaries.containsKey(uniqueOpName)) {
			summary = opSummaries.get(uniqueOpName);
		} else {
			summary = new OperationSummary(uniqueOpName);
			opSummaries.put(uniqueOpName, summary);
		}
		summary.addOperation(timeTaken);
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RoundSummary [opSummaries=");
		builder.append(opSummaries);
		builder.append(", summaryOfRound=");
		builder.append(summaryOfRound);
		builder.append("]");
		return builder.toString();
	}



	class OperationSummary {

		String operationName;
		long cumulativeTime;
		int numberOfOperations;
		long minTime;
		long maxTime;

		public OperationSummary(String uniqueOpName) {
			this.operationName = uniqueOpName;
		}

		public void addOperation(long timeTaken) {
			this.cumulativeTime += timeTaken;
			this.numberOfOperations++;
			if (this.minTime > timeTaken) {
				this.minTime = timeTaken;
			}
			if (this.maxTime < timeTaken) {
				this.maxTime = timeTaken;

			}

		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("OperationSummary [operationName=");
			builder.append(operationName);
			builder.append(", cumulativeTime=");
			builder.append(cumulativeTime);
			builder.append(", numberOfOperations=");
			builder.append(numberOfOperations);
			builder.append(", minTime=");
			builder.append(minTime);
			builder.append(", maxTime=");
			builder.append(maxTime);
			builder.append("]");
			return builder.toString();
		}
		
	}
}
