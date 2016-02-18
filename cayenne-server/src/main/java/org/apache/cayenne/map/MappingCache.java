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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryDescriptor;
import org.apache.commons.collections.collection.CompositeCollection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @since 4.0
 */
class MappingCache implements MappingNamespace {

    private static final ObjEntity OBJ_DUPLICATE_MARKER = new ObjEntity();

    protected static final Log logger = LogFactory.getLog(MappingCache.class);

    protected Collection<DataMap> maps;
    protected Map<String, QueryDescriptor> queryDesriptors;
    protected Map<String, Embeddable> embeddables;
    protected Map<String, SQLResult> results;
    protected Map<String, DbEntity> dbEntities;
    protected Map<String, ObjEntity> objEntities;
    protected Map<String, ObjEntity> objEntitiesByClassName;
    protected Map<String, Procedure> procedures;
    protected Map<String, EntityInheritanceTree> entityInheritanceCache;

    MappingCache(Collection<DataMap> maps) {

        this.maps = maps;

        this.embeddables = new HashMap<>();
        this.queryDesriptors = new HashMap<>();
        this.dbEntities = new HashMap<>();
        this.objEntities = new HashMap<>();
        this.objEntitiesByClassName = new HashMap<>();
        this.procedures = new HashMap<>();
        this.entityInheritanceCache = new HashMap<>();
        this.results = new HashMap<>();

        index();
    }

    private void index() {

        // index DbEntities separately and before ObjEntities to avoid infinite
        // loops when looking up DbEntities during ObjEntity index op

        for (DataMap map : maps) {
            for (DbEntity de : map.getDbEntities()) {
                dbEntities.put(de.getName(), de);
            }
        }

        for (DataMap map : maps) {

            // index ObjEntities
            for (ObjEntity oe : map.getObjEntities()) {

                // index by name
                objEntities.put(oe.getName(), oe);

                // index by class.. use class name as a key to avoid class
                // loading here...
                String className = oe.getJavaClassName();
                if (className == null) {
                    continue;
                }

                // allow duplicates, but put a special marker indicating
                // that this entity can't be looked up by class
                Object existing = objEntitiesByClassName.get(className);
                if (existing != null) {

                    if (existing != OBJ_DUPLICATE_MARKER) {
                        objEntitiesByClassName.put(className, OBJ_DUPLICATE_MARKER);
                    }
                } else {
                    objEntitiesByClassName.put(className, oe);
                }
            }

            // index stored procedures
            for (Procedure proc : map.getProcedures()) {
                procedures.put(proc.getName(), proc);
            }

            // index embeddables
            embeddables.putAll(map.getEmbeddableMap());

            // index query descriptors
            for (QueryDescriptor queryDescriptor : map.getQueryDescriptors()) {
                String name = queryDescriptor.getName();
                QueryDescriptor existingQueryDescriptor = queryDesriptors.put(name, queryDescriptor);

                if (existingQueryDescriptor != null && queryDescriptor != existingQueryDescriptor) {
                    throw new CayenneRuntimeException("More than one QueryDescriptor for name: " + name);
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
                        ObjEntity superOE = objEntities.get(superOEName);
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

    public Embeddable getEmbeddable(String className) {
        return embeddables.get(className);
    }

    public SQLResult getResult(String name) {
        return results.get(name);
    }

    public EntityInheritanceTree getInheritanceTree(String entityName) {
        return entityInheritanceCache.get(entityName);
    }

    public Procedure getProcedure(String procedureName) {
        return procedures.get(procedureName);
    }

    public QueryDescriptor getQueryDescriptor(String queryName) {
        return queryDesriptors.get(queryName);
    }

    public DbEntity getDbEntity(String name) {
        return dbEntities.get(name);
    }

    public ObjEntity getObjEntity(Class<?> entityClass) {
        ObjEntity entity = objEntitiesByClassName.get(entityClass.getName());

        if (entity == OBJ_DUPLICATE_MARKER) {
            throw new CayenneRuntimeException("Can't perform lookup. There is more than one ObjEntity mapped to "
                    + entityClass.getName());
        }

        return entity;
    }

    public ObjEntity getObjEntity(Persistent object) {
        ObjectId id = object.getObjectId();
        if (id != null) {
            return getObjEntity(id.getEntityName());
        } else {
            return getObjEntity(object.getClass());
        }
    }

    public ObjEntity getObjEntity(String name) {
        return objEntities.get(name);
    }

    public Collection<DbEntity> getDbEntities() {
        // TODO: LEGACY SUPPORT:
        // some downstream code (like Modeler and merge framework) expect
        // always fresh list here, so instead of doing the right thing of
        // refreshing the cache and returning cache.entries(), we are scanning
        // the list of DataMaps.

        if (maps.size() == 0) {
            return Collections.emptyList();
        }
        
        if (maps.size() == 1) {
            return maps.iterator().next().getDbEntities();
        }

        CompositeCollection c = new CompositeCollection();
        for (DataMap map : maps) {
            c.addComposited(map.getDbEntities());
        }

        return c;
    }

    public Collection<Procedure> getProcedures() {
        // TODO: LEGACY SUPPORT:
        // some downstream code (like Modeler and merge framework) expect
        // always fresh list here, so instead of doing the right thing of
        // refreshing the cache and returning cache.entries(), we are scanning
        // the list of DataMaps.
        if (maps.size() == 0) {
            return Collections.emptyList();
        }
        
        if (maps.size() == 1) {
            return maps.iterator().next().getProcedures();
        }
        
        CompositeCollection c = new CompositeCollection();
        for (DataMap map : maps) {
            c.addComposited(map.getProcedures());
        }

        return c;
    }

    public Collection<QueryDescriptor> getQueryDescriptors() {
        // TODO: LEGACY SUPPORT:
        // some downstream code (like Modeler and merge framework) expect
        // always fresh list here, so instead of doing the right thing of
        // refreshing the cache and returning cache.entries(), we are scanning
        // the list of DataMaps.
        if (maps.size() == 0) {
            return Collections.emptyList();
        }

        if (maps.size() == 1) {
            return maps.iterator().next().getQueryDescriptors();
        }

        CompositeCollection c = new CompositeCollection();
        for (DataMap map : maps) {
            c.addComposited(map.getQueryDescriptors());
        }

        return c;
    }

    public Collection<ObjEntity> getObjEntities() {
        // TODO: LEGACY SUPPORT:
        // some downstream code (like Modeler and merge framework) expect
        // always fresh list here, so instead of doing the right thing of
        // refreshing the cache and returning cache.entries(), we are scanning
        // the list of DataMaps.
        if (maps.size() == 0) {
            return Collections.emptyList();
        }
        
        if (maps.size() == 1) {
            return maps.iterator().next().getObjEntities();
        }
        
        CompositeCollection c = new CompositeCollection();
        for (DataMap map : maps) {
            c.addComposited(map.getObjEntities());
        }

        return c;
    }

    public Collection<Embeddable> getEmbeddables() {
        // TODO: LEGACY SUPPORT:
        // some downstream code (like Modeler and merge framework) expect
        // always fresh list here, so instead of doing the right thing of
        // refreshing the cache and returning cache.entries(), we are scanning
        // the list of DataMaps.
        if (maps.size() == 0) {
            return Collections.emptyList();
        }
        
        if (maps.size() == 1) {
            return maps.iterator().next().getEmbeddables();
        }
        
        CompositeCollection c = new CompositeCollection();
        for (DataMap map : maps) {
            c.addComposited(map.getEmbeddables());
        }

        return c;
    }

    public Collection<SQLResult> getResults() {
        // TODO: LEGACY SUPPORT:
        // some downstream code (like Modeler and merge framework) expect
        // always fresh list here, so instead of doing the right thing of
        // refreshing the cache and returning cache.entries(), we are scanning
        // the list of DataMaps.
        
        if (maps.size() == 0) {
            return Collections.emptyList();
        }
        
        if (maps.size() == 1) {
            return maps.iterator().next().getResults();
        }
        
        CompositeCollection c = new CompositeCollection();
        for (DataMap map : maps) {
            c.addComposited(map.getResults());
        }

        return c;
    }
}
