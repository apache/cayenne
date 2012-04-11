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
import java.util.Iterator;

import javax.swing.KeyStroke;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoableEdit;

import org.apache.cayenne.configuration.event.DataMapEvent;
import org.apache.cayenne.configuration.event.DataNodeEvent;
import org.apache.cayenne.configuration.event.ProcedureEvent;
import org.apache.cayenne.configuration.event.QueryEvent;
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
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.map.event.EmbeddableEvent;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.ConfirmRemoveDialog;
import org.apache.cayenne.modeler.undo.RemoveCompoundUndoableEdit;
import org.apache.cayenne.modeler.undo.RemoveUndoableEdit;
import org.apache.cayenne.modeler.util.CayenneAction;
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
                    application.getUndoManager().addEdit(
                            new RemoveUndoableEdit(application, mediator
                                    .getCurrentDataMap()));

                    removeDataMap(mediator.getCurrentDataMap());

                }
            }
        }
        else if (mediator.getCurrentDataNode() != null) {
            if (dialog.shouldDelete("data node", mediator.getCurrentDataNode().getName())) {
                application
                        .getUndoManager()
                        .addEdit(
                                new RemoveUndoableEdit(application, mediator
                                        .getCurrentDataNode()));

                removeDataNode(mediator.getCurrentDataNode());
            }
        }

        else if (mediator.getCurrentPaths() != null) { // multiple deletion
            if (dialog.shouldDelete("selected objects")) {
                Object[] paths = mediator.getCurrentPaths();

                CompoundEdit compoundEdit = new RemoveCompoundUndoableEdit();

                for (Object path : paths) {
                    compoundEdit.addEdit(removeLastPathComponent(path));
                }
                compoundEdit.end();

                application.getUndoManager().addEdit(compoundEdit);

            }
        }

    }

    public void removeDataMap(DataMap map) {
        ProjectController mediator = getProjectController();
        DataChannelDescriptor domain = (DataChannelDescriptor) mediator
                .getProject()
                .getRootNode();
        DataMapEvent e = new DataMapEvent(Application.getFrame(), map, MapEvent.REMOVE);
        e.setDomain((DataChannelDescriptor) mediator.getProject().getRootNode());

        domain.getDataMaps().remove(map);
        
        Iterator<DataNodeDescriptor> iterator = domain.getNodeDescriptors().iterator();
        while(iterator.hasNext()){
            DataNodeDescriptor node = iterator.next();
            if(node.getDataMapNames().contains(map.getName())){
                removeDataMapFromDataNode(node, map);
            }
        }
       
        mediator.fireDataMapEvent(e);
    }

    public void removeDataNode(DataNodeDescriptor node) {
        ProjectController mediator = getProjectController();
        DataChannelDescriptor domain = (DataChannelDescriptor) mediator
                .getProject()
                .getRootNode();
        DataNodeEvent e = new DataNodeEvent(Application.getFrame(), node, MapEvent.REMOVE);
        e.setDomain((DataChannelDescriptor) mediator.getProject().getRootNode());

        domain.getNodeDescriptors().remove(node);
        mediator.fireDataNodeEvent(e);
    }

    /**
     * Removes current DbEntity from its DataMap and fires "remove" EntityEvent.
     */
    public void removeDbEntity(DataMap map, DbEntity ent) {
        ProjectController mediator = getProjectController();

        EntityEvent e = new EntityEvent(Application.getFrame(), ent, MapEvent.REMOVE);
        e.setDomain((DataChannelDescriptor) mediator.getProject().getRootNode());

        map.removeDbEntity(ent.getName(), true);
        mediator.fireDbEntityEvent(e);
    }

    /**
     * Removes current Query from its DataMap and fires "remove" QueryEvent.
     */
    public void removeQuery(DataMap map, Query query) {
        ProjectController mediator = getProjectController();

        QueryEvent e = new QueryEvent(Application.getFrame(), query, MapEvent.REMOVE, map);
        e.setDomain((DataChannelDescriptor) mediator.getProject().getRootNode());

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
        e.setDomain((DataChannelDescriptor) mediator.getProject().getRootNode());

        map.removeProcedure(procedure.getName());
        mediator.fireProcedureEvent(e);
    }

    /**
     * Removes current object entity from its DataMap.
     */
    public void removeObjEntity(DataMap map, ObjEntity entity) {
        ProjectController mediator = getProjectController();

        EntityEvent e = new EntityEvent(Application.getFrame(), entity, MapEvent.REMOVE);
        e.setDomain((DataChannelDescriptor) mediator.getProject().getRootNode());

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
                        || (root instanceof String && root.toString().equals(
                                entity.getName()))) {
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
        e.setDomain((DataChannelDescriptor) mediator.getProject().getRootNode());

        map.removeEmbeddable(embeddable.getClassName());
        mediator.fireEmbeddableEvent(e, map);
    }

    public void removeDataMapFromDataNode(DataNodeDescriptor node, DataMap map) {
        ProjectController mediator = getProjectController();

        DataNodeEvent e = new DataNodeEvent(Application.getFrame(), node);
        e.setDomain((DataChannelDescriptor) mediator.getProject().getRootNode());

        node.getDataMapNames().remove(map.getName());

        // Force reloading of the data node in the browse view
        mediator.fireDataNodeEvent(e);
    }

    /**
     * Returns <code>true</code> if last object in the path contains a removable object.
     */
    @Override
    public boolean enableForPath(ConfigurationNode object) {
        if (object == null) {
            return false;
        }

        if (object instanceof DataChannelDescriptor) {
            return true;
        }
        else if (object instanceof DataMap) {
            return true;
        }
        else if (object instanceof DataNodeDescriptor) {
            return true;
        }
        else if (object instanceof Entity) {
            return true;
        }
        else if (object instanceof Attribute) {
            return true;
        }
        else if (object instanceof Relationship) {
            return true;
        }
        else if (object instanceof Procedure) {
            return true;
        }
        else if (object instanceof ProcedureParameter) {
            return true;
        }
        else if (object instanceof Embeddable) {
            return true;
        }
        else if (object instanceof EmbeddableAttribute) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Removes an object, depending on its type
     */
    private UndoableEdit removeLastPathComponent(Object object) {

        UndoableEdit undo = null;

        if (object instanceof DataMap) {
            undo = new RemoveUndoableEdit(application, (DataMap) object);
            removeDataMap((DataMap) object);
        }
        else if (object instanceof DataNodeDescriptor) {
            undo = new RemoveUndoableEdit(application, (DataNodeDescriptor) object);

            removeDataNode((DataNodeDescriptor) object);
        }
        else if (object instanceof DbEntity) {
            undo = new RemoveUndoableEdit(
                    ((DbEntity) object).getDataMap(),
                    (DbEntity) object);

            removeDbEntity(((DbEntity) object).getDataMap(), (DbEntity) object);
        }
        else if (object instanceof ObjEntity) {
            undo = new RemoveUndoableEdit(
                    ((ObjEntity) object).getDataMap(),
                    (ObjEntity) object);

            removeObjEntity(((ObjEntity) object).getDataMap(), (ObjEntity) object);
        }
        else if (object instanceof Query) {
            undo = new RemoveUndoableEdit(((Query) object).getDataMap(), (Query) object);

            removeQuery(((Query) object).getDataMap(), (Query) object);
        }
        else if (object instanceof Procedure) {
            undo = new RemoveUndoableEdit(
                    ((Procedure) object).getDataMap(),
                    (Procedure) object);

            removeProcedure(((Procedure) object).getDataMap(), (Procedure) object);
        }
        else if (object instanceof Embeddable) {
            undo = new RemoveUndoableEdit(
                    ((Embeddable) object).getDataMap(),
                    (Embeddable) object);
            removeEmbeddable(((Embeddable) object).getDataMap(), (Embeddable) object);
        }

        return undo;
    }
}
