package org.umn.distributed.consistent.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.management.RuntimeErrorException;

public class BulletinBoard {

	public static final String FORMAT_START = "{";
	public static final String FORMAT_ENDS = "}";
	// TODO need to protect this map under ReentrantReadWriteLock

	private ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
	private final Lock readL = rwl.readLock();
	private final Lock writeL = rwl.writeLock();

	private TreeMap<Integer, BulletinBoardEntry> map = new TreeMap<Integer, BulletinBoard.BulletinBoardEntry>();

	public boolean addArticle(Article article) {
		// TODO: handle malicious coordinator. It should never return the same
		// id back
		writeL.lock();
		try {
			if (!containsArticle(article.getId())) {
				map.put(article.getId(), new BulletinBoardEntry(article));
			} else {
				// TODO update reply list as content/title cannot be changed
			}
		} finally {

			writeL.unlock();
		}
		return true;
	}

	/**
	 * Why can parentBoardEntry be null, this can happen when this replica gets
	 * selected for a quorum write which has a reply to a an article it has not
	 * yet seen, but will see soon in a sync operation
	 * 
	 * @param article
	 * @return
	 */
	public boolean addArticleReply(Article article) {
		writeL.lock();
		// now we are assured that any previous writes are complete
		try {
			if (addArticle(article)) {
				BulletinBoardEntry boardEntry = map.get(article.getParentId());
				if (boardEntry == null) {
					boardEntry = new BulletinBoardEntry(null);
					boardEntry.addReplyId(article.getId());
					map.put(article.getParentId(), boardEntry);
				} else {
					boardEntry.addReplyId(article.getId());
				}
				// TODO: handle malicious coordinator. It should never return
				// the
				// same id back.
				return true;
			}
			return false;
		} finally {
			writeL.unlock();
		}

	}

	/*
	 * Use this method to check if the BulletinBoard contains the id in the map
	 * or not.
	 */
	public boolean containsArticle(int id) {
		readL.lock();
		try {
			return map.containsKey(id);
		} finally {
			readL.unlock();
		}
	}

	/*
	 * Returns the article with a given id. This will return null if: 1. there
	 * is not entry in the map for a given id 2. article object is null
	 * corresponding to given id Note that the second case will happen in case
	 * of quorum consistency. Even though this method returns null, isArticle()
	 * can still return true (case 2).
	 */
	public Article getArticle(int id) {
		readL.lock();
		try {
			BulletinBoardEntry boardEntry = map.get(id);
			if (boardEntry == null) {
				return null;
			} else {
				return new Article(boardEntry.getArticle());
			}
		} finally {
			readL.unlock();
		}
	}

	public Integer getMaxId() {
		readL.lock();
		try {
			return map.lastEntry().getKey();
		} finally {
			readL.unlock();
		}
	}

	public Set<Integer> getReplyIdList(int id) {
		readL.lock();
		try {
			BulletinBoardEntry boardEntry = map.get(id);
			if (boardEntry == null) {
				return null;
			} else {
				return boardEntry.getReplyIdList();
			}
		} finally {
			readL.unlock();
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
	private String toString(boolean detailed) {
		StringBuilder builder = new StringBuilder();
		// TODO: Check if returns an increasing order iterator or not.
		readL.lock();
		try {
			Iterator<Integer> it = map.descendingKeySet().descendingIterator();
			while (it.hasNext()) {
				BulletinBoardEntry boardEntry = map.get(it.next());
				Article article = boardEntry.getArticle();
				if (article.getParentId() == 0) {
					// TODO: create a copy of the object before passing here
					appendToString(article.getId(), builder, detailed);
				}
			}
			return builder.toString();
		} finally {
			readL.unlock();
		}
	}

	private void appendToString(Integer id, StringBuilder builder,
			boolean detailed) {
		readL.lock();
		try {
			builder.append("{");
			BulletinBoardEntry entry = map.get(id);
			if (detailed) {
				builder.append(entry.getArticle().toString());
			} else {
				builder.append(entry.getArticle().toShortString());
			}
			for (Integer replyId : entry.getReplyIdList()) {
				appendToString(replyId, builder, detailed);
			}
			builder.append("}");
		} finally {
			readL.unlock();
		}
	}

	public void replaceBB(BulletinBoard bbNew) {
		writeL.lock();
		try {
			this.map = bbNew.map;
		} finally {
			writeL.unlock();
		}
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

	public static BulletinBoard mergeBB(BulletinBoard bbToMergeIn,
			BulletinBoard bbTwo) {
		assert(false);
		
		if (bbToMergeIn == null) {
			bbToMergeIn = bbTwo;
		} else {
			// if merge has some value and non null, we need to everything from
			// two to One
			if (bbTwo != null) {
				// TODO cannot simply putAll as we need to modify the reply list
				// in the Article
				Collection<BulletinBoardEntry> entries = bbTwo.map.values();
				for (BulletinBoardEntry entry : entries) {
					//TODO verify that entry cannot be null with dhiman in parsing logic
					BulletinBoardEntry boardEntry = bbToMergeIn.map.get(entry
							.getArticle().getId());
					if (boardEntry == null) {
						// need to add, this is latest
						if (entry.getArticle().isRoot()) {
							bbToMergeIn.addArticle(entry.getArticle());
						}else{
							bbToMergeIn.addArticleReply(entry.getArticle()); 
						}
					} else {
						// merge
						// two cases, one Article is empty but the replyList contains data.
						// Article List is not empty
						if (boardEntry.getArticle() == null){
							bbToMergeIn.addArticle(entry.getArticle()); // no worse than current
						}
						if (entry.getReplyIdList() != null) {
							//merge reply list
							boardEntry.getReplyIdList().addAll(
									entry.getReplyIdList());
						}
						
					}
				}
			}
		}

		return bbToMergeIn;
	}

	private class BulletinBoardEntry {
		private Article article;
		private Set<Integer> replyList;

		BulletinBoardEntry(Article article) {
			this.article = article;
		}

		Article getArticle() {
			return new Article(article);
		}

		Set<Integer> getReplyIdList() {
			Set<Integer> newReplyList = new HashSet<Integer>();
			newReplyList.addAll(replyList);
			return newReplyList;
		}

		void addReplyId(int id) {
			if (replyList == null) {
				replyList = new HashSet<Integer>();
			}
			replyList.add(id);
		}
	}

	public void mergeWithMe(BulletinBoard mergedBulletinBoard) {
		writeL.lock();
		try {
			// TODO cannot simply putAll as we need to modify the reply list in
			// the Article
			// for(Map.Entry<Integer, Article>)
		} finally {
			writeL.unlock();
		}

	}
}
