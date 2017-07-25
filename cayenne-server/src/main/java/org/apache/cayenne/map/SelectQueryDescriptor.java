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
package org.apache.cayenne.map;

import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.util.XMLEncoder;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 4.0
 */
public class SelectQueryDescriptor extends QueryDescriptor {

	private static final long serialVersionUID = -8798258795351950215L;

	protected Expression qualifier;

    protected List<Ordering> orderings = new ArrayList<>();
    protected List<String> prefetches = new ArrayList<>();

    public SelectQueryDescriptor() {
        super(SELECT_QUERY);
    }

    public void setDistinct(boolean value) {
        setProperty(SelectQuery.DISTINCT_PROPERTY, String.valueOf(value));
    }

    public boolean isDistinct() {
        String distinct = getProperty(SelectQuery.DISTINCT_PROPERTY);

        return distinct != null ? Boolean.valueOf(distinct) : false;
    }

    /**
     * Returns qualifier of this query.
     */
    public Expression getQualifier() {
        return qualifier;
    }

    /**
     * Sets qualifier for this query.
     */
    public void setQualifier(Expression qualifier) {
        this.qualifier = qualifier;
    }

    /**
     * Returns list of orderings for this query.
     */
    public List<Ordering> getOrderings() {
        return orderings;
    }

    /**
     * Sets list of orderings for this query.
     */
    public void setOrderings(List<Ordering> orderings) {
        this.orderings = orderings;
    }

    /**
     * Adds single ordering for this query.
     */
    public void addOrdering(Ordering ordering) {
        this.orderings.add(ordering);
    }

    /**
     * Removes single ordering from this query.
     */
    public void removeOrdering(Ordering ordering) {
        this.orderings.remove(ordering);
    }

    /**
     * Returns list of prefetch paths for this query.
     */
    public List<String> getPrefetches() {
        return prefetches;
    }

    /**
     * Sets list of prefetch paths for this query.
     */
    public void setPrefetches(List<String> prefetches) {
        this.prefetches = prefetches;
    }

    /**
     * Adds single prefetch path to this query.
     */
    public void addPrefetch(String prefetchPath) {
        this.prefetches.add(prefetchPath);
    }

    /**
     * Removes single prefetch path from this query.
     */
    public void removePrefetch(String prefetchPath) {
        this.prefetches.remove(prefetchPath);
    }

    @Override
    public SelectQuery<?> buildQuery() {
        SelectQuery<Object> selectQuery = new SelectQuery<>();
        selectQuery.setRoot(this.getRoot());
        selectQuery.setName(this.getName());
        selectQuery.setDataMap(dataMap);
        selectQuery.setQualifier(this.getQualifier());

        List<Ordering> orderings = this.getOrderings();

        if (orderings != null && !orderings.isEmpty()) {
            selectQuery.addOrderings(orderings);
        }

        List<String> prefetches = this.getPrefetches();

        if (prefetches != null && !prefetches.isEmpty()) {
            for (String prefetch : prefetches) {
                selectQuery.addPrefetch(prefetch);
            }
        }

        // init properties
        selectQuery.initWithProperties(this.getProperties());

        return selectQuery;
    }

    @Override
    public void encodeAsXML(XMLEncoder encoder, ConfigurationNodeVisitor delegate) {
        encoder.start("query")
                .attribute("name", getName())
                .attribute("type", type);

        String rootString = null;
        String rootType = null;

        if (root instanceof String) {
            rootType = QueryDescriptor.OBJ_ENTITY_ROOT;
            rootString = root.toString();
        } else if (root instanceof ObjEntity) {
            rootType = QueryDescriptor.OBJ_ENTITY_ROOT;
            rootString = ((ObjEntity) root).getName();
        } else if (root instanceof DbEntity) {
            rootType = QueryDescriptor.DB_ENTITY_ROOT;
            rootString = ((DbEntity) root).getName();
        } else if (root instanceof Procedure) {
            rootType = QueryDescriptor.PROCEDURE_ROOT;
            rootString = ((Procedure) root).getName();
        } else if (root instanceof Class<?>) {
            rootType = QueryDescriptor.JAVA_CLASS_ROOT;
            rootString = ((Class<?>) root).getName();
        }

        if (rootType != null) {
            encoder.attribute("root", rootType).attribute("root-name", rootString);
        }

        // print properties
        encodeProperties(encoder);

        // encode qualifier
        if (qualifier != null) {
            encoder.start("qualifier").nested(qualifier, delegate).end();
        }

        // encode orderings
        encoder.nested(orderings, delegate);

        PrefetchTreeNode prefetchTree = new PrefetchTreeNode();

        for (String prefetchPath : prefetches) {
            PrefetchTreeNode node = prefetchTree.addPath(prefetchPath);
            node.setSemantics(PrefetchTreeNode.UNDEFINED_SEMANTICS);
            node.setPhantom(false);
        }

        encoder.nested(prefetchTree, delegate);

        delegate.visitQuery(this);
        encoder.end();
    }
}
