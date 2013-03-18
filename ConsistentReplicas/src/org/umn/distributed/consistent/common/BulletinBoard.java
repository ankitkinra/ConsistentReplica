package org.umn.distributed.consistent.common;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class BulletinBoard {

	private class BulletinBoardEntry {
		private Article article;
		private List<Integer> replyList;
		
		BulletinBoardEntry(Article article) {
			this.article = article;
		}
		
		Article getArticle( ) {
			return article;
		}
		
		List<Integer> getReplyIdList() {
			return replyList;
		}
		
		void addReplyId(int id) {
			if(replyList == null) {
				replyList = new ArrayList<Integer>();
			}
			replyList.add(id);
		}
	}
	private TreeMap<Integer, BulletinBoardEntry> map = new TreeMap<Integer, BulletinBoard.BulletinBoardEntry>();
	
	public boolean addArticle(Article article) {
		//TODO: handle malicious coordinator. It should never return the same id back
		map.put(article.getId(), new BulletinBoardEntry(article));
		return true;
	}
	
	public boolean addArticleReply(Article article) {
		if(addArticle(article)) {
			BulletinBoardEntry boardEntry = map.get(article.getParentId());
			if(boardEntry == null) {
				boardEntry = new BulletinBoardEntry(null);
				boardEntry.addReplyId(article.getId());
				map.put(article.getParentId(), boardEntry);
			}
			else {
				boardEntry.addReplyId(article.getId());
			}
			//TODO: handle malicious coordinator. It should never return the same id back.
			return true;
		}
		return false;
	}
	
	/*
	 * Use this method to check if the BulletinBoard contains the id in the map or not. 
	 */
	public boolean isArticle(int id) {
		return map.containsKey(id);
	}
	
	/*
	 * Returns the article with a given id. This will return null if:
	 * 1. there is not entry in the map for a given id
	 * 2. article object is null corresponding to given id
	 * Note that the second case will happen in case of quorum consistency. Even
	 * though this method returns null, isArticle() can still return true (case 2).
	 */
	public Article getArticle(int id) {
		BulletinBoardEntry boardEntry = map.get(id);
		if(boardEntry == null) {
			return null;
		}
		else {
			return boardEntry.getArticle();
		}
	}
	
	public List<Integer> getReplyIdList(int id) {
		BulletinBoardEntry boardEntry = map.get(id);
		if(boardEntry == null) {
			return null;
		}
		else {
			return boardEntry.getReplyIdList();
		}
	}
	
	@Override
	public String toString() {
		return null;
	}
	
	
	public static BulletinBoard parseBulletinBoard(String bulletinBoardStr) {
		return new BulletinBoard();
	}
}
