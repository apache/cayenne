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

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.event.DataNodeEvent;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.CreateDataMapAction;
import org.apache.cayenne.modeler.action.CreateDbEntityAction;
import org.apache.cayenne.modeler.action.CreateEmbeddableAction;
import org.apache.cayenne.modeler.action.CreateNodeAction;
import org.apache.cayenne.modeler.action.CreateObjEntityAction;
import org.apache.cayenne.modeler.action.CreateProcedureAction;
import org.apache.cayenne.modeler.action.CreateQueryAction;
import org.apache.cayenne.modeler.action.CreateRelationshipAction;
import org.apache.cayenne.modeler.action.RemoveAction;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class RemoveUndoableEdit extends CayenneUndoableEdit {

    private DataMap map;
    private DbEntity dbEntity;
    private ObjEntity objEntity;
    private QueryDescriptor query;
    private Procedure procedure;

    private DataNodeDescriptor dataNode;
    private DataChannelDescriptor domain;

    private Embeddable embeddable;

    private Map<DbEntity, List<DbRelationship>> dbRelationshipMap = new HashMap<>();
    private Map<ObjEntity, List<ObjRelationship>> objRelationshipMap = new HashMap<>();

    private static enum REMOVE_MODE {
        OBJECT_ENTITY, DB_ENTITY, QUERY, PROCEDURE, MAP_FROM_NODE, MAP_FROM_DOMAIN, NODE, DOMAIN, EMBEDDABLE
    };

    private REMOVE_MODE mode;

    public RemoveUndoableEdit(Application application) {
        this.domain = (DataChannelDescriptor) application.getProject().getRootNode();
        ;
        this.mode = REMOVE_MODE.DOMAIN;
    }

    public RemoveUndoableEdit(Application application, DataNodeDescriptor node,
            DataMap map) {
        this.map = map;
        this.dataNode = node;
        this.mode = REMOVE_MODE.MAP_FROM_NODE;
    }

    public RemoveUndoableEdit(Application application, DataMap map) {
        this.domain = (DataChannelDescriptor) application.getProject().getRootNode();
        ;
        this.map = map;
        this.mode = REMOVE_MODE.MAP_FROM_DOMAIN;
    }

    public RemoveUndoableEdit(Application application, DataNodeDescriptor node) {
        this.domain = (DataChannelDescriptor) application.getProject().getRootNode();
        ;
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

    public RemoveUndoableEdit(DataMap map, QueryDescriptor query) {
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
        RemoveAction action = actionManager.getAction(RemoveAction.class);

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
                action.removeDataMap(map);
                break;
            case NODE:
                action.removeDataNode(dataNode);
                break;
            case EMBEDDABLE:
                action.removeEmbeddable(map, embeddable);
        }
    }

    @Override
    public void undo() throws CannotUndoException {

        CreateRelationshipAction relationshipAction = actionManager
                .getAction(CreateRelationshipAction.class);

        switch (this.mode) {
            case OBJECT_ENTITY: {
                for (Entry<ObjEntity, List<ObjRelationship>> entry : objRelationshipMap
                        .entrySet()) {

                    ObjEntity objEntity = entry.getKey();
                    for (ObjRelationship rel : entry.getValue()) {
                        relationshipAction.createObjRelationship(objEntity, rel);
                    }
                }

                CreateObjEntityAction action = actionManager
                        .getAction(CreateObjEntityAction.class);
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

                CreateDbEntityAction action = actionManager
                        .getAction(CreateDbEntityAction.class);

                action.createEntity(map, dbEntity);

                break;
            }
            case QUERY: {

                this.domain = (DataChannelDescriptor) Application
                        .getInstance()
                        .getFrameController()
                        .getProjectController()
                        .getProject()
                        .getRootNode();

                CreateQueryAction action = actionManager
                        .getAction(CreateQueryAction.class);

                action.createQuery(domain, map, query);

                break;
            }
            case PROCEDURE: {
                CreateProcedureAction action = actionManager
                        .getAction(CreateProcedureAction.class);
                action.createProcedure(map, procedure);
                break;
            }
            case MAP_FROM_NODE: {
                this.dataNode.getDataMapNames().add(map.getName());

                DataNodeEvent e = new DataNodeEvent(Application.getFrame(), this.dataNode);

                ProjectController controller = Application
                        .getInstance()
                        .getFrameController()
                        .getProjectController();

                e
                        .setDomain((DataChannelDescriptor) controller
                                .getProject()
                                .getRootNode());

                controller.fireDataNodeEvent(e);

                break;
            }
            case MAP_FROM_DOMAIN: {
                CreateDataMapAction action = actionManager
                        .getAction(CreateDataMapAction.class);
                action.createDataMap(map);

                break;
            }
            case NODE: {
                CreateNodeAction action = actionManager.getAction(CreateNodeAction.class);
                action.createDataNode(dataNode);

                break;
            }

            case EMBEDDABLE: {
                CreateEmbeddableAction action = actionManager
                        .getAction(CreateEmbeddableAction.class);
                action.createEmbeddable(map, embeddable);

                break;
            }
        }
    }
}
