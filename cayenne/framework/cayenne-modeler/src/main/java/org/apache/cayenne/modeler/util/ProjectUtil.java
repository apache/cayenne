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

package org.apache.cayenne.modeler.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.MappingNamespace;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.query.AbstractQuery;
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

    public static void setDataMapName(DataDomain domain, DataMap map, String newName) {
        String oldName = map.getName();

        // If name hasn't changed, just return
        if (Util.nullSafeEquals(oldName, newName)) {
            return;
        }

        // must fully relink renamed map
        List<DataNode> nodes = new ArrayList<DataNode>();
        Iterator allNodes = domain.getDataNodes().iterator();
        while (allNodes.hasNext()) {
            DataNode node = (DataNode) allNodes.next();
            if (node.getDataMaps().contains(map)) {
                nodes.add(node);
            }
        }

        domain.removeMap(oldName);
        map.setName(newName);
        domain.addMap(map);

        Iterator relinkNodes = nodes.iterator();
        while (relinkNodes.hasNext()) {
            DataNode node = (DataNode) relinkNodes.next();
            node.removeDataMap(oldName);
            node.addDataMap(map);
        }
    }

    public static void setDataDomainName(
            Configuration configuration,
            DataDomain domain,
            String newName) {

        String oldName = domain.getName();
        // If name hasn't changed, just return
        if (Util.nullSafeEquals(oldName, newName)) {
            return;
        }

        domain.setName(newName);
        configuration.removeDomain(oldName);
        configuration.addDomain(domain);
    }

    public static void setDataNodeName(DataDomain domain, DataNode node, String newName) {
        String oldName = node.getName();
        node.setName(newName);
        domain.removeDataNode(oldName);
        domain.addNode(node);
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
            ((EntityResolver) ns).clearCache();
        }
    }

    public static void setQueryName(DataMap map, AbstractQuery query, String newName) {

        String oldName = query.getName();

        // If name hasn't changed, just return
        if (Util.nullSafeEquals(oldName, newName)) {
            return;
        }

        query.setName(newName);
        map.removeQuery(oldName);
        map.addQuery(query);

        // important - clear parent namespace:
        MappingNamespace ns = map.getNamespace();
        if (ns instanceof EntityResolver) {
            ((EntityResolver) ns).clearCache();
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
            ((EntityResolver) ns).clearCache();
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
                ((EntityResolver) ns).clearCache();
            }
        }
    }

    /**
     * Changes the name of the attribute and all references to this attribute.
     */
    public static void setAttributeName(Attribute attribute, String newName) {
        String oldName = attribute.getName();

        attribute.setName(newName);

        Entity entity = attribute.getEntity();

        if (entity != null) {
            entity.removeAttribute(oldName);
            entity.addAttribute(attribute);
        }
    }

    /** Changes the name of the attribute in all places in DataMap. */
    public static void setRelationshipName(Entity entity, Relationship rel, String newName) {

        if (rel == null || rel != entity.getRelationship(rel.getName())) {
            return;
        }

        entity.removeRelationship(rel.getName());
        rel.setName(newName);
        entity.addRelationship(rel);
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

            // check indiv. attributes
            Iterator atts = entity.getAttributes().iterator();
            while (atts.hasNext()) {
                ObjAttribute att = (ObjAttribute) atts.next();
                DbAttribute dbAtt = att.getDbAttribute();
                if (dbAtt != null) {
                    if (dbEnt.getAttribute(dbAtt.getName()) != dbAtt) {
                        att.setDbAttributePath(null);
                    }
                }
            }

            // check indiv. relationships
            Iterator rels = entity.getRelationships().iterator();
            while (rels.hasNext()) {
                ObjRelationship rel = (ObjRelationship) rels.next();

                Iterator dbRels = new ArrayList(rel.getDbRelationships()).iterator();
                while (dbRels.hasNext()) {
                    DbRelationship dbRel = (DbRelationship) dbRels.next();
                    Entity srcEnt = dbRel.getSourceEntity();
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
     * Clears all the mapping between this obj entity and its current db entity. Clears
     * mapping between entities, attributes and relationships.
     */
    public static void clearDbMapping(ObjEntity entity) {
        DbEntity db_entity = entity.getDbEntity();
        if (db_entity == null) {
            return;
        }

        Iterator it = entity.getAttributeMap().values().iterator();
        while (it.hasNext()) {
            ObjAttribute objAttr = (ObjAttribute) it.next();
            DbAttribute dbAttr = objAttr.getDbAttribute();
            if (null != dbAttr) {
                objAttr.setDbAttributePath(null);
            }
        }

        Iterator rel_it = entity.getRelationships().iterator();
        while (rel_it.hasNext()) {
            ObjRelationship obj_rel = (ObjRelationship) rel_it.next();
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

        Iterator it = relationship.getJoins().iterator();
        while (it.hasNext()) {
            DbJoin join = (DbJoin) it.next();
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

        Iterator it = relationship.getJoins().iterator();
        while (it.hasNext()) {
            DbJoin join = (DbJoin) it.next();
            if (join.getTarget() == attribute) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns a collection of DbRelationships that use this attribute as a source.
     */
    public static Collection getRelationshipsUsingAttributeAsSource(DbAttribute attribute) {
        Entity parent = attribute.getEntity();

        if (parent == null) {
            return Collections.EMPTY_LIST;
        }

        Collection parentRelationships = parent.getRelationships();
        Collection relationships = new ArrayList(parentRelationships.size());
        Iterator it = parentRelationships.iterator();
        while (it.hasNext()) {
            DbRelationship relationship = (DbRelationship) it.next();
            if (ProjectUtil.containsSourceAttribute(relationship, attribute)) {
                relationships.add(relationship);
            }
        }
        return relationships;
    }

    /**
     * Returns a collection of DbRelationships that use this attribute as a source.
     */
    public static Collection getRelationshipsUsingAttributeAsTarget(DbAttribute attribute) {
        Entity parent = attribute.getEntity();

        if (parent == null) {
            return Collections.EMPTY_LIST;
        }

        DataMap map = parent.getDataMap();
        if (map == null) {
            return Collections.EMPTY_LIST;
        }

        Collection relationships = new ArrayList();

        Iterator it = map.getDbEntities().iterator();
        while (it.hasNext()) {
            Entity entity = (Entity) it.next();
            if (entity == parent) {
                continue;
            }

            Collection entityRelationships = entity.getRelationships();
            Iterator relationshipsIt = entityRelationships.iterator();
            while (relationshipsIt.hasNext()) {
                DbRelationship relationship = (DbRelationship) relationshipsIt.next();
                if (ProjectUtil.containsTargetAttribute(relationship, attribute)) {
                    relationships.add(relationship);
                }
            }
        }
        return relationships;
    }
}
