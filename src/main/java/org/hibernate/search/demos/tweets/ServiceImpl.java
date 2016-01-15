/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.demos.tweets;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.misc.TermStats;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.query.dsl.QueryBuilder;

/**
 * Example service, how the queries I'm aware of could be implemented.
 * All methods tested by org.hibernate.search.demos.tweets.ServiceTest
 */
public class ServiceImpl {

	private final FullTextEntityManager fullTextEntityManager;

	ServiceImpl(FullTextEntityManager fullTextEntityManager) {
		this.fullTextEntityManager = fullTextEntityManager;
	}

	/**
	 * creates a FullTextQuery for all tweets mentioning a specific word/technology in the message field.
	 */
	public FullTextQuery messagesMentioning(String keyword) {
		Query query = getQueryBuilder().keyword().onField( "message" ).matching( keyword ).createQuery();
		FullTextQuery fullTextQuery = fullTextEntityManager.createFullTextQuery( query );
		return fullTextQuery;
	}

	/**
	 * Searches for all tweets from a specific account. This is case-sensitive.
	 */
	public FullTextQuery messagesBy(String name) {
		Query query = getQueryBuilder().keyword().onField( "sender" ).matching( name ).createQuery();
		FullTextQuery fullTextQuery = fullTextEntityManager.createFullTextQuery( query );
		return fullTextQuery;
	}

	/**
	 * To search for all tweets, sorted in creation order (assuming the timestamp is correct).
	 * @return
	 */
	public FullTextQuery allTweetsSortedByTime() {
		Query query = getQueryBuilder().all().createQuery();
		FullTextQuery fullTextQuery = fullTextEntityManager.createFullTextQuery( query );
		fullTextQuery.setSort( new Sort( new SortedNumericSortField( "timestamp", SortField.Type.LONG ) ) );
		return fullTextQuery;
	}

	/**
	 * This is the most complex case, requiring 'low level' access to a Lucene IndexReader to
	 * directly read frequency information from the index.
	 * This is a practical way to make a tag cloud out of all most relevant indexed terms.
	 * @param inField Will return only scoredTerms in the specified field
	 * @param numberOfResults how many terms should be returned, from the most frequent going down.
	 */
	TermStats[] mostFrequentlyUsedTerms(String inField, int numberOfResults) throws Exception {
		SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
		IndexReader indexReader = searchFactory.getIndexReaderAccessor().open( Tweet.class );
		try {
			return org.apache.lucene.misc.HighFreqTerms.getHighFreqTerms(
					indexReader, numberOfResults, inField,
					new org.apache.lucene.misc.HighFreqTerms.DocFreqComparator() );
		}
		finally {
			searchFactory.getIndexReaderAccessor().close( indexReader );
		}
	}

	private QueryBuilder getQueryBuilder() {
		return fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity( Tweet.class ).get();
	}

}
