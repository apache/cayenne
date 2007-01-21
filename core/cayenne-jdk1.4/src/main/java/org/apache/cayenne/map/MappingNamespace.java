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

import java.util.Collection;

import org.apache.cayenne.query.Query;

/**
 * Defines API of a container of DbEntities, ObjEntities, Procedures, Queries 
 * and other mapping objects.
 * 
 * @since 1.1
 * @author Andrus Adamchik
 */
public interface MappingNamespace {
    /**
     * Returns DbEntity for a given name, or null if no
     * such DbEntity is found in the MappingNamespace.
     */
    public DbEntity getDbEntity(String name);

    /**
     * Returns ObjEntity for a given name, or null if no
     * such ObjEntity is found in the MappingNamespace.
     */
    public ObjEntity getObjEntity(String name);

    /**
     * Returns Procedure for a given name, or null if no
     * such Procedure is found in the MappingNamespace.
     */
    public Procedure getProcedure(String name);

    /**
     * Returns Query for a given name, or null if no
     * such Query is found in the MappingNamespace.
     */
    public Query getQuery(String name);

    /**
     * Returns all DbEntities in the namespace.
     */
    public Collection getDbEntities();

    /**
     * Returns all ObjEntities in the namespace.
     */
    public Collection getObjEntities();

    /**
     * Returns all Procedures in the namespace.
     */
    public Collection getProcedures();

    /**
     * Returns all Queries in the namespace.
     */
    public Collection getQueries();
}
