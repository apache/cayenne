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

package org.apache.cayenne.query;

import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.ObjEntity;

/**
 * Object encapsulating an UPDATE statement. Note that updated attributes are expressed in
 * terms of DbAttribute names.
 * 
 * @deprecated since 3.0. Should be replaced either with EJBQL update query or SQLTemplate.
 */
public class UpdateQuery extends QualifiedQuery {

    protected Map<String, Object> updAttributes = new HashMap<String, Object>();

    /** Creates empty UpdateQuery. */
    public UpdateQuery() {
    }

    private void init(Object root, Expression qualifier) {
        setRoot(root);
        setQualifier(qualifier);
    }

    /**
     * Creates a UpdateQuery with null qualifier, for the specifed ObjEntity
     * 
     * @param root the ObjEntity this UpdateQuery is for.
     */
    public UpdateQuery(ObjEntity root) {
        this(root, null);
    }

    /**
     * Creates a UpdateQuery for the specifed ObjEntity with the given qualifier
     * 
     * @param root the ObjEntity this UpdateQuery is for.
     * @param qualifier an Expression indicating which objects will be updated
     */
    public UpdateQuery(ObjEntity root, Expression qualifier) {
        init(root, qualifier);
    }

    /**
     * Creates a UpdateQuery with null qualifier, for the entity which uses the given
     * class.
     * 
     * @param rootClass the Class of objects this UpdateQuery is for.
     */
    public UpdateQuery(Class rootClass) {
        this(rootClass, null);
    }

    /**
     * Creates a UpdateQuery for the entity which uses the given class, with the given
     * qualifier.
     * 
     * @param rootClass the Class of objects this UpdateQuery is for.
     * @param qualifier an Expression indicating which objects will be updated
     */
    public UpdateQuery(Class rootClass, Expression qualifier) {
        init(rootClass, qualifier);
    }

    /** Creates UpdateQuery with <code>objEntityName</code> parameter. */
    public UpdateQuery(String objEntityName) {
        this(objEntityName, null);
    }

    /**
     * Creates UpdateQuery with <code>objEntityName</code> and <code>qualifier</code>
     * parameters.
     */
    public UpdateQuery(String objEntityName, Expression qualifier) {
        init(objEntityName, qualifier);
    }

    /**
     * Calls "makeUpdate" on the visitor.
     * 
     * @since 1.2
     */
    @Override
    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        return visitor.updateAction(this);
    }

    public void addUpdAttribute(String attrName, Object updatedValue) {
        updAttributes.put(attrName, updatedValue);
    }

    /** Returns a map of updated attributes */
    public Map<String, Object> getUpdAttributes() {
        return updAttributes;
    }
}
