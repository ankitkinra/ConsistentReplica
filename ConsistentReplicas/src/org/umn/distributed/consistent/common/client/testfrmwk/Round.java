package org.umn.distributed.consistent.common.client.testfrmwk;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("round")
public class Round {
	String name;
	List<Operation> operations = new ArrayList<Operation>();
	public Round(String name){
		this.name = name;
	}
	public void addOperation(Operation op){
		this.operations.add(op);
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Round [name=");
		builder.append(name);
		builder.append(", operations=");
		builder.append(operations);
		builder.append("]");
		return builder.toString();
	}
}
