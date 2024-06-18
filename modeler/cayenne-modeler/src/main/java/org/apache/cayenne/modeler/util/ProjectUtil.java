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

package org.apache.cayenne.modeler.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.MappingNamespace;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.util.Util;

/**
 * Provides utility methods to perform various manipulations with project objects.
 */
public class ProjectUtil {

    public static void setProcedureParameterName(
            ProcedureParameter parameter,
            String newName) {

        String oldName = parameter.getName();

        // If name hasn't changed, just return
        if (Util.nullSafeEquals(oldName, newName)) {
            return;
        }

        Procedure procedure = parameter.getProcedure();
        procedure.removeCallParameter(parameter.getName());
        parameter.setName(newName);
        procedure.addCallParameter(parameter);
    }

    public static void setDataMapName(DataChannelDescriptor domain, DataMap map, String newName) {
        String oldName = map.getName();

        // must fully relink renamed map
        List<DataNodeDescriptor> nodes = new ArrayList<>();
        for (DataNodeDescriptor node : domain.getNodeDescriptors())
            if (node.getDataMapNames().contains(map.getName()))
                nodes.add(node);

        map.setName(newName);

        for (DataNodeDescriptor node : nodes) {
            node.getDataMapNames().remove(oldName);
            node.getDataMapNames().add(map.getName());
        }
    }

    public static void setDataNodeName(DataNodeDescriptor node, String newName) {
        node.setName(newName);
    }

    public static void setProcedureName(DataMap map, Procedure procedure, String newName) {

        String oldName = procedure.getName();

        // If name hasn't changed, just return
        if (Util.nullSafeEquals(oldName, newName)) {
            return;
        }

        procedure.setName(newName);
        map.removeProcedure(oldName);
        map.addProcedure(procedure);

        // important - clear parent namespace:
        MappingNamespace ns = map.getNamespace();
        if (ns instanceof EntityResolver) {
            ((EntityResolver) ns).refreshMappingCache();
        }
    }

    public static void setQueryName(DataMap map, QueryDescriptor query, String newName) {

        String oldName = query.getName();

        // If name hasn't changed, just return
        if (Util.nullSafeEquals(oldName, newName)) {
            return;
        }

        query.setName(newName);
        query.setDataMap(map);

        map.removeQueryDescriptor(oldName);
        map.addQueryDescriptor(query);

        // important - clear parent namespace:
        MappingNamespace ns = map.getNamespace();
        if (ns instanceof EntityResolver) {
            ((EntityResolver) ns).refreshMappingCache();
        }
    }

    public static void setObjEntityName(DataMap map, ObjEntity entity, String newName) {
        String oldName = entity.getName();

        // If name hasn't changed, just return
        if (Util.nullSafeEquals(oldName, newName)) {
            return;
        }

        entity.setName(newName);
        map.removeObjEntity(oldName, false);
        map.addObjEntity(entity);

        // important - clear parent namespace:
        MappingNamespace ns = map.getNamespace();
        if (ns instanceof EntityResolver) {
            ((EntityResolver) ns).refreshMappingCache();
        }
    }

    /**
     * Renames a DbEntity and changes the name of all references.
     */
    public static void setDbEntityName(DbEntity entity, String newName) {
        String oldName = entity.getName();

        // If name hasn't changed, just return
        if (Util.nullSafeEquals(oldName, newName)) {
            return;
        }

        entity.setName(newName);
        DataMap map = entity.getDataMap();

        if (map != null) {
            map.removeDbEntity(oldName, false);
            map.addDbEntity(entity);

            // important - clear parent namespace:
            MappingNamespace ns = map.getNamespace();
            if (ns instanceof EntityResolver) {
                ((EntityResolver) ns).refreshMappingCache();
            }
        }
    }

    /**
     * Changes the name of the attribute and all references to this attribute.
     */
    public static void setAttributeName(ObjAttribute attribute, String newName) {
        String oldName = attribute.getName();

        attribute.setName(newName);
        ObjEntity entity = attribute.getEntity();

        if (entity != null) {
            entity.removeAttribute(oldName);
            entity.addAttribute(attribute);
        }
    }


    /**
     * Changes the name of the embeddable attribute and all references to this embeddable attribute.
     */
    public static void setEmbeddableAttributeName(EmbeddableAttribute attribute, String newName) {
        String oldName = attribute.getName();

        attribute.setName(newName);
        Embeddable embeddable = attribute.getEmbeddable();

        if (embeddable != null) {
            embeddable.removeAttribute(oldName);
            embeddable.addAttribute(attribute);
        }
    }

    /**
     * Adds or changes the name of the attribute in all places in DataMap.
     */
    public static void setRelationshipName(ObjEntity entity, ObjRelationship rel, String newName) {
        ObjRelationship existingRelationship = entity.getRelationship(newName);
        if (existingRelationship != null && existingRelationship != rel) {
            throw new IllegalArgumentException("An attempt to override relationship '" + rel.getName() + "'");
        }
        if (rel != null) {
            entity.removeRelationship(rel.getName());
            rel.setName(newName);
            entity.addRelationship(rel);
        }
    }

    /**
     * Cleans any mappings of ObjEntities, ObjAttributes, ObjRelationship to the
     * corresponding Db* objects that not longer exist.
     */
    public static void cleanObjMappings(DataMap map) {

        for (ObjEntity entity : map.getObjEntities()) {
            DbEntity dbEnt = entity.getDbEntity();

            // the whole entity mapping is invalid
            if (dbEnt != null && map.getDbEntity(dbEnt.getName()) != dbEnt) {
                clearDbMapping(entity);
                continue;
            }

            // check individual attributes
            for (ObjAttribute att : entity.getAttributes()) {

                // If flattened attribute
                // TODO: Perfect candidate for CayennePath usage
                String dbAttributePath = att.getDbAttributePath().value();
                if (dbAttributePath != null
                        && dbAttributePath.contains(".")) {
                    String[] pathSplit = dbAttributePath.split("\\.");

                    // If flattened attribute
                    if (pathSplit.length > 1) {

                        boolean isTruePath = isDbAttributePathCorrect(dbEnt, dbAttributePath);

                        if (!isTruePath) {
                            att.setDbAttributePath((String)null);
                        }
                    }
                } else {
                    DbAttribute dbAtt = att.getDbAttribute();
                    if (dbAtt != null) {
                        if (dbEnt.getAttribute(dbAtt.getName()) != dbAtt) {
                            att.setDbAttributePath((String)null);
                        }
                    }
                }
            }

            // check individual relationships
            for (ObjRelationship rel : entity.getRelationships()) {

                List<DbRelationship> dbRelList = new ArrayList<>(rel
                        .getDbRelationships());
                for (DbRelationship dbRel : dbRelList) {
                    DbEntity srcEnt = dbRel.getSourceEntity();
                    if (srcEnt == null
                            || map.getDbEntity(srcEnt.getName()) != srcEnt
                            || srcEnt.getRelationship(dbRel.getName()) != dbRel) {
                        rel.removeDbRelationship(dbRel);
                    }
                }
            }
        }
    }

    /**
     * check if path is correct. path is correct when he consist from <code>DbRelationship</code>
     * objects, each <code>DbRelationship</code> object have  following <code>DbRelationship</code>
     * object as a target, last component is <code>DbAttribute</code>
     *
     * @param currentEnt      current db entity
     * @param dbAttributePath path to check
     * @return if path is correct return true
     */
    public static boolean isDbAttributePathCorrect(DbEntity currentEnt, String dbAttributePath) {
        if (currentEnt == null) {
            return true;
        }

        String[] pathSplit = dbAttributePath.split("\\.");

        int size = pathSplit.length - 1;
        for (int j = 0; j < size; j++) {
            DbRelationship relationship = currentEnt.getRelationship(pathSplit[j]);
            if (relationship == null) {
                return false;
            }
            currentEnt = relationship.getTargetEntity();
        }

        return currentEnt.getAttribute(pathSplit[(size)]) != null;
    }

    /**
     * Clears all the mapping between this obj entity and its current db entity. Clears
     * mapping between entities, attributes and relationships.
     */
    public static void clearDbMapping(ObjEntity entity) {
        DbEntity db_entity = entity.getDbEntity();
        if (db_entity == null) {
            return;
        }

        for (ObjAttribute objAttr : entity.getAttributeMap().values()) {
            DbAttribute dbAttr = objAttr.getDbAttribute();
            if (null != dbAttr) {
                objAttr.setDbAttributePath((String)null);
            }
        }

        for (ObjRelationship obj_rel : entity.getRelationships()) {
            obj_rel.clearDbRelationships();
        }
        entity.setDbEntity(null);
    }

    /**
     * Returns true if one of relationship joins uses a given attribute as a source.
     */
    public static boolean containsSourceAttribute(
            DbRelationship relationship,
            DbAttribute attribute) {
        if (attribute.getEntity() != relationship.getSourceEntity()) {
            return false;
        }

        for (DbJoin join : relationship.getJoins()) {
            if (join.getSource() == attribute) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if one of relationship joins uses a given attribute as a target.
     */
    public static boolean containsTargetAttribute(
            DbRelationship relationship,
            DbAttribute attribute) {
        if (attribute.getEntity() != relationship.getTargetEntity()) {
            return false;
        }

        for (DbJoin join : relationship.getJoins()) {
            if (join.getTarget() == attribute) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns a collection of DbRelationships that use this attribute as a source.
     */
    public static Collection<DbRelationship> getRelationshipsUsingAttributeAsSource(DbAttribute attribute) {
        DbEntity parent = attribute.getEntity();

        if (parent == null) {
            return Collections.emptyList();
        }

        Collection<DbRelationship> parentRelationships = parent.getRelationships();
        Collection<DbRelationship> relationships = new ArrayList<>(parentRelationships.size());
        // Iterator it = parentRelationships.iterator();
        // while (it.hasNext()) {
        // DbRelationship relationship = (DbRelationship) it.next();
        for (DbRelationship relationship : parentRelationships) {
            if (ProjectUtil.containsSourceAttribute(relationship, attribute)) {
                relationships.add(relationship);
            }
        }
        return relationships;
    }

    /**
     * Returns a collection of DbRelationships that use this attribute as a source.
     */
    public static Collection<DbRelationship> getRelationshipsUsingAttributeAsTarget(
            DbAttribute attribute) {
        DbEntity parent = attribute.getEntity();

        if (parent == null) {
            return Collections.emptyList();
        }

        DataMap map = parent.getDataMap();
        if (map == null) {
            return Collections.emptyList();
        }

        Collection<DbRelationship> relationships = new ArrayList<>();
        for (DbEntity entity : map.getDbEntities()) {
            if (entity == parent) {
                continue;
            }

            Collection<DbRelationship> entityRelationships = entity.getRelationships();
            for (DbRelationship relationship : entityRelationships) {
                if (ProjectUtil.containsTargetAttribute(relationship, attribute)) {
                    relationships.add(relationship);
                }
            }
        }
        return relationships;
    }

    public static Collection<ObjEntity> getCollectionOfChildren(ObjEntity objEntity) {
        Collection<ObjEntity> objEntities = new ArrayList<>();
        for (ObjEntity child : objEntity.getDataMap().getObjEntities()) {
            if (child.isSubentityOf(objEntity)) {
                objEntities.add(child);
            }
        }
        return objEntities;
    }

    public static Collection<ObjRelationship> findObjRelationshipsForDbRelationship(ProjectController mediator,
                                                                                    DbRelationship relationship) {
        DataChannelDescriptor domain = (DataChannelDescriptor) mediator.getProject().getRootNode();
        List<ObjRelationship> objRelationships = new ArrayList<>();
        if (domain != null) {
            for (DataMap map : domain.getDataMaps()) {
                for (ObjEntity entity : map.getObjEntities()) {
                    for (ObjRelationship objRelationship : entity.getRelationships()) {
                        if (objRelationship.getDbRelationships().contains(relationship)) {
                            objRelationships.add(objRelationship);
                        }
                    }
                }
            }
        }
        return objRelationships;
    }

    public static Collection<ObjAttribute> findObjAttributesForDbRelationship(ProjectController mediator,
                                                                              DbRelationship relationship) {
        DataChannelDescriptor domain = (DataChannelDescriptor) mediator.getProject().getRootNode();
        List<ObjAttribute> attributes = new ArrayList<>();
        if (domain != null) {
            for (DataMap map : domain.getDataMaps()) {
                for (ObjEntity entity : map.getObjEntities()) {
                    for (ObjAttribute objAttribute : entity.getAttributes()) {
                        if (objAttribute.isFlattened()) {
                            objAttribute.getDbPathIterator().forEachRemaining(entry -> {
                                if (entry instanceof DbRelationship) {
                                    if (entry.equals(relationship)) {
                                        attributes.add(objAttribute);
                                    }
                                }
                            });
                        }
                    }
                }
            }
        }
        return attributes;
    }
}