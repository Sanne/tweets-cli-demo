/*
 /*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.demos.tweets;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.TransactionManager;

import org.apache.lucene.misc.TermStats;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.hibernate.search.demos.tweets.ServiceImpl;
import org.hibernate.search.demos.tweets.Tweet;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Some tests aimed at human reading to introduce
 * the Hibernate Search API and which strange things it can do.
 */
public class ServiceTest {

	private static EntityManagerFactory entityManagerFactory;

	@BeforeClass
	static public void prepareTestData() throws Exception {
		entityManagerFactory = Persistence.createEntityManagerFactory("test-tweets");

		EntityManager entityManager = entityManagerFactory.createEntityManager();
		beginTransaction();
		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager( entityManagerFactory.createEntityManager() );

		// make sure the indexes are empty at start as we're using a filesystem based index:
		fullTextEntityManager.purgeAll( Tweet.class );
		fullTextEntityManager.flushToIndexes();

		// store some tweets
		fullTextEntityManager.persist( new Tweet( "What's Drools? never heard of it", "SmartMarketingGuy", 50l ) );
		fullTextEntityManager.persist( new Tweet( "We love Hibernate", "SmartMarketingGuy", 30l ) );
		fullTextEntityManager.persist( new Tweet( "I wouldn't vote for Drools", "SmartMarketingGuy", 2l ) );
		//note the accent on "I", still needs to match search for "infinispan"
		fullTextEntityManager.persist( new Tweet( "we are looking forward to Ìnfinispan", "AnotherMarketingGuy", 600000l ) );
		fullTextEntityManager.persist( new Tweet( "Hibernate OGM", "AnotherMarketingGuy", 600001l ) );
		fullTextEntityManager.persist( new Tweet( "What is Hibernate OGM?", "ME!", 61000l ) );
		fullTextEntityManager.persist( new Tweet( "Cheating by repeat: hibernate hibernate hibernate hibernate", "ME!", 62000l ) );

		commitTransaction();
		entityManager.close();
	}

	@Test
	public void testHibernateSearchJPAAPIUsage() throws Exception {
		EntityManager entityManager = entityManagerFactory.createEntityManager();

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager( entityManagerFactory.createEntityManager() );
		ServiceImpl service = new ServiceImpl( fullTextEntityManager );

		FullTextQuery droolsQuery = service.messagesMentioning( "Drools" );
		Assert.assertEquals( 2, droolsQuery.getResultSize() );
		List list = droolsQuery.getResultList();

		// now with weird characters, still works fine:
		droolsQuery = service.messagesMentioning( "dRoÖls" );
		Assert.assertEquals( 2, droolsQuery.getResultSize() );

		FullTextQuery infinispanQuery = service.messagesMentioning( "infinispan" );
		Assert.assertEquals( 1, infinispanQuery.getResultSize() );
		Tweet infinispanRelatedTweet = (Tweet) infinispanQuery.getResultList().get( 0 );
		Assert.assertEquals( "we are looking forward to Ìnfinispan", infinispanRelatedTweet.getMessage() );

		FullTextQuery messagesBySmartMarketingGuy = service.messagesBy( "SmartMarketingGuy" );
		Assert.assertEquals( 3, messagesBySmartMarketingGuy.getResultSize() );

		FullTextQuery timeSortedTweets = service.allTweetsSortedByTime();
		List resultList = timeSortedTweets.getResultList();

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
		entityManager.close();
	}

	@Test
	public void testIndexRebuild() throws Exception {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager( entityManagerFactory.createEntityManager() );
		fullTextEntityManager.createIndexer(Tweet.class).startAndWait();
		entityManager.close();
	}

	private static void commitTransaction() throws Exception {
		extractJBossTransactionManager(entityManagerFactory).getTransaction().commit();
	}

	private static void beginTransaction() throws Exception {
		extractJBossTransactionManager(entityManagerFactory).begin();
	}

	public static TransactionManager extractJBossTransactionManager(EntityManagerFactory factory) {
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) ( (HibernateEntityManagerFactory) factory ).getSessionFactory();
		return sessionFactory.getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager();
	}

	@AfterClass
	public static void cleanData() throws Exception {
		if (entityManagerFactory != null) entityManagerFactory.close();
	}

}
