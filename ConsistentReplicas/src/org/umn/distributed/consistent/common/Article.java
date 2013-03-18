package org.umn.distributed.consistent.common;

public class Article {
	private int id;
	private int parentId;
	private String title;
	private String content;
	
	public Article() {
		
	}
	
	public Article(int id, int parentId, String title, String content) {
		this.id = id;
		this.parentId = parentId;
		this.title = title;
		this.content = content;
	}

	public int getId() {
		return id;
	}
	
	public int getParentId() {
		return parentId;
	}

	public String getTitle() {
		return title;
	}

	public String getContent() {
		return content;
	}
	
	public static Article parseArticle(String articleStr) {
		return new Article();
	}
	
	@Override
	public String toString() {
		return null;
	}
}
