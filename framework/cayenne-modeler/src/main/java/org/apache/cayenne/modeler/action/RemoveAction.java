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

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.map.event.DataMapEvent;
import org.apache.cayenne.map.event.DataNodeEvent;
import org.apache.cayenne.map.event.DomainEvent;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.map.event.ProcedureEvent;
import org.apache.cayenne.map.event.QueryEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.ConfirmRemoveDialog;
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
        return KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }

    /**
     * Creates and returns dialog for delete prompt
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
     * @param allowAsking If false, no question will be asked no matter what settings are 
     */
    public void performAction(ActionEvent e, boolean allowAsking) {

        ProjectController mediator = getProjectController();

        ConfirmRemoveDialog dialog = getConfirmDeleteDialog(allowAsking);

        if (mediator.getCurrentObjEntity() != null) {
            if (dialog.shouldDelete("ObjEntity", mediator.getCurrentObjEntity().getName())) {
                removeObjEntity(mediator.getCurrentDataMap(), mediator.getCurrentObjEntity());
            }
        }
        else if (mediator.getCurrentDbEntity() != null) {
            if (dialog.shouldDelete("DbEntity", mediator.getCurrentDbEntity().getName())) {
                removeDbEntity(mediator.getCurrentDataMap(), mediator.getCurrentDbEntity());
            }
        }
        else if (mediator.getCurrentQuery() != null) {
            if (dialog.shouldDelete("query", mediator.getCurrentQuery().getName())) {
                removeQuery(mediator.getCurrentDataMap(), mediator.getCurrentQuery());
            }
        }
        else if (mediator.getCurrentProcedure() != null) {
            if (dialog.shouldDelete("procedure", mediator.getCurrentProcedure().getName())) {
                removeProcedure(mediator.getCurrentDataMap(), mediator.getCurrentProcedure());
            }
        }
        else if (mediator.getCurrentDataMap() != null) {
            if (dialog.shouldDelete("data map", mediator.getCurrentDataMap().getName())) {

                // In context of Data node just remove from Data Node
                if (mediator.getCurrentDataNode() != null) {
                    removeDataMapFromDataNode(mediator.getCurrentDataNode(), mediator.getCurrentDataMap());
                }
                else {
                    // Not under Data Node, remove completely
                    removeDataMap(mediator.getCurrentDataDomain(), mediator.getCurrentDataMap());
                }
            }
        }
        else if (mediator.getCurrentDataNode() != null) {
            if (dialog.shouldDelete("data node", mediator.getCurrentDataNode().getName())) {
                removeDataNode(mediator.getCurrentDataDomain(), mediator.getCurrentDataNode());
            }
        }
        else if (mediator.getCurrentDataDomain() != null) {
            if (dialog.shouldDelete("data domain", mediator.getCurrentDataDomain().getName())) {
                removeDomain(mediator.getCurrentDataDomain());
            }
        }
        else if (mediator.getCurrentPaths() != null) { //multiple deletion
            if (dialog.shouldDelete("selected objects")) {
                ProjectPath[] paths = mediator.getCurrentPaths();
                for (ProjectPath path : paths) {
                    removeLastPathComponent(path);
                }
            }
        }
    }
    
    private void removeDomain(DataDomain domain){
        ApplicationProject project = (ApplicationProject) getCurrentProject();
        ProjectController mediator = getProjectController();
        
        project.getConfiguration().removeDomain(domain.getName());
        mediator.fireDomainEvent(new DomainEvent(
                Application.getFrame(),
                domain,
                MapEvent.REMOVE));
    }

    private void removeDataMap(DataDomain domain, DataMap map) {
        ProjectController mediator = getProjectController();
        
        DataMapEvent e = new DataMapEvent(
                Application.getFrame(),
                map,
                MapEvent.REMOVE);
        e.setDomain(domain);

        domain.removeMap(map.getName());
        mediator.fireDataMapEvent(e);
    }

    private void removeDataNode(DataDomain domain, DataNode node) {
        ProjectController mediator = getProjectController();
        
        DataNodeEvent e = new DataNodeEvent(
                Application.getFrame(),
                node,
                MapEvent.REMOVE);
        e.setDomain(domain);

        domain.removeDataNode(node.getName());
        mediator.fireDataNodeEvent(e);
    }

    /**
     * Removes current DbEntity from its DataMap and fires "remove" EntityEvent.
     */
    private void removeDbEntity(DataMap map, DbEntity ent) {
        ProjectController mediator = getProjectController();
        
        EntityEvent e = new EntityEvent(
                Application.getFrame(),
                ent,
                MapEvent.REMOVE);
        e.setDomain(mediator.findDomain(map));

        map.removeDbEntity(ent.getName(), true);
        mediator.fireDbEntityEvent(e);
    }

    /**
     * Removes current Query from its DataMap and fires "remove" QueryEvent.
     */
    private void removeQuery(DataMap map, Query query) {
        ProjectController mediator = getProjectController();
        
        QueryEvent e = new QueryEvent(
                Application.getFrame(),
                query,
                MapEvent.REMOVE,
                map);
        e.setDomain(mediator.findDomain(map));
        
        map.removeQuery(query.getName());
        mediator.fireQueryEvent(e);
    }

    /**
     * Removes current Procedure from its DataMap and fires "remove" ProcedureEvent.
     */
    private void removeProcedure(DataMap map, Procedure procedure) {
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
    private void removeObjEntity(DataMap map, ObjEntity entity) {
        ProjectController mediator = getProjectController();
        
        EntityEvent e = new EntityEvent(
                Application.getFrame(),
                entity,
                MapEvent.REMOVE);
        e.setDomain(mediator.findDomain(map));
        
        map.removeObjEntity(entity.getName(), true);
        mediator.fireObjEntityEvent(e);

        // remove queries that depend on entity
        // TODO: (Andrus, 09/09/2005) show warning dialog?

        // clone to be able to remove within iterator...
        Iterator it = new ArrayList(map.getQueries()).iterator();
        while (it.hasNext()) {
            AbstractQuery next = (AbstractQuery) it.next();
            Object root = next.getRoot();

            if (root == entity
                    || (root instanceof String && root
                            .toString()
                            .equals(entity.getName()))) {
                removeQuery(map, next);
            }
        }
    }

    private void removeDataMapFromDataNode(DataNode node, DataMap map) {
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
        else {
            return false;
        }
    }
    
    /**
     * Removes an object, depending on its type
     */
    private void removeLastPathComponent(ProjectPath path) {
        Object lastObject = path.getObject();
        
        if (lastObject instanceof DataDomain) {
            removeDomain((DataDomain) lastObject);
        }
        else if (lastObject instanceof DataMap) {
            Object parent = path.getObjectParent();
            
            if(parent instanceof DataDomain)
                removeDataMap((DataDomain) parent, (DataMap) lastObject);
            else //if(parent instanceof DataNode)
                removeDataMapFromDataNode((DataNode) parent, (DataMap) lastObject);
        }
        else if (lastObject instanceof DataNode) {
            removeDataNode((DataDomain) path.getObjectParent(), (DataNode) lastObject);
        }
        else if (lastObject instanceof DbEntity) {
            removeDbEntity((DataMap) path.getObjectParent(), (DbEntity) lastObject);
        }
        else if (lastObject instanceof ObjEntity) {
            removeObjEntity((DataMap) path.getObjectParent(), (ObjEntity) lastObject);
        }
        else if (lastObject instanceof Query) {
            removeQuery((DataMap) path.getObjectParent(), (Query) lastObject);
        }
        else if (lastObject instanceof Procedure) {
            removeProcedure((DataMap) path.getObjectParent(), (Procedure) lastObject);
        }
    }
}
