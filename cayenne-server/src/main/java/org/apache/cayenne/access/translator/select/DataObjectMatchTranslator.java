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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;

/**
 */
public class DataObjectMatchTranslator {

    protected Map<String, DbAttribute> attributes;
    protected Map<String, Object> values;
    protected String operation;
    protected Expression expression;
    protected DbRelationship relationship;
    protected String joinSplitAlias;

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    public void reset() {
        attributes = null;
        values = null;
        operation = null;
        expression = null;
        relationship = null;
    }

    /**
     * Initializes itself to do translation of the match ending with a DbRelationship.
     * 
     * @since 3.0
     */
    public void setRelationship(DbRelationship rel, String joinSplitAlias) {
        this.relationship = rel;
        this.joinSplitAlias = joinSplitAlias;
        attributes = new HashMap<String, DbAttribute>(rel.getJoins().size() * 2);

        if (rel.isToMany() || !rel.isToPK()) {

            // match on target PK
            DbEntity ent = (DbEntity) rel.getTargetEntity();

            // index by name
            for (DbAttribute pkAttr : ent.getPrimaryKeys()) {
                attributes.put(pkAttr.getName(), pkAttr);
            }
        }
        else {

            // match on this FK
            for (DbJoin join : rel.getJoins()) {
                // index by target name
                attributes.put(join.getTargetName(), join.getSource());
            }
        }
    }

    public void setDataObject(Persistent obj) {
        if (obj == null) {
            values = Collections.EMPTY_MAP;
            return;
        }

        setObjectId(obj.getObjectId());
    }

    /**
     * @since 1.2
     */
    public void setObjectId(ObjectId id) {
        if (id == null) {
            throw new CayenneRuntimeException(
                    "Null ObjectId, probably an attempt to use TRANSIENT object as a query parameter.");
        }
        else if (id.isTemporary()) {
            throw new CayenneRuntimeException(
                    "Temporary id, probably an attempt to use NEW object as a query parameter.");
        }
        else {
            values = id.getIdSnapshot();
        }
    }

    public Iterator<String> keys() {
        if (attributes == null) {
            throw new IllegalStateException(
                    "An attempt to use uninitialized DataObjectMatchTranslator: "
                            + "[attributes: null, values: "
                            + values
                            + "]");
        }

        return attributes.keySet().iterator();
    }
    
    /**
     * @since 3.0
     */
    public String getJoinSplitAlias() {
        return joinSplitAlias;
    }

    public DbRelationship getRelationship() {
        return relationship;
    }

    public DbAttribute getAttribute(String key) {
        return attributes.get(key);
    }

    public Object getValue(String key) {
        return values.get(key);
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getOperation() {
        return operation;
    }
}
