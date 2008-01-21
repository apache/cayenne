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
import java.util.Collection;
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
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
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
 * 
 * @author Andrus Adamchik
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

    final Map aliasLookup = new HashMap();

    final List<DbEntity> tableList = new ArrayList<DbEntity>();
    final List<String> aliasList = new ArrayList<String>();
    final List<DbRelationship> dbRelList = new ArrayList<DbRelationship>();

    List<ColumnDescriptor> resultColumns;
    Map attributeOverrides;
    Map defaultAttributesByColumn;

    int aliasCounter;

    boolean suppressingDistinct;

    /**
     * If set to <code>true</code>, indicates that distinct select query is required no
     * matter what the original query settings where. This flag can be set when joins are
     * created using "to-many" relationships.
     */
    boolean forcingDistinct;

    /**
     * Returns query translated to SQL. This is a main work method of the
     * SelectTranslator.
     */
    @Override
    public String createSqlString() throws Exception {
        forcingDistinct = false;

        // build column list
        this.resultColumns = buildResultColumns();

        QualifierTranslator tr = adapter.getQualifierTranslator(this);

        // build qualifier
        String qualifierStr = tr.doTranslation();

        // build ORDER BY
        OrderingTranslator orderingTranslator = new OrderingTranslator(this);
        String orderByStr = orderingTranslator.doTranslation();

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
            for (int i = 0; i < orderByColumnList.size(); i++) {
                // Convert to ColumnDescriptors??
                String orderByColumnExp = orderByColumnList.get(i);
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

        // append table list (unroll loop's 1st element)
        int tableCount = tableList.size();
        appendTable(queryBuf, 0); // assume there is at least 1 table
        for (int i = 1; i < tableCount; i++) {
            queryBuf.append(", ");
            appendTable(queryBuf, i);
        }

        // append db relationship joins if any
        boolean hasWhere = false;
        int dbRelCount = dbRelList.size();
        if (dbRelCount > 0) {
            hasWhere = true;
            queryBuf.append(" WHERE ");

            appendJoins(queryBuf, 0);
            for (int i = 1; i < dbRelCount; i++) {
                queryBuf.append(" AND ");
                appendJoins(queryBuf, i);
            }
        }

        // append qualifier
        if (qualifierStr != null) {
            if (hasWhere) {
                queryBuf.append(" AND (");
                queryBuf.append(qualifierStr);
                queryBuf.append(")");
            }
            else {
                hasWhere = true;
                queryBuf.append(" WHERE ");
                queryBuf.append(qualifierStr);
            }
        }

        // append prebuilt ordering
        if (orderByStr != null) {
            queryBuf.append(" ORDER BY ").append(orderByStr);
        }

        return queryBuf.toString();
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

        return resultColumns
                .toArray(new ColumnDescriptor[resultColumns.size()]);
    }

    /**
     * Returns a map of ColumnDescriptors keyed by ObjAttribute for columns that may need
     * to be reprocessed manually due to incompatible mappings along the inheritance
     * hierarchy.
     * 
     * @since 1.2
     */
    public Map getAttributeOverrides() {
        return attributeOverrides != null ? attributeOverrides : Collections.EMPTY_MAP;
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

        this.defaultAttributesByColumn = new HashMap();

        // create alias for root table
        newAliasForTable(getRootDbEntity());

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

        final Set attributes = new HashSet();

        // fetched attributes include attributes that are either:
        // 
        // * class properties
        // * PK
        // * FK used in relationships
        // * GROUP BY
        // * joined prefetch PK

        ClassDescriptor descriptor = query
                .getMetaData(getEntityResolver())
                .getClassDescriptor();
        ObjEntity oe = descriptor.getEntity();

        PropertyVisitor visitor = new PropertyVisitor() {

            public boolean visitAttribute(AttributeProperty property) {
                ObjAttribute oa = property.getAttribute();
                Iterator dbPathIterator = oa.getDbPathIterator();
                while (dbPathIterator.hasNext()) {
                    Object pathPart = dbPathIterator.next();

                    if (pathPart == null) {
                        throw new CayenneRuntimeException(
                                "ObjAttribute has no component: " + oa.getName());
                    }

                    else if (pathPart instanceof DbRelationship) {
                        DbRelationship rel = (DbRelationship) pathPart;
                        dbRelationshipAdded(rel);
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

                List joins = dbRel.getJoins();
                int len = joins.size();
                for (int i = 0; i < len; i++) {
                    DbJoin join = (DbJoin) joins.get(i);
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

            Iterator extraPaths = ((PrefetchSelectQuery) query)
                    .getResultPaths()
                    .iterator();

            // for each relationship path add closest FK or PK, for each attribute path,
            // add specified column
            while (extraPaths.hasNext()) {

                String path = (String) extraPaths.next();
                Expression pathExp = oe.translateToDbPath(Expression.fromString(path));

                Iterator<CayenneMapEntry> it = table.resolvePathComponents(pathExp);

                // add joins and find terminating element
                CayenneMapEntry pathComponent = null;
                while (it.hasNext()) {
                    pathComponent = it.next();

                    // do not add join for the last DB Rel
                    if (it.hasNext() && pathComponent instanceof DbRelationship) {
                        dbRelationshipAdded((DbRelationship) pathComponent);
                    }
                }

                String labelPrefix = pathExp.toString().substring("db:".length());

                // process terminating element
                if (pathComponent instanceof DbAttribute) {

                    // label prefix already includes relationship name
                    appendColumn(
                            columns,
                            null,
                            (DbAttribute) pathComponent,
                            attributes,
                            labelPrefix);
                }
                else if (pathComponent instanceof DbRelationship) {
                    DbRelationship relationship = (DbRelationship) pathComponent;

                    // add last join
                    if (relationship.isToMany()) {
                        dbRelationshipAdded(relationship);
                    }

                    for (DbJoin j : relationship.getJoins()) {

                        DbAttribute attribute = relationship.isToMany()
                                ? j.getTarget()
                                : j.getSource();

                        // note that we my select a source attribute, but label it as
                        // target for simplified snapshot processing
                        appendColumn(columns, null, attribute, attributes, labelPrefix
                                + '.'
                                + j.getTargetName());
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

                Iterator it = table.resolvePathComponents(dbPrefetch);

                DbRelationship r = null;
                while (it.hasNext()) {
                    r = (DbRelationship) it.next();
                    dbRelationshipAdded(r);
                }

                if (r == null) {
                    throw new CayenneRuntimeException("Invalid joint prefetch '"
                            + prefetch
                            + "' for entity: "
                            + oe.getName());
                }

                // add columns from the target entity, skipping those that are an FK to
                // source entity

                Collection skipColumns = Collections.EMPTY_LIST;
                if (r.getSourceEntity() == table) {
                    skipColumns = new ArrayList(2);
                    Iterator joins = r.getJoins().iterator();
                    while (joins.hasNext()) {
                        DbJoin join = (DbJoin) joins.next();
                        if (attributes.contains(join.getSource())) {
                            skipColumns.add(join.getTarget());
                        }
                    }
                }

                // go via target OE to make sure that Java types are mapped correctly...
                ObjRelationship targetRel = (ObjRelationship) prefetchExp.evaluate(oe);
                Iterator targetObjAttrs = targetRel
                        .getTargetEntity()
                        .getAttributes()
                        .iterator();

                String labelPrefix = dbPrefetch.toString().substring("db:".length());
                while (targetObjAttrs.hasNext()) {
                    ObjAttribute oa = (ObjAttribute) targetObjAttrs.next();
                    Iterator dbPathIterator = oa.getDbPathIterator();
                    while (dbPathIterator.hasNext()) {
                        Object pathPart = dbPathIterator.next();

                        if (pathPart == null) {
                            throw new CayenneRuntimeException(
                                    "ObjAttribute has no component: " + oa.getName());
                        }

                        else if (pathPart instanceof DbRelationship) {
                            DbRelationship rel = (DbRelationship) pathPart;
                            dbRelationshipAdded(rel);
                        }
                        else if (pathPart instanceof DbAttribute) {
                            DbAttribute attribute = (DbAttribute) pathPart;

                            if (!skipColumns.contains(attribute)) {
                                appendColumn(
                                        columns,
                                        oa,
                                        attribute,
                                        attributes,
                                        labelPrefix + '.' + attribute.getName());
                            }
                        }
                    }
                }

                // append remaining target attributes such as keys
                Iterator targetAttributes = r
                        .getTargetEntity()
                        .getAttributes()
                        .iterator();
                while (targetAttributes.hasNext()) {
                    DbAttribute attribute = (DbAttribute) targetAttributes.next();
                    if (!skipColumns.contains(attribute)) {
                        appendColumn(columns, null, attribute, attributes, labelPrefix
                                + '.'
                                + attribute.getName());
                    }
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

            String alias = aliasForTable((DbEntity) attribute.getEntity());
            columns.add(new ColumnDescriptor(attribute, alias));
        }

        return columns;
    }

    private void appendColumn(
            List<ColumnDescriptor> columns,
            ObjAttribute objAttribute,
            DbAttribute attribute,
            Set skipSet,
            String label) {

        if (skipSet.add(attribute)) {
            String alias = aliasForTable((DbEntity) attribute.getEntity());
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
                    ObjAttribute original = (ObjAttribute) defaultAttributesByColumn
                            .remove(column);

                    if (original != null) {
                        if (attributeOverrides == null) {
                            attributeOverrides = new HashMap();
                        }

                        attributeOverrides.put(original, column);
                        column.setJavaClass(Void.TYPE.getName());
                    }

                    break;
                }
            }
        }
    }

    private void appendTable(StringBuilder queryBuf, int index) {
        DbEntity ent = tableList.get(index);
        queryBuf.append(ent.getFullyQualifiedName());
        // The alias should be the alias from the same index in aliasList, not that
        // returned by aliasForTable.
        queryBuf.append(' ').append(aliasList.get(index));
    }

    private void appendJoins(StringBuilder queryBuf, int index) {
        DbRelationship rel = dbRelList.get(index);
        String srcAlias = aliasForTable((DbEntity) rel.getSourceEntity());
        String targetAlias = (String) aliasLookup.get(rel);

        boolean andFlag = false;

        List joins = rel.getJoins();
        int len = joins.size();
        for (int i = 0; i < len; i++) {
            DbJoin join = (DbJoin) joins.get(i);

            if (andFlag) {
                queryBuf.append(" AND ");
            }
            else {
                andFlag = true;
            }

            queryBuf.append(srcAlias).append('.').append(join.getSourceName()).append(
                    " = ").append(targetAlias).append('.').append(join.getTargetName());
        }
    }

    /**
     * Stores a new relationship in an internal list. Later it will be used to create
     * joins to relationship destination table.
     */
    @Override
    public void dbRelationshipAdded(DbRelationship rel) {
        if (rel.isToMany()) {
            forcingDistinct = true;
        }

        String existAlias = (String) aliasLookup.get(rel);

        if (existAlias == null) {
            dbRelList.add(rel);

            // add alias for the destination table of the relationship
            String newAlias = newAliasForTable((DbEntity) rel.getTargetEntity());
            aliasLookup.put(rel, newAlias);
        }
    }

    /**
     * Sets up and returns a new alias for a specified table.
     */
    protected String newAliasForTable(DbEntity ent) {
        String newAlias = "t" + aliasCounter++;
        tableList.add(ent);
        aliasList.add(newAlias);
        return newAlias;
    }

    @Override
    public String aliasForTable(DbEntity ent, DbRelationship rel) {
        return (String) aliasLookup.get(rel);
    }

    /**
     * Overrides superclass implementation. Will return an alias that should be used for a
     * specified DbEntity in the query (or null if this DbEntity is not included in the
     * FROM clause).
     */
    @Override
    public String aliasForTable(DbEntity ent) {

        int entIndex = tableList.indexOf(ent);
        if (entIndex >= 0) {
            return aliasList.get(entIndex);
        }
        else {
            StringBuilder msg = new StringBuilder();
            msg.append("Alias not found, DbEntity: '").append(
                    ent != null ? ent.getName() : "<null entity>").append(
                    "'\nExisting aliases:");
            
            int len = aliasList.size();
            for (int i = 0; i < len; i++) {
                String dbeName = (tableList.get(i) != null)
                        ? tableList.get(i).getName()
                        : "<null entity>";
                msg.append("\n").append(aliasList.get(i)).append(" => ").append(dbeName);
            }

            throw new CayenneRuntimeException(msg.toString());
        }
    }

    /**
     * Always returns true.
     */
    @Override
    public boolean supportsTableAliases() {
        return true;
    }
}
