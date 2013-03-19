package org.umn.distributed.consistent.common;

import org.apache.log4j.Logger;

public class Article {
	protected Logger logger = Logger.getLogger(this.getClass());

	public static final int SHORT_TITLE_CHARS = 16;
	public static final int SHORT_CONTENT_CHARS = 16;
	public static final String FORMAT_START = "[";
	public static final String FORMAT_END = "]";
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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(Article.FORMAT_START).append("").append(id).append("|")
				.append(parentId).append("|").append(title).append("|")
				.append(content).append(Article.FORMAT_END);
		return builder.toString();
	}

	public String toShortString() {
		StringBuilder builder = new StringBuilder();
		builder.append(Article.FORMAT_START).append(id).append("|")
				.append(parentId).append("|")
				.append(title.substring(0, Article.SHORT_TITLE_CHARS))
				.append("|")
				.append(content.substring(0, Article.SHORT_CONTENT_CHARS))
				.append(Article.FORMAT_END);
		return builder.toString();
	}

	public static Article parseArticle(String articleStr)
			throws IllegalArgumentException {
		if (!articleStr.startsWith(Article.FORMAT_START)
				|| !articleStr.endsWith(Article.FORMAT_END)) {
			throw new IllegalArgumentException("Invalid article format");
		}
		articleStr = articleStr.substring(1, articleStr.length() - 1);
		String articleParams[] = articleStr.split("|");
		if (articleParams.length != 4) {
			throw new IllegalArgumentException(
					"Invalid article parameter number");
		}
		int id = 0;
		int parentId = 0;
		try {
			id = Integer.parseInt(articleParams[0]);
			parentId = Integer.parseInt(articleParams[1]);
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException("Invalid article id/parentId");
		}
		return new Article(id, parentId, articleParams[2], articleParams[3]);
	}
}
