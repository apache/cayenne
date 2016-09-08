/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access.translator.select;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.parser.ASTPath;
import org.apache.cayenne.exp.parser.AggregationFunction;
import org.apache.cayenne.map.*;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.select.SelectClause;

import java.sql.Connection;
import java.sql.Types;
import java.util.*;

/**
 * A builder of JDBC PreparedStatements based on Cayenne SelectQueries.
 * Translates SelectQuery to parameterized SQL string and wraps it in a
 * PreparedStatement. SelectTranslator is stateful and thread-unsafe.
 */
public class SelectTranslator extends QueryAssembler {

    protected static final int[] UNSUPPORTED_DISTINCT_TYPES = { Types.BLOB, Types.CLOB, Types.NCLOB,
            Types.LONGVARBINARY, Types.LONGVARCHAR, Types.LONGNVARCHAR };

    protected static boolean isUnsupportedForDistinct(int type) {
        for (int unsupportedDistinctType : UNSUPPORTED_DISTINCT_TYPES) {
            if (unsupportedDistinctType == type) {
                return true;
            }
        }

        return false;
    }

    JoinStack joinStack;
    
    /**
     * @since 4.0
     */
    public SelectTranslator(Query query, DataNode dataNode, Connection connection) {
        super(query, dataNode, connection);
    }

    protected JoinStack getJoinStack() {
        if (joinStack == null) {
            joinStack = createJoinStack();
        }
        return joinStack;
    }

    Map<ObjAttribute, ColumnDescriptor> attributeOverrides;

    boolean suppressingDistinct;

    /**
     * If set to <code>true</code>, indicates that distinct select query is
     * required no matter what the original query settings where. This flag can
     * be set when joins are created using "to-many" relationships.
     */
    boolean forcingDistinct;

    protected JoinStack createJoinStack() {
        return new JoinStack(getAdapter(), this);
    }

    /**
     * Returns query translated to SQL. This is a main work method of the
     * SelectTranslator.
     */
    @Override
    public String createSqlString() throws Exception {
        if (cachedSqlString != null) {
            return cachedSqlString;
        }

        DataMap dataMap = queryMetadata.getDataMap();
        JoinStack joins = getJoinStack();

        forcingDistinct = false;

        SelectPropertyTranslator selectClauseTranslator = new SelectPropertyTranslator(this);

        // build qualifier
        StringBuilder qualifierBuffer = adapter.getQualifierTranslator(this).appendPart(new StringBuilder());

        // build ORDER BY
        OrderingTranslator orderingTranslator = new OrderingTranslator(this);
        StringBuilder orderingBuffer = orderingTranslator.appendPart(new StringBuilder());

        // assemble
        StringBuilder queryBuf = new StringBuilder();
        queryBuf.append("SELECT ");

        // check if DISTINCT is appropriate
        // side effect: "suppressingDistinct" flag may end up being flipped here
        if (forcingDistinct || getSelectQuery().isDistinct()) {
            suppressingDistinct = false;

            for (ColumnDescriptor column : selectClauseTranslator.getResultColumns()) {
                if (isUnsupportedForDistinct(column.getJdbcType())) {
                    suppressingDistinct = true;
                    break;
                }
            }

            if (!suppressingDistinct) {
                queryBuf.append("DISTINCT ");
            }
        }

        SelectQuery<?> selectQuery = getSelectQuery();
        List<String> selectColumnExpList = getSelectColumnExpList(dataMap, selectClauseTranslator.getResultColumns());

        // append any column expressions used in the order by if this query
        // uses the DISTINCT modifier
        if (forcingDistinct || selectQuery.isDistinct()) {
            for (String orderByColumnExp : orderingTranslator.getOrderByColumnList()) {
                // Convert to ColumnDescriptors??
                if (!selectColumnExpList.contains(orderByColumnExp)) {
                    selectColumnExpList.add(orderByColumnExp);
                }
            }
        }


        appendSelectColumns(queryBuf, selectColumnExpList);

        SelectClause select = selectQuery.getSelect();
        if (select != null && select.getAggregationFunction() != null) {
            if (!selectColumnExpList.isEmpty()) {
                queryBuf.append(", ");
            }
            QueryMetadata metaData = query.getMetaData(entityResolver);

            // Magic is here
            String res = selectClauseTranslator.toString(select.getAggregationFunction(),
                    adapter.getQualifierTranslator(this));
            queryBuf.append(res);

            List<ColumnDescriptor> list = new ArrayList<ColumnDescriptor>(selectClauseTranslator.getResultColumns());
            list.add(new ColumnDescriptor(res, Types.INTEGER)); // TODO calculate type from function expression
            metaData.setResultSetMapping(buildSqlResult(list, metaData).getResolvedComponents(entityResolver));
        }


        // append from clause
        queryBuf.append(" FROM ");

        // append tables and joins
        joins.appendRootWithQuoteSqlIdentifiers(queryBuf, getRootDbEntity());

        // join parameters will be added to head of query
        parameterIndex = 0;

        joins.appendJoins(queryBuf);
        joins.appendQualifier(qualifierBuffer, qualifierBuffer.length() == 0);

        // append qualifier
        if (qualifierBuffer.length() > 0) {
            queryBuf.append(" WHERE ");
            queryBuf.append(qualifierBuffer);
        }

        // append prebuilt ordering
        if (orderingBuffer.length() > 0) {
            queryBuf.append(" ORDER BY ").append(orderingBuffer);
        }

        if (select != null && select.getAggregationFunction() != null
                && !selectColumnExpList.isEmpty()) {

            queryBuf.append(" GROUP BY ");
            appendSelectColumns(queryBuf, selectColumnExpList);
        }

        if (!isSuppressingDistinct()) {
            appendLimitAndOffsetClauses(queryBuf);
        }

        cachedSqlString = queryBuf.toString();
        return cachedSqlString;
    }

    private SQLResult buildSqlResult(List<ColumnDescriptor> resultColumns, QueryMetadata metaData) {
        if (resultColumns.isEmpty()) {
            return new SQLResult();
        }

        SQLResult sqlResult = new SQLResult();
        EntityResult result = new EntityResult(metaData.getObjEntity().getName());
        for (ColumnDescriptor column : resultColumns) {
            result.addDbField(column.getDataRowKey(), column.getName());
        }
        sqlResult.addEntityResult(result);

        return sqlResult;
    }

    /**
     * @param dataMap - we need it just to pass into QuotingStrategy#quotedIdentifier method
     *
     * @param columns
     * @return list of quated columns name that we need to include into select statment
     * */
    private List<String> getSelectColumnExpList(DataMap dataMap, List<ColumnDescriptor> columns) {
        // convert ColumnDescriptors to column names
        QuotingStrategy strategy = getAdapter().getQuotingStrategy();
        List<String> selectColumnExpList = new ArrayList<String>();
        for (ColumnDescriptor column : columns) {
            String fullName = strategy.quotedIdentifier(dataMap, column.getNamePrefix(), column.getName());
            selectColumnExpList.add(fullName);
        }
        return selectColumnExpList;
    }

    /**
     * @since 3.1
     */
    protected void appendSelectColumns(StringBuilder buffer, List<String> selectColumnExpList) {
        if (selectColumnExpList.isEmpty()) {
            return;
        }

        // append columns (unroll the loop's first element)
        int columnCount = selectColumnExpList.size();
        buffer.append(selectColumnExpList.get(0));

        // assume there is at least 1 element
        for (int i = 1; i < columnCount; i++) {
            buffer.append(", ");
            buffer.append(selectColumnExpList.get(i));
        }
    }

    /**
     * Handles appending optional limit and offset clauses. This implementation
     * does nothing, deferring to subclasses to define the LIMIT/OFFSET clause
     * syntax.
     * 
     * @since 3.0
     */
    protected void appendLimitAndOffsetClauses(StringBuilder buffer) {

    }

    @Override
    public String getCurrentAlias() {
        return getJoinStack().getCurrentAlias();
    }

    /**
     * Returns a list of ColumnDescriptors for the query columns.
     * 
     * @since 1.2
     */
    public ColumnDescriptor[] getResultColumns() {
        List<ColumnDescriptor> columns = new SelectPropertyTranslator(this).getResultColumns();
        return columns.toArray(new ColumnDescriptor[columns.size()]);
    }

    /**
     * Returns a map of ColumnDescriptors keyed by ObjAttribute for columns that
     * may need to be reprocessed manually due to incompatible mappings along
     * the inheritance hierarchy.
     * 
     * @since 1.2
     */
    public Map<ObjAttribute, ColumnDescriptor> getAttributeOverrides() {
        if (attributeOverrides == null) {
            attributeOverrides = new HashMap<ObjAttribute, ColumnDescriptor>();
        }

        return attributeOverrides;
    }

    /**
     * Returns true if SelectTranslator determined that a query requiring
     * DISTINCT can't be run with DISTINCT keyword for internal reasons. If this
     * method returns true, DataNode may need to do in-memory distinct
     * filtering.
     * 
     * @since 1.1
     */
    public boolean isSuppressingDistinct() {
        return suppressingDistinct;
    }

    private SelectQuery<?> getSelectQuery() {
        return (SelectQuery<?>) getQuery();
    }

    /**
     * @since 3.0
     */
    @Override
    public void resetJoinStack() {
        getJoinStack().resetStack();
    }

    /**
     * @since 3.0
     */
    @Override
    public void dbRelationshipAdded(DbRelationship relationship, JoinType joinType, String joinSplitAlias) {
        if (relationship.isToMany()) {
            forcingDistinct = true;
        }

        getJoinStack().pushJoin(relationship, joinType, joinSplitAlias);
    }

    /**
     * Always returns true.
     */
    @Override
    public boolean supportsTableAliases() {
        return true;
    }
}
