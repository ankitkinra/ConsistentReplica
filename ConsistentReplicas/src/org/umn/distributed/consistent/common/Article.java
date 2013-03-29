package org.umn.distributed.consistent.common;

public class Article {
	public static final int SHORT_TITLE_CHARS = 4;
	public static final int SHORT_CONTENT_CHARS = 16;
	public static final String FORMAT_START = "[";
	public static final String FORMAT_END = "]";
	private int id;
	private int parentId;
	private String title;
	private String content;

	public Article(int id, int parentId, String title, String content) {
		this.id = id;
		this.parentId = parentId;
		this.title = title;
		this.content = content;
	}

	public Article(Article toCopy) {
		this.id = toCopy.id;
		this.parentId = toCopy.parentId;
		this.title = toCopy.title;
		this.content = toCopy.content;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getParentId() {
		return parentId;
	}

	public void setParentId(int parentId) {
		this.parentId = parentId;
	}

	public boolean isRoot() {
		return this.parentId == 0;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(Article.FORMAT_START).append(id).append("|")
				.append(parentId).append("|").append(title).append("|")
				.append(content).append(Article.FORMAT_END);
		return builder.toString();
	}

	public String toShortString() {
		StringBuilder builder = new StringBuilder();
		builder.append(Article.FORMAT_START).append(id).append("|")
				.append(parentId).append("|");
		int maxLen = Article.SHORT_TITLE_CHARS;
		if (maxLen > title.length()) {
			maxLen = title.length();
		}
		builder.append(title.substring(0, maxLen)).append("|");
		maxLen = Article.SHORT_CONTENT_CHARS;
		if (maxLen > content.length()) {
			maxLen = content.length();
		}
		builder.append(content.substring(0, maxLen)).append(Article.FORMAT_END);
		return builder.toString();
	}

	public static Article parseArticle(String articleStr)
			throws IllegalArgumentException {
		if (!articleStr.startsWith(Article.FORMAT_START)
				|| !articleStr.endsWith(Article.FORMAT_END)) {
			throw new IllegalArgumentException("Invalid article format = " + articleStr);
		}
		articleStr = articleStr.substring(1, articleStr.length() - 1);
		String articleParams[] = articleStr.split("\\|", -1);
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
