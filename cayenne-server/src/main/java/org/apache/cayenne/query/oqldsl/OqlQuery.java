/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cayenne.query.oqldsl;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.BaseConfigurationNodeVisitor;
import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.*;
import org.apache.cayenne.query.*;
import org.apache.cayenne.query.Select;
import org.apache.cayenne.query.oqldsl.dsl.Dsl;
import org.apache.cayenne.query.oqldsl.model.From;
import org.apache.cayenne.query.oqldsl.model.SelectResult;
import org.apache.cayenne.query.oqldsl.model.SelectResult.SelectAttr;
import org.apache.cayenne.query.oqldsl.model.SelectResult.SelectFrom;
import org.apache.cayenne.util.ToStringBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonList;

/**
 * @since 4.0
 */
public class OqlQuery<T> extends AbstractQuery implements Select<T>, Dsl.Select {

    private String name;

    /**
     * The root object this query. May be an entity name, Java class, ObjEntity or
     * DbEntity, depending on the specific query and how it was constructed.
     */
    private DataMap dataMap;

    private final NormalizedOqlQueryBuilder queryBuilder = new NormalizedOqlQueryBuilder();

    public OqlQuery() {

    }

    public OqlQuery(ObjEntity entity) {
        this.root = entity;

        From.Entity from = queryBuilder.guessNameIfAny(new From.Entity(name, entity));

        queryBuilder.addSelectResult(from)
                    .addFrom(from);
    }

    /**
     *
     * @param selectResults - here ugly interface ConfigurationNode but expected OqlExpression
     *                      you should be able to pass obj-entity, obj-attribute and compound expression
     *                      (i.e. aggregation function)
     * @param <T>
     *           Select result type - not used for now :(
     * @return
     */
    public static <T> OqlQuery<T> select(ConfigurationNode ... selectResults) {
        final OqlQuery<T> query = new OqlQuery<T>();

        for (ConfigurationNode selectResult : selectResults) {
            selectResult.acceptVisitor(new BaseConfigurationNodeVisitor<SelectResult>() {

                private Map<ObjEntity, SelectFrom> entityMap = new HashMap<ObjEntity, SelectFrom>();

                @Override
                public SelectFrom visitObjEntity(ObjEntity entity) {
                    SelectFrom from = entityMap.get(entity);
                    if (from != null) {
                        return from;
                    }

                    SelectFrom result = new SelectFrom(new From.Entity(null, entity));
                    entityMap.put(entity, from);
                    query.queryBuilder.addSelectResult(result);
                    return result;
                }

                @Override
                public SelectResult visitObjAttribute(ObjAttribute attribute) {
                    SelectFrom from = visitObjEntity(attribute.getEntity());

                    SelectAttr result = new SelectAttr(from.from, attribute);
                    query.queryBuilder.addSelectResult(result);
                    return result;
                }
            });
        }


        return query;
    }

    /**
     * @since 3.1
     */
    public <T> T acceptVisitor(ConfigurationNodeVisitor<T> visitor) {
        return visitor.visitQuery(this);
    }

    /**
     * @since 3.1
     */
    public DataMap getDataMap() {
        return dataMap;
    }

    /**
     * @since 3.1
     */
    public void setDataMap(DataMap dataMap) {
        this.dataMap = dataMap;
    }

    /**
     * Returns a symbolic name of the query.
     *
     * @since 1.1
     */
    public String getName() {
        return name;
    }

    /**
     * Sets a symbolic name of the query.
     *
     * @since 1.1
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the root of the query
     *
     * @param value The new root
     * @throws IllegalArgumentException if value is not a String, ObjEntity, DbEntity,
     *             Procedure, DataMap, Class or null.
     */
    public void setRoot(Object value) {
        // sanity check
        if (!(value instanceof String
                || value instanceof ObjEntity
                || value instanceof DbEntity
                || value instanceof Class
                || value instanceof Procedure || value instanceof DataMap)) {

            String rootClass = value != null ? value.getClass().getName() : "null";

            throw new IllegalArgumentException(
                    getClass().getName()
                            + ": \"setRoot(..)\" takes a DataMap, String, ObjEntity, DbEntity, Procedure, "
                            + "or Class. It was passed a "
                            + rootClass);
        }

    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("root", root)
                .append("name", getName())
                .toString();
    }

    /**
     * Calls "makeSelect" on the visitor.
     *
     * @since 1.2
     */
    @Override
    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        return visitor.oqlDslAction(this);
    }


    /**
     * Implements default routing mechanism relying on the EntityResolver to find DataMap
     * based on the query root. This mechanism should be sufficient for most queries that
     * "know" their root.
     *
     * @since 1.2
     */
    public void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery) {
        DataMap map = getMetaData(resolver).getDataMap();

        if (map == null) {
            throw new CayenneRuntimeException("No DataMap found, can't route query "
                    + this);
        }

        router.route(router.engineForDataMap(map), this, substitutedQuery);
    }

    @Override
    public Dsl.From from(ObjEntity entity) {
        return null;
    }

    public org.apache.cayenne.query.oqldsl.model.Select getSelect() {
        return queryBuilder.buildSelect();
    }
}
