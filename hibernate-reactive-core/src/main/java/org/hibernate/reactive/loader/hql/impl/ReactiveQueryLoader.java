/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.loader.hql.impl;

import org.hibernate.HibernateException;
import org.hibernate.QueryException;
import org.hibernate.cache.spi.QueryKey;
import org.hibernate.cache.spi.QueryResultsCache;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.hql.internal.ast.QueryTranslatorImpl;
import org.hibernate.hql.internal.ast.tree.SelectClause;
import org.hibernate.loader.hql.QueryLoader;
import org.hibernate.loader.spi.AfterLoadAction;
import org.hibernate.reactive.loader.CachingReactiveLoader;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static org.hibernate.reactive.sql.impl.Parameters.processParameters;

/**
 * A reactive {@link QueryLoader} for HQL queries.
 */
public class ReactiveQueryLoader extends QueryLoader implements CachingReactiveLoader {

	private final QueryTranslatorImpl queryTranslator;
	private final SessionFactoryImplementor factory;
	private final SelectClause selectClause;

	public ReactiveQueryLoader(
			QueryTranslatorImpl queryTranslator,
			SessionFactoryImplementor factory,
			SelectClause selectClause) {
		super( queryTranslator, factory, selectClause );
		this.queryTranslator = queryTranslator;
		this.factory = factory;
		this.selectClause = selectClause;
	}

	public CompletionStage<List<Object>> reactiveList(
			SessionImplementor session,
			QueryParameters queryParameters) throws HibernateException {
		checkQuery( queryParameters );
		return reactiveList(
				session,
				queryParameters,
				queryTranslator.getQuerySpaces(),
				selectClause.getQueryReturnTypes()
		);
	}

	/**
	 * Return the query results, using the query cache, called
	 * by subclasses that implement cacheable queries
	 * @see QueryLoader#list(SharedSessionContractImplementor, QueryParameters, Set, Type[])
	 */
	protected CompletionStage<List<Object>> reactiveList(
			final SessionImplementor session,
			final QueryParameters queryParameters,
			final Set<Serializable> querySpaces,
			final Type[] resultTypes) throws HibernateException {

		final boolean cacheable = factory.getSessionFactoryOptions().isQueryCacheEnabled()
				&& queryParameters.isCacheable();

		if ( cacheable ) {
			return reactiveListUsingQueryCache( getSQLString(), getQueryIdentifier(), session, queryParameters, querySpaces, resultTypes );
		}
		else {
			return reactiveListIgnoreQueryCache( getSQLString(), getQueryIdentifier(), session, queryParameters );
		}
	}

	@Override @SuppressWarnings("unchecked")
	public List<Object> processResultSet(ResultSet rs,
										 QueryParameters queryParameters,
										 SharedSessionContractImplementor session,
										 boolean returnProxies,
										 ResultTransformer forcedResultTransformer,
										 int maxRows, List<AfterLoadAction> afterLoadActions) throws SQLException {
		final RowSelection rowSelection = queryParameters.getRowSelection();
		final ResultSet resultSetPreprocessed = preprocessResultSet(
				rs,
				rowSelection,
				getLimitHandler( rowSelection )
		);
		return super.processResultSet(resultSetPreprocessed, queryParameters, session, returnProxies,
				forcedResultTransformer, maxRows, afterLoadActions);
	}

	@Override
	public String preprocessSQL(String sql,
								QueryParameters queryParameters,
								SessionFactoryImplementor factory,
								List<AfterLoadAction> afterLoadActions) {
		String processed = super.preprocessSQL(sql, queryParameters, factory, afterLoadActions);
		return processParameters( processed, factory.getJdbcServices().getDialect() );
	}

	@Override
	public boolean[] includeInResultRow() {
		return super.includeInResultRow();
	}

	@Override @SuppressWarnings("unchecked")
	public List<Object> getResultFromQueryCache(SessionImplementor session, QueryParameters queryParameters, Set<Serializable> querySpaces, Type[] resultTypes, QueryResultsCache queryCache, QueryKey key) {
		return super.getResultFromQueryCache(session, queryParameters, querySpaces, resultTypes, queryCache, key);
	}

	@Override
	public void putResultInQueryCache(SessionImplementor session, QueryParameters queryParameters, Type[] resultTypes, QueryResultsCache queryCache, QueryKey key, List<Object> cachableList) {
		super.putResultInQueryCache(session, queryParameters, resultTypes, queryCache, key, cachableList);
	}

	@Override
	public ResultTransformer resolveResultTransformer(ResultTransformer resultTransformer) {
		return super.resolveResultTransformer(resultTransformer);
	}

	@Override
	public String[] getResultRowAliases() {
		return super.getResultRowAliases();
	}

	@Override
	public boolean areResultSetRowsTransformedImmediately() {
		return super.areResultSetRowsTransformedImmediately();
	}

	@Override @SuppressWarnings("unchecked")
	public List<Object> getResultList(List results, ResultTransformer resultTransformer) throws QueryException {
		return super.getResultList(results, resultTransformer);
	}

	/**
	 * This is based on private method,
	 * {@link Loader#processResultSet(ResultSet, RowSelection, LimitHandler limitHandler, boolean, SharedSessionContractImplementor),
	 */
	private ResultSet preprocessResultSet(
			ResultSet resultSet,
			final RowSelection selection,
			final LimitHandler limitHandler
	) throws SQLException, HibernateException {
		if ( !limitHandler.supportsLimitOffset()
				|| !LimitHelper.useLimit( limitHandler, selection ) ) {
			for (int i = 0,
				 firstRow = LimitHelper.getFirstRow(selection);
				 i < firstRow; i++ ) {
				resultSet.next();
			}
		}
		return resultSet;
	}

	@Override
	public int bindParameterValues(PreparedStatement statement, QueryParameters queryParameters, int startIndex,
								   SharedSessionContractImplementor session) throws SQLException {
		return super.bindParameterValues( statement, queryParameters, startIndex, session );
	}
}
