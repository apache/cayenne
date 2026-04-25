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

package org.apache.cayenne.modeler.pref;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.modeler.event.display.AttributeDisplayEvent;
import org.apache.cayenne.modeler.event.display.DataMapDisplayEvent;
import org.apache.cayenne.modeler.event.display.DataNodeDisplayEvent;
import org.apache.cayenne.modeler.event.display.DomainDisplayEvent;
import org.apache.cayenne.modeler.event.display.EmbeddableAttributeDisplayEvent;
import org.apache.cayenne.modeler.event.display.EmbeddableDisplayEvent;
import org.apache.cayenne.modeler.event.display.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.display.MultipleObjectsDisplayEvent;
import org.apache.cayenne.modeler.event.display.ProcedureDisplayEvent;
import org.apache.cayenne.modeler.event.display.ProcedureParameterDisplayEvent;
import org.apache.cayenne.modeler.event.display.QueryDisplayEvent;
import org.apache.cayenne.modeler.event.display.RelationshipDisplayEvent;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.util.CayenneMapEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Saves and restores the modeler's last project selection through {@link ProjectStatePreferences}.
 * Replaces the parallel {@code DisplayEventType} hierarchy by deriving the selection kind directly
 * from the controller state and dispatching to the right {@code controller.display*()} method.
 */
final class ProjectSelectionMemo {

    enum Kind {
        domain, dataNode, dataMap,
        objEntity, dbEntity,
        objAttributes, dbAttributes,
        objRelationships, dbRelationships,
        embeddable, embeddableAttributes,
        procedure, procedureParameters,
        query, multipleObjects
    }

    private ProjectSelectionMemo() {
    }

    static void save(ProjectController controller, ProjectStatePreferences prefs) {
        Kind kind = leafKind(controller);
        if (kind == null) {
            return;
        }

        prefs.setEvent(kind.name());

        switch (kind) {
            case domain:
                saveDomainPath(controller, prefs);
                break;
            case dataNode:
                saveNodePath(controller, prefs);
                break;
            case dataMap:
                saveDataMapPath(controller, prefs);
                break;
            case objEntity:
            case dbEntity:
                saveEntityPath(controller, prefs);
                break;
            case objAttributes:
                saveEntityPath(controller, prefs);
                prefs.setObjAttrs(joinNames(controller.getSelectedObjAttributes()));
                break;
            case dbAttributes:
                saveEntityPath(controller, prefs);
                prefs.setDbAttrs(joinNames(controller.getSelectedDbAttributes()));
                break;
            case objRelationships:
                saveEntityPath(controller, prefs);
                prefs.setObjRels(joinNames(controller.getSelectedObjRelationships()));
                break;
            case dbRelationships:
                saveEntityPath(controller, prefs);
                prefs.setDbRels(joinNames(controller.getSelectedDbRelationships()));
                break;
            case embeddable:
                saveEmbeddablePath(controller, prefs);
                break;
            case embeddableAttributes:
                saveEmbeddablePath(controller, prefs);
                prefs.setEmbAttrs(joinEmbeddableAttributeNames(controller.getSelectedEmbeddableAttributes()));
                break;
            case procedure:
                saveProcedurePath(controller, prefs);
                break;
            case procedureParameters:
                saveProcedurePath(controller, prefs);
                prefs.setProcedureParams(joinNames(controller.getSelectedProcedureParameters()));
                break;
            case query:
                saveQueryPath(controller, prefs);
                break;
            case multipleObjects:
                saveMultipleObjects(controller, prefs);
                break;
        }
    }

    static void restore(ProjectController controller, ProjectStatePreferences prefs) {
        Kind kind = parseKind(prefs.getEvent());
        if (kind == null) {
            return;
        }

        DataChannelDescriptor domain = (DataChannelDescriptor) controller.getProject().getRootNode();
        if (!domain.getName().equals(prefs.getDomain())) {
            return;
        }

        switch (kind) {
            case domain:
                controller.displayDomain(new DomainDisplayEvent(ProjectSelectionMemo.class, domain));
                break;
            case dataNode:
                restoreDataNode(controller, domain, prefs);
                break;
            case dataMap:
                restoreDataMap(controller, domain, prefs);
                break;
            case objEntity:
            case dbEntity:
                restoreEntity(controller, domain, prefs);
                break;
            case objAttributes:
            case dbAttributes:
                restoreAttributes(controller, domain, prefs);
                break;
            case objRelationships:
            case dbRelationships:
                restoreRelationships(controller, domain, prefs);
                break;
            case embeddable:
                restoreEmbeddable(controller, domain, prefs);
                break;
            case embeddableAttributes:
                restoreEmbeddableAttributes(controller, domain, prefs);
                break;
            case procedure:
                restoreProcedure(controller, domain, prefs);
                break;
            case procedureParameters:
                restoreProcedureParameters(controller, domain, prefs);
                break;
            case query:
                restoreQuery(controller, domain, prefs);
                break;
            case multipleObjects:
                restoreMultipleObjects(controller, domain, prefs);
                break;
        }
    }

    private static Kind leafKind(ProjectController c) {
        if (c.getSelectedObjAttributes() != null && c.getSelectedObjAttributes().length > 0) {
            return Kind.objAttributes;
        }
        if (c.getSelectedDbAttributes() != null && c.getSelectedDbAttributes().length > 0) {
            return Kind.dbAttributes;
        }
        if (c.getSelectedObjRelationships() != null && c.getSelectedObjRelationships().length > 0) {
            return Kind.objRelationships;
        }
        if (c.getSelectedDbRelationships() != null && c.getSelectedDbRelationships().length > 0) {
            return Kind.dbRelationships;
        }
        if (c.getSelectedEmbeddableAttributes() != null && c.getSelectedEmbeddableAttributes().length > 0) {
            return Kind.embeddableAttributes;
        }
        if (c.getSelectedProcedureParameters() != null && c.getSelectedProcedureParameters().length > 0) {
            return Kind.procedureParameters;
        }
        if (c.getSelectedObjEntity() != null) {
            return Kind.objEntity;
        }
        if (c.getSelectedDbEntity() != null) {
            return Kind.dbEntity;
        }
        if (c.getSelectedEmbeddable() != null) {
            return Kind.embeddable;
        }
        if (c.getSelectedProcedure() != null) {
            return Kind.procedure;
        }
        if (c.getSelectedQuery() != null) {
            return Kind.query;
        }
        if (c.getSelectedDataMap() != null) {
            return Kind.dataMap;
        }
        if (c.getSelectedDataNode() != null) {
            return Kind.dataNode;
        }
        if (c.getSelectedPaths() != null && c.getSelectedPaths().length > 0) {
            return Kind.multipleObjects;
        }
        if (c.getSelectedDataDomain() != null) {
            return Kind.domain;
        }
        return null;
    }

    private static Kind parseKind(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        try {
            return Kind.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // -------------------- save helpers --------------------

    private static void saveDomainPath(ProjectController controller, ProjectStatePreferences prefs) {
        prefs.setDomain(controller.getSelectedDataDomain().getName());
    }

    private static void saveNodePath(ProjectController controller, ProjectStatePreferences prefs) {
        prefs.setDomain(controller.getSelectedDataDomain().getName());
        prefs.setNode(controller.getSelectedDataNode().getName());
    }

    private static void saveDataMapPath(ProjectController controller, ProjectStatePreferences prefs) {
        DataChannelDescriptor domain = controller.getSelectedDataDomain();
        if (domain == null) {
            return;
        }
        prefs.setDomain(domain.getName());
        DataNodeDescriptor node = controller.getSelectedDataNode();
        prefs.setNode(node != null ? node.getName() : "");
        DataMap dataMap = controller.getSelectedDataMap();
        if (dataMap != null) {
            prefs.setDataMap(dataMap.getName());
        }
    }

    private static void saveEntityPath(ProjectController controller, ProjectStatePreferences prefs) {
        prefs.setDomain(controller.getSelectedDataDomain().getName());
        DataNodeDescriptor node = controller.getSelectedDataNode();
        prefs.setNode(node != null ? node.getName() : "");
        prefs.setDataMap(controller.getSelectedDataMap().getName());

        ObjEntity objEntity = controller.getSelectedObjEntity();
        DbEntity dbEntity = controller.getSelectedDbEntity();
        if (objEntity != null) {
            prefs.setObjEntity(objEntity.getName());
            prefs.setDbEntity(null);
        } else if (dbEntity != null) {
            prefs.setDbEntity(dbEntity.getName());
            prefs.setObjEntity(null);
        }
    }

    private static void saveEmbeddablePath(ProjectController controller, ProjectStatePreferences prefs) {
        prefs.setDomain(controller.getSelectedDataDomain().getName());
        prefs.setDataMap(controller.getSelectedDataMap().getName());
        prefs.setEmbeddable(controller.getSelectedEmbeddable().getClassName());
    }

    private static void saveProcedurePath(ProjectController controller, ProjectStatePreferences prefs) {
        prefs.setDomain(controller.getSelectedDataDomain().getName());
        prefs.setDataMap(controller.getSelectedDataMap().getName());
        prefs.setProcedure(controller.getSelectedProcedure().getName());
    }

    private static void saveQueryPath(ProjectController controller, ProjectStatePreferences prefs) {
        prefs.setDomain(controller.getSelectedDataDomain().getName());
        prefs.setDataMap(controller.getSelectedDataMap().getName());
        prefs.setQuery(controller.getSelectedQuery().getName());
    }

    private static void saveMultipleObjects(ProjectController controller, ProjectStatePreferences prefs) {
        prefs.setParentObject(nameOf(controller.getSelectedParentPath()));

        ConfigurationNode[] paths = controller.getSelectedPaths();
        if (paths == null) {
            prefs.setMultipleObjects("");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (ConfigurationNode object : paths) {
            String objectName = nameOf(object);
            if (!objectName.isEmpty()) {
                sb.append(objectName).append(",");
            }
        }
        prefs.setMultipleObjects(sb.toString());
    }

    // -------------------- restore helpers --------------------

    private static void restoreDataNode(ProjectController controller, DataChannelDescriptor domain,
                                        ProjectStatePreferences prefs) {
        DataNodeDescriptor node = domain.getNodeDescriptor(prefs.getNode());
        if (node == null) {
            return;
        }
        controller.displayDataNode(new DataNodeDisplayEvent(ProjectSelectionMemo.class, domain, node));
    }

    private static void restoreDataMap(ProjectController controller, DataChannelDescriptor domain,
                                       ProjectStatePreferences prefs) {
        DataMap dataMap = domain.getDataMap(prefs.getDataMap());
        if (dataMap == null) {
            return;
        }
        DataNodeDescriptor node = domain.getNodeDescriptor(prefs.getNode());
        controller.displayDataMap(new DataMapDisplayEvent(ProjectSelectionMemo.class, dataMap, domain, node));
    }

    private static void restoreEntity(ProjectController controller, DataChannelDescriptor domain,
                                      ProjectStatePreferences prefs) {
        DataMap dataMap = domain.getDataMap(prefs.getDataMap());
        if (dataMap == null) {
            return;
        }
        Entity<?, ?, ?> entity = lookupEntity(dataMap, prefs);
        if (entity == null) {
            return;
        }
        DataNodeDescriptor node = domain.getNodeDescriptor(prefs.getNode());
        EntityDisplayEvent event = new EntityDisplayEvent(ProjectSelectionMemo.class, entity, dataMap, node, domain);
        if (entity instanceof ObjEntity) {
            controller.displayObjEntity(event);
        } else if (entity instanceof DbEntity) {
            controller.displayDbEntity(event);
        }
    }

    private static void restoreAttributes(ProjectController controller, DataChannelDescriptor domain,
                                          ProjectStatePreferences prefs) {
        DataMap dataMap = domain.getDataMap(prefs.getDataMap());
        if (dataMap == null) {
            return;
        }
        Entity<?, ?, ?> entity = lookupEntity(dataMap, prefs);
        if (entity == null) {
            return;
        }
        DataNodeDescriptor node = domain.getNodeDescriptor(prefs.getNode());

        EntityDisplayEvent entityEvent = new EntityDisplayEvent(ProjectSelectionMemo.class, entity, dataMap, node, domain);
        Attribute<?, ?, ?>[] attrs = lookupEntityAttributes(entity, prefs);
        AttributeDisplayEvent attrEvent = new AttributeDisplayEvent(ProjectSelectionMemo.class, attrs, entity, dataMap, domain);

        if (entity instanceof ObjEntity) {
            controller.displayObjEntity(entityEvent);
            controller.displayObjAttribute(attrEvent);
        } else if (entity instanceof DbEntity) {
            controller.displayDbEntity(entityEvent);
            controller.displayDbAttribute(attrEvent);
        }
    }

    private static void restoreRelationships(ProjectController controller, DataChannelDescriptor domain,
                                             ProjectStatePreferences prefs) {
        DataMap dataMap = domain.getDataMap(prefs.getDataMap());
        if (dataMap == null) {
            return;
        }
        Entity<?, ?, ?> entity = lookupEntity(dataMap, prefs);
        if (entity == null) {
            return;
        }
        DataNodeDescriptor node = domain.getNodeDescriptor(prefs.getNode());

        EntityDisplayEvent entityEvent = new EntityDisplayEvent(ProjectSelectionMemo.class, entity, dataMap, node, domain);
        Relationship<?, ?, ?>[] rels = lookupEntityRelationships(entity, prefs);
        RelationshipDisplayEvent relEvent = new RelationshipDisplayEvent(ProjectSelectionMemo.class, rels, entity, dataMap, domain);

        if (entity instanceof ObjEntity) {
            controller.displayObjEntity(entityEvent);
            controller.displayObjRelationship(relEvent);
        } else if (entity instanceof DbEntity) {
            controller.displayDbEntity(entityEvent);
            controller.displayDbRelationship(relEvent);
        }
    }

    private static void restoreEmbeddable(ProjectController controller, DataChannelDescriptor domain,
                                          ProjectStatePreferences prefs) {
        DataMap dataMap = domain.getDataMap(prefs.getDataMap());
        if (dataMap == null) {
            return;
        }
        Embeddable embeddable = dataMap.getEmbeddable(prefs.getEmbeddable());
        if (embeddable == null) {
            return;
        }
        controller.displayEmbeddable(new EmbeddableDisplayEvent(ProjectSelectionMemo.class, embeddable, dataMap, domain));
    }

    private static void restoreEmbeddableAttributes(ProjectController controller, DataChannelDescriptor domain,
                                                    ProjectStatePreferences prefs) {
        DataMap dataMap = domain.getDataMap(prefs.getDataMap());
        if (dataMap == null) {
            return;
        }
        Embeddable embeddable = dataMap.getEmbeddable(prefs.getEmbeddable());
        if (embeddable == null) {
            return;
        }
        controller.displayEmbeddable(new EmbeddableDisplayEvent(ProjectSelectionMemo.class, embeddable, dataMap, domain));

        EmbeddableAttribute[] attrs = lookupEmbeddableAttributes(embeddable, prefs);
        controller.displayEmbeddableAttribute(new EmbeddableAttributeDisplayEvent(
                ProjectSelectionMemo.class, embeddable, attrs, dataMap, domain));
    }

    private static void restoreProcedure(ProjectController controller, DataChannelDescriptor domain,
                                         ProjectStatePreferences prefs) {
        DataMap dataMap = domain.getDataMap(prefs.getDataMap());
        if (dataMap == null) {
            return;
        }
        Procedure procedure = dataMap.getProcedure(prefs.getProcedure());
        if (procedure == null) {
            return;
        }
        controller.displayProcedure(new ProcedureDisplayEvent(ProjectSelectionMemo.class, procedure, dataMap, domain));
    }

    private static void restoreProcedureParameters(ProjectController controller, DataChannelDescriptor domain,
                                                   ProjectStatePreferences prefs) {
        DataMap dataMap = domain.getDataMap(prefs.getDataMap());
        if (dataMap == null) {
            return;
        }
        Procedure procedure = dataMap.getProcedure(prefs.getProcedure());
        if (procedure == null) {
            return;
        }
        controller.displayProcedure(new ProcedureDisplayEvent(ProjectSelectionMemo.class, procedure, dataMap, domain));

        ProcedureParameter[] params = lookupProcedureParameters(procedure, prefs);
        controller.displayProcedureParameter(new ProcedureParameterDisplayEvent(
                ProjectSelectionMemo.class, params, procedure, dataMap, domain));
    }

    private static void restoreQuery(ProjectController controller, DataChannelDescriptor domain,
                                     ProjectStatePreferences prefs) {
        DataMap dataMap = domain.getDataMap(prefs.getDataMap());
        if (dataMap == null) {
            return;
        }
        QueryDescriptor query = dataMap.getQueryDescriptor(prefs.getQuery());
        if (query == null) {
            return;
        }
        controller.displayQuery(new QueryDisplayEvent(ProjectSelectionMemo.class, query, dataMap, domain));
    }

    private static void restoreMultipleObjects(ProjectController controller, DataChannelDescriptor domain,
                                               ProjectStatePreferences prefs) {
        String parentName = prefs.getParentObject();
        ConfigurationNode parent;
        ConfigurationNode[] objects;

        DataMap parentMap = domain.getDataMap(parentName);
        DataNodeDescriptor parentNode = domain.getNodeDescriptor(parentName);

        if (parentMap != null) {
            parent = parentMap;
            objects = lookupMultipleObjects(parentMap, prefs);
        } else if (parentNode != null) {
            parent = parentNode;
            objects = lookupMultipleObjects(parentNode, prefs);
        } else {
            parent = domain;
            objects = lookupMultipleObjects(domain, prefs);
        }

        controller.displayMultipleObjects(new MultipleObjectsDisplayEvent(ProjectSelectionMemo.class, objects, parent));
    }

    // -------------------- name lookups --------------------

    private static Entity<?, ?, ?> lookupEntity(DataMap dataMap, ProjectStatePreferences prefs) {
        return !prefs.getObjEntity().isEmpty()
                ? dataMap.getObjEntity(prefs.getObjEntity())
                : dataMap.getDbEntity(prefs.getDbEntity());
    }

    private static Attribute<?, ?, ?>[] lookupEntityAttributes(Entity<?, ?, ?> entity, ProjectStatePreferences prefs) {
        String stored = (entity instanceof ObjEntity) ? prefs.getObjAttrs() : prefs.getDbAttrs();
        List<Attribute<?, ?, ?>> result = new ArrayList<>();
        for (String name : stored.split(",")) {
            Attribute<?, ?, ?> attr = entity.getAttribute(name);
            if (attr != null) {
                result.add(attr);
            }
        }
        return result.toArray(new Attribute[0]);
    }

    private static Relationship<?, ?, ?>[] lookupEntityRelationships(Entity<?, ?, ?> entity, ProjectStatePreferences prefs) {
        String stored = (entity instanceof ObjEntity) ? prefs.getObjRels() : prefs.getDbRels();
        List<Relationship<?, ?, ?>> result = new ArrayList<>();
        for (String name : stored.split(",")) {
            Relationship<?, ?, ?> rel = entity.getRelationship(name);
            if (rel != null) {
                result.add(rel);
            }
        }
        return result.toArray(new Relationship[0]);
    }

    private static EmbeddableAttribute[] lookupEmbeddableAttributes(Embeddable embeddable, ProjectStatePreferences prefs) {
        String stored = prefs.getEmbAttrs();
        List<EmbeddableAttribute> result = new ArrayList<>();
        if (stored.isEmpty()) {
            return new EmbeddableAttribute[0];
        }
        for (String name : stored.split(",")) {
            EmbeddableAttribute attr = embeddable.getAttribute(name);
            if (attr != null) {
                result.add(attr);
            }
        }
        return result.toArray(new EmbeddableAttribute[0]);
    }

    private static ProcedureParameter[] lookupProcedureParameters(Procedure procedure, ProjectStatePreferences prefs) {
        String stored = prefs.getProcedureParams();
        List<ProcedureParameter> result = new ArrayList<>();
        if (stored.isEmpty()) {
            return new ProcedureParameter[0];
        }
        for (String name : stored.split(",")) {
            for (ProcedureParameter p : procedure.getCallParameters()) {
                if (p.getName().equals(name)) {
                    result.add(p);
                }
            }
        }
        return result.toArray(new ProcedureParameter[0]);
    }

    private static ConfigurationNode[] lookupMultipleObjects(DataChannelDescriptor domain, ProjectStatePreferences prefs) {
        String stored = prefs.getMultipleObjects();
        if (stored.isEmpty()) {
            return new ConfigurationNode[0];
        }
        List<ConfigurationNode> result = new ArrayList<>();
        for (String name : stored.split(",")) {
            ConfigurationNode node = findInDomain(domain, name);
            if (node != null) {
                result.add(node);
            }
        }
        return result.toArray(new ConfigurationNode[0]);
    }

    private static ConfigurationNode[] lookupMultipleObjects(DataNodeDescriptor node, ProjectStatePreferences prefs) {
        String stored = prefs.getMultipleObjects();
        if (stored.isEmpty()) {
            return new ConfigurationNode[0];
        }
        List<ConfigurationNode> result = new ArrayList<>();
        for (String name : stored.split(",")) {
            if (node.getDataMapNames().contains(name)) {
                result.add(node.getDataChannelDescriptor().getDataMap(name));
            }
        }
        return result.toArray(new ConfigurationNode[0]);
    }

    private static ConfigurationNode[] lookupMultipleObjects(DataMap dataMap, ProjectStatePreferences prefs) {
        String stored = prefs.getMultipleObjects();
        if (stored.isEmpty()) {
            return new ConfigurationNode[0];
        }
        List<ConfigurationNode> result = new ArrayList<>();
        for (String name : stored.split(",")) {
            if (dataMap.getObjEntity(name) != null) {
                result.add(dataMap.getObjEntity(name));
            } else if (dataMap.getDbEntity(name) != null) {
                result.add(dataMap.getDbEntity(name));
            } else if (dataMap.getEmbeddable(name) != null) {
                result.add(dataMap.getEmbeddable(name));
            } else if (dataMap.getProcedure(name) != null) {
                result.add(dataMap.getProcedure(name));
            } else if (dataMap.getQueryDescriptor(name) != null) {
                result.add(dataMap.getQueryDescriptor(name));
            }
        }
        return result.toArray(new ConfigurationNode[0]);
    }

    private static ConfigurationNode findInDomain(DataChannelDescriptor domain, String name) {
        if (domain.getName().equals(name)) {
            return domain;
        }
        for (DataNodeDescriptor n : domain.getNodeDescriptors()) {
            if (n.getName().equals(name)) {
                return n;
            }
        }
        for (DataMap m : domain.getDataMaps()) {
            if (m.getName().equals(name)) {
                return m;
            }
        }
        return null;
    }

    // -------------------- name formatting --------------------

    private static String joinNames(CayenneMapEntry[] entries) {
        if (entries == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (CayenneMapEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            sb.append(entry.getName()).append(",");
        }
        return sb.toString();
    }

    private static String joinEmbeddableAttributeNames(EmbeddableAttribute[] entries) {
        if (entries == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (EmbeddableAttribute entry : entries) {
            if (entry == null) {
                continue;
            }
            sb.append(entry.getName()).append(",");
        }
        return sb.toString();
    }

    private static String nameOf(ConfigurationNode object) {
        if (object instanceof CayenneMapEntry) {
            return ((CayenneMapEntry) object).getName();
        } else if (object instanceof DataChannelDescriptor) {
            return ((DataChannelDescriptor) object).getName();
        } else if (object instanceof DataNodeDescriptor) {
            return ((DataNodeDescriptor) object).getName();
        } else if (object instanceof DataMap) {
            return ((DataMap) object).getName();
        } else if (object instanceof Embeddable) {
            return ((Embeddable) object).getClassName();
        } else if (object instanceof QueryDescriptor) {
            return ((QueryDescriptor) object).getName();
        }
        return "";
    }
}
