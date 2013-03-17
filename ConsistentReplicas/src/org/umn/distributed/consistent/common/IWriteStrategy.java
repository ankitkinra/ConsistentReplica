package org.umn.distributed.consistent.common;

public interface IWriteStrategy {
	String write(String article, String parentId);
}
