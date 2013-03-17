package org.umn.distributed.consistent.common;

public interface IReadStrategy {
	String readItem(String id);
	String readItemList();
}
