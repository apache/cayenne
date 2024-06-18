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
package org.apache.cayenne.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.util.XMLEncoder;

/**
 * @since 4.0
 */
public class SelectQueryDescriptor extends QueryDescriptor {

	private static final long serialVersionUID = -8798258795351950215L;

    public static final String DISTINCT_PROPERTY = "cayenne.SelectQuery.distinct";
    public static final boolean DISTINCT_DEFAULT = false;

	protected Expression qualifier;

    protected List<Ordering> orderings = new ArrayList<>();
    protected Map<String, Integer> prefetchesMap = new HashMap<>();

    public SelectQueryDescriptor() {
        super(SELECT_QUERY);
    }

    public void setDistinct(boolean value) {
        setProperty(DISTINCT_PROPERTY, String.valueOf(value));
    }

    public boolean isDistinct() {
        String distinct = getProperty(DISTINCT_PROPERTY);
        return distinct != null ? Boolean.parseBoolean(distinct) : DISTINCT_DEFAULT;
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
     * Returns map of prefetch paths with semantics for this query.
     *
     * @since 4.1
     */
    public Map<String, Integer> getPrefetchesMap() {
        return prefetchesMap;
    }

    /**
     * Sets map of prefetch paths with semantics for this query.
     *
     * @since 4.1
     */
    public void setPrefetchesMap(HashMap<String, Integer> prefetchesMap){
        this.prefetchesMap = prefetchesMap;
    }

    /**
     * Adds prefetch path with semantics to this query.
     *
     * @since 4.1
     */
    public void addPrefetch(String prefetchPath, int semantics){
        this.prefetchesMap.put(prefetchPath, semantics);
    }

    /**
     * Removes single prefetch path from this query.
     */
    public void removePrefetch(String prefetchPath) {
        this.prefetchesMap.remove(prefetchPath);
    }

    @Override
    public ObjectSelect<?> buildQuery() {
        // resolve root
        Object root = getRoot();
        String rootEntityName;
        if(root instanceof ObjEntity) {
            rootEntityName = ((ObjEntity) root).getName();
        } else if(root instanceof String) {
            rootEntityName = (String)root;
        } else {
            throw new CayenneRuntimeException("Unexpected root for the SelectQueryDescriptor '%s'.", root);
        }

        ObjectSelect<?> query = ObjectSelect.query(Object.class, getQualifier());
        query.entityName(rootEntityName);
        query.setRoot(root);

        List<Ordering> orderings = this.getOrderings();
        if (orderings != null && !orderings.isEmpty()) {
            query.orderBy(orderings);
        }

        if (prefetchesMap != null) {
            prefetchesMap.forEach(query::prefetch);
        }

        query.initWithProperties(this.getProperties());
        if(this.isDistinct()) {
            query.distinct();
        }
        return query;
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

        for (String prefetchPath : prefetchesMap.keySet()) {
            PrefetchTreeNode node = prefetchTree.addPath(prefetchPath);
            node.setSemantics(prefetchesMap.get(prefetchPath));
            node.setPhantom(false);
        }

        encoder.nested(prefetchTree, delegate);

        delegate.visitQuery(this);
        encoder.end();
    }
}
