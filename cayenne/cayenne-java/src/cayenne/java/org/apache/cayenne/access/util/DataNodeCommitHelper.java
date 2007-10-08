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


package org.apache.cayenne.access.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.Query;

/**
 * @deprecated Unused since 1.2.
 * @author Andrei Adamchik
 */
public class DataNodeCommitHelper {
    public static final int INSERT = 1;
    public static final int UPDATE = 2;
    public static final int DELETE = 3;

    protected DataNode node;
    protected List objEntitiesForInsert = new ArrayList();
    protected List objEntitiesForDelete = new ArrayList();
    protected List objEntitiesForUpdate = new ArrayList();
    protected Map flattenedInsertQueries = new HashMap();
    protected Map flattenedDeleteQueries = new HashMap();
    protected List queries = new ArrayList();

    /**
     * Finds an existing helper for DataNode, creates a new one if no matching
     * helper is found.
     */
    public static DataNodeCommitHelper getHelperForNode(
        List helpers,
        DataNode node) {

        DataNodeCommitHelper helper = null;
        Iterator it = helpers.iterator();
        while (it.hasNext()) {
            DataNodeCommitHelper itHelper = (DataNodeCommitHelper) it.next();
            if (itHelper.getNode() == node) {
                helper = itHelper;
                break;
            }
        }

        if (helper == null) {
            helper = new DataNodeCommitHelper(node);
            helpers.add(helper);
        }

        return helper;
    }

    public DataNodeCommitHelper(DataNode node) {
        this.node = node;
    }
    

    public void addToEntityList(ObjEntity ent, int listType) {
        switch (listType) {
            case 1 :
                objEntitiesForInsert.add(ent);
                break;
            case 2 :
                objEntitiesForUpdate.add(ent);
                break;
            case 3 :
                objEntitiesForDelete.add(ent);
                break;
        }
    }
    
    public void addToQueries(Query q) {
    	queries.add(q);
    }

    /**
     * Returns the node.
     */
    public DataNode getNode() {
        return node;
    }

    /**
     * Returns the queries.
     */
    public List getQueries() {
        return queries;
    }
    /**
     * Returns the objEntitiesForDelete.
     * @return List
     */
    public List getObjEntitiesForDelete() {
        return objEntitiesForDelete;
    }

    /**
     * Returns the objEntitiesForInsert.
     * @return List
     */
    public List getObjEntitiesForInsert() {
        return objEntitiesForInsert;
    }

    /**
     * Returns the objEntitiesForUpdate.
     * @return List
     */
    public List getObjEntitiesForUpdate() {
        return objEntitiesForUpdate;
    }
    
    public Map getFlattenedDeleteQueries() {
        return flattenedDeleteQueries;
    }

    public Map getFlattenedInsertQueries() {
        return flattenedInsertQueries;
    }
}
