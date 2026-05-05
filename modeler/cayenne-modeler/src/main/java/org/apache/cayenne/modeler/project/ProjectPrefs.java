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

package org.apache.cayenne.modeler.project;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.modeler.event.display.DataMapDisplayEvent;
import org.apache.cayenne.modeler.event.display.DataNodeDisplayEvent;
import org.apache.cayenne.modeler.event.display.DbAttributeDisplayEvent;
import org.apache.cayenne.modeler.event.display.DbEntityDisplayEvent;
import org.apache.cayenne.modeler.event.display.DbRelationshipDisplayEvent;
import org.apache.cayenne.modeler.event.display.DomainDisplayEvent;
import org.apache.cayenne.modeler.event.display.EmbeddableAttributeDisplayEvent;
import org.apache.cayenne.modeler.event.display.EmbeddableDisplayEvent;
import org.apache.cayenne.modeler.event.display.MultipleObjectsDisplayEvent;
import org.apache.cayenne.modeler.event.display.ObjAttributeDisplayEvent;
import org.apache.cayenne.modeler.event.display.ObjEntityDisplayEvent;
import org.apache.cayenne.modeler.event.display.ObjRelationshipDisplayEvent;
import org.apache.cayenne.modeler.event.display.ProcedureDisplayEvent;
import org.apache.cayenne.modeler.event.display.ProcedureParameterDisplayEvent;
import org.apache.cayenne.modeler.event.display.QueryDisplayEvent;
import org.apache.cayenne.modeler.pref.PreferenceAdapter;
import org.apache.cayenne.modeler.pref.PreferencesRepository;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.util.CayenneMapEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Binding-style handler for the Modeler's last project selection. Loaded on project
 * open to restore selection, flushed on project close or Modeler close to persist it.
 */
public final class ProjectPrefs implements PreferenceAdapter {

    private static final String EVENT_KEY = "event";

    private static final String DOMAIN_KEY = "domain";
    private static final String NODE_KEY = "node";
    private static final String DATA_MAP_KEY = "dataMap";
    private static final String OBJ_ENTITY_KEY = "objEntity";
    private static final String DB_ENTITY_KEY = "dbEntity";
    private static final String EMBEDDABLE_KEY = "embeddable";
    private static final String EMBEDDABLE_ATTRS_KEY = "embAttrs";
    private static final String OBJ_ATTRS_KEY = "objAttrs";
    private static final String DB_ATTRS_KEY = "dbAttrs";
    private static final String OBJ_RELS_KEY = "objRels";
    private static final String DB_RELS_KEY = "dbRels";
    private static final String PROCEDURE_KEY = "procedure";
    private static final String PROCEDURE_PARAMS_KEY = "procedureParams";
    private static final String QUERY_KEY = "query";
    private static final String MULTIPLE_OBJECTS_KEY = "multipleObjects";
    private static final String PARENT_OBJECT_KEY = "parentObject";

    private enum Kind {
        domain, dataNode, dataMap,
        objEntity, dbEntity,
        objAttributes, dbAttributes,
        objRelationships, dbRelationships,
        embeddable, embeddableAttributes,
        procedure, procedureParameters,
        query, multipleObjects
    }

    public static ProjectPrefs of(PreferencesRepository repository, Project project) {
        return new ProjectPrefs(repository.projectPref(project, null));
    }

    private final Preferences prefs;

    private ProjectPrefs(Preferences prefs) {
        this.prefs = prefs;
    }

    public void load(ProjectSession session) {
        Kind kind = parseKind(prefs.get(EVENT_KEY, ""));
        if (kind == null) {
            return;
        }

        DataChannelDescriptor domain = (DataChannelDescriptor) session.project().getRootNode();
        if (!domain.getName().equals(prefs.get(DOMAIN_KEY, ""))) {
            return;
        }

        switch (kind) {
            case domain:
                session.displayDomain(new DomainDisplayEvent(ProjectPrefs.class, domain));
                break;
            case dataNode:
                restoreDataNode(session, domain);
                break;
            case dataMap:
                restoreDataMap(session, domain);
                break;
            case objEntity:
            case dbEntity:
                restoreEntity(session, domain);
                break;
            case objAttributes:
            case dbAttributes:
                restoreAttributes(session, domain);
                break;
            case objRelationships:
            case dbRelationships:
                restoreRelationships(session, domain);
                break;
            case embeddable:
                restoreEmbeddable(session, domain);
                break;
            case embeddableAttributes:
                restoreEmbeddableAttributes(session, domain);
                break;
            case procedure:
                restoreProcedure(session, domain);
                break;
            case procedureParameters:
                restoreProcedureParameters(session, domain);
                break;
            case query:
                restoreQuery(session, domain);
                break;
            case multipleObjects:
                restoreMultipleObjects(session, domain);
                break;
        }
    }

    public void flush(ProjectSession session) {
        if (prefs == null) {
            return;
        }
        try {
            prefs.clear();
        } catch (BackingStoreException e) {
            // ignore
        }

        Kind kind = leafKind(session);
        if (kind == null) {
            return;
        }
        prefs.put(EVENT_KEY, kind.name());

        switch (kind) {
            case domain:
                saveDomainPath(session);
                break;
            case dataNode:
                saveNodePath(session);
                break;
            case dataMap:
                saveDataMapPath(session);
                break;
            case objEntity:
            case dbEntity:
                saveEntityPath(session);
                break;
            case objAttributes:
                saveEntityPath(session);
                prefs.put(OBJ_ATTRS_KEY, joinNames(session.getSelectedObjAttributes()));
                break;
            case dbAttributes:
                saveEntityPath(session);
                prefs.put(DB_ATTRS_KEY, joinNames(session.getSelectedDbAttributes()));
                break;
            case objRelationships:
                saveEntityPath(session);
                prefs.put(OBJ_RELS_KEY, joinNames(session.getSelectedObjRelationships()));
                break;
            case dbRelationships:
                saveEntityPath(session);
                prefs.put(DB_RELS_KEY, joinNames(session.getSelectedDbRelationships()));
                break;
            case embeddable:
                saveEmbeddablePath(session);
                break;
            case embeddableAttributes:
                saveEmbeddablePath(session);
                prefs.put(EMBEDDABLE_ATTRS_KEY, joinEmbeddableAttributeNames(session.getSelectedEmbeddableAttributes()));
                break;
            case procedure:
                saveProcedurePath(session);
                break;
            case procedureParameters:
                saveProcedurePath(session);
                prefs.put(PROCEDURE_PARAMS_KEY, joinNames(session.getSelectedProcedureParameters()));
                break;
            case query:
                saveQueryPath(session);
                break;
            case multipleObjects:
                saveMultipleObjects(session);
                break;
        }
    }

    private static Kind leafKind(ProjectSession c) {
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

    private void saveDomainPath(ProjectSession session) {
        prefs.put(DOMAIN_KEY, session.getSelectedDataDomain().getName());
    }

    private void saveNodePath(ProjectSession session) {
        prefs.put(DOMAIN_KEY, session.getSelectedDataDomain().getName());
        prefs.put(NODE_KEY, session.getSelectedDataNode().getName());
    }

    private void saveDataMapPath(ProjectSession session) {
        DataChannelDescriptor domain = session.getSelectedDataDomain();
        if (domain == null) {
            return;
        }
        prefs.put(DOMAIN_KEY, domain.getName());
        DataNodeDescriptor node = session.getSelectedDataNode();
        prefs.put(NODE_KEY, node != null ? node.getName() : "");
        DataMap dataMap = session.getSelectedDataMap();
        if (dataMap != null) {
            prefs.put(DATA_MAP_KEY, dataMap.getName());
        }
    }

    private void saveEntityPath(ProjectSession session) {
        prefs.put(DOMAIN_KEY, session.getSelectedDataDomain().getName());
        DataNodeDescriptor node = session.getSelectedDataNode();
        prefs.put(NODE_KEY, node != null ? node.getName() : "");
        prefs.put(DATA_MAP_KEY, session.getSelectedDataMap().getName());

        ObjEntity objEntity = session.getSelectedObjEntity();
        DbEntity dbEntity = session.getSelectedDbEntity();
        if (objEntity != null) {
            prefs.put(OBJ_ENTITY_KEY, objEntity.getName());
        } else if (dbEntity != null) {
            prefs.put(DB_ENTITY_KEY, dbEntity.getName());
        }
    }

    private void saveEmbeddablePath(ProjectSession session) {
        prefs.put(DOMAIN_KEY, session.getSelectedDataDomain().getName());
        prefs.put(DATA_MAP_KEY, session.getSelectedDataMap().getName());
        prefs.put(EMBEDDABLE_KEY, session.getSelectedEmbeddable().getClassName());
    }

    private void saveProcedurePath(ProjectSession session) {
        prefs.put(DOMAIN_KEY, session.getSelectedDataDomain().getName());
        prefs.put(DATA_MAP_KEY, session.getSelectedDataMap().getName());
        prefs.put(PROCEDURE_KEY, session.getSelectedProcedure().getName());
    }

    private void saveQueryPath(ProjectSession session) {
        prefs.put(DOMAIN_KEY, session.getSelectedDataDomain().getName());
        prefs.put(DATA_MAP_KEY, session.getSelectedDataMap().getName());
        prefs.put(QUERY_KEY, session.getSelectedQuery().getName());
    }

    private void saveMultipleObjects(ProjectSession session) {
        prefs.put(PARENT_OBJECT_KEY, nameOf(session.getSelectedParentPath()));

        ConfigurationNode[] paths = session.getSelectedPaths();
        if (paths == null) {
            prefs.put(MULTIPLE_OBJECTS_KEY, "");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (ConfigurationNode object : paths) {
            String objectName = nameOf(object);
            if (!objectName.isEmpty()) {
                sb.append(objectName).append(",");
            }
        }
        prefs.put(MULTIPLE_OBJECTS_KEY, sb.toString());
    }

    // -------------------- restore helpers --------------------

    private void restoreDataNode(ProjectSession session, DataChannelDescriptor domain) {
        DataNodeDescriptor node = domain.getNodeDescriptor(prefs.get(NODE_KEY, ""));
        if (node == null) {
            return;
        }
        session.displayDataNode(new DataNodeDisplayEvent(ProjectPrefs.class, domain, node));
    }

    private void restoreDataMap(ProjectSession session, DataChannelDescriptor domain) {
        DataMap dataMap = domain.getDataMap(prefs.get(DATA_MAP_KEY, ""));
        if (dataMap == null) {
            return;
        }
        DataNodeDescriptor node = domain.getNodeDescriptor(prefs.get(NODE_KEY, ""));
        session.displayDataMap(new DataMapDisplayEvent(ProjectPrefs.class, domain, dataMap, node));
    }

    private void restoreEntity(ProjectSession session, DataChannelDescriptor domain) {
        DataMap dataMap = domain.getDataMap(prefs.get(DATA_MAP_KEY, ""));
        if (dataMap == null) {
            return;
        }
        Entity<?, ?, ?> entity = lookupEntity(dataMap);
        if (entity == null) {
            return;
        }
        if (entity instanceof ObjEntity) {
            session.displayObjEntity(new ObjEntityDisplayEvent(
                    ProjectPrefs.class, domain, dataMap, (ObjEntity) entity));
        } else if (entity instanceof DbEntity) {
            session.displayDbEntity(new DbEntityDisplayEvent(
                    ProjectPrefs.class, domain, dataMap, (DbEntity) entity));
        }
    }

    private void restoreAttributes(ProjectSession session, DataChannelDescriptor domain) {
        DataMap dataMap = domain.getDataMap(prefs.get(DATA_MAP_KEY, ""));
        if (dataMap == null) {
            return;
        }
        Entity<?, ?, ?> entity = lookupEntity(dataMap);
        if (entity == null) {
            return;
        }
        Attribute<?, ?, ?>[] attrs = lookupEntityAttributes(entity);

        if (entity instanceof ObjEntity) {
            ObjEntity objEntity = (ObjEntity) entity;
            ObjAttribute[] objAttrs = new ObjAttribute[attrs.length];
            System.arraycopy(attrs, 0, objAttrs, 0, attrs.length);
            session.displayObjEntity(new ObjEntityDisplayEvent(
                    ProjectPrefs.class, domain, dataMap, objEntity));
            session.displayObjAttribute(new ObjAttributeDisplayEvent(
                    ProjectPrefs.class, domain, dataMap, objEntity, objAttrs));
        } else if (entity instanceof DbEntity) {
            DbEntity dbEntity = (DbEntity) entity;
            DbAttribute[] dbAttrs = new DbAttribute[attrs.length];
            System.arraycopy(attrs, 0, dbAttrs, 0, attrs.length);
            session.displayDbEntity(new DbEntityDisplayEvent(
                    ProjectPrefs.class, domain, dataMap, dbEntity));
            session.displayDbAttribute(new DbAttributeDisplayEvent(
                    ProjectPrefs.class, domain, dataMap, dbEntity, dbAttrs));
        }
    }

    private void restoreRelationships(ProjectSession session, DataChannelDescriptor domain) {
        DataMap dataMap = domain.getDataMap(prefs.get(DATA_MAP_KEY, ""));
        if (dataMap == null) {
            return;
        }
        Entity<?, ?, ?> entity = lookupEntity(dataMap);
        if (entity == null) {
            return;
        }
        Relationship<?, ?, ?>[] rels = lookupEntityRelationships(entity);

        if (entity instanceof ObjEntity) {
            ObjEntity objEntity = (ObjEntity) entity;
            ObjRelationship[] objRels = new ObjRelationship[rels.length];
            System.arraycopy(rels, 0, objRels, 0, rels.length);
            session.displayObjEntity(new ObjEntityDisplayEvent(
                    ProjectPrefs.class, domain, dataMap, objEntity));
            session.displayObjRelationship(new ObjRelationshipDisplayEvent(
                    ProjectPrefs.class, domain, dataMap, objEntity, objRels));
        } else if (entity instanceof DbEntity) {
            DbEntity dbEntity = (DbEntity) entity;
            DbRelationship[] dbRels = new DbRelationship[rels.length];
            System.arraycopy(rels, 0, dbRels, 0, rels.length);
            session.displayDbEntity(new DbEntityDisplayEvent(
                    ProjectPrefs.class, domain, dataMap, dbEntity));
            session.displayDbRelationship(new DbRelationshipDisplayEvent(
                    ProjectPrefs.class, domain, dataMap, dbEntity, dbRels));
        }
    }

    private void restoreEmbeddable(ProjectSession session, DataChannelDescriptor domain) {
        DataMap dataMap = domain.getDataMap(prefs.get(DATA_MAP_KEY, ""));
        if (dataMap == null) {
            return;
        }
        Embeddable embeddable = dataMap.getEmbeddable(prefs.get(EMBEDDABLE_KEY, ""));
        if (embeddable == null) {
            return;
        }
        session.displayEmbeddable(new EmbeddableDisplayEvent(ProjectPrefs.class, domain, dataMap, embeddable));
    }

    private void restoreEmbeddableAttributes(ProjectSession session, DataChannelDescriptor domain) {
        DataMap dataMap = domain.getDataMap(prefs.get(DATA_MAP_KEY, ""));
        if (dataMap == null) {
            return;
        }
        Embeddable embeddable = dataMap.getEmbeddable(prefs.get(EMBEDDABLE_KEY, ""));
        if (embeddable == null) {
            return;
        }
        session.displayEmbeddable(new EmbeddableDisplayEvent(ProjectPrefs.class, domain, dataMap, embeddable));

        EmbeddableAttribute[] attrs = lookupEmbeddableAttributes(embeddable);
        session.displayEmbeddableAttribute(new EmbeddableAttributeDisplayEvent(
                ProjectPrefs.class, domain, dataMap, embeddable, attrs));
    }

    private void restoreProcedure(ProjectSession session, DataChannelDescriptor domain) {
        DataMap dataMap = domain.getDataMap(prefs.get(DATA_MAP_KEY, ""));
        if (dataMap == null) {
            return;
        }
        Procedure procedure = dataMap.getProcedure(prefs.get(PROCEDURE_KEY, ""));
        if (procedure == null) {
            return;
        }
        session.displayProcedure(new ProcedureDisplayEvent(ProjectPrefs.class, domain, dataMap, procedure));
    }

    private void restoreProcedureParameters(ProjectSession session, DataChannelDescriptor domain) {
        DataMap dataMap = domain.getDataMap(prefs.get(DATA_MAP_KEY, ""));
        if (dataMap == null) {
            return;
        }
        Procedure procedure = dataMap.getProcedure(prefs.get(PROCEDURE_KEY, ""));
        if (procedure == null) {
            return;
        }
        session.displayProcedure(new ProcedureDisplayEvent(ProjectPrefs.class, domain, dataMap, procedure));

        ProcedureParameter[] params = lookupProcedureParameters(procedure);
        session.displayProcedureParameter(new ProcedureParameterDisplayEvent(
                ProjectPrefs.class, domain, dataMap, procedure, params));
    }

    private void restoreQuery(ProjectSession session, DataChannelDescriptor domain) {
        DataMap dataMap = domain.getDataMap(prefs.get(DATA_MAP_KEY, ""));
        if (dataMap == null) {
            return;
        }
        QueryDescriptor query = dataMap.getQueryDescriptor(prefs.get(QUERY_KEY, ""));
        if (query == null) {
            return;
        }
        session.displayQuery(new QueryDisplayEvent(ProjectPrefs.class, domain, dataMap, query));
    }

    private void restoreMultipleObjects(ProjectSession session, DataChannelDescriptor domain) {
        String parentName = prefs.get(PARENT_OBJECT_KEY, "");
        ConfigurationNode parent;
        ConfigurationNode[] objects;

        DataMap parentMap = domain.getDataMap(parentName);
        DataNodeDescriptor parentNode = domain.getNodeDescriptor(parentName);

        if (parentMap != null) {
            parent = parentMap;
            objects = lookupMultipleObjects(parentMap);
        } else if (parentNode != null) {
            parent = parentNode;
            objects = lookupMultipleObjects(parentNode);
        } else {
            parent = domain;
            objects = lookupMultipleObjects(domain);
        }

        session.displayMultipleObjects(new MultipleObjectsDisplayEvent(ProjectPrefs.class, parent, objects));
    }

    // -------------------- name lookups --------------------

    private Entity<?, ?, ?> lookupEntity(DataMap dataMap) {
        String objName = prefs.get(OBJ_ENTITY_KEY, "");
        return !objName.isEmpty()
                ? dataMap.getObjEntity(objName)
                : dataMap.getDbEntity(prefs.get(DB_ENTITY_KEY, ""));
    }

    private Attribute<?, ?, ?>[] lookupEntityAttributes(Entity<?, ?, ?> entity) {
        String stored = (entity instanceof ObjEntity)
                ? prefs.get(OBJ_ATTRS_KEY, "")
                : prefs.get(DB_ATTRS_KEY, "");
        List<Attribute<?, ?, ?>> result = new ArrayList<>();
        for (String name : stored.split(",")) {
            Attribute<?, ?, ?> attr = entity.getAttribute(name);
            if (attr != null) {
                result.add(attr);
            }
        }
        return result.toArray(new Attribute[0]);
    }

    private Relationship<?, ?, ?>[] lookupEntityRelationships(Entity<?, ?, ?> entity) {
        String stored = (entity instanceof ObjEntity)
                ? prefs.get(OBJ_RELS_KEY, "")
                : prefs.get(DB_RELS_KEY, "");
        List<Relationship<?, ?, ?>> result = new ArrayList<>();
        for (String name : stored.split(",")) {
            Relationship<?, ?, ?> rel = entity.getRelationship(name);
            if (rel != null) {
                result.add(rel);
            }
        }
        return result.toArray(new Relationship[0]);
    }

    private EmbeddableAttribute[] lookupEmbeddableAttributes(Embeddable embeddable) {
        String stored = prefs.get(EMBEDDABLE_ATTRS_KEY, "");
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

    private ProcedureParameter[] lookupProcedureParameters(Procedure procedure) {
        String stored = prefs.get(PROCEDURE_PARAMS_KEY, "");
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

    private ConfigurationNode[] lookupMultipleObjects(DataChannelDescriptor domain) {
        String stored = prefs.get(MULTIPLE_OBJECTS_KEY, "");
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

    private ConfigurationNode[] lookupMultipleObjects(DataNodeDescriptor node) {
        String stored = prefs.get(MULTIPLE_OBJECTS_KEY, "");
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

    private ConfigurationNode[] lookupMultipleObjects(DataMap dataMap) {
        String stored = prefs.get(MULTIPLE_OBJECTS_KEY, "");
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
