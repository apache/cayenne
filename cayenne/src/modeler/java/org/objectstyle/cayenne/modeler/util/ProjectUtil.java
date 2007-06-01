/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.modeler.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.map.Attribute;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbJoin;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.MappingNamespace;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.map.ProcedureParameter;
import org.objectstyle.cayenne.map.Relationship;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.util.Util;

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

        Procedure procedure = parameter.getEntity();
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
        List nodes = new ArrayList();
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

    public static void setQueryName(DataMap map, Query query, String newName) {

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
        Iterator ents = map.getObjEntities().iterator();
        while (ents.hasNext()) {
            ObjEntity ent = (ObjEntity) ents.next();
            DbEntity dbEnt = ent.getDbEntity();

            // the whole entity mapping is invalid
            if (dbEnt != null && map.getDbEntity(dbEnt.getName()) != dbEnt) {
                clearDbMapping(ent);
                continue;
            }

            // check indiv. attributes
            Iterator atts = ent.getAttributes().iterator();
            while (atts.hasNext()) {
                ObjAttribute att = (ObjAttribute) atts.next();
                DbAttribute dbAtt = att.getDbAttribute();
                if (dbAtt != null) {
                    if (dbEnt.getAttribute(dbAtt.getName()) != dbAtt) {
                        att.setDbAttribute(null);
                    }
                }
            }

            // check indiv. relationships
            Iterator rels = ent.getRelationships().iterator();
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
                objAttr.setDbAttribute(null);
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