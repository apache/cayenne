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
import org.apache.cayenne.map.DerivedDbEntity;
import org.apache.cayenne.map.EntityInheritanceTree;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.PrefetchSelectQuery;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.SelectQuery;

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

    final List tableList = new ArrayList();
    final List aliasList = new ArrayList();
    final List dbRelList = new ArrayList();

    List resultColumns;
    List groupByList;
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
    public String createSqlString() throws Exception {
        forcingDistinct = false;

        // build column list
        this.resultColumns = buildResultColumns();

        QualifierTranslator tr = adapter.getQualifierTranslator(this);

        // build parent qualifier
        // Parent qualifier translation must PRECEED main qualifier
        // since it will be appended first and its parameters must
        // go first as well
        String parentQualifierStr = null;
        if (getSelectQuery().isQualifiedOnParent()) {
            tr.setTranslateParentQual(true);
            parentQualifierStr = tr.doTranslation();
        }

        // build main qualifier
        tr.setTranslateParentQual(false);
        String qualifierStr = tr.doTranslation();

        // build GROUP BY
        this.groupByList = buildGroupByList();

        // build ORDER BY
        OrderingTranslator orderingTranslator = new OrderingTranslator(this);
        String orderByStr = orderingTranslator.doTranslation();

        // assemble
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append("SELECT ");

        // check if DISTINCT is appropriate
        // side effect: "suppressingDistinct" flag may end up being flipped here
        if (forcingDistinct || getSelectQuery().isDistinct()) {

            suppressingDistinct = false;
            Iterator it = resultColumns.iterator();
            while (it.hasNext()) {
                ColumnDescriptor column = (ColumnDescriptor) it.next();
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
        List selectColumnExpList = new ArrayList();

        Iterator it = resultColumns.iterator();
        while (it.hasNext()) {
            ColumnDescriptor column = (ColumnDescriptor) it.next();
            selectColumnExpList.add(column.getQualifiedColumnName());
        }

        // append any column expressions used in the order by if this query
        // uses the DISTINCT modifier
        if (forcingDistinct || getSelectQuery().isDistinct()) {
            List orderByColumnList = orderingTranslator.getOrderByColumnList();
            for (int i = 0; i < orderByColumnList.size(); i++) {
                // Convert to ColumnDescriptors??
                Object orderByColumnExp = orderByColumnList.get(i);
                if (!selectColumnExpList.contains(orderByColumnExp)) {
                    selectColumnExpList.add(orderByColumnExp);
                }
            }
        }

        // append columns (unroll the loop's first element)
        int columnCount = selectColumnExpList.size();
        queryBuf.append((String) selectColumnExpList.get(0));
        // assume there is at least 1 element
        for (int i = 1; i < columnCount; i++) {
            queryBuf.append(", ");
            queryBuf.append((String) selectColumnExpList.get(i));
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

        // append parent qualifier if any
        if (parentQualifierStr != null) {
            if (hasWhere) {
                queryBuf.append(" AND (");
                queryBuf.append(parentQualifierStr);
                queryBuf.append(")");
            }
            else {
                hasWhere = true;
                queryBuf.append(" WHERE ");
                queryBuf.append(parentQualifierStr);
            }
        }

        // append group by
        boolean hasGroupBy = false;
        if (groupByList != null) {
            int groupByCount = groupByList.size();
            if (groupByCount > 0) {
                hasGroupBy = true;
                queryBuf.append(" GROUP BY ");
                appendGroupBy(queryBuf, 0);
                for (int i = 1; i < groupByCount; i++) {
                    queryBuf.append(", ");
                    appendGroupBy(queryBuf, i);
                }
            }
        }

        // append qualifier
        if (qualifierStr != null) {
            if (hasGroupBy) {
                queryBuf.append(" HAVING ");
                queryBuf.append(qualifierStr);
            }
            else {
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

        return (ColumnDescriptor[]) resultColumns
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

    /**
     * Creates a list of columns used in the query's GROUP BY clause.
     */
    private List buildGroupByList() {
        DbEntity dbEntity = getRootDbEntity();
        return (dbEntity instanceof DerivedDbEntity) ? ((DerivedDbEntity) dbEntity)
                .getGroupByAttributes() : Collections.EMPTY_LIST;
    }

    List buildResultColumns() {

        this.defaultAttributesByColumn = new HashMap();

        // create alias for root table
        newAliasForTable(getRootDbEntity());

        List columns = new ArrayList();
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
    List appendQueryColumns(List columns, SelectQuery query) {

        Set attributes = new HashSet();

        // fetched attributes include attributes that are either:
        // 
        // * class properties
        // * PK
        // * FK used in relationships
        // * GROUP BY
        // * joined prefetch PK

        ObjEntity oe = getRootEntity();

        // null tree will indicate that we don't take inheritance into account
        EntityInheritanceTree tree = null;

        if (query.isResolvingInherited()) {
            tree = getRootInheritanceTree();
        }

        // ObjEntity attrs
        Iterator attrs = (tree != null) ? tree.allAttributes().iterator() : oe
                .getAttributes()
                .iterator();
        while (attrs.hasNext()) {
            ObjAttribute oa = (ObjAttribute) attrs.next();
            Iterator dbPathIterator = oa.getDbPathIterator();
            while (dbPathIterator.hasNext()) {
                Object pathPart = dbPathIterator.next();
                if (pathPart instanceof DbRelationship) {
                    DbRelationship rel = (DbRelationship) pathPart;
                    dbRelationshipAdded(rel);
                }
                else if (pathPart instanceof DbAttribute) {
                    DbAttribute dbAttr = (DbAttribute) pathPart;
                    if (dbAttr == null) {
                        throw new CayenneRuntimeException(
                                "ObjAttribute has no DbAttribute: " + oa.getName());
                    }

                    appendColumn(columns, oa, dbAttr, attributes, null);
                }
            }
        }

        // relationship keys
        Iterator rels = (tree != null) ? tree.allRelationships().iterator() : oe
                .getRelationships()
                .iterator();
        while (rels.hasNext()) {
            ObjRelationship rel = (ObjRelationship) rels.next();
            DbRelationship dbRel = (DbRelationship) rel.getDbRelationships().get(0);

            List joins = dbRel.getJoins();
            int len = joins.size();
            for (int i = 0; i < len; i++) {
                DbJoin join = (DbJoin) joins.get(i);
                DbAttribute src = join.getSource();
                appendColumn(columns, null, src, attributes, null);
            }
        }

        // add remaining needed attrs from DbEntity

        DbEntity table = getRootDbEntity();
        Iterator pk = table.getPrimaryKey().iterator();
        while (pk.hasNext()) {
            DbAttribute dba = (DbAttribute) pk.next();
            appendColumn(columns, null, dba, attributes, null);
        }

        // special handling of a disjoint query...

        // TODO, Andrus 11/17/2005 - resultPath mechansim is generic and should probably
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

                Iterator it = table.resolvePathComponents(pathExp);

                // add joins and find terminating element
                Object pathComponent = null;
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

                    Iterator joins = relationship.getJoins().iterator();
                    while (joins.hasNext()) {
                        DbJoin j = (DbJoin) joins.next();

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

            Iterator jointPrefetches = query
                    .getPrefetchTree()
                    .adjacentJointNodes()
                    .iterator();

            while (jointPrefetches.hasNext()) {
                PrefetchTreeNode prefetch = (PrefetchTreeNode) jointPrefetches.next();

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
                        if (pathPart instanceof DbRelationship) {
                            DbRelationship rel = (DbRelationship) pathPart;
                            dbRelationshipAdded(rel);
                        }
                        else if (pathPart instanceof DbAttribute) {
                            DbAttribute attribute = (DbAttribute) pathPart;
                            if (attribute == null) {
                                throw new CayenneRuntimeException(
                                        "ObjAttribute has no DbAttribute: "
                                                + oa.getName());
                            }

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
    List appendCustomColumns(List columns, SelectQuery query) {

        List customAttributes = query.getCustomDbAttributes();
        DbEntity table = getRootDbEntity();
        int len = customAttributes.size();

        for (int i = 0; i < len; i++) {
            DbAttribute attribute = (DbAttribute) table
                    .getAttribute((String) customAttributes.get(i));
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
            List columns,
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
            for (int i = 0; i < columns.size(); i++) {
                ColumnDescriptor column = (ColumnDescriptor) columns.get(i);
                if (attribute.getName().equals(column.getName())) {

                    if (attributeOverrides == null) {
                        attributeOverrides = new HashMap();
                    }

                    attributeOverrides.put(objAttribute, column);

                    // kick out the original attribute
                    ObjAttribute original = (ObjAttribute) defaultAttributesByColumn
                            .remove(column);

                    if (original != null) {
                        attributeOverrides.put(original, column);
                        column.setJavaClass(Void.TYPE.getName());
                    }

                    break;
                }
            }
        }
    }

    private void appendGroupBy(StringBuffer queryBuf, int index) {
        DbAttribute column = (DbAttribute) groupByList.get(index);
        String alias = aliasForTable((DbEntity) column.getEntity());
        queryBuf.append(column.getAliasedName(alias));
    }

    private void appendTable(StringBuffer queryBuf, int index) {
        DbEntity ent = (DbEntity) tableList.get(index);
        queryBuf.append(ent.getFullyQualifiedName());
        // The alias should be the alias from the same index in aliasList, not that
        // returned by aliasForTable.
        queryBuf.append(' ').append((String) aliasList.get(index));
    }

    private void appendJoins(StringBuffer queryBuf, int index) {
        DbRelationship rel = (DbRelationship) dbRelList.get(index);
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
     * Sets up and returns a new alias for a speciafied table.
     */
    protected String newAliasForTable(DbEntity ent) {
        if (ent instanceof DerivedDbEntity) {
            ent = ((DerivedDbEntity) ent).getParentEntity();
        }

        String newAlias = "t" + aliasCounter++;
        tableList.add(ent);
        aliasList.add(newAlias);
        return newAlias;
    }

    public String aliasForTable(DbEntity ent, DbRelationship rel) {
        return (String) aliasLookup.get(rel);
    }

    /**
     * Overrides superclass implementation. Will return an alias that should be used for a
     * specified DbEntity in the query (or null if this DbEntity is not included in the
     * FROM clause).
     */
    public String aliasForTable(DbEntity ent) {
        if (ent instanceof DerivedDbEntity) {
            ent = ((DerivedDbEntity) ent).getParentEntity();
        }

        int entIndex = tableList.indexOf(ent);
        if (entIndex >= 0) {
            return (String) aliasList.get(entIndex);
        }
        else {
            StringBuffer msg = new StringBuffer();
            msg.append("Alias not found, DbEntity: '").append(
                    ent != null ? ent.getName() : "<null entity>").append(
                    "'\nExisting aliases:");

            int len = aliasList.size();
            for (int i = 0; i < len; i++) {
                String dbeName = (tableList.get(i) != null) ? ((DbEntity) tableList
                        .get(i)).getName() : "<null entity>";
                msg.append("\n").append(aliasList.get(i)).append(" => ").append(dbeName);
            }

            throw new CayenneRuntimeException(msg.toString());
        }
    }

    /**
     * Always returns true.
     */
    public boolean supportsTableAliases() {
        return true;
    }
}
