/*
 /*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.demos.tweets;

import java.util.List;

import org.apache.lucene.misc.TermStats;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Some tests aimed at human reading to introduce
 * the Hibernate Search API and which strange things it can do.
 */
public class ServiceTest {

	private SessionFactory buildSessionFactory;

	@Before
	public void prepareTestData() throws Exception {
		buildSessionFactory = HibernateUtil.buildSessionFactory();

		try ( Session session = buildSessionFactory.openSession() ) {
			Transaction transaction = session.beginTransaction();
			FullTextSession ftSession = Search.getFullTextSession( session );

			// make sure the indexes are empty at start as we're using a filesystem based index:
			ftSession.purgeAll( Tweet.class );
			ftSession.flushToIndexes();

			// store some tweets
			ftSession.persist( new Tweet( "What's Drools? never heard of it", "SmartMarketingGuy", 50l ) );
			ftSession.persist( new Tweet( "We love Hibernate", "SmartMarketingGuy", 30l ) );
			ftSession.persist( new Tweet( "I wouldn't vote for Drools", "SmartMarketingGuy", 2l ) );
			//note the accent on "I", still needs to match search for "infinispan"
			ftSession.persist( new Tweet( "we are looking forward to Ìnfinispan", "AnotherMarketingGuy", 600000l ) );
			ftSession.persist( new Tweet( "Hibernate OGM", "AnotherMarketingGuy", 600001l ) );
			ftSession.persist( new Tweet( "What is Hibernate OGM?", "ME!", 61000l ) );
			ftSession.persist( new Tweet( "Cheating by repeat: hibernate hibernate hibernate hibernate", "ME!", 62000l ) );

			transaction.commit();
		}
	}

	@Test
	public void testHibernateSearchUsingHibernateNativeAPIs() throws Exception {
		try ( Session session = buildSessionFactory.openSession() ) {

			FullTextSession ftSession = Search.getFullTextSession( session );
			ServiceImpl service = new ServiceImpl( ftSession );

			FullTextQuery droolsQuery = service.messagesMentioning( "Drools" );
			Assert.assertEquals( 2, droolsQuery.getResultSize() );
			List list = droolsQuery.list();

			// now with weird characters, still works fine:
			droolsQuery = service.messagesMentioning( "dRoÖls" );
			Assert.assertEquals( 2, droolsQuery.getResultSize() );

			FullTextQuery infinispanQuery = service.messagesMentioning( "infinispan" );
			Assert.assertEquals( 1, infinispanQuery.getResultSize() );
			Tweet infinispanRelatedTweet = (Tweet) infinispanQuery.list().get( 0 );
			Assert.assertEquals( "we are looking forward to Ìnfinispan", infinispanRelatedTweet.getMessage() );

			FullTextQuery messagesBySmartMarketingGuy = service.messagesBy( "SmartMarketingGuy" );
			Assert.assertEquals( 3, messagesBySmartMarketingGuy.getResultSize() );

			FullTextQuery timeSortedTweets = service.allTweetsSortedByTime();
			List resultList = timeSortedTweets.list();

			Assert.assertEquals( 7, resultList.size() );
			Assert.assertEquals( 2l, ((Tweet) resultList.get( 0 ) ).getTimestamp() );
			Assert.assertEquals( 30l, ((Tweet) resultList.get( 1 ) ).getTimestamp() );
			Assert.assertEquals( 50l, ((Tweet) resultList.get( 2 ) ).getTimestamp() );
			Assert.assertEquals( 61000l, ((Tweet) resultList.get( 3 ) ).getTimestamp() );
			Assert.assertEquals( 62000l, ((Tweet) resultList.get( 4 ) ).getTimestamp() );
			Assert.assertEquals( 600000l, ((Tweet) resultList.get( 5 ) ).getTimestamp() );
			Assert.assertEquals( 600001l, ((Tweet) resultList.get( 6 ) ).getTimestamp() );

			TermStats[] mostFrequentlyUsedTerms = service.mostFrequentlyUsedTerms( "message", 10 );
			int i = 0;
			for ( TermStats scoredTerm : mostFrequentlyUsedTerms ) {
				if ( scoredTerm.termtext.utf8ToString().equals( "hibernate" ) ) {
					Assert.assertEquals( 4, scoredTerm.docFreq );
					i++;
				}
				if ( scoredTerm.termtext.utf8ToString().equals( "drools" ) ) {
					Assert.assertEquals( 2, scoredTerm.docFreq );
					i++;
				}
				if ( scoredTerm.termtext.utf8ToString().equals( "are" ) ) {
					Assert.fail( "should not find 'are' as it's in the stopwords list" );
				}
			}
			Assert.assertEquals( 2, i );

			// Now rebuild the index:
			ftSession.createIndexer().startAndWait();

			//And check the index content again:
			messagesBySmartMarketingGuy = service.messagesBy( "SmartMarketingGuy" );
			Assert.assertEquals( 3, messagesBySmartMarketingGuy.getResultSize() );
		}
	}

	@After
	public void cleanData() throws Exception {
		HibernateUtil.shutdown();
	}

}
