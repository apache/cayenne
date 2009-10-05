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
package org.apache.cayenne.modeler.undo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.map.event.DataNodeEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.CreateDataMapAction;
import org.apache.cayenne.modeler.action.CreateDbEntityAction;
import org.apache.cayenne.modeler.action.CreateDomainAction;
import org.apache.cayenne.modeler.action.CreateEmbeddableAction;
import org.apache.cayenne.modeler.action.CreateNodeAction;
import org.apache.cayenne.modeler.action.CreateObjEntityAction;
import org.apache.cayenne.modeler.action.CreateProcedureAction;
import org.apache.cayenne.modeler.action.CreateQueryAction;
import org.apache.cayenne.modeler.action.CreateRelationshipAction;
import org.apache.cayenne.modeler.action.RemoveAction;
import org.apache.cayenne.query.Query;

public class RemoveUndoableEdit extends CayenneUndoableEdit {

    

    private DataMap map;
    private DbEntity dbEntity;
    private ObjEntity objEntity;
    private Query query;
    private Procedure procedure;

    private DataNode dataNode;
    private DataDomain domain;

    private Embeddable embeddable;

    private Map<DbEntity, List<DbRelationship>> dbRelationshipMap = new HashMap<DbEntity, List<DbRelationship>>();
    private Map<ObjEntity, List<ObjRelationship>> objRelationshipMap = new HashMap<ObjEntity, List<ObjRelationship>>();

    private static enum REMOVE_MODE {
        OBJECT_ENTITY, DB_ENTITY, QUERY, PROCEDURE, MAP_FROM_NODE, MAP_FROM_DOMAIN, NODE, DOMAIN, EMBEDDABLE
    };

    private REMOVE_MODE mode;

    public RemoveUndoableEdit(Application application, DataDomain domain) {
        this.domain = domain;
        this.mode = REMOVE_MODE.DOMAIN;
    }

    public RemoveUndoableEdit(Application application, DataNode node, DataMap map) {
        this.map = map;
        this.dataNode = node;
        this.mode = REMOVE_MODE.MAP_FROM_NODE;
    }

    public RemoveUndoableEdit(Application application, DataDomain domain, DataMap map) {
        this.domain = domain;
        this.map = map;
        this.mode = REMOVE_MODE.MAP_FROM_DOMAIN;
    }

    public RemoveUndoableEdit(Application application, DataDomain domain, DataNode node) {
        this.domain = domain;
        this.dataNode = node;
        this.mode = REMOVE_MODE.NODE;
    }

    public RemoveUndoableEdit(DataMap map, ObjEntity objEntity) {
        this.map = map;
        this.objEntity = objEntity;
        this.mode = REMOVE_MODE.OBJECT_ENTITY;

        for (ObjEntity ent : map.getObjEntities()) {
            // take a copy since we're going to modify the entity
            for (Relationship relationship : new ArrayList<Relationship>(ent
                    .getRelationships())) {

                if (this.objEntity.getName().equals(relationship.getTargetEntityName())) {

                    ObjRelationship rel = (ObjRelationship) relationship;

                    if (objRelationshipMap.get(rel.getSourceEntity()) == null) {
                        objRelationshipMap.put(
                                (ObjEntity) rel.getSourceEntity(),
                                new LinkedList<ObjRelationship>());
                    }

                    objRelationshipMap.get(rel.getSourceEntity()).add(rel);
                }
            }
        }
    }

    public RemoveUndoableEdit(DataMap map, DbEntity dbEntity) {
        this.map = map;
        this.dbEntity = dbEntity;
        this.mode = REMOVE_MODE.DB_ENTITY;

        for (ObjEntity objEnt : map.getObjEntities()) {
            for (Relationship rel : objEnt.getRelationships()) {
                for (DbRelationship dbRel : ((ObjRelationship) rel).getDbRelationships()) {
                    if (dbRel.getTargetEntity() == dbEntity) {

                        if (dbRelationshipMap.get(dbRel.getSourceEntity()) == null) {
                            dbRelationshipMap.put(
                                    (DbEntity) dbRel.getSourceEntity(),
                                    new LinkedList<DbRelationship>());
                        }
                        dbRelationshipMap.get(dbRel.getSourceEntity()).add(dbRel);

                        break;
                    }
                }
            }
        }
    }

    public RemoveUndoableEdit(DataMap map, Query query) {
        this.map = map;
        this.query = query;
        this.mode = REMOVE_MODE.QUERY;
    }

    public RemoveUndoableEdit(DataMap map, Procedure procedure) {
        this.map = map;
        this.procedure = procedure;
        this.mode = REMOVE_MODE.PROCEDURE;
    }

    public RemoveUndoableEdit(DataMap map, Embeddable embeddable) {
        this.map = map;
        this.embeddable = embeddable;
        this.mode = REMOVE_MODE.EMBEDDABLE;
    }

    @Override
    public String getPresentationName() {
        switch (this.mode) {
            case OBJECT_ENTITY:
                return "Remove Object Entity";
            case DB_ENTITY:
                return "Remove Db Entity";
            case QUERY:
                return "Remove Query";
            case PROCEDURE:
                return "Remove Procedure";
            case MAP_FROM_NODE:
                return "Remove DataMap";
            case MAP_FROM_DOMAIN:
                return "Remove DataMap";
            case NODE:
                return "Remove DataNode";
            case DOMAIN:
                return "Remove DataDomain";
            case EMBEDDABLE:
                return "Remove Embeddable";
            default:
                return "Remove";

        }
    }

    @Override
    public void redo() throws CannotRedoException {
        RemoveAction action = (RemoveAction) actionManager.getAction(RemoveAction
                .getActionName());

        switch (this.mode) {
            case OBJECT_ENTITY:
                action.removeObjEntity(map, objEntity);
                break;
            case DB_ENTITY:
                action.removeDbEntity(map, dbEntity);
                break;
            case QUERY:
                action.removeQuery(map, query);
                break;
            case PROCEDURE:
                action.removeProcedure(map, procedure);
            case MAP_FROM_NODE:
                action.removeDataMapFromDataNode(dataNode, map);
                break;
            case MAP_FROM_DOMAIN:
                action.removeDataMap(domain, map);
                break;
            case NODE:
                action.removeDataNode(domain, dataNode);
                break;
            case DOMAIN:
                action.removeDomain(domain);
                break;
            case EMBEDDABLE:
                action.removeEmbeddable(map, embeddable);
        }
    }

    @Override
    public void undo() throws CannotUndoException {

        CreateRelationshipAction relationshipAction = (CreateRelationshipAction) actionManager
                .getAction(CreateRelationshipAction.getActionName());

        switch (this.mode) {
            case OBJECT_ENTITY: {
                for (Entry<ObjEntity, List<ObjRelationship>> entry : objRelationshipMap
                        .entrySet()) {

                    ObjEntity objEntity = entry.getKey();
                    for (ObjRelationship rel : entry.getValue()) {
                        relationshipAction.createObjRelationship(objEntity, rel);
                    }
                }

                CreateObjEntityAction action = (CreateObjEntityAction) actionManager
                        .getAction(CreateObjEntityAction.getActionName());
                action.createObjEntity(map, objEntity);

                break;
            }
            case DB_ENTITY: {

                for (Entry<DbEntity, List<DbRelationship>> entry : dbRelationshipMap
                        .entrySet()) {
                    DbEntity dbEntity = entry.getKey();
                    for (DbRelationship rel : entry.getValue()) {
                        relationshipAction.createDbRelationship(dbEntity, rel);
                    }
                }

                CreateDbEntityAction action = (CreateDbEntityAction) actionManager
                        .getAction(CreateDbEntityAction.getActionName());

                action.createEntity(map, dbEntity);

                break;
            }
            case QUERY: {
                this.domain = Application
                        .getInstance()
                        .getFrameController()
                        .getProjectController()
                        .findDomain(map);

                CreateQueryAction action = (CreateQueryAction) actionManager
                        .getAction(CreateQueryAction.getActionName());

                action.createQuery(domain, map, query);

                break;
            }
            case PROCEDURE: {
                CreateProcedureAction action = (CreateProcedureAction) actionManager
                        .getAction(CreateProcedureAction.getActionName());
                action.createProcedure(map, procedure);
                break;
            }
            case MAP_FROM_NODE: {
                this.dataNode.addDataMap(map);

                DataNodeEvent e = new DataNodeEvent(Application.getFrame(), this.dataNode);

                ProjectController controller = Application
                        .getInstance()
                        .getFrameController()
                        .getProjectController();

                e.setDomain(controller.findDomain(this.dataNode));

                controller.fireDataNodeEvent(e);
                
                break;
            }
            case MAP_FROM_DOMAIN: {
                CreateDataMapAction action = (CreateDataMapAction) actionManager
                        .getAction(CreateDataMapAction.getActionName());
                action.createDataMap(domain, map);
                
                break;
            }
            case NODE: {
                CreateNodeAction action = (CreateNodeAction) actionManager
                        .getAction(CreateNodeAction.getActionName());
                action.createDataNode(domain, dataNode);
                
                break;
            }

            case DOMAIN: {
                CreateDomainAction action = (CreateDomainAction) actionManager
                        .getAction(CreateDomainAction.getActionName());
                action.createDomain(domain);
                
                break;
            }

            case EMBEDDABLE: {
                CreateEmbeddableAction action = (CreateEmbeddableAction) actionManager
                        .getAction(CreateEmbeddableAction.getActionName());
                action.createEmbeddable(map, embeddable);
                
                break;
            }
        }
    }
}
