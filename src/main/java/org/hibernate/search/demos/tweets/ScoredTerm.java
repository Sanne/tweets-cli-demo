/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.demos.tweets;

/**
 * A simple indexed term value representation with its frequency count.
 * It's comparable so that you can iterate a sorted collection from the most
 * frequent term.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class ScoredTerm implements Comparable<ScoredTerm> {

	public final String term;
	public final int frequency;

	public ScoredTerm(String term, int frequency) {
		if ( term == null ) throw new IllegalArgumentException( "term argument is not optional" );
		this.term = term;
		this.frequency = frequency;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + frequency;
		result = prime * result + ( ( term == null ) ? 0 : term.hashCode() );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj )
			return true;
		if ( obj == null )
			return false;
		if ( getClass() != obj.getClass() )
			return false;
		ScoredTerm other = (ScoredTerm) obj;
		if ( frequency != other.frequency )
			return false;
		if ( term == null ) {
			if ( other.term != null )
				return false;
		}
		else if ( !term.equals( other.term ) )
			return false;
		return true;
	}

	@Override
	public int compareTo(ScoredTerm o) {
		int diff = o.frequency - this.frequency;
		if ( diff == 0 ) {
			return this.term.compareTo( o.term );
		}
		else {
			return diff;
		}
	}

	@Override
	public String toString() {
		return "ScoredTerm [term=" + term + ", frequency=" + frequency + "]";
	}

}
