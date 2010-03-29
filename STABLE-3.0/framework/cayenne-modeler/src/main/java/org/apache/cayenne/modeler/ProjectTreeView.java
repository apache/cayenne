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

package org.apache.cayenne.modeler;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.event.DataMapEvent;
import org.apache.cayenne.map.event.DataMapListener;
import org.apache.cayenne.map.event.DataNodeEvent;
import org.apache.cayenne.map.event.DataNodeListener;
import org.apache.cayenne.map.event.DbEntityListener;
import org.apache.cayenne.map.event.DomainEvent;
import org.apache.cayenne.map.event.DomainListener;
import org.apache.cayenne.map.event.EmbeddableEvent;
import org.apache.cayenne.map.event.EmbeddableListener;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.ObjEntityListener;
import org.apache.cayenne.map.event.ProcedureEvent;
import org.apache.cayenne.map.event.ProcedureListener;
import org.apache.cayenne.map.event.QueryEvent;
import org.apache.cayenne.map.event.QueryListener;
import org.apache.cayenne.modeler.action.CopyAction;
import org.apache.cayenne.modeler.action.CreateDataMapAction;
import org.apache.cayenne.modeler.action.CreateDbEntityAction;
import org.apache.cayenne.modeler.action.CreateDomainAction;
import org.apache.cayenne.modeler.action.CreateEmbeddableAction;
import org.apache.cayenne.modeler.action.CreateNodeAction;
import org.apache.cayenne.modeler.action.CreateObjEntityAction;
import org.apache.cayenne.modeler.action.CreateProcedureAction;
import org.apache.cayenne.modeler.action.CreateQueryAction;
import org.apache.cayenne.modeler.action.CutAction;
import org.apache.cayenne.modeler.action.ObjEntitySyncAction;
import org.apache.cayenne.modeler.action.PasteAction;
import org.apache.cayenne.modeler.action.RemoveAction;
import org.apache.cayenne.modeler.event.DataMapDisplayEvent;
import org.apache.cayenne.modeler.event.DataMapDisplayListener;
import org.apache.cayenne.modeler.event.DataNodeDisplayEvent;
import org.apache.cayenne.modeler.event.DataNodeDisplayListener;
import org.apache.cayenne.modeler.event.DbEntityDisplayListener;
import org.apache.cayenne.modeler.event.DomainDisplayEvent;
import org.apache.cayenne.modeler.event.DomainDisplayListener;
import org.apache.cayenne.modeler.event.EmbeddableDisplayEvent;
import org.apache.cayenne.modeler.event.EmbeddableDisplayListener;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.MultipleObjectsDisplayEvent;
import org.apache.cayenne.modeler.event.MultipleObjectsDisplayListener;
import org.apache.cayenne.modeler.event.ObjEntityDisplayListener;
import org.apache.cayenne.modeler.event.ProcedureDisplayEvent;
import org.apache.cayenne.modeler.event.ProcedureDisplayListener;
import org.apache.cayenne.modeler.event.QueryDisplayEvent;
import org.apache.cayenne.modeler.event.QueryDisplayListener;
import org.apache.cayenne.modeler.util.CellRenderers;
import org.apache.cayenne.modeler.util.Comparators;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.reflect.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Panel displaying Cayenne project as a tree.
 */
public class ProjectTreeView extends JTree implements DomainDisplayListener,
        DomainListener, DataMapDisplayListener, DataMapListener, DataNodeDisplayListener,
        DataNodeListener, ObjEntityListener, ObjEntityDisplayListener, DbEntityListener,
        DbEntityDisplayListener, QueryListener, QueryDisplayListener, ProcedureListener,
        ProcedureDisplayListener, MultipleObjectsDisplayListener,
        EmbeddableDisplayListener, EmbeddableListener {

    private static final Log logObj = LogFactory.getLog(ProjectTreeView.class);

    protected ProjectController mediator;
    protected TreeSelectionListener treeSelectionListener;

    /**
     * Popup menu containing basic functions
     */
    protected JPopupMenu popup;

    public ProjectTreeView(ProjectController mediator) {
        super();
        this.mediator = mediator;

        initView();
        initController();
        initFromModel(Application.getProject());
    }

    private void initView() {
        setCellRenderer(CellRenderers.treeRenderer());
    }

    private void initController() {
        treeSelectionListener = new TreeSelectionListener() {

            public void valueChanged(TreeSelectionEvent e) {
                TreePath[] paths = getSelectionPaths();

                if (paths != null) {
                    if (paths.length > 1) {
                        ProjectPath[] projectPaths = new ProjectPath[paths.length];
                        for (int i = 0; i < paths.length; i++) {
                            projectPaths[i] = createProjectPath(paths[i]);
                        }

                        mediator
                                .fireMultipleObjectsDisplayEvent(new MultipleObjectsDisplayEvent(
                                        this,
                                        projectPaths));
                    }
                    else if (paths.length == 1) {
                        processSelection(paths[0]);
                    }
                }
            }

            /**
             * Converts TreePath to ProjectPath
             */
            private ProjectPath createProjectPath(TreePath treePath) {
                Object[] path = treePath.getPath();
                Object[] projectPath = new Object[path.length];

                for (int i = 0; i < projectPath.length; i++) {
                    projectPath[i] = ((DefaultMutableTreeNode) path[i]).getUserObject();
                }

                return new ProjectPath(projectPath);
            }
        };

        addTreeSelectionListener(treeSelectionListener);

        addMouseListener(new PopupHandler());

        mediator.addDomainListener(this);
        mediator.addDomainDisplayListener(this);
        mediator.addDataNodeListener(this);
        mediator.addDataNodeDisplayListener(this);
        mediator.addDataMapListener(this);
        mediator.addDataMapDisplayListener(this);
        mediator.addObjEntityListener(this);
        mediator.addObjEntityDisplayListener(this);
        mediator.addDbEntityListener(this);
        mediator.addDbEntityDisplayListener(this);
        mediator.addEmbeddableDisplayListener(this);
        mediator.addEmbeddableListener(this);
        mediator.addProcedureListener(this);
        mediator.addProcedureDisplayListener(this);
        mediator.addQueryListener(this);
        mediator.addQueryDisplayListener(this);

        mediator.getApplication().getActionManager().setupCCP(
                this,
                CutAction.getActionName(),
                CopyAction.getActionName());
    }

    private void initFromModel(Project project) {
        // build model
        ProjectTreeModel model = new ProjectTreeModel(project);
        setRootVisible(false);
        setModel(model);

        // expand top level
        getSelectionModel().setSelectionMode(
                TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        Enumeration level = model.getRootNode().children();
        while (level.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) level.nextElement();
            TreePath path = new TreePath(node.getPath());
            expandPath(path);
        }
    }

    /**
     * Returns tree model cast to ProjectTreeModel.
     */
    ProjectTreeModel getProjectModel() {
        return (ProjectTreeModel) getModel();
    }

    /**
     * Returns a "name" property of the tree node.
     */
    @Override
    public String convertValueToText(
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {

        // unwrap
        while (value instanceof DefaultMutableTreeNode) {
            value = ((DefaultMutableTreeNode) value).getUserObject();
        }

        // String - just return it
        if (value instanceof String) {
            return value.toString();
        }

        // Project - return the name of top file
        if (value instanceof Project) {
            File f = ((Project) value).getMainFile();
            return (f != null) ? f.getPath() : "";
        }

        // read name property
        try {
            if (value instanceof Embeddable) {
                return (value != null) ? String.valueOf(PropertyUtils.getProperty(
                        value,
                        "className")) : "";
            }

            return (value != null) ? String.valueOf(PropertyUtils.getProperty(
                    value,
                    "name")) : "";

        }
        catch (Exception e) {
            String objectClass = (value == null) ? "(unknown)" : value
                    .getClass()
                    .getName();
            logObj.warn("Exception reading property 'name', class " + objectClass, e);
            return "";
        }
    }

    public void currentDomainChanged(DomainDisplayEvent e) {
        if ((e.getSource() == this || !e.isDomainChanged()) && !e.isRefired()) {
            return;
        }

        showNode(new Object[] {
            e.getDomain()
        });
    }

    public void currentDataNodeChanged(DataNodeDisplayEvent e) {
        if ((e.getSource() == this || !e.isDataNodeChanged()) && !e.isRefired()) {
            return;
        }

        showNode(new Object[] {
                e.getDomain(), e.getDataNode()
        });
    }

    public void currentDataMapChanged(DataMapDisplayEvent e) {
        if ((e.getSource() == this || !e.isDataMapChanged()) && !e.isRefired()) {
            return;
        }

        showNode(new Object[] {
                e.getDomain(), e.getDataMap()
        });
    }

    public void currentObjEntityChanged(EntityDisplayEvent e) {
        e.setEntityChanged(true);
        currentEntityChanged(e);
    }

    public void currentDbEntityChanged(EntityDisplayEvent e) {
        e.setEntityChanged(true);
        currentEntityChanged(e);
    }

    protected void currentEntityChanged(EntityDisplayEvent e) {
        if ((e.getSource() == this || !e.isEntityChanged()) && !e.isRefired()) {
            return;
        }

        showNode(new Object[] {
                e.getDomain(), e.getDataMap(), e.getEntity()
        });
    }

    public void currentProcedureChanged(ProcedureDisplayEvent e) {
        if ((e.getSource() == this || !e.isProcedureChanged()) && !e.isRefired()) {
            return;
        }

        showNode(new Object[] {
                e.getDomain(), e.getDataMap(), e.getProcedure()
        });
    }

    public void currentQueryChanged(QueryDisplayEvent e) {
        if ((e.getSource() == this || !e.isQueryChanged()) && !e.isRefired()) {
            return;
        }

        showNode(new Object[] {
                e.getDomain(), e.getDataMap(), e.getQuery()
        });
    }

    public void currentObjectsChanged(MultipleObjectsDisplayEvent e) {
    }

    public void procedureAdded(ProcedureEvent e) {

        DefaultMutableTreeNode node = getProjectModel().getNodeForObjectPath(
                new Object[] {
                        mediator.findDomain(e.getProcedure().getDataMap()),
                        e.getProcedure().getDataMap()
                });

        if (node == null) {
            return;
        }

        Procedure procedure = e.getProcedure();
        DefaultMutableTreeNode currentNode = new DefaultMutableTreeNode(procedure, false);
        positionNode(node, currentNode, Comparators.getDataMapChildrenComparator());
        showNode(currentNode);
    }

    public void procedureChanged(ProcedureEvent e) {
        if (e.isNameChange()) {
            Object[] path = new Object[] {
                    mediator.findDomain(e.getProcedure().getDataMap()),
                    e.getProcedure().getDataMap(), e.getProcedure()
            };

            updateNode(path);
            positionNode(path, Comparators.getDataMapChildrenComparator());
            showNode(path);
        }
    }

    public void procedureRemoved(ProcedureEvent e) {

        removeNode(new Object[] {
                mediator.findDomain(e.getProcedure().getDataMap()),
                e.getProcedure().getDataMap(), e.getProcedure()
        });
    }

    public void queryAdded(QueryEvent e) {

        DefaultMutableTreeNode node = getProjectModel().getNodeForObjectPath(
                new Object[] {
                        e.getDomain() != null ? e.getDomain() : mediator.findDomain(e
                                .getDataMap()), e.getDataMap()
                });

        if (node == null) {
            return;
        }

        Query query = e.getQuery();
        DefaultMutableTreeNode currentNode = new DefaultMutableTreeNode(query, false);
        positionNode(node, currentNode, Comparators.getDataMapChildrenComparator());
        showNode(currentNode);
    }

    public void queryChanged(QueryEvent e) {

        if (e.isNameChange()) {
            Object[] path = new Object[] {
                    e.getDomain() != null ? e.getDomain() : mediator.findDomain(e
                            .getDataMap()), e.getQuery()
            };

            updateNode(path);
            positionNode(path, Comparators.getDataMapChildrenComparator());
            showNode(path);
        }
    }

    public void queryRemoved(QueryEvent e) {
        removeNode(new Object[] {
                e.getDomain() != null ? e.getDomain() : mediator.findDomain(e
                        .getDataMap()), e.getDataMap(), e.getQuery()
        });
    }

    public void domainChanged(DomainEvent e) {

        Object[] path = new Object[] {
            e.getDomain()
        };

        updateNode(path);

        if (e.isNameChange()) {
            positionNode(path, Comparators.getNamedObjectComparator());
            showNode(path);
        }
    }

    public void domainAdded(DomainEvent e) {
        DataDomain dataDomain = e.getDomain();
        DefaultMutableTreeNode newNode = ProjectTreeModel.wrapProjectNode(dataDomain);

        positionNode(null, newNode, Comparators.getNamedObjectComparator());
        showNode(newNode);
    }

    public void domainRemoved(DomainEvent e) {
        removeNode(new Object[] {
            e.getDomain()
        });
    }

    public void dataNodeChanged(DataNodeEvent e) {

        DefaultMutableTreeNode node = getProjectModel().getNodeForObjectPath(
                new Object[] {
                        e.getDomain() != null ? e.getDomain() : mediator.findDomain(e
                                .getDataNode()), e.getDataNode()
                });

        if (node != null) {

            if (e.isNameChange()) {
                positionNode((DefaultMutableTreeNode) node.getParent(), node, Comparators
                        .getDataDomainChildrenComparator());
                showNode(node);
            }
            else {

                getProjectModel().nodeChanged(node);

                // check for DataMap additions/removals...

                Object[] maps = e.getDataNode().getDataMaps().toArray();
                int mapCount = maps.length;

                // DataMap was linked
                if (mapCount > node.getChildCount()) {

                    for (int i = 0; i < mapCount; i++) {
                        boolean found = false;
                        for (int j = 0; j < node.getChildCount(); j++) {
                            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node
                                    .getChildAt(j);
                            if (maps[i] == child.getUserObject()) {
                                found = true;
                                break;
                            }
                        }

                        if (!found) {
                            DefaultMutableTreeNode newMapNode = new DefaultMutableTreeNode(
                                    maps[i],
                                    false);
                            positionNode(node, newMapNode, Comparators
                                    .getNamedObjectComparator());
                            break;
                        }
                    }
                }
                // DataMap was unlinked
                else if (mapCount < node.getChildCount()) {
                    for (int j = 0; j < node.getChildCount(); j++) {
                        boolean found = false;
                        DefaultMutableTreeNode child;
                        child = (DefaultMutableTreeNode) node.getChildAt(j);
                        Object obj = child.getUserObject();
                        for (int i = 0; i < mapCount; i++) {
                            if (maps[i] == obj) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            removeNode(child);
                            break;
                        }
                    }
                }
            }
        }
    }

    public void dataNodeAdded(DataNodeEvent e) {
        if (e.getSource() == this) {
            return;
        }

        DefaultMutableTreeNode node = getProjectModel().getNodeForObjectPath(
                new Object[] {
                    e.getDomain() != null ? e.getDomain() : mediator.findDomain(e
                            .getDataNode())
                });

        if (node == null) {
            return;
        }

        DataNode dataNode = e.getDataNode();
        DefaultMutableTreeNode currentNode = ProjectTreeModel.wrapProjectNode(dataNode);
        positionNode(node, currentNode, Comparators.getDataDomainChildrenComparator());
        showNode(currentNode);
    }

    public void dataNodeRemoved(DataNodeEvent e) {
        if (e.getSource() == this) {
            return;
        }

        removeNode(new Object[] {
                e.getDomain() != null ? e.getDomain() : mediator.findDomain(e
                        .getDataNode()), e.getDataNode()
        });
    }

    public void dataMapChanged(DataMapEvent e) {

        Object[] path = new Object[] {
                e.getDomain() != null ? e.getDomain() : mediator.findDomain(e
                        .getDataMap()), e.getDataMap()
        };

        updateNode(path);

        if (e.isNameChange()) {
            positionNode(path, Comparators.getDataDomainChildrenComparator());
            showNode(path);
        }
    }

    public void dataMapAdded(DataMapEvent e) {
        DefaultMutableTreeNode domainNode = getProjectModel().getNodeForObjectPath(
                new Object[] {
                    e.getDomain() != null ? e.getDomain() : mediator.findDomain(e
                            .getDataMap())
                });

        DefaultMutableTreeNode newMapNode = ProjectTreeModel.wrapProjectNode(e
                .getDataMap());
        positionNode(domainNode, newMapNode, Comparators
                .getDataDomainChildrenComparator());
        showNode(newMapNode);
    }

    public void dataMapRemoved(DataMapEvent e) {
        DataMap map = e.getDataMap();
        DataDomain domain = e.getDomain() != null ? e.getDomain() : mediator.findDomain(e
                .getDataMap());

        removeNode(new Object[] {
                domain, map
        });

        // Clean up map from the nodes
        for (DataNode dataNode : new ArrayList<DataNode>(domain.getDataNodes())) {
            removeNode(new Object[] {
                    domain, dataNode, map
            });
        }
    }

    public void objEntityChanged(EntityEvent e) {
        entityChanged(e);
    }

    public void objEntityAdded(EntityEvent e) {
        entityAdded(e);
    }

    public void objEntityRemoved(EntityEvent e) {
        entityRemoved(e);
    }

    public void dbEntityChanged(EntityEvent e) {
        entityChanged(e);
    }

    public void dbEntityAdded(EntityEvent e) {
        entityAdded(e);
    }

    public void dbEntityRemoved(EntityEvent e) {
        entityRemoved(e);
    }

    /**
     * Makes Entity visible and selected.
     * <ul>
     * <li>If entity is from the current node, refreshes the node making sure changes in
     * the entity name are reflected.</li>
     * <li>If entity is in a different node, makes that node visible and selected.</li>
     * </ul>
     */
    protected void entityChanged(EntityEvent e) {
        if (e.isNameChange()) {
            Object[] path = new Object[] {
                    e.getDomain() != null ? e.getDomain() : mediator.findDomain(e
                            .getEntity()
                            .getDataMap()), e.getEntity().getDataMap(), e.getEntity()
            };

            updateNode(path);
            positionNode(path, Comparators.getDataMapChildrenComparator());
            showNode(path);
        }
    }

    /**
     * Event handler for ObjEntity and DbEntity additions. Adds a tree node for the entity
     * and make it selected.
     */
    protected void entityAdded(EntityEvent e) {

        Entity entity = e.getEntity();

        DefaultMutableTreeNode mapNode = getProjectModel().getNodeForObjectPath(
                new Object[] {
                        e.getDomain() != null ? e.getDomain() : mediator.findDomain(e
                                .getEntity()
                                .getDataMap()), e.getEntity().getDataMap()
                });

        if (mapNode == null) {
            return;
        }

        DefaultMutableTreeNode currentNode = new DefaultMutableTreeNode(entity, false);
        positionNode(mapNode, currentNode, Comparators.getDataMapChildrenComparator());
        showNode(currentNode);
    }

    /**
     * Event handler for ObjEntity and DbEntity removals. Removes a tree node for the
     * entity and selects its sibling.
     */
    protected void entityRemoved(EntityEvent e) {
        if (e.getSource() == this) {
            return;
        }

        // remove from DataMap tree
        removeNode(new Object[] {
                e.getDomain() != null ? e.getDomain() : mediator.findDomain(e
                        .getEntity()
                        .getDataMap()), e.getEntity().getDataMap(), e.getEntity()
        });
    }

    /**
     * Removes current node from the tree. Selects a new node adjacent to the currently
     * selected node instead.
     */
    protected void removeNode(DefaultMutableTreeNode toBeRemoved) {

        // lookup for the new selected node
        Object selectedNode = null;

        TreePath selectionPath = getSelectionPath();
        if (selectionPath != null) {
            selectedNode = selectionPath.getLastPathComponent();
        }

        if (toBeRemoved == selectedNode) {

            // first search siblings
            DefaultMutableTreeNode newSelection = toBeRemoved.getNextSibling();
            if (newSelection == null) {
                newSelection = toBeRemoved.getPreviousSibling();

                // try parent
                if (newSelection == null) {
                    newSelection = (DefaultMutableTreeNode) toBeRemoved.getParent();

                    // search the whole tree
                    if (newSelection == null) {

                        newSelection = toBeRemoved.getNextNode();
                        if (newSelection == null) {

                            newSelection = toBeRemoved.getPreviousNode();
                        }
                    }
                }
            }

            showNode(newSelection);
        }

        // remove this node
        getProjectModel().removeNodeFromParent(toBeRemoved);
    }

    /** Makes node current, visible and selected. */
    protected void showNode(DefaultMutableTreeNode node) {
        TreePath path = new TreePath(node.getPath());
        scrollPathToVisible(path);
        setSelectionPath(path);
    }

    protected void showNode(Object[] path) {
        if (path == null) {
            return;
        }

        DefaultMutableTreeNode node = getProjectModel().getNodeForObjectPath(path);

        if (node == null) {
            return;
        }

        this.showNode(node);
    }

    protected void updateNode(Object[] path) {
        if (path == null) {
            return;
        }

        DefaultMutableTreeNode node = getProjectModel().getNodeForObjectPath(path);
        if (node != null) {
            getProjectModel().nodeChanged(node);
        }
    }

    protected void removeNode(Object[] path) {
        if (path == null) {
            return;
        }

        DefaultMutableTreeNode node = getProjectModel().getNodeForObjectPath(path);
        if (node != null) {
            removeNode(node);
        }
    }

    /**
     * Processes node selection regardless of whether a new node was selected, or an
     * already selected node was clicked again. Normally called from event listener
     * methods.
     */
    public void processSelection(TreePath path) {
        if (path == null) {
            return;
        }

        DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) path
                .getLastPathComponent();

        Object[] data = getUserObjects(currentNode);
        if (data.length == 0) {
            // this should clear the right-side panel
            mediator.fireDomainDisplayEvent(new DomainDisplayEvent(this, null));
            return;
        }

        Object obj = data[data.length - 1];
        if (obj instanceof DataDomain) {
            mediator
                    .fireDomainDisplayEvent(new DomainDisplayEvent(this, (DataDomain) obj));
        }
        else if (obj instanceof DataMap) {
            if (data.length == 3) {
                mediator.fireDataMapDisplayEvent(new DataMapDisplayEvent(
                        this,
                        (DataMap) obj,
                        (DataDomain) data[data.length - 3],
                        (DataNode) data[data.length - 2]));
            }
            else if (data.length == 2) {
                mediator.fireDataMapDisplayEvent(new DataMapDisplayEvent(
                        this,
                        (DataMap) obj,
                        (DataDomain) data[data.length - 2]));
            }
        }
        else if (obj instanceof DataNode) {
            if (data.length == 2) {
                mediator.fireDataNodeDisplayEvent(new DataNodeDisplayEvent(
                        this,
                        (DataDomain) data[data.length - 2],
                        (DataNode) obj));
            }
        }
        else if (obj instanceof Entity) {
            EntityDisplayEvent e = new EntityDisplayEvent(this, (Entity) obj);
            e.setUnselectAttributes(true);
            if (data.length == 4) {
                e.setDataMap((DataMap) data[data.length - 2]);
                e.setDomain((DataDomain) data[data.length - 4]);
                e.setDataNode((DataNode) data[data.length - 3]);
            }
            else if (data.length == 3) {
                e.setDataMap((DataMap) data[data.length - 2]);
                e.setDomain((DataDomain) data[data.length - 3]);
            }

            if (obj instanceof ObjEntity) {
                mediator.fireObjEntityDisplayEvent(e);
            }
            else if (obj instanceof DbEntity) {
                mediator.fireDbEntityDisplayEvent(e);
            }
        }
        else if (obj instanceof Embeddable) {
            EmbeddableDisplayEvent e = new EmbeddableDisplayEvent(
                    this,
                    (Embeddable) obj,
                    (DataMap) data[data.length - 2],
                    (DataDomain) data[data.length - 3]);
            mediator.fireEmbeddableDisplayEvent(e);
        }
        else if (obj instanceof Procedure) {
            ProcedureDisplayEvent e = new ProcedureDisplayEvent(
                    this,
                    (Procedure) obj,
                    (DataMap) data[data.length - 2],
                    (DataDomain) data[data.length - 3]);
            mediator.fireProcedureDisplayEvent(e);
        }
        else if (obj instanceof Query) {
            QueryDisplayEvent e = new QueryDisplayEvent(
                    this,
                    (Query) obj,
                    (DataMap) data[data.length - 2],
                    (DataDomain) data[data.length - 3]);
            mediator.fireQueryDisplayEvent(e);
        }

        this.scrollPathToVisible(path);
    }

    /**
     * Returns array of the user objects ending with this and starting with one under
     * root. That is the array of actual objects rather than wrappers.
     */
    private Object[] getUserObjects(DefaultMutableTreeNode node) {
        List list = new ArrayList();
        while (!node.isRoot()) {
            list.add(0, node.getUserObject());
            node = (DefaultMutableTreeNode) node.getParent();
        }
        return list.toArray();
    }

    private void positionNode(Object[] path, Comparator comparator) {
        if (path == null) {
            return;
        }

        DefaultMutableTreeNode node = getProjectModel().getNodeForObjectPath(path);
        if (node == null) {
            return;
        }

        positionNode(null, node, comparator);
    }

    private void positionNode(
            MutableTreeNode parent,
            DefaultMutableTreeNode treeNode,
            Comparator comparator) {

        removeTreeSelectionListener(treeSelectionListener);
        try {
            getProjectModel().positionNode(parent, treeNode, comparator);
        }
        finally {
            addTreeSelectionListener(treeSelectionListener);
        }
    }

    public TreeSelectionListener getTreeSelectionListener() {
        return treeSelectionListener;
    }

    /**
     * Creates JPopupMenu containing main functions
     */
    private JPopupMenu createJPopupMenu() {
        JPopupMenu popup = new JPopupMenu();

        popup.add(buildMenu(CreateDomainAction.getActionName()));
        popup.add(buildMenu(CreateNodeAction.getActionName()));
        popup.add(buildMenu(CreateDataMapAction.getActionName()));

        popup.add(buildMenu(CreateObjEntityAction.getActionName()));
        popup.add(buildMenu(CreateEmbeddableAction.getActionName()));
        popup.add(buildMenu(CreateDbEntityAction.getActionName()));
  
        popup.add(buildMenu(CreateProcedureAction.getActionName()));
        popup.add(buildMenu(CreateQueryAction.getActionName()));
        popup.addSeparator();
        popup.add(buildMenu(ObjEntitySyncAction.getActionName()));
        popup.addSeparator();
        popup.add(buildMenu(RemoveAction.getActionName()));
        popup.addSeparator();
        popup.add(buildMenu(CutAction.getActionName()));
        popup.add(buildMenu(CopyAction.getActionName()));
        popup.add(buildMenu(PasteAction.getActionName()));

        return popup;
    }

    /**
     * Creates and returns an menu item associated with the key.
     * 
     * @param key action key
     */
    private JMenuItem buildMenu(String key) {
        return mediator.getApplication().getAction(key).buildMenu();
    }

    /**
     * Class to handle right-click and show popup for selected tree row
     */
    class PopupHandler extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            mouseReleased(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                if (popup == null)
                    popup = createJPopupMenu();

                /**
                 * Selecting specified row
                 */
                int row = getRowForLocation(e.getX(), e.getY());
                if (row != -1 && !isRowSelected(row))
                    setSelectionRow(row);

                popup.show(ProjectTreeView.this, e.getX(), e.getY());
            }
        }
    }

    public void embeddableAdded(EmbeddableEvent e, DataMap map) {
        Embeddable embeddable = e.getEmbeddable();

        DefaultMutableTreeNode mapNode = getProjectModel().getNodeForObjectPath(
                new Object[] {
                        e.getDomain() != null ? e.getDomain() : mediator.findDomain(map),
                        map
                });

        if (mapNode == null) {
            return;
        }

        DefaultMutableTreeNode currentNode = new DefaultMutableTreeNode(embeddable, false);
        positionNode(mapNode, currentNode, Comparators.getDataMapChildrenComparator());
        showNode(currentNode);
    }

    public void embeddableChanged(EmbeddableEvent e, DataMap map) {
        if (e.isNameChange()) {
            Object[] path = new Object[] {
                    e.getDomain() != null ? e.getDomain() : mediator.findDomain(map),
                    map, e.getEmbeddable()
            };

            updateNode(path);
            positionNode(path, Comparators.getDataMapChildrenComparator());
            showNode(path);
        }
    }

    public void embeddableRemoved(EmbeddableEvent e, DataMap map) {
        if (e.getSource() == this) {
            return;
        }

        // remove from DataMap tree
        removeNode(new Object[] {
                e.getDomain() != null ? e.getDomain() : mediator.findDomain(map), map,
                e.getEmbeddable()
        });
    }

    public void currentEmbeddableChanged(EmbeddableDisplayEvent e) {
        e.setEmbeddableChanged(true);
        
        if ((e.getSource() == this || !e.isEmbeddableChanged()) && !e.isRefired()) {
            return;
        }

        showNode(new Object[] {
                e.getDomain(), e.getDataMap(), e.getEmbeddable()
        });
        
    }
}
