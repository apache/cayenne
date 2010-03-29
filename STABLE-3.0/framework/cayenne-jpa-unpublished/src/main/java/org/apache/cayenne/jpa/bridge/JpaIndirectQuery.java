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

package org.apache.cayenne.jpa.bridge;

import java.util.Map;

import org.apache.cayenne.jpa.JpaProviderException;
import org.apache.cayenne.jpa.map.JpaNamedQuery;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.IndirectQuery;
import org.apache.cayenne.query.ParameterizedQuery;
import org.apache.cayenne.query.Query;

/**
 * A superclass of indirect queries that map JPA to Cayenne queries.
 * 
 */
public abstract class JpaIndirectQuery extends IndirectQuery implements
        ParameterizedQuery {

    protected JpaNamedQuery jpaQuery;
    protected DataMap parentMap;
    protected ObjEntity parentEntity;
    protected Map<String, ?> parameters;

    public Query createQuery(Map<String, ?> parameters) {
        JpaIndirectQuery clone;
        try {
            clone = (JpaIndirectQuery) getClass().newInstance();
        }
        catch (Exception e) {
            throw new JpaProviderException("Error cloning a query", e);
        }

        clone.setJpaQuery(jpaQuery);
        clone.setParentEntity(parentEntity);
        clone.setParentMap(parentMap);
        clone.parameters = parameters;

        return clone;
    }

    public JpaNamedQuery getJpaQuery() {
        return jpaQuery;
    }

    public void setJpaQuery(JpaNamedQuery query) {
        this.jpaQuery = query;
    }

    public ObjEntity getParentEntity() {
        return parentEntity;
    }

    public void setParentEntity(ObjEntity parentEntity) {
        this.parentEntity = parentEntity;
    }

    public DataMap getParentMap() {
        return parentMap;
    }

    public void setParentMap(DataMap parentMap) {
        this.parentMap = parentMap;
    }
}
