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
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.query.Query;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @since 3.2
 */
class DefaultMappingCache implements MappingCache {

    private static final ObjEntity OBJ_DUPLICATE_MARKER = new ObjEntity();

    protected static final Log logger = LogFactory.getLog(DefaultMappingCache.class);

    protected Map<String, Query> queryCache;
    protected Map<String, Embeddable> embeddableCache;
    protected Map<String, SQLResult> resultsCache;
    protected Map<String, DbEntity> dbEntityCache;
    protected Map<String, ObjEntity> objEntityCache;
    protected Map<String, Procedure> procedureCache;
    protected Map<String, EntityInheritanceTree> entityInheritanceCache;

    DefaultMappingCache(Collection<DataMap> maps) {
        this.embeddableCache = new HashMap<String, Embeddable>();
        this.queryCache = new HashMap<String, Query>();
        this.dbEntityCache = new HashMap<String, DbEntity>();
        this.objEntityCache = new HashMap<String, ObjEntity>();
        this.procedureCache = new HashMap<String, Procedure>();
        this.entityInheritanceCache = new HashMap<String, EntityInheritanceTree>();
        this.resultsCache = new HashMap<String, SQLResult>();

        index(maps);
    }

    private void index(Collection<DataMap> maps) {

        // index DbEntities separately and before ObjEntities to avoid infinite
        // loops when looking up DbEntities during ObjEntity index op

        for (DataMap map : maps) {
            for (DbEntity de : map.getDbEntities()) {
                dbEntityCache.put(de.getName(), de);
            }
        }

        for (DataMap map : maps) {

            // index ObjEntities
            for (ObjEntity oe : map.getObjEntities()) {

                // index by name
                objEntityCache.put(oe.getName(), oe);

                // index by class.. use class name as a key to avoid class
                // loading here...
                String className = oe.getJavaClassName();
                if (className == null) {
                    continue;
                }

                String classKey = classKey(className);

                // allow duplicates, but put a special marker indicating
                // that this entity can't be looked up by class
                Object existing = objEntityCache.get(classKey);
                if (existing != null) {

                    if (existing != OBJ_DUPLICATE_MARKER) {
                        objEntityCache.put(classKey, OBJ_DUPLICATE_MARKER);
                    }
                } else {
                    objEntityCache.put(classKey, oe);
                }
            }

            // index stored procedures
            for (Procedure proc : map.getProcedures()) {
                procedureCache.put(proc.getName(), proc);
            }

            // index embeddables
            embeddableCache.putAll(map.getEmbeddableMap());

            // index queries
            for (Query query : map.getQueries()) {
                String name = query.getName();
                Object existingQuery = queryCache.put(name, query);

                if (existingQuery != null && query != existingQuery) {
                    throw new CayenneRuntimeException("More than one Query for name" + name);
                }
            }
        }

        // restart the map iterator to index inheritance
        for (DataMap map : maps) {

            // index ObjEntity inheritance
            for (ObjEntity oe : map.getObjEntities()) {

                // build inheritance tree
                EntityInheritanceTree node = entityInheritanceCache.get(oe.getName());
                if (node == null) {
                    node = new EntityInheritanceTree(oe);
                    entityInheritanceCache.put(oe.getName(), node);
                }

                String superOEName = oe.getSuperEntityName();
                if (superOEName != null) {
                    EntityInheritanceTree superNode = entityInheritanceCache.get(superOEName);

                    if (superNode == null) {
                        // do direct entity lookup to avoid recursive cache
                        // rebuild
                        ObjEntity superOE = objEntityCache.get(superOEName);
                        if (superOE != null) {
                            superNode = new EntityInheritanceTree(superOE);
                            entityInheritanceCache.put(superOEName, superNode);
                        } else {
                            // bad mapping? Or most likely some classloader
                            // issue
                            logger.warn("No super entity mapping for '" + superOEName + "'");
                            continue;
                        }
                    }

                    superNode.addChildNode(node);
                }
            }
        }
    }

    /**
     * Generates a map key for the object class.
     */
    private String classKey(String className) {
        // need to ensure that there is no conflict with entity names... I guess
        // such prefix is enough to guarantee that:
        return "^cl^" + className;
    }

    public Embeddable getEmbeddable(String className) {
        return embeddableCache.get(className);
    }

    public SQLResult getResult(String name) {
        return resultsCache.get(name);
    }

    public EntityInheritanceTree getInheritanceTree(String entityName) {
        return entityInheritanceCache.get(entityName);
    }

    public Procedure getProcedure(String procedureName) {
        return procedureCache.get(procedureName);
    }

    public Query getQuery(String queryName) {
        return queryCache.get(queryName);
    }

    public DbEntity getDbEntity(String name) {
        return dbEntityCache.get(name);
    }

    public ObjEntity getObjEntity(Class<?> entityClass) {
        return getObjEntity(classKey(entityClass.getName()));
    }

    public ObjEntity getObjEntity(String name) {
        ObjEntity entity = objEntityCache.get(name);

        if (entity == OBJ_DUPLICATE_MARKER) {
            throw new CayenneRuntimeException("Can't perform lookup. There is more than one ObjEntity mapped to "
                    + name);
        }

        return entity;
    }
}
