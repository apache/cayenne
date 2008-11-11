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

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.ObjEntity;

/**
 * @deprecated since 3.0. Should be replaced either with EJBQL update query or SQLTemplate.
 */
public class DeleteQuery extends QualifiedQuery {

    /** Creates empty DeleteQuery. */
    public DeleteQuery() {
        super();
    }

    private void init(Object root, Expression qualifier) {
        this.setRoot(root);
        this.setQualifier(qualifier);
    }

    /**
     * Creates a DeleteQuery with null qualifier, for the specifed ObjEntity
     * 
     * @param root the ObjEntity this DeleteQuery is for.
     */
    public DeleteQuery(ObjEntity root) {
        this(root, null);
    }

    /**
     * Creates a DeleteQuery for the specifed ObjEntity with the given qualifier
     * 
     * @param root the ObjEntity this DeleteQuery is for.
     * @param qualifier an Expression indicating which objects should be deleted
     */
    public DeleteQuery(ObjEntity root, Expression qualifier) {
        this();
        this.init(root, qualifier);
    }

    /**
     * Creates a DeleteQuery with null qualifier, for the entity which uses the given
     * class
     * 
     * @param rootClass the Class of objects this DeleteQuery is for.
     */
    public DeleteQuery(Class rootClass) {
        this(rootClass, null);
    }

    /**
     * Creates a DeleteQuery for the entity which uses the given class, with the given
     * qualifier
     * 
     * @param rootClass the Class of objects this DeleteQuery is for.
     * @param qualifier an Expression indicating which objects should be deleted
     */
    public DeleteQuery(Class rootClass, Expression qualifier) {
        this.init(rootClass, qualifier);
    }

    /** Creates DeleteQuery with <code>objEntityName</code> parameter. */
    public DeleteQuery(String objEntityName) {
        this(objEntityName, null);
    }

    /**
     * Creates DeleteQuery with <code>objEntityName</code> and <code>qualifier</code>
     * parameters.
     */
    public DeleteQuery(String objEntityName, Expression qualifier) {
        this.init(objEntityName, qualifier);
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
}
