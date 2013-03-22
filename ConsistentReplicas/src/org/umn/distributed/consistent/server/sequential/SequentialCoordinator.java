package org.umn.distributed.consistent.server.sequential;

import org.umn.distributed.consistent.common.Props;
import org.umn.distributed.consistent.server.coordinator.Coordinator;

public class SequentialCoordinator extends Coordinator {

	public SequentialCoordinator() {
		super(STRATEGY.SEQUENTIAL);
	}

}
