package org.umn.distributed.consistent.common.client.testfrmwk;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("testsuite")
public class TestSuite {
	String name;
	List<Round> rounds = new ArrayList<Round>();

	public TestSuite(String name) {
		this.name = name;
	}

	public void addRound(Round r) {
		this.rounds.add(r);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TestSuite [name=");
		builder.append(name);
		builder.append(", rounds=");
		builder.append(rounds);
		builder.append("]");
		return builder.toString();
	}

}
