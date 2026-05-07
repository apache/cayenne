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

package org.apache.cayenne.modeler.ui.project.tree;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.modeler.event.display.DataMapDisplayEvent;
import org.apache.cayenne.modeler.event.display.DataMapDisplayListener;
import org.apache.cayenne.modeler.event.display.DataNodeDisplayEvent;
import org.apache.cayenne.modeler.event.display.DataNodeDisplayListener;
import org.apache.cayenne.modeler.event.display.DbEntityDisplayEvent;
import org.apache.cayenne.modeler.event.display.DbEntityDisplayListener;
import org.apache.cayenne.modeler.event.display.DomainDisplayEvent;
import org.apache.cayenne.modeler.event.display.DomainDisplayListener;
import org.apache.cayenne.modeler.event.display.EmbeddableDisplayEvent;
import org.apache.cayenne.modeler.event.display.EmbeddableDisplayListener;
import org.apache.cayenne.modeler.event.display.MultipleObjectsDisplayEvent;
import org.apache.cayenne.modeler.event.display.MultipleObjectsDisplayListener;
import org.apache.cayenne.modeler.event.display.ObjEntityDisplayEvent;
import org.apache.cayenne.modeler.event.display.ObjEntityDisplayListener;
import org.apache.cayenne.modeler.event.display.ProcedureDisplayEvent;
import org.apache.cayenne.modeler.event.display.ProcedureDisplayListener;
import org.apache.cayenne.modeler.event.display.QueryDisplayEvent;
import org.apache.cayenne.modeler.event.display.QueryDisplayListener;
import org.apache.cayenne.modeler.event.model.DataMapEvent;
import org.apache.cayenne.modeler.event.model.DataMapListener;
import org.apache.cayenne.modeler.event.model.DataNodeEvent;
import org.apache.cayenne.modeler.event.model.DataNodeListener;
import org.apache.cayenne.modeler.event.model.DbEntityEvent;
import org.apache.cayenne.modeler.event.model.DbEntityListener;
import org.apache.cayenne.modeler.event.model.DomainEvent;
import org.apache.cayenne.modeler.event.model.DomainListener;
import org.apache.cayenne.modeler.event.model.EmbeddableEvent;
import org.apache.cayenne.modeler.event.model.EmbeddableListener;
import org.apache.cayenne.modeler.event.model.ObjEntityEvent;
import org.apache.cayenne.modeler.event.model.ObjEntityListener;
import org.apache.cayenne.modeler.event.model.ProcedureEvent;
import org.apache.cayenne.modeler.event.model.ProcedureListener;
import org.apache.cayenne.modeler.event.model.QueryEvent;
import org.apache.cayenne.modeler.event.model.QueryListener;
import org.apache.cayenne.modeler.toolkit.Renderers;
import org.apache.cayenne.modeler.toolkit.border.TopBorder;
import org.apache.cayenne.modeler.ui.action.CopyAction;
import org.apache.cayenne.modeler.ui.action.CreateDataMapAction;
import org.apache.cayenne.modeler.ui.action.CreateDbEntityAction;
import org.apache.cayenne.modeler.ui.action.CreateEmbeddableAction;
import org.apache.cayenne.modeler.ui.action.CreateNodeAction;
import org.apache.cayenne.modeler.ui.action.CreateObjEntityAction;
import org.apache.cayenne.modeler.ui.action.CreateProcedureAction;
import org.apache.cayenne.modeler.ui.action.CreateQueryAction;
import org.apache.cayenne.modeler.ui.action.CutAction;
import org.apache.cayenne.modeler.ui.action.DbEntitySyncAction;
import org.apache.cayenne.modeler.ui.action.LinkDataMapsAction;
import org.apache.cayenne.modeler.toolkit.AppAction;
import org.apache.cayenne.modeler.ui.action.ObjEntitySyncAction;
import org.apache.cayenne.modeler.ui.action.PasteAction;
import org.apache.cayenne.modeler.ui.action.RemoveAction;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.project.ProjectComparators;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.reflect.PropertyUtils;
import org.apache.cayenne.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.MenuElement;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.dnd.MouseDragGestureRecognizer;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Project navigation tree. Subscribes to project events and translates between session
 * state and tree presentation: selection→display events, model changes→node updates.
 */
public class ProjectTree extends JTree
        implements DomainDisplayListener, DomainListener, DataMapDisplayListener,
        DataMapListener, DataNodeDisplayListener, DataNodeListener, ObjEntityListener,
        ObjEntityDisplayListener, DbEntityListener, DbEntityDisplayListener, QueryListener,
        QueryDisplayListener, ProcedureListener, ProcedureDisplayListener,
        MultipleObjectsDisplayListener, EmbeddableDisplayListener, EmbeddableListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectTree.class);

    private static final Color SELECTION_COLOR = UIManager.getColor("Tree.selectionBackground");

    private final ProjectSession session;
    private final TreeSelectionListener treeSelectionListener;
    private JPopupMenu popup;

    public ProjectTree(ProjectSession session) {
        this.session = session;

        setCellRenderer(Renderers.treeRenderer());
        setOpaque(false);
        setBorder(TopBorder.create());
        setRootVisible(true);
        getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

        // install the tree model from the currently open project
        setModel(new ProjectTreeModel(session.project()));

        this.treeSelectionListener = createTreeSelectionListener();
        addTreeSelectionListener(treeSelectionListener);
        addTreeWillExpandListener(createTreeExpandListener());
        addMouseListener(new MouseClickHandler());

        session.addDomainListener(this);
        session.addDomainDisplayListener(this);
        session.addDataNodeListener(this);
        session.addDataNodeDisplayListener(this);
        session.addDataMapListener(this);
        session.addDataMapDisplayListener(this);
        session.addObjEntityListener(this);
        session.addObjEntityDisplayListener(this);
        session.addDbEntityListener(this);
        session.addDbEntityDisplayListener(this);
        session.addEmbeddableDisplayListener(this);
        session.addEmbeddableListener(this);
        session.addProcedureListener(this);
        session.addProcedureDisplayListener(this);
        session.addQueryListener(this);
        session.addQueryDisplayListener(this);
        session.addMultipleObjectsDisplayListener(this);

        session.app().getActionManager().setupCutCopyPaste(
                this,
                CutAction.class,
                CopyAction.class);

        DragSource dragSource = new DragSource();
        Toolkit.getDefaultToolkit().createDragGestureRecognizer(
                MouseDragGestureRecognizer.class,
                dragSource,
                this,
                DnDConstants.ACTION_COPY,
                new TreeDragSource(dragSource, this, session));
    }

    /**
     * Returns tree model cast to ProjectTreeModel.
     */
    public ProjectTreeModel getProjectModel() {
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
        while (value instanceof DefaultMutableTreeNode node) {
            value = node.getUserObject();
        }

        // String - just return it
        if (value instanceof String) {
            return value.toString();
        }

        // Project - return the name of top file
        if (value instanceof Project p) {
            Resource resource = p.getConfigurationResource();
            return (resource != null) ? resource.getURL().getPath() : "";
        }

        // read name property
        try {
            if (value instanceof Embeddable) {
                return String.valueOf(PropertyUtils.getProperty(value, "className"));
            }

            return (value != null) ? String.valueOf(PropertyUtils.getProperty(value, "name")) : "";
        } catch (Exception e) {
            LOGGER.warn("Exception reading property 'name', class " + value.getClass().getName(), e);
            return "";
        }
    }

    /**
     * Makes node current, visible and selected.
     */
    void navigateTo(DefaultMutableTreeNode node) {
        TreePath path = new TreePath(node.getPath());

        if (!isVisible(path)) {
            makeVisible(path);

            Rectangle bounds = getPathBounds(path);
            if (bounds != null) {
                bounds.height = getVisibleRect().height;
                scrollRectToVisible(bounds);
            }
        }

        setSelectionPath(path);
    }

    /**
     * Makes node current, visible but not selected.
     */
    void setSelected(DefaultMutableTreeNode node) {
        TreePath path = new TreePath(node.getPath());
        if (!isVisible(path)) {
            makeVisible(path);
        }
    }

    void updateNode(DefaultMutableTreeNode node) {
        if (node != null) {
            getProjectModel().nodeChanged(node);
        }
    }

    /**
     * Removes current node from the tree. Selects a new node adjacent to the currently
     * selected node instead.
     */
    void removeNode(DefaultMutableTreeNode toBeRemoved) {
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
                if (newSelection == null) {
                    newSelection = (DefaultMutableTreeNode) toBeRemoved.getParent();
                    if (newSelection == null) {
                        newSelection = toBeRemoved.getNextNode();
                        if (newSelection == null) {
                            newSelection = toBeRemoved.getPreviousNode();
                        }
                    }
                }
            }
            navigateTo(newSelection);
        }

        getProjectModel().removeNodeFromParent(toBeRemoved);
    }

    @Override
    public void paintComponent(Graphics g) {
        g.setColor(Color.white);
        g.fillRect(0, 0, getWidth(), getHeight());
        if (getSelectionCount() > 0) {
            g.setColor(SELECTION_COLOR);
            int[] rows = getSelectionRows();
            if (rows != null) {
                for (int i : rows) {
                    Rectangle r = getRowBounds(i);
                    g.fillRect(0, r.y, getWidth(), r.height);
                }
            }
        }
        super.paintComponent(g);
    }

    private TreeSelectionListener createTreeSelectionListener() {
        return new TreeSelectionListener() {

            @Override
            public void valueChanged(TreeSelectionEvent e) {
                TreePath[] paths = getSelectionPaths();

                if (paths != null) {
                    if (paths.length > 1) {
                        ConfigurationNode projectParentPath = null;
                        ConfigurationNode[] projectPaths = new ConfigurationNode[paths.length];
                        boolean commonParentPath = true;

                        for (int i = 0; i < paths.length; i++) {
                            projectPaths[i] = createProjectPath(paths[i]);

                            TreePath parentPath = paths[i].getParentPath();
                            if (i > 0 && parentPath != null && !parentPath.equals(paths[i - 1].getParentPath())) {
                                commonParentPath = false;
                            }
                        }

                        if (commonParentPath) {
                            TreePath parentPath = paths[0].getParentPath();
                            projectParentPath = createProjectPath(parentPath);
                        }

                        session.displayMultipleObjects(new MultipleObjectsDisplayEvent(
                                this,
                                projectParentPath, projectPaths));
                    } else if (paths.length == 1) {
                        processSelection(paths[0]);
                    }
                }
            }

            /**
             * Converts TreePath to Object
             */
            private ConfigurationNode createProjectPath(TreePath treePath) {
                Object[] path = treePath.getPath();
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path[path.length - 1];
                return (ConfigurationNode) treeNode.getUserObject();
            }
        };
    }

    private TreeWillExpandListener createTreeExpandListener() {
        return new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent e) {
                TreePath path = e.getPath();
                if (!isPathSelected(path) && !isSelectionEmpty()) {
                    setSelectionPath(path);
                }
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent e) {
                TreePath path = e.getPath();
                if (!isPathSelected(path) && !isSelectionEmpty()) {
                    setSelectionPath(path);
                }
            }
        };
    }

    @Override
    public void domainSelected(DomainDisplayEvent e) {
        navigateTo(e.getDomain());
    }

    @Override
    public void dataNodeSlected(DataNodeDisplayEvent e) {
        navigateTo(e.getDomain(), e.getDataNode());
    }

    @Override
    public void dataMapSelected(DataMapDisplayEvent e) {
        navigateTo(e.getDomain(), e.getDataMap());
    }

    @Override
    public void objEntitySelected(ObjEntityDisplayEvent e) {
        navigateTo(e.getDomain(), e.getDataMap(), e.getEntity());
    }

    @Override
    public void dbEntitySelected(DbEntityDisplayEvent e) {
        navigateTo(e.getDomain(), e.getDataMap(), e.getEntity());
    }

    @Override
    public void procedureSelected(ProcedureDisplayEvent e) {
        navigateTo(e.getDomain(), e.getDataMap(), e.getProcedure());
    }

    @Override
    public void querySelected(QueryDisplayEvent e) {
        navigateTo(e.getDomain(), e.getDataMap(), e.getQuery());
    }

    @Override
    public void multipleObjectsSelected(MultipleObjectsDisplayEvent e) {
        if (e.getSource() == this || e.getParentNode() == null) {
            return;
        }

        ConfigurationNode[] nodes = e.getNodes();
        TreePath[] treePaths = new TreePath[nodes.length];

        for (int i = 0; i < nodes.length; i++) {
            DefaultMutableTreeNode treeNode = getProjectModel().getNodeForObjectPath(e.getParentNode(), nodes[i]);
            if (treeNode != null) {
                treePaths[i] = new TreePath(treeNode.getPath());
            } else if (e.getParentNode() == nodes[i]) {
                treeNode = getProjectModel().getNodeForObjectPath(e.getParentNode());
                treePaths[i] = new TreePath(treeNode.getPath());
            }
        }

        if (!isVisible(treePaths[0])) {
            makeVisible(treePaths[0]);

            Rectangle bounds = getPathBounds(treePaths[0]);
            if (bounds != null) {
                bounds.height = getVisibleRect().height;
                scrollRectToVisible(bounds);
            }
        }

        setSelectionPaths(treePaths);
    }

    @Override
    public void embeddableSelected(EmbeddableDisplayEvent e) {
        navigateTo(e.getDomain(), e.getDataMap(), e.getEmbeddable());
    }

    @Override
    public void procedureAdded(ProcedureEvent e) {

        DefaultMutableTreeNode node = getProjectModel().getNodeForObjectPath(
                session.project().getRootNode(),
                e.getProcedure().getDataMap());

        if (node == null) {
            return;
        }

        Procedure procedure = e.getProcedure();
        DefaultMutableTreeNode currentNode = new DefaultMutableTreeNode(procedure, false);
        positionNode(node, currentNode, ProjectComparators.forDataMapChildren());
        navigateTo(currentNode);
    }

    @Override
    public void procedureChanged(ProcedureEvent e) {
        if (e.isNameChange()) {
            Object[] path = new Object[]{
                    session.project().getRootNode(),
                    e.getProcedure().getDataMap(), e.getProcedure()
            };

            updateNode(path);
            positionNode(ProjectComparators.forDataMapChildren(), path);
            navigateTo(path);
        }
    }

    @Override
    public void procedureRemoved(ProcedureEvent e) {
        removeNode(session.project().getRootNode(), e.getProcedure().getDataMap(), e.getProcedure());
    }

    @Override
    public void queryAdded(QueryEvent e) {

        DefaultMutableTreeNode node = getProjectModel().getNodeForObjectPath(
                session.project().getRootNode(),
                e.getDataMap());

        if (node == null) {
            return;
        }

        QueryDescriptor query = e.getQuery();
        DefaultMutableTreeNode currentNode = new DefaultMutableTreeNode(query, false);
        positionNode(node, currentNode, ProjectComparators.forDataMapChildren());
        navigateTo(currentNode);
    }

    @Override
    public void queryChanged(QueryEvent e) {
        if (e.isNameChange()) {
            Object[] path = new Object[]{
                    session.project().getRootNode(),
                    e.getQuery().getDataMap(), e.getQuery()
            };

            updateNode(path);
            positionNode(ProjectComparators.forDataMapChildren(), path);
            navigateTo(path);
        }
    }

    @Override
    public void queryRemoved(QueryEvent e) {
        removeNode(session.project().getRootNode(), e.getDataMap(), e.getQuery());
    }

    @Override
    public void domainChanged(DomainEvent e) {
        Object[] path = new Object[]{e.getDomain()};
        updateNode(path);
        if (e.isNameChange()) {
            positionNode(ProjectComparators.forNamedObjects(), path);
            navigateTo(path);
        }
    }

    @Override
    public void dataNodeChanged(DataNodeEvent e) {
        DefaultMutableTreeNode node = getProjectModel().getNodeForObjectPath(
                session.project().getRootNode(),
                e.getDataNode());

        if (node != null) {
            if (e.isNameChange()) {
                positionNode((DefaultMutableTreeNode) node.getParent(), node,
                        ProjectComparators.forDataDomainChildren());
                navigateTo(node);
            } else {
                getProjectModel().nodeChanged(node);

                DataChannelDescriptor domain = (DataChannelDescriptor) session.project().getRootNode();

                String[] mapsName = e.getDataNode().getDataMapNames().toArray(new String[0]);
                int mapCount = mapsName.length;

                // DataMap was linked
                if (mapCount > node.getChildCount()) {
                    for (String aMapsName : mapsName) {
                        boolean found = false;
                        for (int j = 0; j < node.getChildCount(); j++) {
                            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(j);
                            if (domain.getDataMap(aMapsName) == child.getUserObject()) {
                                found = true;
                                break;
                            }
                        }

                        if (!found) {
                            DefaultMutableTreeNode newMapNode =
                                    new DefaultMutableTreeNode(domain.getDataMap(aMapsName), false);
                            positionNode(node, newMapNode, ProjectComparators.forNamedObjects());
                            break;
                        }
                    }
                } else if (mapCount < node.getChildCount()) {
                    // DataMap was unlinked
                    int j = 0;
                    while (j < node.getChildCount()) {
                        boolean found = false;
                        DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(j);
                        Object obj = child.getUserObject();
                        for (Object aMapsName : mapsName) {
                            if (domain.getDataMap(aMapsName.toString()) == obj) {
                                found = true;
                                j++;
                            }
                        }
                        if (!found) {
                            removeNode(child);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void dataNodeAdded(DataNodeEvent e) {
        if (e.getSource() == this) {
            return;
        }

        DefaultMutableTreeNode node = getProjectModel().getNodeForObjectPath(session.project().getRootNode());

        if (node == null) {
            return;
        }

        DataNodeDescriptor dataNode = e.getDataNode();
        DefaultMutableTreeNode currentNode = ProjectTreeFactory.wrapProjectNode(dataNode);
        positionNode(node, currentNode, ProjectComparators.forDataDomainChildren());
        navigateTo(currentNode);
    }

    @Override
    public void dataNodeRemoved(DataNodeEvent e) {
        if (e.getSource() == this) {
            return;
        }

        removeNode(session.project().getRootNode(), e.getDataNode());
    }

    @Override
    public void dataMapChanged(DataMapEvent e) {

        Object[] path = new Object[]{
                session.project().getRootNode(),
                e.getDataMap()
        };

        updateNode(path);

        if (e.isNameChange()) {
            session.updateEntityResolver();
            positionNode(ProjectComparators.forDataDomainChildren(), path);
            navigateTo(path);
        }
    }

    @Override
    public void dataMapAdded(DataMapEvent e) {
        DataChannelDescriptor dataChannelDescriptor =
                (DataChannelDescriptor) session.project().getRootNode();
        DefaultMutableTreeNode domainNode = getProjectModel().getNodeForObjectPath(dataChannelDescriptor);

        DefaultMutableTreeNode newMapNode = ProjectTreeFactory.wrapProjectNode(e.getDataMap());

        session.entityResolver().addDataMap(e.getDataMap());

        positionNode(domainNode, newMapNode, ProjectComparators.forDataDomainChildren());
        if (session.app().getFrame().getDbImportResultDialog().isGlobalImport()) {
            setSelected(newMapNode);
        } else {
            navigateTo(newMapNode);
        }

        for (DataNodeDescriptor dataNode : new ArrayList<>(dataChannelDescriptor.getNodeDescriptors())) {
            for (String dataMapName : dataNode.getDataMapNames()) {
                if (e.getDataMap().getName().equals(dataMapName)) {
                    session.fireDataNodeEvent(DataNodeEvent.ofChange(this, dataNode));
                }
            }
        }
    }

    @Override
    public void dataMapRemoved(DataMapEvent e) {
        DataMap map = e.getDataMap();
        DataChannelDescriptor dataChannelDescriptor = (DataChannelDescriptor) session.project().getRootNode();
        removeNode(dataChannelDescriptor, map);

        session.entityResolver().removeDataMap(e.getDataMap());

        // Clean up map from the nodes
        for (DataNodeDescriptor dataNode : new ArrayList<>(dataChannelDescriptor.getNodeDescriptors())) {
            removeNode(dataChannelDescriptor, dataNode, map);
        }
    }

    @Override
    public void objEntityChanged(ObjEntityEvent e) {
        entityChanged(e.getEntity(), e.isNameChange());
    }

    @Override
    public void objEntityAdded(ObjEntityEvent e) {
        entityAdded(e.getEntity());
    }

    @Override
    public void objEntityRemoved(ObjEntityEvent e) {
        entityRemoved(e.getEntity(), e.getSource());
    }

    @Override
    public void dbEntityChanged(DbEntityEvent e) {
        entityChanged(e.getEntity(), e.isNameChange());
    }

    @Override
    public void dbEntityAdded(DbEntityEvent e) {
        entityAdded(e.getEntity());
    }

    @Override
    public void dbEntityRemoved(DbEntityEvent e) {
        entityRemoved(e.getEntity(), e.getSource());
    }

    /**
     * Makes Entity visible and selected.
     * <ul>
     * <li>If entity is from the current node, refreshes the node making sure changes in
     * the entity name are reflected.</li>
     * <li>If entity is in a different node, makes that node visible and selected.</li>
     * </ul>
     */
    private void entityChanged(Entity<?, ?, ?> entity, boolean nameChange) {
        if (nameChange) {
            Object[] path = new Object[]{
                    session.project().getRootNode(),
                    entity.getDataMap(), entity
            };

            updateNode(path);
            positionNode(ProjectComparators.forDataMapChildren(), path);
            navigateTo(path);
        }
    }

    /**
     * Event handler for ObjEntity and DbEntity additions. Adds a tree node for the entity
     * and make it selected.
     */
    private void entityAdded(Entity<?, ?, ?> entity) {

        DefaultMutableTreeNode mapNode = getProjectModel().getNodeForObjectPath(
                session.project().getRootNode(),
                entity.getDataMap());

        if (mapNode == null) {
            return;
        }

        DefaultMutableTreeNode currentNode = new DefaultMutableTreeNode(entity, false);
        positionNode(mapNode, currentNode, ProjectComparators.forDataMapChildren());
        navigateTo(currentNode);
    }

    /**
     * Event handler for ObjEntity and DbEntity removals. Removes a tree node for the
     * entity and selects its sibling.
     */
    private void entityRemoved(Entity<?, ?, ?> entity, Object source) {
        if (source == this) {
            return;
        }

        // remove from DataMap tree
        removeNode(session.project().getRootNode(), entity.getDataMap(), entity);
    }

    @Override
    public void embeddableAdded(EmbeddableEvent e, DataMap map) {
        Embeddable embeddable = e.getEmbeddable();

        DefaultMutableTreeNode mapNode = getProjectModel().getNodeForObjectPath(
                session.project().getRootNode(),
                map);

        if (mapNode == null) {
            return;
        }

        DefaultMutableTreeNode currentNode = new DefaultMutableTreeNode(embeddable, false);
        positionNode(mapNode, currentNode, ProjectComparators.forDataMapChildren());
        navigateTo(currentNode);
    }

    @Override
    public void embeddableChanged(EmbeddableEvent e, DataMap map) {
        if (e.isNameChange()) {
            Object[] path = new Object[]{
                    session.project().getRootNode(),
                    map, e.getEmbeddable()
            };

            updateNode(path);
            positionNode(ProjectComparators.forDataMapChildren(), path);
            navigateTo(path);
        }
    }

    @Override
    public void embeddableRemoved(EmbeddableEvent e, DataMap map) {
        if (e.getSource() == this) {
            return;
        }
        removeNode(session.project().getRootNode(), map, e.getEmbeddable());
    }

    /**
     * Processes node selection regardless of whether a new node was selected, or an
     * already selected node was clicked again.
     */
    private void processSelection(TreePath path) {
        if (path == null) {
            return;
        }

        DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) path.getLastPathComponent();

        Object[] data = getUserObjects(currentNode);
        if (data.length == 0) {
            // this should clear the right-side panel
            session.displayDomain(new DomainDisplayEvent(
                    this,
                    (DataChannelDescriptor) session.project().getRootNode()));
            return;
        }

        DataChannelDescriptor domain = (DataChannelDescriptor) session.project().getRootNode();
        Object obj = data[data.length - 1];
        if (obj instanceof DataChannelDescriptor dcd) {
            session.displayDomain(new DomainDisplayEvent(this, dcd));
        } else if (obj instanceof DataMap dm) {
            if (data.length == 2) {
                session.displayDataMap(new DataMapDisplayEvent(
                        this,
                        domain,
                        dm,
                        (DataNodeDescriptor) data[data.length - 2]));
            } else if (data.length == 1) {
                session.displayDataMap(new DataMapDisplayEvent(
                        this,
                        domain,
                        dm));
            }
        } else if (obj instanceof DataNodeDescriptor dnd) {
            if (data.length == 1) {
                session.displayDataNode(new DataNodeDisplayEvent(
                        this,
                        domain,
                        dnd));
            }
        } else if (obj instanceof ObjEntity oe) {
            session.displayObjEntity(new ObjEntityDisplayEvent(
                    this,
                    domain,
                    (DataMap) data[data.length - 2],
                    oe,
                    false,
                    true));
        } else if (obj instanceof DbEntity de) {
            session.displayDbEntity(new DbEntityDisplayEvent(
                    this,
                    domain,
                    (DataMap) data[data.length - 2],
                    de,
                    false,
                    true));
        } else if (obj instanceof Embeddable emb) {
            session.displayEmbeddable(new EmbeddableDisplayEvent(
                    this,
                    domain,
                    (DataMap) data[data.length - 2],
                    emb));
        } else if (obj instanceof Procedure proc) {
            session.displayProcedure(new ProcedureDisplayEvent(
                    this,
                    domain,
                    (DataMap) data[data.length - 2],
                    proc));
        } else if (obj instanceof QueryDescriptor qd) {
            session.displayQuery(new QueryDisplayEvent(
                    this,
                    domain,
                    (DataMap) data[data.length - 2],
                    qd));
        }

        scrollPathToVisible(path);
    }

    /**
     * Returns array of the user objects ending with this and starting with one under
     * root. That is the array of actual objects rather than wrappers.
     */
    private Object[] getUserObjects(DefaultMutableTreeNode node) {
        List<Object> list = new ArrayList<>();
        while (!node.isRoot()) {
            list.add(0, node.getUserObject());
            node = (DefaultMutableTreeNode) node.getParent();
        }
        return list.toArray();
    }

    private void navigateTo(Object... path) {
        if (path == null) {
            return;
        }

        DefaultMutableTreeNode node = getProjectModel().getNodeForObjectPath(path);
        if (node == null) {
            return;
        }
        navigateTo(node);
    }

    private void updateNode(Object... path) {
        if (path == null) {
            return;
        }

        DefaultMutableTreeNode node = getProjectModel().getNodeForObjectPath(path);
        if (node != null) {
            updateNode(node);
        }
    }

    private void removeNode(Object... path) {
        if (path == null) {
            return;
        }

        DefaultMutableTreeNode node = getProjectModel().getNodeForObjectPath(path);
        if (node != null) {
            removeNode(node);
        }
    }

    private void positionNode(Comparator<ConfigurationNode> comparator, Object... path) {
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
            Comparator<ConfigurationNode> comparator) {

        removeTreeSelectionListener(treeSelectionListener);
        try {
            getProjectModel().positionNode(parent, treeNode, comparator);
        } finally {
            addTreeSelectionListener(treeSelectionListener);
        }
    }

    /**
     * Creates JPopupMenu containing main functions.
     */
    private JPopupMenu createJPopupMenu() {
        JPopupMenu popup = new JPopupMenu();

        popup.add(buildMenu(CreateNodeAction.class));
        popup.add(buildMenu(CreateDataMapAction.class));
        popup.add(buildMenu(CreateObjEntityAction.class));
        popup.add(buildMenu(CreateEmbeddableAction.class));
        popup.add(buildMenu(CreateDbEntityAction.class));
        popup.add(buildMenu(CreateProcedureAction.class));
        popup.add(buildMenu(CreateQueryAction.class));
        popup.addSeparator();
        popup.add(buildMenu(ObjEntitySyncAction.class));
        popup.add(buildMenu(DbEntitySyncAction.class));
        popup.add(buildMenu(LinkDataMapsAction.class));
        popup.add(buildMenu(RemoveAction.class));
        popup.add(buildMenu(CutAction.class));
        popup.add(buildMenu(CopyAction.class));
        popup.add(buildMenu(PasteAction.class));

        return popup;
    }

    /**
     * Creates and returns a menu item associated with the given action type.
     */
    private JMenuItem buildMenu(Class<? extends Action> actionType) {
        AppAction action = (AppAction) session.app()
                .getActionManager()
                .getAction(actionType);
        return action.buildMenu();
    }

    // Filter all disabled actions in popupMenu, but skip Cut-Copy-Paste block. It should always exist.
    private void popupMenuFilter() {
        Action cutAction = session.app().getActionManager().getAction(CutAction.class);
        for (MenuElement element : popup.getSubElements()) {
            JMenuItem item = (JMenuItem) element;
            if (!item.getAction().equals(cutAction)) {
                item.setVisible(item.isEnabled());
            } else {
                break;
            }
        }
    }

    /**
     * Handles right-click for popup display and left-click row selection based on the full
     * row length (instead of the default selection based on label bounds).
     */
    private class MouseClickHandler extends MouseAdapter {

        void selectRowForEvent(MouseEvent e) {
            int closestRow = getClosestRowForLocation(e.getX(), e.getY());
            Rectangle closestRowBounds = getRowBounds(closestRow);
            if (e.getY() >= closestRowBounds.getY()
                    && e.getY() < closestRowBounds.getY() + closestRowBounds.getHeight()
                    && !isRowSelected(closestRow)) {
                setSelectionRow(closestRow);
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                selectRowForEvent(e);
            }
            mouseReleased(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                selectRowForEvent(e);
                if (popup == null) {
                    popup = createJPopupMenu();
                }
                popupMenuFilter();
                popup.show(ProjectTree.this, e.getX(), e.getY());
            }
        }
    }
}
