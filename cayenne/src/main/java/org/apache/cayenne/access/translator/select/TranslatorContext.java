/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access.translator.select;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.sqlbuilder.SQLGenerationContext;
import org.apache.cayenne.access.sqlbuilder.SelectBuilder;
import org.apache.cayenne.access.sqlbuilder.SQLBuilder;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.translator.DbAttributeBinding;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.exp.parser.ASTAggregateFunctionCall;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.EntityResult;
import org.apache.cayenne.map.SQLResult;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.QueryMetadata;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.exp;
import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.node;

/**
 * Context that holds all data necessary for query translation as well as a result of that translation.
 *
 * @since 4.2
 */
public class TranslatorContext implements SQLGenerationContext {

    private final TableTree tableTree;

    /**
     * Result columns, can be following:
     * - root object attributes (including flattened)
     * - root object additional db attributes (PKs and FKs)
     * - flattened attributes additional PKs
     * - prefetched objects attributes and additional db attributes (PKs and FKs)
     * - order by expressions if query is distinct?
     */
    private final List<ColumnDescriptor> columnDescriptors;


    /**
     * Scalar values bindings in order of appearance in final SQL,
     * may be should be filled by SQL node visitor.
     * <p>
     * Can be from expressions encountered in:
     * - attributes
     * - order by expressions
     * - where expression (including qualifiers from all used DbEntities and ObjEntities)
     */
    private final Collection<DbAttributeBinding> bindings;

    // Translated query
    private final TranslatableQueryWrapper query;
    private final QueryMetadata metadata;

    private final EntityResolver resolver;
    private final DbAdapter adapter;
    private final QuotingStrategy quotingStrategy;

    // Select builder that builds final SQL tree
    private final SelectBuilder selectBuilder;
    private final QualifierTranslator qualifierTranslator;
    private final PathTranslator pathTranslator;
    // Parent context will be not null in case of a nested query
    private final TranslatorContext parentContext;
    // List of SQL tree nodes that describe resulting rows of this query
    private final List<ResultNodeDescriptor> resultNodeList;

    // resulting qualifier for this query ('where' qualifier and qualifiers from entities)
    private Node qualifierNode;
    // if true SQL generation stage will be skipped, needed for nested queries translation
    private boolean skipSQLGeneration;
    // translated SQL string
    private String finalSQL;
    // suppress DISTINCT clause
    private boolean distinctSuppression;

    private Boolean hasAggregate;

    // index of a last result node of a root entity
    private int rootSegmentEnd;
    // should current entity be linked to root object
    // (prefetch entity should, while unrelated entity in a column select shouldn't)
    // this flag can be removed if logic that converts result row into an object tree allows random order of columns in a row.
    private boolean appendResultToRoot;

    private SQLResult sqlResult;
    private EntityResult rootEntityResult;

    TranslatorContext(TranslatableQueryWrapper query, DbAdapter adapter, EntityResolver resolver, TranslatorContext parentContext) {
        this.query = query;
        this.adapter = adapter;
        this.resolver = resolver;
        this.metadata = query.getMetaData(resolver);
        this.parentContext = parentContext;
        this.tableTree = new TableTree(metadata.getDbEntity(), parentContext == null ? null : parentContext.getTableTree());
        this.columnDescriptors = new ArrayList<>();
        this.bindings = new ArrayList<>(4);
        this.selectBuilder = SQLBuilder.select();
        this.pathTranslator = new PathTranslator(this);
        this.qualifierTranslator = new QualifierTranslator(this);
        this.quotingStrategy = adapter.getQuotingStrategy();
        this.resultNodeList = new LinkedList<>();
        if(query.needsResultSetMapping()) {
            this.sqlResult = new SQLResult();
        }
    }

    /**
     * Mark start of a new class descriptor, to be able to process result columns properly.
     * @param type of a descriptor
     * @see #addResultNode(Node, boolean, Property, CayennePath)
     */
    void markDescriptorStart(DescriptorType type) {
        if(type == DescriptorType.PREFETCH) {
            appendResultToRoot = true;
        }
    }

    void markDescriptorEnd(DescriptorType type) {
        if(type == DescriptorType.ROOT) {
            rootSegmentEnd = resultNodeList.size() - 1;
        } else if(type == DescriptorType.PREFETCH) {
            appendResultToRoot = false;
        }
    }

    SelectBuilder getSelectBuilder() {
        return selectBuilder;
    }

    Collection<ColumnDescriptor> getColumnDescriptors() {
        return columnDescriptors;
    }

    public Collection<DbAttributeBinding> getBindings() {
        return bindings;
    }

    TableTree getTableTree() {
        return tableTree;
    }

    QualifierTranslator getQualifierTranslator() {
        return qualifierTranslator;
    }

    PathTranslator getPathTranslator() {
        return pathTranslator;
    }

    int getTableCount() {
        return tableTree.getNodeCount();
    }

    TranslatableQueryWrapper getQuery() {
        return query;
    }

    QueryMetadata getMetadata() {
        return metadata;
    }

    EntityResolver getResolver() {
        return resolver;
    }

    public DbAdapter getAdapter() {
        return adapter;
    }

    public DbEntity getRootDbEntity() {
        return metadata.getDbEntity();
    }

    public QuotingStrategy getQuotingStrategy() {
        return quotingStrategy;
    }

    boolean hasAggregate() {
        if(hasAggregate != null) {
            return hasAggregate;
        }

        if(getQuery().getHavingQualifier() != null) {
            return (hasAggregate = true);
        }

        for(ResultNodeDescriptor resultNode : getResultNodeList()) {
            if(resultNode.isAggregate()) {
                return (hasAggregate = true);
            }
        }

        if(getQuery().getOrderings() != null) {
            for(Ordering ordering : getQuery().getOrderings()) {
                if(ordering.getSortSpec() instanceof ASTAggregateFunctionCall) {
                    return (hasAggregate = true);
                }
            }
        }

        return (hasAggregate = false);
    }

    void setDistinctSuppression(boolean distinctSuppression) {
        this.distinctSuppression = distinctSuppression;
    }

    boolean isDistinctSuppression() {
        return distinctSuppression;
    }

    void setFinalSQL(String SQL) {
        this.finalSQL = SQL;
    }

    String getFinalSQL() {
        return finalSQL;
    }

    List<ResultNodeDescriptor> getResultNodeList() {
        return resultNodeList;
    }

    ResultNodeDescriptor addResultNode(Node node) {
        return addResultNode(node, false, null, null);
    }

    ResultNodeDescriptor addResultNode(Node node, CayennePath dataRowKey) {
        return addResultNode(node, true, null, dataRowKey);
    }

    ResultNodeDescriptor addResultNode(Node node, boolean inDataRow, Property<?> property, CayennePath dataRowKey) {
        ResultNodeDescriptor resultNode = new ResultNodeDescriptor(node, inDataRow, property, dataRowKey);
        if(appendResultToRoot) {
            resultNodeList.add(rootSegmentEnd + 1, resultNode);
        } else {
            resultNodeList.add(resultNode);
        }
        return resultNode;
    }

    TranslatorContext getParentContext() {
        return parentContext;
    }

    void setSkipSQLGeneration(boolean skipSQLGeneration) {
        this.skipSQLGeneration = skipSQLGeneration;
    }

    boolean isSkipSQLGeneration() {
        return skipSQLGeneration;
    }

    SQLResult getSqlResult() {
        return sqlResult;
    }

    void setRootEntityResult(EntityResult rootEntityResult) {
        this.rootEntityResult = rootEntityResult;
    }

    EntityResult getRootEntityResult() {
        return rootEntityResult;
    }

    void setQualifierNode(Node qualifierNode) {
        this.qualifierNode = qualifierNode;
    }

    void appendQualifierNode(Node qualifierNode) {
        if(this.qualifierNode == null) {
            this.qualifierNode = qualifierNode;
        } else {
            this.qualifierNode = exp(node(this.qualifierNode)).and(node(qualifierNode)).build();
        }
    }

    Node getQualifierNode() {
        return qualifierNode;
    }

    enum DescriptorType {
        ROOT,
        PREFETCH,
        OTHER
    }

}
