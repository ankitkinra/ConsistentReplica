package org.umn.distributed.consistent.common.client.testfrmwk;

import java.util.HashMap;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("operation")
public class Operation {
	int sno = 0;
	String name;
	int repeatations = 0;
	HashMap<String, String> params = new HashMap<String, String>();

	public Operation(int sno, String name, int repeatations) {
		this.sno = sno;
		this.name = name;
		this.repeatations = repeatations;
	}

	public void addParam(String name, String value) {
		this.params.put(name, value);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Operation [sno=");
		builder.append(sno);
		builder.append(", name=");
		builder.append(name);
		builder.append(", repeatations=");
		builder.append(repeatations);
		builder.append(", params=");
		builder.append(params);
		builder.append("]");
		return builder.toString();
	}

}
