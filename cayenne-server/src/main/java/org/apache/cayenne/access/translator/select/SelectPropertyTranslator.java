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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.exp.parser.AggregationFunction;
import org.apache.cayenne.map.*;
import org.apache.cayenne.query.*;
import org.apache.cayenne.query.select.SelectClause;
import org.apache.cayenne.reflect.*;
import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.cayenne.util.EqualsBuilder;
import org.apache.cayenne.util.HashCodeBuilder;

import java.io.IOException;
import java.sql.Types;
import java.util.*;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Translates query select clause to SQL.
 */
public class SelectPropertyTranslator extends QueryAssemblerHelper {

    private final SelectTranslator selectTranslator;

    private List<ColumnDescriptor> resultColumns;

    public SelectPropertyTranslator(SelectTranslator selectTranslator) {
        super(selectTranslator);

        this.selectTranslator = selectTranslator;
    }

    /**
     * Translates query Ordering list to SQL ORDER BY clause. Ordering list is obtained
     * from <code>queryAssembler</code>'s query object. In a process of building of
     * ORDER BY clause, <code>queryAssembler</code> is notified when a join needs to be
     * added.
     * 
     * @since 3.0
     */
    @Override
    protected void doAppendPart() throws IOException {


    }

    public String toString(AggregationFunction func, QualifierTranslator qualifierTranslator) {
        if (qualifierTranslator == null) {
            throw new IllegalArgumentException("qualifierTranslator can't be null");
        }

        if (func == null) {
            throw new IllegalArgumentException("functions can't be null");
        }

        String res = "*";

        if (func.getExpression() != null) {
            StringBuilder out = new StringBuilder();
            qualifierTranslator.setOut(out);

            func.getExpression().traverse(qualifierTranslator);

            res = out.toString();
        }

        if (isBlank(func.getName())) {
            return func.getFunction().sql() + "(" + res + ")";
        } else {
            return func.getFunction().sql() + "(" + res + ") as " + func.getName();
        }
    }

    protected void buildResultColumns() {

        SelectQuery<?> query = (SelectQuery<?>) selectTranslator.getQuery();
        SelectClause selectClause = query.getSelect();
        if (selectClause != null) {
            this.resultColumns = appendQueryResultColumns();
        } else if (query.getRoot() instanceof DbEntity) {
            this.resultColumns = appendDbEntityColumns();
        } else if (queryAssembler.getQueryMetadata().getPageSize() > 0) {
            this.resultColumns = appendIdColumns();
        } else {
            this.resultColumns = appendQueryColumns(query);
        }
    }

    private List<ColumnDescriptor> appendQueryResultColumns() {
        Query q = selectTranslator.getQuery();
        if (q == null || !(q instanceof SelectQuery)) {
            return new LinkedList<ColumnDescriptor>();
        }

        this.resultColumns = new LinkedList<ColumnDescriptor>();

        SelectClause selectClause = ((SelectQuery<?>) q).getSelect();
        for (Expression exp : selectClause.getProperties()) {
            this.out = new StringBuilder();
            if (exp.getType() == Expression.OBJ_PATH) {
                appendObjPath(exp);
            } else if (exp.getType() == Expression.DB_PATH) {
                appendDbPath(exp);
            } else {
                throw new CayenneRuntimeException("Unsupported ordering expression: " + exp); // TODO exception name is strange
            }


/*            this.resultColumns.add(new ColumnDescriptor(out.toString(),
                    getObjEntity().lastPathComponent(exp, queryAssembler.getPathAliases())
                            .getAttribute().getDbAttribute().getType()));*/
        }

/*        AggregationFunction func = selectClause.getAggregationFunction();
        if (func != null) {

            // Magic is here
            String res = toString(func, this.selectTranslator.getAdapter()
                    .getQualifierTranslator(this.selectTranslator));

            this.resultColumns.add(new ColumnDescriptor(res, Types.INTEGER)); // TODO calculate type
        }*/

        return this.resultColumns;
    }

    private List<ColumnDescriptor> appendDbEntityColumns() {
        List<ColumnDescriptor> columns = new LinkedList<ColumnDescriptor>();
        Set<ColumnTracker> attributes = new HashSet<ColumnTracker>();

        DbEntity table = queryAssembler.getRootDbEntity();
        for (DbAttribute dba : table.getAttributes()) {
            appendColumn(columns, null, dba, attributes, null);
        }

        return columns;
    }


    /**
     * Appends columns needed for object SelectQuery to the provided columns
     * list.
     */
    private <T> List<ColumnDescriptor> appendQueryColumns(SelectQuery<T> query) {
        final List<ColumnDescriptor> columns = new LinkedList<ColumnDescriptor>();
        final Set<ColumnTracker> attributes = new HashSet<ColumnTracker>();

        // fetched attributes include attributes that are either:
        //
        // * class properties
        // * PK
        // * FK used in relationship
        // * joined prefetch PK

        ClassDescriptor descriptor = selectTranslator.getQueryMetadata().getClassDescriptor();
        ObjEntity oe = descriptor.getEntity();

        PropertyVisitor visitor = new PropertyVisitor() {

            public boolean visitAttribute(AttributeProperty property) {
                ObjAttribute oa = property.getAttribute();

                selectTranslator.resetJoinStack();
                Iterator<CayenneMapEntry> dbPathIterator = oa.getDbPathIterator();
                while (dbPathIterator.hasNext()) {
                    Object pathPart = dbPathIterator.next();

                    if (pathPart == null) {
                        throw new CayenneRuntimeException("ObjAttribute has no component: " + oa.getName());

                    } else if (pathPart instanceof DbRelationship) {
                        selectTranslator.dbRelationshipAdded((DbRelationship) pathPart, JoinType.LEFT_OUTER, null);

                    } else if (pathPart instanceof DbAttribute) {
                        appendColumn(columns, oa, (DbAttribute) pathPart, attributes, null);

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
                selectTranslator.resetJoinStack();

                ObjRelationship rel = property.getRelationship();
                DbRelationship dbRel = rel.getDbRelationships().get(0);

                List<DbJoin> joins = dbRel.getJoins();
                for (DbJoin join : joins) {
                    DbAttribute src = join.getSource();
                    appendColumn(columns, null, src, attributes, null);
                }
            }
        };

        descriptor.visitAllProperties(visitor);

        // stack should be reset, because all root table attributes go with "t0" table alias
        selectTranslator.resetJoinStack();

        // add remaining needed attrs from DbEntity
        DbEntity table = selectTranslator.getRootDbEntity();
        for (DbAttribute dba : table.getPrimaryKeys()) {
            appendColumn(columns, null, dba, attributes, null);
        }

        // special handling of a disjoint query...

        if (query instanceof PrefetchSelectQuery) {

            // for each relationship path add PK of the target entity...
            for (String path : ((PrefetchSelectQuery) query).getResultPaths()) {

                ASTDbPath pathExp = (ASTDbPath) oe.translateToDbPath(Expression.fromString(path));

                // add joins and find terminating element

                selectTranslator.resetJoinStack();

                PathComponent<DbAttribute, DbRelationship> lastComponent = null;
                for (PathComponent<DbAttribute, DbRelationship> component : table
                        .resolvePath(pathExp, selectTranslator.getPathAliases())) {

                    if (component.getRelationship() != null) {
                        // do not invoke dbRelationshipAdded(), invoke pushJoin() instead.
                        // This is to prevent 'forcingDistinct' flipping to true, that will result
                        // in unneeded extra processing and sometimes in invalid results (see CAY-1979).
                        // Distinctness of each row is guaranteed by the prefetch query semantics - we
                        // include target ID in the result columns
                        selectTranslator.getJoinStack().pushJoin(component.getRelationship(), component.getJoinType(), null);
                    }

                    lastComponent = component;
                }

                // process terminating element
                if (lastComponent == null) {
                    continue;
                }

                DbRelationship relationship = lastComponent.getRelationship();
                if (relationship == null) {
                    continue;
                }

                String labelPrefix = pathExp.getPath();
                DbEntity targetEntity = relationship.getTargetEntity();

                for (DbAttribute pk : targetEntity.getPrimaryKeys()) {

                    // note that we my select a source attribute, but label it as
                    // target for simplified snapshot processing
                    appendColumn(columns, null, pk, attributes, labelPrefix + '.' + pk.getName());
                }
            }
        }

        // handle joint prefetches directly attached to this query...
        if (query.getPrefetchTree() != null) {

            for (PrefetchTreeNode prefetch : query.getPrefetchTree().adjacentJointNodes()) {

                // for each prefetch add all joins plus columns from the target
                // entity
                Expression prefetchExp = Expression.fromString(prefetch.getPath());
                ASTDbPath dbPrefetch = (ASTDbPath) oe.translateToDbPath(prefetchExp);

                selectTranslator.resetJoinStack();
                DbRelationship r = null;
                for (PathComponent<DbAttribute, DbRelationship> component
                        : table.resolvePath(dbPrefetch, selectTranslator.getPathAliases())) {

                    r = component.getRelationship();
                    selectTranslator.dbRelationshipAdded(r, JoinType.LEFT_OUTER, null);
                }

                if (r == null) {
                    throw new CayenneRuntimeException("Invalid joint prefetch '" + prefetch + "' for entity: "
                            + oe.getName());
                }

                // add columns from the target entity, including those that are matched
                // against the FK of the source entity. This is needed to determine
                // whether optional relationships are null

                // go via target OE to make sure that Java types are mapped correctly...
                ObjRelationship targetRel = (ObjRelationship) prefetchExp.evaluate(oe);
                ObjEntity targetEntity = targetRel.getTargetEntity();

                String labelPrefix = dbPrefetch.getPath();
                for (ObjAttribute oa : targetEntity.getAttributes()) {
                    Iterator<CayenneMapEntry> dbPathIterator = oa.getDbPathIterator();
                    while (dbPathIterator.hasNext()) {
                        Object pathPart = dbPathIterator.next();

                        if (pathPart == null) {
                            throw new CayenneRuntimeException("ObjAttribute has no component: " + oa.getName());
                        } else if (pathPart instanceof DbRelationship) {
                            selectTranslator.dbRelationshipAdded((DbRelationship) pathPart, JoinType.INNER, null);

                        } else if (pathPart instanceof DbAttribute) {
                            DbAttribute attribute = (DbAttribute) pathPart;

                            appendColumn(columns, oa, attribute, attributes, labelPrefix + '.' + attribute.getName());
                        }
                    }
                }

                // append remaining target attributes such as keys
                DbEntity targetDbEntity = r.getTargetEntity();
                for (DbAttribute attribute : targetDbEntity.getAttributes()) {
                    appendColumn(columns, null, attribute, attributes, labelPrefix + '.' + attribute.getName());
                }
            }
        }

        return columns;
    }

    private List<ColumnDescriptor> appendIdColumns() {
        List<ColumnDescriptor> columns = new LinkedList<ColumnDescriptor>();
        Set<ColumnTracker> skipSet = new HashSet<ColumnTracker>();

        ClassDescriptor descriptor = selectTranslator.getQueryMetadata().getClassDescriptor();
        ObjEntity oe = descriptor.getEntity();
        DbEntity dbEntity = oe.getDbEntity();
        for (ObjAttribute attribute : oe.getPrimaryKeys()) {

            // synthetic objattributes can't reliably lookup their DbAttribute, so do it manually..
            DbAttribute dbAttribute = dbEntity.getAttribute(attribute.getDbAttributeName());
            appendColumn(columns, attribute, dbAttribute, skipSet, null);
        }

        return columns;
    }

    private void appendColumn(List<ColumnDescriptor> columns, ObjAttribute objAttribute, DbAttribute attribute,
                              Set<ColumnTracker> skipSet, String label) {

        String alias = queryAssembler.getCurrentAlias();
        if (skipSet.add(new ColumnTracker(alias, attribute))) {

            ColumnDescriptor column = objAttribute == null
                    ? new ColumnDescriptor(attribute, alias)
                    : new ColumnDescriptor(objAttribute, attribute, alias);

            if (label != null) {
                column.setDataRowKey(label);
            }

            columns.add(column);

            // TODO: andrus, 5/7/2006 - replace 'columns' collection with this map, as it is redundant
            if (objAttribute != null) {
                selectTranslator.getAttributeOverrides().put(objAttribute, column);
            }
        } else if (objAttribute != null) {
            ColumnDescriptor column = findColumnByName(columns, attribute.getName());
            if (column == null) {
                return;
            }

            column.setJavaClass(Void.TYPE.getName());
            selectTranslator.getAttributeOverrides().put(objAttribute, column);
        }
    }

    private ColumnDescriptor findColumnByName(List<ColumnDescriptor> columns, String attributeName) {
        for (ColumnDescriptor column : columns) {
            if (attributeName.equals(column.getName())) {
                return column;
            }
        }
        return null;
    }


    @Override
    protected void processColumnWithQuoteSqlIdentifiers(DbAttribute dbAttr, Expression pathExp) {
        super.processColumnWithQuoteSqlIdentifiers(dbAttr, pathExp);

        appendColumn(resultColumns, null, dbAttr, new HashSet<ColumnTracker>(), null);
    }

    public List<ColumnDescriptor> getResultColumns() {
        if (resultColumns == null) {
            buildResultColumns();
        }
        return resultColumns;
    }

    static final class ColumnTracker {

        private final DbAttribute attribute;
        private final String alias;

        ColumnTracker(String alias, DbAttribute attribute) {
            this.attribute = attribute;
            this.alias = alias;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof ColumnTracker)) {
                return false;
            }

            ColumnTracker other = (ColumnTracker) object;
            return new EqualsBuilder()
                    .append(alias, other.alias)
                    .append(attribute, other.attribute) // TODO DbAttribute doesn't override equals
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(31, 5)
                    .append(alias)
                    .append(attribute)
                    .toHashCode();
        }
    }
}
