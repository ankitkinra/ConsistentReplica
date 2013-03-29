package org.umn.distributed.consistent.common.client.testfrmwk;

import org.umn.distributed.consistent.common.Article;
import org.umn.distributed.consistent.common.Machine;

public class ArticlesPublished {
	private int roundIndexPublishedOn;
	private Article article;
	private Machine publishedOn;
	private long timeRequiredToPublish;
	public ArticlesPublished(int roundIndexPublishedOn, Article article,
			Machine publishedOn, long timeRequiredToPublish) {
		super();
		this.roundIndexPublishedOn = roundIndexPublishedOn;
		this.article = article;
		this.publishedOn = publishedOn;
		this.timeRequiredToPublish = timeRequiredToPublish;
	}
	public int getRoundIndexPublishedOn() {
		return roundIndexPublishedOn;
	}
	public Article getArticle() {
		return article;
	}
	public Machine getPublishedOn() {
		return publishedOn;
	}
	public long getTimeRequiredToPublish() {
		return timeRequiredToPublish;
	}
	
	
	/*
	 * INFO quorum details are not accessible at this level
	 * hence to make graphs we need to make the system stable and 
	 * then publish articles by invoking a test client
	 */
}
