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

package org.apache.cayenne.modeler;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.event.DataMapEvent;
import org.apache.cayenne.configuration.event.DataMapListener;
import org.apache.cayenne.configuration.event.DataNodeEvent;
import org.apache.cayenne.configuration.event.DataNodeListener;
import org.apache.cayenne.configuration.event.DomainEvent;
import org.apache.cayenne.configuration.event.DomainListener;
import org.apache.cayenne.configuration.event.ProcedureEvent;
import org.apache.cayenne.configuration.event.ProcedureListener;
import org.apache.cayenne.configuration.event.QueryEvent;
import org.apache.cayenne.configuration.event.QueryListener;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.event.DbEntityListener;
import org.apache.cayenne.map.event.EmbeddableEvent;
import org.apache.cayenne.map.event.EmbeddableListener;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.ObjEntityListener;
import org.apache.cayenne.modeler.action.CopyAction;
import org.apache.cayenne.modeler.action.CreateDataMapAction;
import org.apache.cayenne.modeler.action.CreateDbEntityAction;
import org.apache.cayenne.modeler.action.CreateEmbeddableAction;
import org.apache.cayenne.modeler.action.CreateNodeAction;
import org.apache.cayenne.modeler.action.CreateObjEntityAction;
import org.apache.cayenne.modeler.action.CreateProcedureAction;
import org.apache.cayenne.modeler.action.CreateQueryAction;
import org.apache.cayenne.modeler.action.CutAction;
import org.apache.cayenne.modeler.action.DbEntitySyncAction;
import org.apache.cayenne.modeler.action.LinkDataMapsAction;
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
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.modeler.util.CellRenderers;
import org.apache.cayenne.modeler.util.Comparators;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.reflect.PropertyUtils;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.swing.components.TopBorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.MenuElement;
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
import java.awt.dnd.DnDConstants;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Panel displaying Cayenne project as a tree.
 */
public class ProjectTreeView extends JTree implements DomainDisplayListener,
        DomainListener, DataMapDisplayListener, DataMapListener, DataNodeDisplayListener,
        DataNodeListener, ObjEntityListener, ObjEntityDisplayListener, DbEntityListener,
        DbEntityDisplayListener, QueryListener, QueryDisplayListener, ProcedureListener,
        ProcedureDisplayListener, MultipleObjectsDisplayListener,
        EmbeddableDisplayListener, EmbeddableListener {

    private static final Logger logObj = LoggerFactory.getLogger(ProjectTreeView.class);

    private static final Color SELECTION_COLOR = UIManager.getColor("Tree.selectionBackground");

    protected ProjectController mediator;
    protected TreeSelectionListener treeSelectionListener;
    protected TreeWillExpandListener treeWillExpandListener;
    protected JPopupMenu popup;
    private TreeDragSource tds;

    public ProjectTreeView(ProjectController mediator) {
        super();
        this.mediator = mediator;

        initView();
        initController();
        initFromModel(Application.getInstance().getProject());
        this.tds = new TreeDragSource(this, DnDConstants.ACTION_COPY, mediator);
    }

    private void initView() {
        setCellRenderer(CellRenderers.treeRenderer());
        setOpaque(false);
        setBorder(TopBorder.create());
    }

    private void initController() {
        initTreeSelectionListener();
        initTreeExpandListener();
        addMouseListener(new MouseClickHandler());
        setupMediator();
    }

	private void initTreeSelectionListener() {
		treeSelectionListener = new TreeSelectionListener() {

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
                            if(i>0 && parentPath != null && !parentPath.equals(paths[i - 1].getParentPath())) {
                                commonParentPath = false;
                            }
                        }

                        if(commonParentPath) {
                            TreePath parentPath = paths[0].getParentPath();
                            projectParentPath = createProjectPath(parentPath);
                        }

                        mediator.fireMultipleObjectsDisplayEvent(new MultipleObjectsDisplayEvent(
                                        this,
                                        projectPaths, projectParentPath));
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
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)path[path.length - 1];
                return (ConfigurationNode) treeNode.getUserObject();
            }
        };

        addTreeSelectionListener(treeSelectionListener);
	}

	private void initTreeExpandListener() {
        treeWillExpandListener = new TreeWillExpandListener() {
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


        addTreeWillExpandListener(treeWillExpandListener);
    }

	private void setupMediator() {
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
        mediator.addMultipleObjectsDisplayListener(this);
        mediator.getApplication().getActionManager().setupCutCopyPaste(
                this,
                CutAction.class,
                CopyAction.class);
	}

    private void initFromModel(Project project) {

        // build model
        ProjectTreeModel model = new ProjectTreeModel(project);
        setRootVisible(true);
        setModel(model);
        getSelectionModel().setSelectionMode(
                TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
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
        while (value instanceof DefaultMutableTreeNode) {
            value = ((DefaultMutableTreeNode) value).getUserObject();
        }

        // String - just return it
        if (value instanceof String) {
            return value.toString();
        }

        // Project - return the name of top file
        if (value instanceof Project) {
            Resource resource = ((Project) value).getConfigurationResource();
            return (resource != null) ? resource.getURL().getPath() : "";
        }

        // read name property
        try {
            if (value instanceof Embeddable) {
                return String.valueOf(PropertyUtils.getProperty(value, "className"));
            }

            return (value != null) ? String.valueOf(PropertyUtils.getProperty(value, "name")) : "";
        } catch (Exception e) {
            logObj.warn("Exception reading property 'name', class " + value.getClass().getName(), e);
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

    public void currentObjectsChanged(MultipleObjectsDisplayEvent e, Application application) {
        if (e.getSource() == this || e.getParentNode() == null) {
            return;
        }

        ConfigurationNode[] nodes = e.getNodes();
        TreePath[] treePaths = new TreePath[nodes.length];

        for (int i = 0; i < nodes.length; i++) {
            DefaultMutableTreeNode treeNode = getProjectModel().getNodeForObjectPath(new Object[] {e.getParentNode(), nodes[i]});
            if (treeNode != null) {
                treePaths[i] = new TreePath(treeNode.getPath());
            } else if (e.getParentNode() == nodes[i]) {
                treeNode = getProjectModel().getNodeForObjectPath(new Object[] {e.getParentNode()});
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

    public void procedureAdded(ProcedureEvent e) {

        DefaultMutableTreeNode node = getProjectModel().getNodeForObjectPath(
                new Object[] {
                        mediator.getProject().getRootNode(),
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
                    mediator.getProject().getRootNode(),
                    e.getProcedure().getDataMap(), e.getProcedure()
            };

            updateNode(path);
            positionNode(path, Comparators.getDataMapChildrenComparator());
            showNode(path);
        }
    }

    public void procedureRemoved(ProcedureEvent e) {

        removeNode(new Object[] {
                mediator.getProject().getRootNode(),
                e.getProcedure().getDataMap(), e.getProcedure()
        });
    }

    public void queryAdded(QueryEvent e) {

        DefaultMutableTreeNode node = getProjectModel().getNodeForObjectPath(
                new Object[] {
                        e.getDomain() != null
                                ? e.getDomain()
                                : (DataChannelDescriptor) mediator
                                        .getProject()
                                        .getRootNode(), e.getDataMap()
                });

        if (node == null) {
            return;
        }

        QueryDescriptor query = e.getQuery();
        DefaultMutableTreeNode currentNode = new DefaultMutableTreeNode(query, false);
        positionNode(node, currentNode, Comparators.getDataMapChildrenComparator());
        showNode(currentNode);
    }

    public void queryChanged(QueryEvent e) {

        if (e.isNameChange()) {
            Object[] path = new Object[] {
                    e.getDomain() != null
                            ? e.getDomain()
                            : (DataChannelDescriptor) mediator.getProject().getRootNode(),
                    e.getQuery().getDataMap(), e.getQuery()
            };

            updateNode(path);
            positionNode(path, Comparators.getDataMapChildrenComparator());
            showNode(path);
        }
    }

    public void queryRemoved(QueryEvent e) {
        removeNode(new Object[] {
                e.getDomain() != null ? e.getDomain() : (DataChannelDescriptor) mediator
                        .getProject()
                        .getRootNode(), e.getDataMap(), e.getQuery()
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

    public void dataNodeChanged(DataNodeEvent e) {

        DefaultMutableTreeNode node = getProjectModel().getNodeForObjectPath(
                new Object[] {
                        e.getDomain() != null
                                ? e.getDomain()
                                : (DataChannelDescriptor) mediator
                                        .getProject()
                                        .getRootNode(), e.getDataNode()
                });

        if (node != null) {

            if (e.isNameChange()) {
                positionNode((DefaultMutableTreeNode) node.getParent(), node,
                        Comparators.getDataDomainChildrenComparator());
                showNode(node);
            } else {

                getProjectModel().nodeChanged(node);

                DataChannelDescriptor domain = (DataChannelDescriptor) mediator.getProject().getRootNode();

                // check for DataMap additions/removals...
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
                            positionNode(node, newMapNode, Comparators.getNamedObjectComparator());
                            break;
                        }
                    }
                } else if (mapCount < node.getChildCount()) {
                    // DataMap was unlinked
                    int j = 0;
                    while (j < node.getChildCount()) {
                        boolean found = false;
                        DefaultMutableTreeNode child;
                        child = (DefaultMutableTreeNode) node.getChildAt(j);
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

    public void dataNodeAdded(DataNodeEvent e) {
        if (e.getSource() == this) {
            return;
        }

        DefaultMutableTreeNode node = getProjectModel().getNodeForObjectPath(
                new Object[] {
                    e.getDomain() != null
                            ? e.getDomain()
                            : (DataChannelDescriptor) mediator.getProject().getRootNode()
                });

        if (node == null) {
            return;
        }

        DataNodeDescriptor dataNode = e.getDataNode();
        DefaultMutableTreeNode currentNode = ProjectTreeFactory.wrapProjectNode(dataNode);
        positionNode(node, currentNode, Comparators.getDataDomainChildrenComparator());
        showNode(currentNode);
    }

    public void dataNodeRemoved(DataNodeEvent e) {
        if (e.getSource() == this) {
            return;
        }

        removeNode(new Object[] {
                e.getDomain() != null ? e.getDomain() : (DataChannelDescriptor) mediator
                        .getProject()
                        .getRootNode(), e.getDataNode()
        });
    }

    public void dataMapChanged(DataMapEvent e) {

        Object[] path = new Object[] {
                e.getDomain() != null ? e.getDomain() : (DataChannelDescriptor) mediator
                        .getProject()
                        .getRootNode(), e.getDataMap()
        };

        updateNode(path);

        if (e.isNameChange()) {
            mediator.updateEntityResolver();
            positionNode(path, Comparators.getDataDomainChildrenComparator());
            showNode(path);
        }
    }

    public void dataMapAdded(DataMapEvent e) {
        DataChannelDescriptor dataChannelDescriptor = e.getDomain() != null ? e.getDomain() :
                (DataChannelDescriptor) mediator.getProject().getRootNode();
        DefaultMutableTreeNode domainNode = getProjectModel().getNodeForObjectPath(
                new Object[] {
                   dataChannelDescriptor
                });

        DefaultMutableTreeNode newMapNode = ProjectTreeFactory.wrapProjectNode(e
                .getDataMap());

        mediator.getEntityResolver().addDataMap(e.getDataMap());

        positionNode(domainNode, newMapNode, Comparators
                .getDataDomainChildrenComparator());
        if(!Application.getInstance().getFrameController().getDbImportController().isGlobalImport()) {
            showNode(newMapNode);
        } else {
            setSelected(newMapNode);
        }

        for (DataNodeDescriptor dataNode : new ArrayList<>(dataChannelDescriptor.getNodeDescriptors())) {
            for(String dataMapName : dataNode.getDataMapNames()) {
                if(e.getDataMap().getName().equals(dataMapName)) {
                    mediator.fireDataNodeEvent(new DataNodeEvent(this, dataNode));
                }
            }
        }
    }

    public void dataMapRemoved(DataMapEvent e) {
        DataMap map = e.getDataMap();
        DataChannelDescriptor dataChannelDescriptor = (DataChannelDescriptor) Application
                .getInstance()
                .getProject()
                .getRootNode();
        removeNode(new Object[] {
                dataChannelDescriptor, map
        });

        mediator.getEntityResolver().removeDataMap(e.getDataMap());

        // Clean up map from the nodes
        for (DataNodeDescriptor dataNode : new ArrayList<>(dataChannelDescriptor.getNodeDescriptors())) {
            removeNode(new Object[] {
                    dataChannelDescriptor, dataNode, map
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
                    e.getDomain() != null
                            ? e.getDomain()
                            : (DataChannelDescriptor) mediator.getProject().getRootNode(),
                    e.getEntity().getDataMap(), e.getEntity()
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
                        e.getDomain() != null
                                ? e.getDomain()
                                : (DataChannelDescriptor) mediator
                                        .getProject()
                                        .getRootNode(), e.getEntity().getDataMap()
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
                e.getDomain() != null ? e.getDomain() : (DataChannelDescriptor) mediator
                        .getProject()
                        .getRootNode(), e.getEntity().getDataMap(), e.getEntity()
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

    /** Makes node current, visible but not selected. */
    private void setSelected(DefaultMutableTreeNode node) {
        TreePath path = new TreePath(node.getPath());
        if(!isVisible(path)) {
            makeVisible(path);
        }
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

        DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) path.getLastPathComponent();

        Object[] data = getUserObjects(currentNode);
        if (data.length == 0) {
            // this should clear the right-side panel
            DomainDisplayEvent domEvent = new DomainDisplayEvent(this, null);
            domEvent.setDomain((DataChannelDescriptor) mediator
                    .getProject()
                    .getRootNode());
            mediator.fireDomainDisplayEvent(domEvent);
            return;
        }

        Object obj = data[data.length - 1];
        if (obj instanceof DataChannelDescriptor) {
            mediator.fireDomainDisplayEvent(new DomainDisplayEvent(
                    this,
                    (DataChannelDescriptor) obj));
        } else if (obj instanceof DataMap) {
            if (data.length == 2) {
                mediator.fireDataMapDisplayEvent(new DataMapDisplayEvent(
                        this,
                        (DataMap) obj,
                        (DataChannelDescriptor) mediator.getProject().getRootNode(),
                        (DataNodeDescriptor) data[data.length - 2]));
            } else if (data.length == 1) {
                mediator.fireDataMapDisplayEvent(new DataMapDisplayEvent(
                        this,
                        (DataMap) obj,
                        (DataChannelDescriptor) mediator.getProject().getRootNode()));
            }
        } else if (obj instanceof DataNodeDescriptor) {
            if (data.length == 1) {
                mediator.fireDataNodeDisplayEvent(new DataNodeDisplayEvent(
                        this,
                        (DataChannelDescriptor) mediator.getProject().getRootNode(),
                        (DataNodeDescriptor) obj));
            }
        } else if (obj instanceof Entity) {
            EntityDisplayEvent e = new EntityDisplayEvent(this, (Entity) obj);
            e.setUnselectAttributes(true);
            if (data.length == 3) {
                e.setDataMap((DataMap) data[data.length - 2]);
                e.setDomain((DataChannelDescriptor) mediator.getProject().getRootNode());
                e.setDataNode((DataNodeDescriptor) data[data.length - 3]);
            } else if (data.length == 2) {
                e.setDataMap((DataMap) data[data.length - 2]);
                e.setDomain((DataChannelDescriptor) mediator.getProject().getRootNode());
            }

            if (obj instanceof ObjEntity) {
                mediator.fireObjEntityDisplayEvent(e);
            } else if (obj instanceof DbEntity) {
                mediator.fireDbEntityDisplayEvent(e);
            }
        } else if (obj instanceof Embeddable) {
            EmbeddableDisplayEvent e = new EmbeddableDisplayEvent(
                    this,
                    (Embeddable) obj,
                    (DataMap) data[data.length - 2],
                    (DataChannelDescriptor) mediator.getProject().getRootNode());
            mediator.fireEmbeddableDisplayEvent(e);
        } else if (obj instanceof Procedure) {
            ProcedureDisplayEvent e = new ProcedureDisplayEvent(
                    this,
                    (Procedure) obj,
                    (DataMap) data[data.length - 2],
                    (DataChannelDescriptor) mediator.getProject().getRootNode());
            mediator.fireProcedureDisplayEvent(e);
        } else if (obj instanceof QueryDescriptor) {
            QueryDisplayEvent e = new QueryDisplayEvent(
                    this,
                    (QueryDescriptor) obj,
                    (DataMap) data[data.length - 2],
                    (DataChannelDescriptor) mediator.getProject().getRootNode());
            mediator.fireQueryDisplayEvent(e);
        }

        this.scrollPathToVisible(path);
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
        } finally {
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
     * Creates and returns an menu item associated with the key.
     * 
     * @param actionType action type
     */
    private JMenuItem buildMenu(Class<? extends Action> actionType) {
        CayenneAction action = (CayenneAction) mediator
                .getApplication()
                .getActionManager()
                .getAction(actionType);
        return action.buildMenu();
    }

    /**
     * Class to handle:
     *  - right-click and show popup for selected tree row
     *  - left click row selection based on full row length (instead of default selection based on label size)
     */
    class MouseClickHandler extends MouseAdapter {

        void selectRowForEvent(MouseEvent e) {
            int closestRow = getClosestRowForLocation(e.getX(), e.getY());
            Rectangle closestRowBounds = getRowBounds(closestRow);
            if(e.getY() >= closestRowBounds.getY()
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
                // Selecting specified row
                selectRowForEvent(e);
                if (popup == null) {
                    popup = createJPopupMenu();
                }
                popupMenuFilter();
                popup.show(ProjectTreeView.this, e.getX(), e.getY());
            }
        }
    }

    public void embeddableAdded(EmbeddableEvent e, DataMap map) {
        Embeddable embeddable = e.getEmbeddable();

        DefaultMutableTreeNode mapNode = getProjectModel().getNodeForObjectPath(
                new Object[] {
                        e.getDomain() != null
                                ? e.getDomain()
                                : (DataChannelDescriptor) mediator
                                        .getProject()
                                        .getRootNode(), map
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
                    e.getDomain() != null
                            ? e.getDomain()
                            : (DataChannelDescriptor) mediator.getProject().getRootNode(),
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
                e.getDomain() != null ? e.getDomain() : (DataChannelDescriptor) mediator
                        .getProject()
                        .getRootNode(), map, e.getEmbeddable()
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

    // Filter all disabled actions in popupMenu, but skip Cut-Copy-Paste block. It should always exist.
    public void popupMenuFilter() {
        Action cutAction = mediator.getApplication().getActionManager().getAction(CutAction.class);
        for (MenuElement element : popup.getSubElements()) {
            JMenuItem item = (JMenuItem) element;
            if (!item.getAction().equals(cutAction)) {
                item.setVisible(item.isEnabled());
            } else {
                break;
            }
        }
    }

    public TreeDragSource getTds() {
        return tds;
    }

    @Override
    public void paintComponent(Graphics g) {
        g.setColor(Color.white);
        g.fillRect(0, 0, getWidth(), getHeight());
        if (getSelectionCount() > 0) {
            g.setColor(SELECTION_COLOR);
            int[] rows = getSelectionRows();
            if(rows != null) {
                for (int i : rows) {
                    Rectangle r = getRowBounds(i);
                    g.fillRect(0, r.y, getWidth(), r.height);
                }
            }
        }
        super.paintComponent(g);
    }
}
