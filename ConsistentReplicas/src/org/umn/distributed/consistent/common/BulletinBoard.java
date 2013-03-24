package org.umn.distributed.consistent.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedMap;
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
			}
			/*
			 * if article is already present, we need to update the reply list
			 * using the addReply method
			 */
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

	/**
	 * No locking consistency here, hence make sure only one thread is accessing
	 * these bb's
	 * 
	 * @param bbToMergeIn
	 * @param bbTwo
	 * @return
	 */
	public static BulletinBoard mergeBB(BulletinBoard bbToMergeIn,
			BulletinBoard bbTwo) {
		assert (false);

		if (bbToMergeIn == null) {
			bbToMergeIn = bbTwo;
		} else {
			if (bbTwo != null) {
				mergeTwoBBs(bbToMergeIn, bbTwo);
			}
		}

		return bbToMergeIn;
	}

	private static void mergeTwoBBs(BulletinBoard bbToMergeIn,
			BulletinBoard bbTwo) {
		Collection<BulletinBoardEntry> entries = bbTwo.map.values();
		for (BulletinBoardEntry entry : entries) {
			/**
			 * Logic if the bbToMergeIn does not contain a particular entry from
			 * bbTwo this means that bbTwo has something extra, and we need to
			 * add this to bbToMergeIn. z
			 * 
			 * <pre>
			 * If on the other hand if the entry exits, we still might need to update
			 * the reply list.
			 * <code>
			 * bbOne			bbTwo
			 * 1,A1,0,<2>		1,A1,0,<3>
			 * 2,A2,1,<>		null
			 * 3,null,<4>		3,A2,1,<>
			 * 4,A4,3,<>		4,null,0,<5>
			 * null				5,A5,4,<>
			 * 6,null,0,<7>		6,null,0,<8>
			 * 7,A7,6,<>		null
			 * null				8,A8,6,<>
			 * </code>
			 */
			BulletinBoardEntry boardEntry = bbToMergeIn.map.get(entry
					.getArticle().getId());
			/**
			 * <pre>
			 * Case 1:
			 * As the original entry is null,boardEntry == null, we can have a new parentId, which would mean that
			 * we need to update an existing entry in the bbToMergeIn
			 * Case 2: If new entry also does not have any parentID, then we can assume that this 
			 * is a root level and hence we do not need to disturb any other reply
			 * Case 3: Article is null in both the entries, but the reply list is different
			 * But in this case, as we process the list further we will hit the reply which caused
			 * the reply list to be different and then we will update the parent 
			 * reply list of the merged entity and hence it will be automatically handled
			 * </pre>
			 */
			if (boardEntry == null) {
				/*
				 * If mainBB or bbToMergeIn does not have an entry, then this is
				 * as latest as we are going to get hence simply add the
				 * BulletinBoardEntry to the mergeBB
				 */
				bbToMergeIn.map.put(entry.getArticle().getId(), entry);
				// but as this was not here in mergeMap initially,
				// if it had a parent, then the merge process would have
				// already merged the reply List

			} else {
				/**
				 * merge two cases,
				 * 
				 * <pre>
				 * case 1: Article is null but the replyList contains data.
				 * case 2: Article is not null, hence no new information to update, but need to merge list
				 * </pre>
				 */
				if (boardEntry.getArticle() == null) {
					boardEntry.setArticle(entry.getArticle());
					// a new parent could have come and a new replyList could
					// have come
					// need to take care only of the reply list, as parent id
					// would have been handled already
				}
				if (entry.getReplyIdList() != null) {
					// merge reply list
					boardEntry.getReplyIdList().addAll(entry.getReplyIdList());
				}

			}
		}
	}

	public void mergeWithMe(BulletinBoard bbToMerge) {
		writeL.lock();
		try {
			if (bbToMerge != null) {
				// Should work as I have obtained lock on the my own instance
				mergeTwoBBs(this, bbToMerge);
			}
		} finally {
			writeL.unlock();
		}

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

		private void setArticle(Article a) {
			this.article = a;
		}

		private Set<Integer> getReplyIdList() {
			Set<Integer> newReplyList = new HashSet<Integer>();
			newReplyList.addAll(replyList);
			return newReplyList;
		}

		private void addReplyId(int id) {
			if (replyList == null) {
				replyList = new HashSet<Integer>();
			}
			replyList.add(id);
		}
	}

	public List<Article> getArticlesFrom(int lastSyncArticleId) {
		List<Article> articles = new LinkedList<Article>();
		readL.lock();
		try {
			// we need articles from more than lastSyncArticleId, so base case
			// is lastSyncArticleId == 0
			Entry<Integer, BulletinBoardEntry> entry = map
					.ceilingEntry(lastSyncArticleId + 1);
			SortedMap<Integer,BulletinBoardEntry> tailMap = map.tailMap(entry.getKey());
			
			for(Entry<Integer, BulletinBoardEntry> entryItr : tailMap.entrySet()){
				articles.add(entryItr.getValue().getArticle());
			}
		} finally {
			readL.unlock();
		}
		return articles;
	}

	/**
	 * expects articleString as article1;article2;..
	 * @param articleListInString
	 * @return
	 */
	public static BulletinBoard parseBBFromArticleList(String articleListInString) {
		BulletinBoard bb = new BulletinBoard();
		String[] articleList = articleListInString.split(";");
		for(String art:articleList){
			Article a = Article.parseArticle(art);
			if(a.isRoot()){
				bb.addArticle(a);
			}else{
				bb.addArticleReply(a);
			}
		}
		return bb;
	}

}
