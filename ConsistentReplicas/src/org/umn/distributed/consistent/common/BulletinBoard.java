package org.umn.distributed.consistent.common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

public class BulletinBoard {

	public static final String FORMAT_START = "{";
	public static final String FORMAT_ENDS = "}";
	private TreeMap<Integer, BulletinBoardEntry> map = new TreeMap<Integer, BulletinBoard.BulletinBoardEntry>();

	public synchronized boolean addArticle(Article article) {
		// TODO: handle malicious coordinator. It should never return the same
		// id back
		map.put(article.getId(), new BulletinBoardEntry(article));
		return true;
	}

	public synchronized boolean addArticleReply(Article article) {
		if (addArticle(article)) {
			BulletinBoardEntry boardEntry = map.get(article.getParentId());
			if (boardEntry == null) {
				boardEntry = new BulletinBoardEntry(null);
				boardEntry.addReplyId(article.getId());
				map.put(article.getParentId(), boardEntry);
			} else {
				boardEntry.addReplyId(article.getId());
			}
			// TODO: handle malicious coordinator. It should never return the
			// same id back.
			return true;
		}
		return false;
	}

	/*
	 * Use this method to check if the BulletinBoard contains the id in the map
	 * or not.
	 */
	public synchronized boolean containsArticle(int id) {
		return map.containsKey(id);
	}

	/*
	 * Returns the article with a given id. This will return null if: 1. there
	 * is not entry in the map for a given id 2. article object is null
	 * corresponding to given id Note that the second case will happen in case
	 * of quorum consistency. Even though this method returns null, isArticle()
	 * can still return true (case 2).
	 */
	public synchronized Article getArticle(int id) {
		BulletinBoardEntry boardEntry = map.get(id);
		if (boardEntry == null) {
			return null;
		} else {
			return boardEntry.getArticle();
		}
	}

	public synchronized List<Integer> getReplyIdList(int id) {
		BulletinBoardEntry boardEntry = map.get(id);
		if (boardEntry == null) {
			return null;
		} else {
			return boardEntry.getReplyIdList();
		}
	}

	@Override
	public String toString() {
		return toString(true);
	}

	public String toShortString() {
		return toString(false);
	}

	/*
	 * This method assumes that map is in synch
	 */
	private synchronized String toString(boolean detailed) {
		StringBuilder builder = new StringBuilder();
		// TODO: Check if returns an increasing order iterator or not.
		Iterator<Integer> it = map.descendingKeySet().descendingIterator();
		while (it.hasNext()) {
			BulletinBoardEntry boardEntry = map.get(it.next());
			Article article = boardEntry.getArticle();
			if (article.getParentId() == 0) {
				// TODO: create a copy of the object before passing here
				appendToString(this, article.getId(), builder, detailed);
			}
		}
		return builder.toString();
	}

	private static void appendToString(BulletinBoard board, Integer id,
			StringBuilder builder, boolean detailed) {
		builder.append("{");
		BulletinBoardEntry entry = board.map.get(id);
		if (detailed) {
			builder.append(entry.getArticle().toString());
		} else {
			builder.append(entry.getArticle().toShortString());
		}
		for (Integer replyId : entry.getReplyIdList()) {
			appendToString(board, replyId, builder, detailed);
		}
		builder.append("}");
	}

	public static BulletinBoard parseBulletinBoard(String boardStr) {
		BulletinBoard board = new BulletinBoard();
		parseBulletinbBoardEntries(boardStr, board);
		return board;
	}

	private static String parseBulletinbBoardEntries(String boardStr,
			BulletinBoard board) {
		if (boardStr.length() <= 2) {
			return null;
		}
		int index = -1;
		Article article = null;
		do {
			index = boardStr.indexOf(Article.FORMAT_END);
			article = Article.parseArticle(boardStr.substring(1, index + 1));
			if (article.getParentId() == 0) {
				board.addArticle(article);
			} else {
				board.addArticleReply(article);
			}
			boardStr = boardStr.substring(index + 1);
			if (boardStr.startsWith(BulletinBoard.FORMAT_START)) {
				boardStr = parseBulletinbBoardEntries(boardStr, board);
			}
			if (boardStr.startsWith(BulletinBoard.FORMAT_ENDS)) {
				break;
			}
		} while (index != -1);
		return boardStr.substring(1);
	}

	private class BulletinBoardEntry {
		private Article article;
		private List<Integer> replyList;

		BulletinBoardEntry(Article article) {
			this.article = article;
		}

		Article getArticle() {
			return article;
		}

		List<Integer> getReplyIdList() {
			return replyList;
		}

		void addReplyId(int id) {
			if (replyList == null) {
				replyList = new ArrayList<Integer>();
			}
			replyList.add(id);
		}
	}
}
