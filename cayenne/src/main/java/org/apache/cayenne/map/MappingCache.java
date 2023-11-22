/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.cayenne.util.commons.CompositeCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 4.0
 */
class MappingCache implements MappingNamespace {

    private static final ObjEntity OBJ_DUPLICATE_MARKER = new ObjEntity();

    protected static final Logger logger = LoggerFactory.getLogger(MappingCache.class);

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
            checkNameDuplicates(map.getDbEntityMap(), dbEntities, map);
            dbEntities.putAll(map.getDbEntityMap());
        }

        for (DataMap map : maps) {
            // index ObjEntities by name
            checkNameDuplicates(map.getObjEntityMap(), objEntities, map);
            objEntities.putAll(map.getObjEntityMap());

            // index ObjEntities by class name
            for (ObjEntity oe : map.getObjEntities()) {
                // use class name as a key to avoid class loading here...
                String className = oe.getJavaClassName();
                if (className == null) {
                    continue;
                }

                // allow duplicates, but put a special marker indicating
                // that this entity can't be looked up by class
                Object existing = objEntitiesByClassName.get(className);
                if (existing != null && existing != OBJ_DUPLICATE_MARKER) {
                    objEntitiesByClassName.put(className, OBJ_DUPLICATE_MARKER);
                } else {
                    objEntitiesByClassName.put(className, oe);
                }
            }

            // index stored procedures
            checkNameDuplicates(map.getProcedureMap(), procedures, map);
            procedures.putAll(map.getProcedureMap());

            // index embeddables
            embeddables.putAll(map.getEmbeddableMap());

            // index query descriptors
            queryDesriptors.putAll(map.getQueryDescriptorMap());
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

    /**
     * Utility method to warn about name duplicates in different DataMaps.
     *
     * @param src map
     * @param dst map with already added entities
     * @param srcMap source DataMap
     */
    private void checkNameDuplicates(Map<String, ? extends CayenneMapEntry> src,
                                     Map<String, ? extends CayenneMapEntry> dst,
                                     DataMap srcMap) {
        for(CayenneMapEntry entry : src.values()) {
            CayenneMapEntry duplicate = dst.get(entry.getName());
            if(duplicate != null) {
                DataMap parent = (DataMap) duplicate.getParent();
                logger.warn("Found duplicated name " + entry.getName()
                        + " in datamaps " + srcMap.getName() + " and " + parent.getName());
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
            throw new CayenneRuntimeException("Can't perform lookup. There is more than one ObjEntity mapped to %s"
                    , entityClass.getName());
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

        CompositeCollection<DbEntity> c = new CompositeCollection<>();
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
        
        CompositeCollection<Procedure> c = new CompositeCollection<>();
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

        CompositeCollection<QueryDescriptor> c = new CompositeCollection<>();
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
        
        CompositeCollection<ObjEntity> c = new CompositeCollection<>();
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
        
        CompositeCollection<Embeddable> c = new CompositeCollection<>();
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
        
        CompositeCollection<SQLResult> c = new CompositeCollection<>();
        for (DataMap map : maps) {
            c.addComposited(map.getResults());
        }

        return c;
    }
}
