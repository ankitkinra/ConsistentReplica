package org.umn.distributed.consistent.common;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.umn.distributed.consistent.server.AbstractServer;

public class BulletinBoard {

	private static final Integer BASE_ARTICLE_ID = 0;
	public static final String FORMAT_START = "{";
	public static final String FORMAT_ENDS = "}";
	public static final String NULL_ARTICLE_START = Article.FORMAT_START
			+ "null";
	// TODO need to protect this map under ReentrantReadWriteLock

	private ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
	private final Lock readL = rwl.readLock();
	private final Lock writeL = rwl.writeLock();

	private TreeMap<Integer, BulletinBoardEntry> map = new TreeMap<Integer, BulletinBoard.BulletinBoardEntry>();

	public boolean addArticle(Article article) {
		writeL.lock();
		try {
			/*
			 * If a bb is already present we check if it has null article or
			 * not. We create null articles or full article. In case any full
			 * article is present in the boardEntry then we are sure that its
			 * valid.
			 */
			BulletinBoardEntry boardEntry = map.get(article.getId());
			if (boardEntry == null) {
				map.put(article.getId(), new BulletinBoardEntry(article));
			} else {
				Article bbArticle = boardEntry.getArticle();
				if (bbArticle == null) {
					boardEntry.setArticle(article);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
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
					map.put(article.getParentId(), boardEntry);
				}
				boardEntry.addReplyId(article.getId());
				return true;
			}
			return false;
		} finally {
			writeL.unlock();
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
				return boardEntry.getArticle();
			}
		} finally {
			readL.unlock();
		}
	}

	public Integer getMaxId() {
		readL.lock();
		try {
			if (map.size() > 0) {
				return map.lastEntry().getKey();
			} else {
				return BASE_ARTICLE_ID;
			}
		} finally {
			readL.unlock();
		}
	}

	public Article getMaxArticle() {
		readL.lock();
		try {
			if (map.size() > 0) {
				BulletinBoardEntry entry = map.lastEntry().getValue();
				if (entry != null) {
					return entry.getArticle();
				} else {
					return null;
				}
			} else {
				return null;
			}
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
			// Iterator<Integer> it =
			// map.descendingKeySet().descendingIterator();
			for (Entry<Integer, BulletinBoardEntry> entry : map.entrySet()) {
				BulletinBoardEntry boardEntry = entry.getValue();
				Article article = boardEntry.getArticle();
				if (article == null || article.getParentId() == 0) {
					// TODO: create a copy of the object before passing here
					appendToString(entry.getKey(), builder, detailed);
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
			Article article = entry.getArticle();
			String articleStr = null;
			if (article == null) {
				articleStr = NULL_ARTICLE_START + "|" + id + Article.FORMAT_END;
			} else {
				if (detailed) {
					articleStr = entry.getArticle().toString();
				} else {
					articleStr = entry.getArticle().toShortString();
				}
			}
			builder.append(articleStr);
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

	public static BulletinBoard parseBulletinBoard(String boardStr)
			throws IllegalArgumentException {
		BulletinBoard board = new BulletinBoard();
		parseAndAddBBEntriesIntoBB(board, boardStr);
		return board;
	}

	// TODO: test it
	public static void parseAndAddBBEntriesIntoBB(BulletinBoard board,
			String boardStr) throws IllegalArgumentException {
		if (boardStr == null || boardStr.length() <= 2) {
			return;
		}
		int indexStart = 0;
		int indexEnd = 0;
		while ((indexStart = boardStr.indexOf(Article.FORMAT_START, indexEnd)) > -1
				&& (indexEnd = boardStr.indexOf(Article.FORMAT_END, indexEnd)) > -1) {
			if (indexStart > indexEnd) {
				throw new IllegalArgumentException(
						"Error parsing bulletin board. Illegal format.");
			}
			indexEnd += 1;
			String articleStr = boardStr.substring(indexStart, indexEnd);
			if (!articleStr.startsWith(NULL_ARTICLE_START)) {
				Article article = Article.parseArticle(articleStr);
				if (article.isRoot()) {
					board.addArticle(article);
				} else {
					board.addArticleReply(article);
				}
			}
		}
		// do {
		// index = boardStr.indexOf(Article.FORMAT_END);
		// article = Article.parseArticle(boardStr.substring(1, index + 1));
		// if (article.getParentId() == 0) {
		// board.addArticle(article);
		// } else {
		// board.addArticleReply(article);
		// }
		// boardStr = boardStr.substring(index + 1);
		// if (boardStr.startsWith(BulletinBoard.FORMAT_START)) {
		// boardStr = parseBulletinbBoardEntries(boardStr, board);
		// }
		// if (boardStr.startsWith(BulletinBoard.FORMAT_ENDS)) {
		// break;
		// }
		// } while (index != -1);
	}

	/**
	 * No locking consistency here, hence make sure only one thread is accessing
	 * these bb's
	 * 
	 * @param bbToMergeIn
	 * @param bbTwo
	 * @return
	 */
	// public static BulletinBoard mergeBB(BulletinBoard bbToMergeIn,
	// BulletinBoard bbTwo) {
	// if (bbToMergeIn == null) {
	// bbToMergeIn = bbTwo;
	// } else {
	// if (bbTwo != null) {
	// mergeTwoBBs(bbToMergeIn, bbTwo);
	// }
	// }
	//
	// return bbToMergeIn;
	// }

	// private static void mergeTwoBBs(BulletinBoard bbToMergeIn,
	// BulletinBoard bbTwo) {
	// Collection<BulletinBoardEntry> entries = bbTwo.map.values();
	// for (BulletinBoardEntry entry : entries) {
	// /**
	// * Logic if the bbToMergeIn does not contain a particular entry from
	// * bbTwo this means that bbTwo has something extra, and we need to
	// * add this to bbToMergeIn. z
	// *
	// * <pre>
	// * If on the other hand if the entry exits, we still might need to update
	// * the reply list.
	// * <code>
	// * bbOne bbTwo
	// * 1,A1,0,<2> 1,A1,0,<3>
	// * 2,A2,1,<> null
	// * 3,null,<4> 3,A2,1,<>
	// * 4,A4,3,<> 4,null,0,<5>
	// * null 5,A5,4,<>
	// * 6,null,0,<7> 6,null,0,<8>
	// * 7,A7,6,<> null
	// * null 8,A8,6,<>
	// * </code>
	// */
	// Article secondArticle = entry.getArticle();
	// BulletinBoardEntry boardEntryToMergeIn = null;
	// if (secondArticle != null) {
	// boardEntryToMergeIn = bbToMergeIn.map
	// .get(secondArticle.getId());
	// }
	// /**
	// * <pre>
	// * Case 1:
	// * As the original entry is null,boardEntry == null, we can have a new
	// parentId, which would mean that
	// * we need to update an existing entry in the bbToMergeIn
	// * Case 2: If new entry also does not have any parentID, then we can
	// assume that this
	// * is a root level and hence we do not need to disturb any other reply
	// * Case 3: Article is null in both the entries, but the reply list is
	// different
	// * But in this case, as we process the list further we will hit the reply
	// which caused
	// * the reply list to be different and then we will update the parent
	// * reply list of the merged entity and hence it will be automatically
	// handled
	// * </pre>
	// */
	// if (boardEntryToMergeIn == null) {
	// /*
	// * If mainBB or bbToMergeIn does not have an entry, then this is
	// * as latest as we are going to get hence simply add the
	// * BulletinBoardEntry to the mergeBB
	// */
	// bbToMergeIn.map.put(entry.getArticle().getId(), entry);
	// // but as this was not here in mergeMap initially,
	// // if it had a parent, then the merge process would have
	// // already merged the reply List
	//
	// } else {
	// /**
	// * merge two cases,
	// *
	// * <pre>
	// * case 1: Article is null but the replyList contains data.
	// * case 2: Article is not null, hence no new information to update, but
	// need to merge list
	// * </pre>
	// */
	// if (boardEntryToMergeIn.getArticle() == null) {
	// boardEntryToMergeIn.setArticle(entry.getArticle());
	// // a new parent could have come and a new replyList could
	// // have come
	// // need to take care only of the reply list, as parent id
	// // would have been handled already
	// }
	// if (entry.getReplyIdList() != null) {
	// // merge reply list
	// boardEntryToMergeIn.getReplyIdList().addAll(
	// entry.getReplyIdList());
	// }
	//
	// }
	// }
	// }

	public void mergeWithMe(BulletinBoard bbToMerge) {
		if (bbToMerge != null) {
			// Should work as I have obtained lock on the my own instance
			Collection<BulletinBoardEntry> entries = bbToMerge.map.values();
			for (BulletinBoardEntry entry : entries) {
				Article article = entry.getArticle();
				if (article != null) {
					if (article.isRoot()) {
						this.addArticle(article);
					} else {
						this.addArticleReply(article);
					}
				}
			}
		}
	}

	private class BulletinBoardEntry {
		private Article article;
		private Set<Integer> replyList;

		BulletinBoardEntry(Article article) {
			this.article = article;
		}

		Article getArticle() {
			if (article == null) {
				return null;
			}
			return new Article(article);
		}

		private void setArticle(Article a) {
			this.article = a;
		}

		private Set<Integer> getReplyIdList() {
			Set<Integer> newReplyList = new HashSet<Integer>();
			if (replyList != null) {
				newReplyList.addAll(replyList);
			}
			return newReplyList;
		}

		private void addReplyId(int id) {
			if (replyList == null) {
				replyList = new HashSet<Integer>();
			}
			if (!replyList.contains(id)) {
				replyList.add(id);
			}
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("BulletinBoardEntry [article=");
			builder.append(article);
			builder.append(", replyList=");
			builder.append(replyList);
			builder.append("]");
			return builder.toString();
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
			System.out.println(String.format("entry=%s, lastSyncArticleId=%s",
					entry, lastSyncArticleId));
			SortedMap<Integer, BulletinBoardEntry> tailMap = entry != null ? map
					.tailMap(entry.getKey()) : null;
			if (tailMap != null) {
				for (Entry<Integer, BulletinBoardEntry> entryItr : tailMap
						.entrySet()) {
					Article article = entryItr.getValue().getArticle();
					if (article != null) {
						articles.add(article);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			readL.unlock();
		}
		return articles;
	}

	public List<Article> getAllArticles() {
		List<Article> articles = new LinkedList<Article>();
		readL.lock();
		try {
			if (map != null && map.size() > 0) {
				for (Entry<Integer, BulletinBoardEntry> entryItr : map
						.entrySet()) {
					Article article = entryItr.getValue().getArticle();
					if (article != null) {
						articles.add(article);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			readL.unlock();
		}
		return articles;
	}

	/**
	 * expects articleString as article1;article2;.. unused
	 * 
	 * @param articleListInString
	 * @return
	 */
	public static BulletinBoard parseBBFromArticleList(
			String articleListInString) {
		BulletinBoard bb = new BulletinBoard();
		if (!Utils.isEmpty(articleListInString)) {
			String[] articleList = articleListInString
					.split(AbstractServer.LIST_SEPARATOR);
			for (String art : articleList) {
				Article a = null;
				try {
					a = Article.parseArticle(art);
					System.out.println("Parsed article; a = " + a);
					if (a.isRoot()) {
						bb.addArticle(a);
					} else {
						bb.addArticleReply(a);
					}
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
					throw e;
				}
			}
		}
		return bb;
	}

}
