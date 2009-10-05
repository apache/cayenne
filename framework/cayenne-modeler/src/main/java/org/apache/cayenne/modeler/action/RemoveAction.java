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

package org.apache.cayenne.modeler.action;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.KeyStroke;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoableEdit;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.map.event.DataMapEvent;
import org.apache.cayenne.map.event.DataNodeEvent;
import org.apache.cayenne.map.event.DomainEvent;
import org.apache.cayenne.map.event.EmbeddableEvent;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.map.event.ProcedureEvent;
import org.apache.cayenne.map.event.QueryEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.ConfirmRemoveDialog;
import org.apache.cayenne.modeler.undo.RemoveCompoundUndoableEdit;
import org.apache.cayenne.modeler.undo.RemoveUndoableEdit;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.project.ApplicationProject;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.query.AbstractQuery;
import org.apache.cayenne.query.Query;

/**
 * Removes currently selected object from the project. This can be Domain, DataNode,
 * Entity, Attribute or Relationship.
 */
public class RemoveAction extends CayenneAction {

    

    public static String getActionName() {
        return "Remove";
    }

    public RemoveAction(Application application) {
        super(getActionName(), application);
    }

    protected RemoveAction(String actionName, Application application) {
        super(actionName, application);
    }

    @Override
    public String getIconName() {
        return "icon-trash.gif";
    }

    @Override
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit
                .getDefaultToolkit()
                .getMenuShortcutKeyMask());
    }

    /**
     * Creates and returns dialog for delete prompt
     * 
     * @param allowAsking If false, no question will be asked no matter what settings are
     */
    public ConfirmRemoveDialog getConfirmDeleteDialog(boolean allowAsking) {
        return new ConfirmRemoveDialog(allowAsking);
    }

    @Override
    public void performAction(ActionEvent e) {
        performAction(e, true);
    }

    /**
     * Performs delete action
     * 
     * @param allowAsking If false, no question will be asked no matter what settings are
     */
    public void performAction(ActionEvent e, boolean allowAsking) {

        ProjectController mediator = getProjectController();

        ConfirmRemoveDialog dialog = getConfirmDeleteDialog(allowAsking);

        if (mediator.getCurrentObjEntity() != null) {
            if (dialog
                    .shouldDelete("ObjEntity", mediator.getCurrentObjEntity().getName())) {
                application.getUndoManager().addEdit(
                        new RemoveUndoableEdit(mediator.getCurrentDataMap(), mediator
                                .getCurrentObjEntity()));
                removeObjEntity(mediator.getCurrentDataMap(), mediator
                        .getCurrentObjEntity());
            }
        }
        else if (mediator.getCurrentDbEntity() != null) {
            if (dialog.shouldDelete("DbEntity", mediator.getCurrentDbEntity().getName())) {
                application.getUndoManager().addEdit(
                        new RemoveUndoableEdit(mediator.getCurrentDataMap(), mediator
                                .getCurrentDbEntity()));
                removeDbEntity(mediator.getCurrentDataMap(), mediator
                        .getCurrentDbEntity());
            }
        }
        else if (mediator.getCurrentQuery() != null) {
            if (dialog.shouldDelete("query", mediator.getCurrentQuery().getName())) {
                application.getUndoManager().addEdit(
                        new RemoveUndoableEdit(mediator.getCurrentDataMap(), mediator
                                .getCurrentQuery()));
                removeQuery(mediator.getCurrentDataMap(), mediator.getCurrentQuery());
            }
        }
        else if (mediator.getCurrentProcedure() != null) {
            if (dialog
                    .shouldDelete("procedure", mediator.getCurrentProcedure().getName())) {

                application.getUndoManager().addEdit(
                        new RemoveUndoableEdit(mediator.getCurrentDataMap(), mediator
                                .getCurrentProcedure()));

                removeProcedure(mediator.getCurrentDataMap(), mediator
                        .getCurrentProcedure());

            }
        }
        else if (mediator.getCurrentEmbeddable() != null) {
            if (dialog.shouldDelete("embeddable", mediator
                    .getCurrentEmbeddable()
                    .getClassName())) {

                application.getUndoManager().addEdit(
                        new RemoveUndoableEdit(mediator.getCurrentDataMap(), mediator
                                .getCurrentEmbeddable()));

                removeEmbeddable(mediator.getCurrentDataMap(), mediator
                        .getCurrentEmbeddable());
            }
        }
        else if (mediator.getCurrentDataMap() != null) {
            if (dialog.shouldDelete("data map", mediator.getCurrentDataMap().getName())) {

                // In context of Data node just remove from Data Node
                if (mediator.getCurrentDataNode() != null) {
                    application.getUndoManager().addEdit(
                            new RemoveUndoableEdit(application, mediator
                                    .getCurrentDataNode(), mediator.getCurrentDataMap()));

                    removeDataMapFromDataNode(mediator.getCurrentDataNode(), mediator
                            .getCurrentDataMap());

                }
                else {
                    // Not under Data Node, remove completely
                    application
                            .getUndoManager()
                            .addEdit(
                                    new RemoveUndoableEdit(application, mediator
                                            .getCurrentDataDomain(), mediator
                                            .getCurrentDataMap()));

                    removeDataMap(mediator.getCurrentDataDomain(), mediator
                            .getCurrentDataMap());

                }
            }
        }
        else if (mediator.getCurrentDataNode() != null) {
            if (dialog.shouldDelete("data node", mediator.getCurrentDataNode().getName())) {
                application.getUndoManager().addEdit(
                        new RemoveUndoableEdit(application, mediator
                                .getCurrentDataDomain(), mediator.getCurrentDataNode()));

                removeDataNode(mediator.getCurrentDataDomain(), mediator
                        .getCurrentDataNode());
            }
        }
        else if (mediator.getCurrentDataDomain() != null) {
            if (dialog.shouldDelete("data domain", mediator
                    .getCurrentDataDomain()
                    .getName())) {

                application.getUndoManager().addEdit(
                        new RemoveUndoableEdit(application, mediator
                                .getCurrentDataDomain()));

                removeDomain(mediator.getCurrentDataDomain());
            }
        }
        else if (mediator.getCurrentPaths() != null) { // multiple deletion
            if (dialog.shouldDelete("selected objects")) {
                ProjectPath[] paths = mediator.getCurrentPaths();

                CompoundEdit compoundEdit = new RemoveCompoundUndoableEdit();

                for (ProjectPath path : paths) {
                    compoundEdit.addEdit(removeLastPathComponent(path));
                }

                application.getUndoManager().addEdit(compoundEdit);

            }
        }

    }

    public void removeDomain(DataDomain domain) {
        ApplicationProject project = (ApplicationProject) getCurrentProject();
        ProjectController mediator = getProjectController();

        project.getConfiguration().removeDomain(domain.getName());
        mediator.fireDomainEvent(new DomainEvent(
                Application.getFrame(),
                domain,
                MapEvent.REMOVE));
    }

    public void removeDataMap(DataDomain domain, DataMap map) {
        ProjectController mediator = getProjectController();

        DataMapEvent e = new DataMapEvent(Application.getFrame(), map, MapEvent.REMOVE);
        e.setDomain(domain);

        domain.removeMap(map.getName());
        mediator.fireDataMapEvent(e);
    }

    public void removeDataNode(DataDomain domain, DataNode node) {
        ProjectController mediator = getProjectController();

        DataNodeEvent e = new DataNodeEvent(Application.getFrame(), node, MapEvent.REMOVE);
        e.setDomain(domain);

        domain.removeDataNode(node.getName());
        mediator.fireDataNodeEvent(e);
    }

    /**
     * Removes current DbEntity from its DataMap and fires "remove" EntityEvent.
     */
    public void removeDbEntity(DataMap map, DbEntity ent) {
        ProjectController mediator = getProjectController();

        EntityEvent e = new EntityEvent(Application.getFrame(), ent, MapEvent.REMOVE);
        e.setDomain(mediator.findDomain(map));

        map.removeDbEntity(ent.getName(), true);
        mediator.fireDbEntityEvent(e);
    }

    /**
     * Removes current Query from its DataMap and fires "remove" QueryEvent.
     */
    public void removeQuery(DataMap map, Query query) {
        ProjectController mediator = getProjectController();

        QueryEvent e = new QueryEvent(Application.getFrame(), query, MapEvent.REMOVE, map);
        e.setDomain(mediator.findDomain(map));

        map.removeQuery(query.getName());
        mediator.fireQueryEvent(e);
    }

    /**
     * Removes current Procedure from its DataMap and fires "remove" ProcedureEvent.
     */
    public void removeProcedure(DataMap map, Procedure procedure) {
        ProjectController mediator = getProjectController();

        ProcedureEvent e = new ProcedureEvent(
                Application.getFrame(),
                procedure,
                MapEvent.REMOVE);
        e.setDomain(mediator.findDomain(map));

        map.removeProcedure(procedure.getName());
        mediator.fireProcedureEvent(e);
    }

    /**
     * Removes current object entity from its DataMap.
     */
    public void removeObjEntity(DataMap map, ObjEntity entity) {
        ProjectController mediator = getProjectController();

        EntityEvent e = new EntityEvent(Application.getFrame(), entity, MapEvent.REMOVE);
        e.setDomain(mediator.findDomain(map));

        map.removeObjEntity(entity.getName(), true);
        mediator.fireObjEntityEvent(e);

        // remove queries that depend on entity
        // TODO: (Andrus, 09/09/2005) show warning dialog?

        // clone to be able to remove within iterator...
        for (Query query : new ArrayList<Query>(map.getQueries())) {
            if (query instanceof AbstractQuery) {
                AbstractQuery next = (AbstractQuery) query;
                Object root = next.getRoot();
    
                if (root == entity
                        || (root instanceof String && root
                                .toString()
                                .equals(entity.getName()))) {
                    removeQuery(map, next);
                }
            }
        }
    }

    public void removeEmbeddable(DataMap map, Embeddable embeddable) {
        ProjectController mediator = getProjectController();

        EmbeddableEvent e = new EmbeddableEvent(
                Application.getFrame(),
                embeddable,
                MapEvent.REMOVE);
        e.setDomain(mediator.findDomain(map));

        map.removeEmbeddable(embeddable.getClassName());
        mediator.fireEmbeddableEvent(e, map);
    }

    public void removeDataMapFromDataNode(DataNode node, DataMap map) {
        ProjectController mediator = getProjectController();

        DataNodeEvent e = new DataNodeEvent(Application.getFrame(), node);
        e.setDomain(mediator.findDomain(node));

        node.removeDataMap(map.getName());

        // Force reloading of the data node in the browse view
        mediator.fireDataNodeEvent(e);
    }

    /**
     * Returns <code>true</code> if last object in the path contains a removable object.
     */
    @Override
    public boolean enableForPath(ProjectPath path) {
        if (path == null) {
            return false;
        }

        Object lastObject = path.getObject();

        if (lastObject instanceof DataDomain) {
            return true;
        }
        else if (lastObject instanceof DataMap) {
            return true;
        }
        else if (lastObject instanceof DataNode) {
            return true;
        }
        else if (lastObject instanceof Entity) {
            return true;
        }
        else if (lastObject instanceof Attribute) {
            return true;
        }
        else if (lastObject instanceof Relationship) {
            return true;
        }
        else if (lastObject instanceof Procedure) {
            return true;
        }
        else if (lastObject instanceof ProcedureParameter) {
            return true;
        }
        else if (lastObject instanceof Embeddable) {
            return true;
        }
        else if (lastObject instanceof EmbeddableAttribute) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Removes an object, depending on its type
     */
    private UndoableEdit removeLastPathComponent(ProjectPath path) {
        Object lastObject = path.getObject();

        UndoableEdit undo = null;

        if (lastObject instanceof DataDomain) {
            undo = new RemoveUndoableEdit(application, (DataDomain) lastObject);
            removeDomain((DataDomain) lastObject);
        }
        else if (lastObject instanceof DataMap) {
            Object parent = path.getObjectParent();

            if (parent instanceof DataDomain) {
                undo = new RemoveUndoableEdit(
                        application,
                        (DataDomain) parent,
                        (DataMap) lastObject);

                removeDataMap((DataDomain) parent, (DataMap) lastObject);
            }
            else { // if(parent instanceof DataNode)
                undo = new RemoveUndoableEdit(
                        application,
                        (DataNode) parent,
                        (DataMap) lastObject);

                removeDataMapFromDataNode((DataNode) parent, (DataMap) lastObject);
            }
        }
        else if (lastObject instanceof DataNode) {
            undo = new RemoveUndoableEdit(application, (DataDomain) path
                    .getObjectParent(), (DataNode) lastObject);

            removeDataNode((DataDomain) path.getObjectParent(), (DataNode) lastObject);
        }
        else if (lastObject instanceof DbEntity) {
            undo = new RemoveUndoableEdit(
                    (DataMap) path.getObjectParent(),
                    (DbEntity) lastObject);

            removeDbEntity((DataMap) path.getObjectParent(), (DbEntity) lastObject);
        }
        else if (lastObject instanceof ObjEntity) {
            undo = new RemoveUndoableEdit(
                    (DataMap) path.getObjectParent(),
                    (ObjEntity) lastObject);

            removeObjEntity((DataMap) path.getObjectParent(), (ObjEntity) lastObject);
        }
        else if (lastObject instanceof Query) {
            undo = new RemoveUndoableEdit(
                    (DataMap) path.getObjectParent(),
                    (Query) lastObject);

            removeQuery((DataMap) path.getObjectParent(), (Query) lastObject);
        }
        else if (lastObject instanceof Procedure) {
            undo = new RemoveUndoableEdit(
                    (DataMap) path.getObjectParent(),
                    (Procedure) lastObject);

            removeProcedure((DataMap) path.getObjectParent(), (Procedure) lastObject);
        }
        else if (lastObject instanceof Embeddable) {
            undo = new RemoveUndoableEdit(
                    (DataMap) path.getObjectParent(),
                    (Embeddable) lastObject);
            removeEmbeddable((DataMap) path.getObjectParent(), (Embeddable) lastObject);
        }

        return undo;
    }
}
