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

package org.apache.cayenne.wocompat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.query.Query;
import org.apache.commons.collections.Transformer;

/**
 * An extension of ObjEntity used to accomodate extra EOModel entity properties.
 */
public class EOObjEntity extends ObjEntity {
    
    protected boolean subclass;
    protected boolean abstractEntity;

    private Collection filteredQueries;
    private Map eoMap;

    public EOObjEntity() {
    }

    public EOObjEntity(String name) {
        super(name);
    }

    /**
     * Returns stored EOQuery.
     * 
     * @since 1.1
     */
    public EOQuery getEOQuery(String queryName) {
        Query query = getDataMap().getQuery(qualifiedQueryName(queryName));
        if (query instanceof EOQuery) {
            return (EOQuery) query;
        }

        return null;
    }

    /**
     * Overrides super to support translation of EO attributes that have no ObjAttributes.
     * 
     * @since 1.2
     */
    @Override
    public Expression translateToDbPath(Expression expression) {

        if (expression == null) {
            return null;
        }

        if (getDbEntity() == null) {
            throw new CayenneRuntimeException(
                    "Can't translate expression to DB_PATH, no DbEntity for '"
                            + getName()
                            + "'.");
        }

        // converts all OBJ_PATH expressions to DB_PATH expressions
        // and pass control to the DB entity
        return expression.transform(new DBPathConverter());
    }

    /**
     * @since 1.2
     */
    // TODO: andrus, 5/27/2006 - make public after 1.2. Also maybe move entity
    // initialization code from EOModelProcessor to this class, kind of like EOQuery does.
    Map getEoMap() {
        return eoMap;
    }

    /**
     * @since 1.2
     */
    // TODO: andrus, 5/27/2006 - make public after 1.2. Also maybe move entity
    // initialization code from EOModelProcessor to this class, kind of like EOQuery does.
    void setEoMap(Map eoMap) {
        this.eoMap = eoMap;
    }

    /**
     * Returns a collection of queries for this entity.
     * 
     * @since 1.1
     */
    public Collection getEOQueries() {
        if (filteredQueries == null) {
            Collection queries = getDataMap().getQueries();
            if (queries.isEmpty()) {
                filteredQueries = Collections.EMPTY_LIST;
            }
            else {
                Map params = Collections.singletonMap("root", EOObjEntity.this);
                Expression filter = Expression
                        .fromString("root = $root")
                        .expWithParameters(params);

                filteredQueries = filter.filter(queries, new ArrayList());
            }
        }

        return filteredQueries;
    }

    public boolean isAbstractEntity() {
        return abstractEntity;
    }

    public void setAbstractEntity(boolean abstractEntity) {
        this.abstractEntity = abstractEntity;
    }

    public boolean isSubclass() {
        return subclass;
    }

    public void setSubclass(boolean subclass) {
        this.subclass = subclass;
    }

    /**
     * Translates query name local to the ObjEntity to the global name. This translation
     * is needed since EOModels store queries by entity, while Cayenne DataMaps store them
     * globally.
     * 
     * @since 1.1
     */
    public String qualifiedQueryName(String queryName) {
        return getName() + "_" + queryName;
    }

    /**
     * @since 1.1
     */
    public String localQueryName(String qualifiedQueryName) {
        return (qualifiedQueryName != null && qualifiedQueryName.startsWith(getName()
                + "_"))
                ? qualifiedQueryName.substring(getName().length() + 1)
                : qualifiedQueryName;
    }

    final class DBPathConverter implements Transformer {

        public Object transform(Object input) {

            if (!(input instanceof Expression)) {
                return input;
            }

            Expression expression = (Expression) input;

            if (expression.getType() != Expression.OBJ_PATH) {
                return input;
            }

            // convert obj_path to db_path

            StringBuilder buffer = new StringBuilder();
            EOObjEntity entity = EOObjEntity.this;
            StringTokenizer toks = new StringTokenizer(expression.toString(), ".");
            while (toks.hasMoreTokens() && entity != null) {
                String chunk = toks.nextToken();

                if (toks.hasMoreTokens()) {
                    // this is a relationship
                    if (buffer.length() > 0) {
                        buffer.append(Entity.PATH_SEPARATOR);
                    }

                    buffer.append(chunk);

                    Relationship r = entity.getRelationship(chunk);
                    if (r == null) {
                        throw new ExpressionException("Invalid path component: " + chunk);
                    }

                    entity = (EOObjEntity) r.getTargetEntity();
                }
                // this is an attribute...
                else {

                    List attributes = (List) entity.getEoMap().get("attributes");
                    Iterator it = attributes.iterator();
                    while (it.hasNext()) {
                        Map attribute = (Map) it.next();
                        if (chunk.equals(attribute.get("name"))) {

                            if (buffer.length() > 0) {
                                buffer.append(Entity.PATH_SEPARATOR);
                            }

                            buffer.append(attribute.get("columnName"));
                            break;
                        }
                    }
                }
            }

            Expression exp = ExpressionFactory.expressionOfType(Expression.DB_PATH);
            exp.setOperand(0, buffer.toString());
            return exp;
        }
    }
}
