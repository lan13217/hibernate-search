/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.query.facet;

import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.query.engine.spi.FacetManager;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetSortOrder;
import org.hibernate.search.query.facet.FacetingRequest;

/**
 * @author Hardy Ferentschik
 */
public class SimpleFacetingTest extends AbstractFacetTest {
	private final String indexFieldName = "cubicCapacity";
	private final String facetName = "ccs";

	public void testSimpleFaceting() throws Exception {
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( indexFieldName )
				.discrete()
				.createFacetRequest();
		FullTextQuery query = queryHondaWithFacet( request );

		List<Facet> facetList = query.getFacetManager().getFacets( facetName );
		assertEquals( "Wrong number of facets", 4, facetList.size() );
	}

	public void testDefaultSortOrderIsCount() throws Exception {
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( indexFieldName )
				.discrete()
				.createFacetRequest();
		FullTextQuery query = queryHondaWithFacet( request );

		List<Facet> facetList = query.getFacetManager().getFacets( facetName );
		assertFacetCounts( facetList, new int[] { 5, 4, 4, 0 } );
	}

	public void testCountSortOrderAsc() throws Exception {
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( indexFieldName )
				.discrete()
				.orderedBy( FacetSortOrder.COUNT_ASC )
				.createFacetRequest();
		FullTextQuery query = queryHondaWithFacet( request );

		List<Facet> facetList = query.getFacetManager().getFacets( facetName );
		assertFacetCounts( facetList, new int[] { 0, 4, 4, 5 } );
	}

	public void testCountSortOrderDesc() throws Exception {
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( indexFieldName )
				.discrete()
				.orderedBy( FacetSortOrder.COUNT_DESC )
				.createFacetRequest();
		FullTextQuery query = queryHondaWithFacet( request );

		List<Facet> facetList = query.getFacetManager().getFacets( facetName );
		assertFacetCounts( facetList, new int[] { 5, 4, 4, 0 } );
	}

	public void testAlphabeticalSortOrder() throws Exception {
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( indexFieldName )
				.discrete()
				.orderedBy( FacetSortOrder.FIELD_VALUE )
				.createFacetRequest();
		FullTextQuery query = queryHondaWithFacet( request );

		List<Facet> facetList = query.getFacetManager().getFacets( facetName );
		for ( int i = 1; i < facetList.size() - 1; i++ ) {
			String previousFacetValue = facetList.get( i - 1 ).getValue();
			String currentFacetValue = facetList.get( i ).getValue();
			assertTrue( "Wrong alphabetical sort order", previousFacetValue.compareTo( currentFacetValue ) < 0 );
		}
	}

	public void testZeroCountsExcluded() throws Exception {
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( indexFieldName )
				.discrete()
				.orderedBy( FacetSortOrder.COUNT_DESC )
				.includeZeroCounts( false )
				.createFacetRequest();
		FullTextQuery query = queryHondaWithFacet( request );

		List<Facet> facetList = query.getFacetManager().getFacets( facetName );
		assertFacetCounts( facetList, new int[] { 5, 4, 4 } );
	}

	public void testMaxFacetCounts() throws Exception {
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( indexFieldName )
				.discrete()
				.maxFacetCount( 1 )
				.createFacetRequest();
		FullTextQuery query = queryHondaWithFacet( request );

		List<Facet> facetList = query.getFacetManager().getFacets( facetName );
		assertEquals( "The number of facets should be restricted", 1, facetList.size() );
	}

	public void testNullFieldNameThrowsException() {
		try {
			queryBuilder( Car.class ).facet()
					.name( facetName )
					.onField( null )
					.discrete()
					.createFacetRequest();
			fail( "null should not be a valid field name" );
		}
		catch ( IllegalArgumentException e ) {
			// success
		}
	}

	public void testNullRequestNameThrowsException() {
		try {
			queryBuilder( Car.class ).facet()
					.name( null )
					.onField( indexFieldName )
					.discrete()
					.createFacetRequest();
			fail( "null should not be a valid request name" );
		}
		catch ( IllegalArgumentException e ) {
			// success
		}
	}

	// todo - decide on the final behavior of this. Maybe throw an exception?
	public void testUnknownFieldNameReturnsEmptyResults() {
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( "foo" )
				.onField( "foobar" )
				.discrete()
				.createFacetRequest();
		FullTextQuery query = queryHondaWithFacet( request );
		assertTrue(
				"A unknown field name should not create any facets",
				query.getFacetManager().getFacets( "foo" ).size() == 0
		);
	}

	public void testEnableDisableFacets() {
		FacetingRequest request = queryBuilder( Car.class ).facet()
				.name( facetName )
				.onField( indexFieldName )
				.discrete()
				.createFacetRequest();
		FullTextQuery query = queryHondaWithFacet( request );

		assertTrue( "We should have facet results", query.getFacetManager().getFacets( facetName ).size() > 0 );

		query.getFacetManager().disableFaceting( facetName );
		query.list();

		assertTrue( "We should have no facets", query.getFacetManager().getFacets( facetName ).size() == 0 );
	}

	public void testMultipleFacets() {
		final String descendingOrderedFacet = "desc";
		FacetingRequest requestDesc = queryBuilder( Car.class ).facet()
				.name( descendingOrderedFacet )
				.onField( indexFieldName )
				.discrete()
				.createFacetRequest();

		final String ascendingOrderedFacet = "asc";
		FacetingRequest requestAsc = queryBuilder( Car.class ).facet()
				.name( ascendingOrderedFacet )
				.onField( indexFieldName )
				.discrete()
				.orderedBy( FacetSortOrder.COUNT_ASC )
				.createFacetRequest();
		TermQuery term = new TermQuery( new Term( "make", "honda" ) );
		FullTextQuery query = fullTextSession.createFullTextQuery( term, Car.class );
		FacetManager facetManager = query.getFacetManager();

		facetManager.enableFaceting( requestDesc );
		facetManager.enableFaceting( requestAsc );

		assertFacetCounts( facetManager.getFacets( descendingOrderedFacet ), new int[] { 5, 4, 4, 0 } );
		assertFacetCounts( facetManager.getFacets( ascendingOrderedFacet ), new int[] { 0, 4, 4, 5 } );

		facetManager.disableFaceting( descendingOrderedFacet );
		assertTrue(
				"descendingOrderedFacet should be disabled", query.getFacetManager().getFacets(
				descendingOrderedFacet
		).isEmpty()
		);
		assertFacetCounts( facetManager.getFacets( ascendingOrderedFacet ), new int[] { 0, 4, 4, 5 } );

		facetManager.disableFaceting( ascendingOrderedFacet );
		assertTrue(
				"descendingOrderedFacet should be disabled",
				facetManager.getFacets( descendingOrderedFacet ).isEmpty()
		);
		assertTrue(
				"ascendingOrderedFacet should be disabled",
				facetManager.getFacets( ascendingOrderedFacet ).isEmpty()
		);
	}

	private FullTextQuery queryHondaWithFacet(FacetingRequest request) {
		Query luceneQuery = queryBuilder( Car.class ).keyword().onField( "make" ).matching( "Honda" ).createQuery();
		FullTextQuery query = fullTextSession.createFullTextQuery( luceneQuery, Car.class );
		query.getFacetManager().enableFaceting( request );
		assertEquals( "Wrong number of query matches", 13, query.getResultSize() );
		return query;
	}

	public void loadTestData(Session session) {
		Transaction tx = session.beginTransaction();
		for ( String make : makes ) {
			for ( String color : colors ) {
				for ( int cc : ccs ) {
					Car car = new Car( make, color, cc );
					session.save( car );
				}
			}
		}
		Car car = new Car( "Honda", "yellow", 2407 );
		session.save( car );

		car = new Car( "Ford", "yellow", 2500 );
		session.save( car );
		tx.commit();
		session.clear();
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Car.class
		};
	}
}