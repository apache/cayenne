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
import org.apache.cayenne.modeler.dialog.ConfirmDeleteDialog;
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

    public String getIconName() {
        return "icon-trash.gif";
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }

    public ConfirmDeleteDialog getConfirmDeleteDialog() {
        return new ConfirmDeleteDialog();
    }

    public void performAction(ActionEvent e) {

        ProjectController mediator = getProjectController();

        ConfirmDeleteDialog dialog = getConfirmDeleteDialog();

        if (mediator.getCurrentObjEntity() != null) {
            if (dialog.shouldDelete("ObjEntity", mediator.getCurrentObjEntity().getName())) {
                removeObjEntity();
            }
        }
        else if (mediator.getCurrentDbEntity() != null) {
            if (dialog.shouldDelete("DbEntity", mediator.getCurrentDbEntity().getName())) {
                removeDbEntity();
            }
        }
        else if (mediator.getCurrentQuery() != null) {
            if (dialog.shouldDelete("query", mediator.getCurrentQuery().getName())) {
                removeQuery();
            }
        }
        else if (mediator.getCurrentProcedure() != null) {
            if (dialog.shouldDelete("procedure", mediator.getCurrentProcedure().getName())) {
                removeProcedure();
            }
        }
        else if (mediator.getCurrentDataMap() != null) {
            if (dialog.shouldDelete("data map", mediator.getCurrentDataMap().getName())) {

                // In context of Data node just remove from Data Node
                if (mediator.getCurrentDataNode() != null) {
                    removeDataMapFromDataNode();
                }
                else {
                    // Not under Data Node, remove completely
                    removeDataMap();
                }
            }
        }
        else if (mediator.getCurrentDataNode() != null) {
            if (dialog.shouldDelete("data node", mediator.getCurrentDataNode().getName())) {
                removeDataNode();
            }
        }
        else if (mediator.getCurrentDataDomain() != null) {
            if (dialog.shouldDelete("data domain", mediator.getCurrentDataDomain().getName())) {
                removeDomain();
            }
        }
    }

    protected void removeDomain() {
        ApplicationProject project = (ApplicationProject) getCurrentProject();
        ProjectController mediator = getProjectController();
        DataDomain domain = mediator.getCurrentDataDomain();
        project.getConfiguration().removeDomain(domain.getName());
        mediator.fireDomainEvent(new DomainEvent(
                Application.getFrame(),
                domain,
                MapEvent.REMOVE));
    }

    protected void removeDataMap() {
        ProjectController mediator = getProjectController();
        DataMap map = mediator.getCurrentDataMap();
        DataDomain domain = mediator.getCurrentDataDomain();
        domain.removeMap(map.getName());
        mediator.fireDataMapEvent(new DataMapEvent(
                Application.getFrame(),
                map,
                MapEvent.REMOVE));
    }

    protected void removeDataNode() {
        ProjectController mediator = getProjectController();
        DataNode node = mediator.getCurrentDataNode();
        DataDomain domain = mediator.getCurrentDataDomain();
        domain.removeDataNode(node.getName());
        mediator.fireDataNodeEvent(new DataNodeEvent(
                Application.getFrame(),
                node,
                MapEvent.REMOVE));
    }

    /**
     * Removes current DbEntity from its DataMap and fires "remove" EntityEvent.
     */
    protected void removeDbEntity() {
        ProjectController mediator = getProjectController();
        DbEntity ent = mediator.getCurrentDbEntity();
        DataMap map = mediator.getCurrentDataMap();
        map.removeDbEntity(ent.getName(), true);
        mediator.fireDbEntityEvent(new EntityEvent(
                Application.getFrame(),
                ent,
                MapEvent.REMOVE));
    }

    /**
     * Removes current Query from its DataMap and fires "remove" QueryEvent.
     */
    protected void removeQuery() {
        ProjectController mediator = getProjectController();
        Query query = mediator.getCurrentQuery();
        DataMap map = mediator.getCurrentDataMap();
        map.removeQuery(query.getName());
        mediator.fireQueryEvent(new QueryEvent(
                Application.getFrame(),
                query,
                MapEvent.REMOVE));
    }

    /**
     * Removes current Procedure from its DataMap and fires "remove" ProcedureEvent.
     */
    protected void removeProcedure() {
        ProjectController mediator = getProjectController();
        Procedure procedure = mediator.getCurrentProcedure();
        DataMap map = mediator.getCurrentDataMap();
        map.removeProcedure(procedure.getName());
        mediator.fireProcedureEvent(new ProcedureEvent(
                Application.getFrame(),
                procedure,
                MapEvent.REMOVE));
    }

    /**
     * Removes current object entity from its DataMap.
     */
    protected void removeObjEntity() {
        ProjectController mediator = getProjectController();
        ObjEntity entity = mediator.getCurrentObjEntity();

        DataMap map = mediator.getCurrentDataMap();
        map.removeObjEntity(entity.getName(), true);
        mediator.fireObjEntityEvent(new EntityEvent(
                Application.getFrame(),
                entity,
                MapEvent.REMOVE));

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
                map.removeQuery(next.getName());
                mediator.fireQueryEvent(new QueryEvent(
                        Application.getFrame(),
                        next,
                        MapEvent.REMOVE));
            }
        }
    }

    protected void removeDataMapFromDataNode() {
        ProjectController mediator = getProjectController();
        DataNode node = mediator.getCurrentDataNode();
        DataMap map = mediator.getCurrentDataMap();
        node.removeDataMap(map.getName());

        // Force reloading of the data node in the browse view
        mediator.fireDataNodeEvent(new DataNodeEvent(Application.getFrame(), node));
    }

    /**
     * Returns <code>true</code> if last object in the path contains a removable object.
     */
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
}
