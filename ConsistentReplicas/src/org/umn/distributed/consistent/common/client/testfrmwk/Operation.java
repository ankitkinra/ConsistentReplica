package org.umn.distributed.consistent.common.client.testfrmwk;

import java.util.HashMap;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("operation")
public class Operation {
	int sno = 0;
	String name;
	
	HashMap<String, String> params = new HashMap<String, String>();

	public Operation(int sno, String name) {
		this.sno = sno;
		this.name = name;
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
		builder.append(", params=");
		builder.append(params);
		builder.append("]");
		return builder.toString();
	}

}
