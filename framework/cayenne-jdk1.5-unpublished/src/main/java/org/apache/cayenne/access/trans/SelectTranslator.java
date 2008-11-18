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

package org.apache.cayenne.access.trans;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.JoinType;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.PathComponent;
import org.apache.cayenne.query.PrefetchSelectQuery;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;
import org.apache.cayenne.util.CayenneMapEntry;

/**
 * A builder of JDBC PreparedStatements based on Cayenne SelectQueries. Translates
 * SelectQuery to parameterized SQL string and wraps it in a PreparedStatement.
 * SelectTranslator is stateful and thread-unsafe.
 */
public class SelectTranslator extends QueryAssembler {

    protected static final int[] UNSUPPORTED_DISTINCT_TYPES = new int[] {
            Types.BLOB, Types.CLOB, Types.LONGVARBINARY, Types.LONGVARCHAR
    };

    protected static boolean isUnsupportedForDistinct(int type) {
        for (int i = 0; i < UNSUPPORTED_DISTINCT_TYPES.length; i++) {
            if (UNSUPPORTED_DISTINCT_TYPES[i] == type) {
                return true;
            }
        }

        return false;
    }

    JoinStack joinStack = createJoinStack();

    List<ColumnDescriptor> resultColumns;
    Map<ObjAttribute, ColumnDescriptor> attributeOverrides;
    Map<ColumnDescriptor, ObjAttribute> defaultAttributesByColumn;

    boolean suppressingDistinct;

    /**
     * If set to <code>true</code>, indicates that distinct select query is required no
     * matter what the original query settings where. This flag can be set when joins are
     * created using "to-many" relationships.
     */
    boolean forcingDistinct;

    protected JoinStack createJoinStack() {
        return new JoinStack();
    }

    /**
     * Returns query translated to SQL. This is a main work method of the
     * SelectTranslator.
     */
    @Override
    public String createSqlString() throws Exception {
        forcingDistinct = false;

        // build column list
        this.resultColumns = buildResultColumns();

        // build qualifier
        StringBuilder qualifierBuffer = new StringBuilder();
        adapter.getQualifierTranslator(this).appendPart(qualifierBuffer);

        // build ORDER BY
        OrderingTranslator orderingTranslator = new OrderingTranslator(this);
        StringBuilder orderingBuffer = new StringBuilder();
        orderingTranslator.appendPart(orderingBuffer);

        // assemble
        StringBuilder queryBuf = new StringBuilder();
        queryBuf.append("SELECT ");

        // check if DISTINCT is appropriate
        // side effect: "suppressingDistinct" flag may end up being flipped here
        if (forcingDistinct || getSelectQuery().isDistinct()) {

            suppressingDistinct = false;

            for (ColumnDescriptor column : resultColumns) {
                if (isUnsupportedForDistinct(column.getJdbcType())) {
                    suppressingDistinct = true;
                    break;
                }
            }

            if (!suppressingDistinct) {
                queryBuf.append("DISTINCT ");
            }
        }

        // convert ColumnDescriptors to column names
        List<String> selectColumnExpList = new ArrayList<String>();
        for (ColumnDescriptor column : resultColumns) {
            selectColumnExpList.add(column.getQualifiedColumnName());
        }

        // append any column expressions used in the order by if this query
        // uses the DISTINCT modifier
        if (forcingDistinct || getSelectQuery().isDistinct()) {
            List<String> orderByColumnList = orderingTranslator.getOrderByColumnList();
            for (String orderByColumnExp : orderByColumnList) {
                // Convert to ColumnDescriptors??
                if (!selectColumnExpList.contains(orderByColumnExp)) {
                    selectColumnExpList.add(orderByColumnExp);
                }
            }
        }

        // append columns (unroll the loop's first element)
        int columnCount = selectColumnExpList.size();
        queryBuf.append(selectColumnExpList.get(0));
        // assume there is at least 1 element
        for (int i = 1; i < columnCount; i++) {
            queryBuf.append(", ");
            queryBuf.append(selectColumnExpList.get(i));
        }

        // append from clause
        queryBuf.append(" FROM ");

        // append tables and joins
        joinStack.appendRoot(queryBuf, getRootDbEntity());
        joinStack.appendJoins(queryBuf);
        joinStack.appendQualifier(qualifierBuffer, qualifierBuffer.length() == 0);

        // append qualifier
        if (qualifierBuffer.length() > 0) {
            queryBuf.append(" WHERE ");
            queryBuf.append(qualifierBuffer);
        }

        // append prebuilt ordering
        if (orderingBuffer.length() > 0) {
            queryBuf.append(" ORDER BY ").append(orderingBuffer);
        }

        if (!isSuppressingDistinct()) {
            appendLimitAndOffsetClauses(queryBuf);
        }

        return queryBuf.toString();
    }

    /**
     * Handles appending optional limit and offset clauses. This implementation does
     * nothing, deferring to subclasses to define the LIMIT/OFFSET clause syntax.
     * 
     * @since 3.0
     */
    protected void appendLimitAndOffsetClauses(StringBuilder buffer) {

    }

    @Override
    public String getCurrentAlias() {
        return joinStack.getCurrentAlias();
    }

    /**
     * Returns a list of ColumnDescriptors for the query columns.
     * 
     * @since 1.2
     */
    public ColumnDescriptor[] getResultColumns() {
        if (resultColumns == null || resultColumns.isEmpty()) {
            return new ColumnDescriptor[0];
        }

        return resultColumns.toArray(new ColumnDescriptor[resultColumns.size()]);
    }

    /**
     * Returns a map of ColumnDescriptors keyed by ObjAttribute for columns that may need
     * to be reprocessed manually due to incompatible mappings along the inheritance
     * hierarchy.
     * 
     * @since 1.2
     */
    public Map<ObjAttribute, ColumnDescriptor> getAttributeOverrides() {
        if (attributeOverrides != null) {
            return attributeOverrides;
        }
        else {
            return Collections.emptyMap();
        }
    }

    /**
     * Returns true if SelectTranslator determined that a query requiring DISTINCT can't
     * be run with DISTINCT keyword for internal reasons. If this method returns true,
     * DataNode may need to do in-memory distinct filtering.
     * 
     * @since 1.1
     */
    public boolean isSuppressingDistinct() {
        return suppressingDistinct;
    }

    private SelectQuery getSelectQuery() {
        return (SelectQuery) getQuery();
    }

    List<ColumnDescriptor> buildResultColumns() {

        this.defaultAttributesByColumn = new HashMap<ColumnDescriptor, ObjAttribute>();

        List<ColumnDescriptor> columns = new ArrayList<ColumnDescriptor>();
        SelectQuery query = getSelectQuery();

        // for query with custom attributes use a different strategy
        if (query.isFetchingCustomAttributes()) {
            appendCustomColumns(columns, query);
        }
        else {
            appendQueryColumns(columns, query);
        }

        return columns;
    }

    /**
     * Appends columns needed for object SelectQuery to the provided columns list.
     */
    // TODO: this whole method screams REFACTORING!!!
    List<ColumnDescriptor> appendQueryColumns(
            final List<ColumnDescriptor> columns,
            SelectQuery query) {

        final Set<DbAttribute> attributes = new HashSet<DbAttribute>();

        // fetched attributes include attributes that are either:
        // 
        // * class properties
        // * PK
        // * FK used in relationships
        // * GROUP BY
        // * joined prefetch PK

        ClassDescriptor descriptor = query
                .getMetaData(entityResolver)
                .getClassDescriptor();
        ObjEntity oe = descriptor.getEntity();

        PropertyVisitor visitor = new PropertyVisitor() {

            public boolean visitAttribute(AttributeProperty property) {
                ObjAttribute oa = property.getAttribute();

                resetJoinStack();
                Iterator<CayenneMapEntry> dbPathIterator = oa.getDbPathIterator();
                while (dbPathIterator.hasNext()) {
                    Object pathPart = dbPathIterator.next();

                    if (pathPart == null) {
                        throw new CayenneRuntimeException(
                                "ObjAttribute has no component: " + oa.getName());
                    }
                    else if (pathPart instanceof DbRelationship) {
                        DbRelationship rel = (DbRelationship) pathPart;
                        dbRelationshipAdded(rel, JoinType.INNER, null);
                    }
                    else if (pathPart instanceof DbAttribute) {
                        DbAttribute dbAttr = (DbAttribute) pathPart;

                        appendColumn(columns, oa, dbAttr, attributes, null);
                    }
                }
                return true;
            }

            public boolean visitToMany(ToManyProperty property) {
                visitRelationship(property);
                return true;
            }

            public boolean visitToOne(ToOneProperty property) {
                visitRelationship(property);
                return true;
            }

            private void visitRelationship(ArcProperty property) {
                ObjRelationship rel = property.getRelationship();
                DbRelationship dbRel = rel.getDbRelationships().get(0);

                List<DbJoin> joins = dbRel.getJoins();
                int len = joins.size();
                for (int i = 0; i < len; i++) {
                    DbJoin join = joins.get(i);
                    DbAttribute src = join.getSource();
                    appendColumn(columns, null, src, attributes, null);
                }
            }
        };

        if (query.isResolvingInherited()) {
            descriptor.visitAllProperties(visitor);
        }
        else {
            descriptor.visitProperties(visitor);
        }

        // add remaining needed attrs from DbEntity
        DbEntity table = getRootDbEntity();
        for (final DbAttribute dba : table.getPrimaryKeys()) {
            appendColumn(columns, null, dba, attributes, null);
        }

        // special handling of a disjoint query...

        // TODO, Andrus 11/17/2005 - resultPath mechanism is generic and should probably
        // be moved in the superclass (SelectQuery), replacing customDbAttributes.

        if (query instanceof PrefetchSelectQuery) {

            // for each relationship path add closest FK or PK, for each attribute path,
            // add specified column
            for (String path : ((PrefetchSelectQuery) query).getResultPaths()) {

                Expression pathExp = oe.translateToDbPath(Expression.fromString(path));

                // add joins and find terminating element

                resetJoinStack();

                PathComponent<DbAttribute, DbRelationship> lastComponent = null;
                for (PathComponent<DbAttribute, DbRelationship> component : table
                        .resolvePath(pathExp, getPathAliases())) {

                    // do not add join for the last DB Rel
                    if (component.getRelationship() != null && !component.isLast()) {
                        dbRelationshipAdded(component.getRelationship(), component
                                .getJoinType(), null);
                    }

                    lastComponent = component;
                }

                String labelPrefix = pathExp.toString().substring("db:".length());

                // process terminating element
                if (lastComponent != null) {

                    DbRelationship relationship = lastComponent.getRelationship();

                    if (relationship != null) {

                        // add last join
                        if (relationship.isToMany()) {
                            dbRelationshipAdded(relationship, JoinType.INNER, null);
                        }

                        for (DbJoin j : relationship.getJoins()) {

                            DbAttribute attribute = relationship.isToMany() ? j
                                    .getTarget() : j.getSource();

                            // note that we my select a source attribute, but label it as
                            // target for simplified snapshot processing
                            appendColumn(
                                    columns,
                                    null,
                                    attribute,
                                    attributes,
                                    labelPrefix + '.' + j.getTargetName());
                        }
                    }

                    else {

                        // label prefix already includes relationship name
                        appendColumn(
                                columns,
                                null,
                                lastComponent.getAttribute(),
                                attributes,
                                labelPrefix);
                    }

                }
            }
        }

        // handle joint prefetches directly attached to this query...
        if (query.getPrefetchTree() != null) {

            for (PrefetchTreeNode prefetch : query.getPrefetchTree().adjacentJointNodes()) {

                // for each prefetch add all joins plus columns from the target entity
                Expression prefetchExp = Expression.fromString(prefetch.getPath());
                Expression dbPrefetch = oe.translateToDbPath(prefetchExp);

                resetJoinStack();
                DbRelationship r = null;
                for (PathComponent<DbAttribute, DbRelationship> component : table
                        .resolvePath(dbPrefetch, getPathAliases())) {
                    r = component.getRelationship();
                    dbRelationshipAdded(r, JoinType.LEFT_OUTER, null);
                }

                if (r == null) {
                    throw new CayenneRuntimeException("Invalid joint prefetch '"
                            + prefetch
                            + "' for entity: "
                            + oe.getName());
                }

                // add columns from the target entity, including those that are matched
                // against the FK of the source entity. This is needed to determine
                // whether optional relationships are null

                // go via target OE to make sure that Java types are mapped correctly...
                ObjRelationship targetRel = (ObjRelationship) prefetchExp.evaluate(oe);
                Iterator<ObjAttribute> targetObjAttrs = (Iterator<ObjAttribute>) targetRel
                        .getTargetEntity()
                        .getAttributes()
                        .iterator();

                String labelPrefix = dbPrefetch.toString().substring("db:".length());
                while (targetObjAttrs.hasNext()) {
                    ObjAttribute oa = targetObjAttrs.next();
                    Iterator<CayenneMapEntry> dbPathIterator = oa.getDbPathIterator();
                    while (dbPathIterator.hasNext()) {
                        Object pathPart = dbPathIterator.next();

                        if (pathPart == null) {
                            throw new CayenneRuntimeException(
                                    "ObjAttribute has no component: " + oa.getName());
                        }

                        else if (pathPart instanceof DbRelationship) {
                            DbRelationship rel = (DbRelationship) pathPart;
                            dbRelationshipAdded(rel, JoinType.INNER, null);
                        }
                        else if (pathPart instanceof DbAttribute) {
                            DbAttribute attribute = (DbAttribute) pathPart;

                            appendColumn(columns, oa, attribute, attributes, labelPrefix
                                    + '.'
                                    + attribute.getName());
                        }
                    }
                }

                // append remaining target attributes such as keys
                Iterator<DbAttribute> targetAttributes = (Iterator<DbAttribute>) r
                        .getTargetEntity()
                        .getAttributes()
                        .iterator();
                while (targetAttributes.hasNext()) {
                    DbAttribute attribute = targetAttributes.next();
                    appendColumn(columns, null, attribute, attributes, labelPrefix
                            + '.'
                            + attribute.getName());
                }
            }
        }

        return columns;
    }

    /**
     * Appends custom columns from SelectQuery to the provided list.
     */
    List<ColumnDescriptor> appendCustomColumns(
            List<ColumnDescriptor> columns,
            SelectQuery query) {

        List<String> customAttributes = query.getCustomDbAttributes();
        DbEntity table = getRootDbEntity();
        int len = customAttributes.size();

        for (int i = 0; i < len; i++) {
            DbAttribute attribute = (DbAttribute) table.getAttribute(customAttributes
                    .get(i));
            if (attribute == null) {
                throw new CayenneRuntimeException("Attribute does not exist: "
                        + customAttributes.get(i));
            }

            columns.add(new ColumnDescriptor(attribute, getCurrentAlias()));
        }

        return columns;
    }

    private void appendColumn(
            List<ColumnDescriptor> columns,
            ObjAttribute objAttribute,
            DbAttribute attribute,
            Set<DbAttribute> skipSet,
            String label) {

        if (skipSet.add(attribute)) {
            String alias = getCurrentAlias();
            ColumnDescriptor column = (objAttribute != null) ? new ColumnDescriptor(
                    objAttribute,
                    attribute,
                    alias) : new ColumnDescriptor(attribute, alias);

            if (label != null) {
                column.setLabel(label);
            }

            columns.add(column);

            // TODO: andrus, 5/7/2006 - replace 'columns' collection with this map, as it
            // is redundant
            defaultAttributesByColumn.put(column, objAttribute);
        }
        else if (objAttribute != null) {

            // record ObjAttribute override
            for (ColumnDescriptor column : columns) {
                if (attribute.getName().equals(column.getName())) {

                    // kick out the original attribute
                    ObjAttribute original = defaultAttributesByColumn.remove(column);

                    if (original != null) {
                        if (attributeOverrides == null) {
                            attributeOverrides = new HashMap<ObjAttribute, ColumnDescriptor>();
                        }

                        attributeOverrides.put(original, column);
                        column.setJavaClass(Void.TYPE.getName());
                    }

                    break;
                }
            }
        }
    }

    /**
     * @since 3.0
     */
    @Override
    public void resetJoinStack() {
        joinStack.resetStack();
    }

    /**
     * @since 3.0
     */
    @Override
    public void dbRelationshipAdded(
            DbRelationship relationship,
            JoinType joinType,
            String joinSplitAlias) {
        if (relationship.isToMany()) {
            forcingDistinct = true;
        }

        joinStack.pushJoin(relationship, joinType, joinSplitAlias);
    }

    /**
     * Always returns true.
     */
    @Override
    public boolean supportsTableAliases() {
        return true;
    }
}
