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
import org.apache.cayenne.map.event.DbEntityListener;
import org.apache.cayenne.map.event.EmbeddableEvent;
import org.apache.cayenne.map.event.EmbeddableListener;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.ObjEntityListener;
import org.apache.cayenne.modeler.Application;
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
import org.apache.cayenne.modeler.ui.action.ObjEntitySyncAction;
import org.apache.cayenne.modeler.ui.action.PasteAction;
import org.apache.cayenne.modeler.ui.action.RemoveAction;
import org.apache.cayenne.modeler.event.display.DataMapDisplayEvent;
import org.apache.cayenne.modeler.event.display.DataMapDisplayListener;
import org.apache.cayenne.modeler.event.display.DataNodeDisplayEvent;
import org.apache.cayenne.modeler.event.display.DataNodeDisplayListener;
import org.apache.cayenne.modeler.event.display.DbEntityDisplayListener;
import org.apache.cayenne.modeler.event.display.DomainDisplayEvent;
import org.apache.cayenne.modeler.event.display.DomainDisplayListener;
import org.apache.cayenne.modeler.event.display.EmbeddableDisplayEvent;
import org.apache.cayenne.modeler.event.display.EmbeddableDisplayListener;
import org.apache.cayenne.modeler.event.display.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.display.MultipleObjectsDisplayEvent;
import org.apache.cayenne.modeler.event.display.MultipleObjectsDisplayListener;
import org.apache.cayenne.modeler.event.display.ObjEntityDisplayListener;
import org.apache.cayenne.modeler.event.display.ProcedureDisplayEvent;
import org.apache.cayenne.modeler.event.display.ProcedureDisplayListener;
import org.apache.cayenne.modeler.event.display.QueryDisplayEvent;
import org.apache.cayenne.modeler.event.display.QueryDisplayListener;
import org.apache.cayenne.modeler.event.model.DataMapEvent;
import org.apache.cayenne.modeler.event.model.DataMapListener;
import org.apache.cayenne.modeler.event.model.DataNodeEvent;
import org.apache.cayenne.modeler.event.model.DataNodeListener;
import org.apache.cayenne.modeler.event.model.DomainEvent;
import org.apache.cayenne.modeler.event.model.DomainListener;
import org.apache.cayenne.modeler.event.model.ProcedureEvent;
import org.apache.cayenne.modeler.event.model.ProcedureListener;
import org.apache.cayenne.modeler.event.model.QueryEvent;
import org.apache.cayenne.modeler.event.model.QueryListener;
import org.apache.cayenne.modeler.mvc.ChildController;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.ui.action.ModelerAbstractAction;
import org.apache.cayenne.modeler.util.Comparators;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.MenuElement;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
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
 * Controller for the project navigation tree. Owns the {@link ProjectTreeView} and translates
 * between project events and tree state.
 */
public class ProjectTreeController extends ChildController<ProjectController>
        implements DomainDisplayListener, DomainListener, DataMapDisplayListener,
        DataMapListener, DataNodeDisplayListener, DataNodeListener, ObjEntityListener,
        ObjEntityDisplayListener, DbEntityListener, DbEntityDisplayListener, QueryListener,
        QueryDisplayListener, ProcedureListener, ProcedureDisplayListener,
        MultipleObjectsDisplayListener, EmbeddableDisplayListener, EmbeddableListener {

    private final ProjectTreeView view;
    private final TreeSelectionListener treeSelectionListener;
    private JPopupMenu popup;

    public ProjectTreeController(ProjectController parent) {
        super(parent);

        this.view = new ProjectTreeView();

        // install the tree model from the currently open project
        view.setModel(new ProjectTreeModel(parent.getProject()));

        this.treeSelectionListener = createTreeSelectionListener();
        view.addTreeSelectionListener(treeSelectionListener);
        view.addTreeWillExpandListener(createTreeExpandListener());
        view.addMouseListener(new MouseClickHandler());

        parent.addDomainListener(this);
        parent.addDomainDisplayListener(this);
        parent.addDataNodeListener(this);
        parent.addDataNodeDisplayListener(this);
        parent.addDataMapListener(this);
        parent.addDataMapDisplayListener(this);
        parent.addObjEntityListener(this);
        parent.addObjEntityDisplayListener(this);
        parent.addDbEntityListener(this);
        parent.addDbEntityDisplayListener(this);
        parent.addEmbeddableDisplayListener(this);
        parent.addEmbeddableListener(this);
        parent.addProcedureListener(this);
        parent.addProcedureDisplayListener(this);
        parent.addQueryListener(this);
        parent.addQueryDisplayListener(this);
        parent.addMultipleObjectsDisplayListener(this);

        parent.getApplication().getActionManager().setupCutCopyPaste(
                view,
                CutAction.class,
                CopyAction.class);

        DragSource dragSource = new DragSource();
        Toolkit.getDefaultToolkit().createDragGestureRecognizer(
                MouseDragGestureRecognizer.class,
                dragSource,
                view,
                DnDConstants.ACTION_COPY,
                new TreeDragSource(dragSource, view, parent));
    }

    @Override
    public ProjectTreeView getView() {
        return view;
    }

    private TreeSelectionListener createTreeSelectionListener() {
        return new TreeSelectionListener() {

            @Override
            public void valueChanged(TreeSelectionEvent e) {
                TreePath[] paths = view.getSelectionPaths();

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

                        parent.displayMultipleObjects(new MultipleObjectsDisplayEvent(
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
                if (!view.isPathSelected(path) && !view.isSelectionEmpty()) {
                    view.setSelectionPath(path);
                }
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent e) {
                TreePath path = e.getPath();
                if (!view.isPathSelected(path) && !view.isSelectionEmpty()) {
                    view.setSelectionPath(path);
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
    public void objEntitySelected(EntityDisplayEvent e) {
        navigateTo(e.getDomain(), e.getDataMap(), e.getEntity());
    }

    @Override
    public void dbEntitySelected(EntityDisplayEvent e) {
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
            DefaultMutableTreeNode treeNode = view.getProjectModel().getNodeForObjectPath(new Object[]{e.getParentNode(), nodes[i]});
            if (treeNode != null) {
                treePaths[i] = new TreePath(treeNode.getPath());
            } else if (e.getParentNode() == nodes[i]) {
                treeNode = view.getProjectModel().getNodeForObjectPath(new Object[]{e.getParentNode()});
                treePaths[i] = new TreePath(treeNode.getPath());
            }
        }

        if (!view.isVisible(treePaths[0])) {
            view.makeVisible(treePaths[0]);

            Rectangle bounds = view.getPathBounds(treePaths[0]);
            if (bounds != null) {
                bounds.height = view.getVisibleRect().height;
                view.scrollRectToVisible(bounds);
            }
        }

        view.setSelectionPaths(treePaths);
    }

    @Override
    public void embeddableSelected(EmbeddableDisplayEvent e) {
        navigateTo(e.getDomain(), e.getDataMap(), e.getEmbeddable());
    }

    @Override
    public void procedureAdded(ProcedureEvent e) {

        DefaultMutableTreeNode node = view.getProjectModel().getNodeForObjectPath(
                new Object[]{
                        parent.getProject().getRootNode(),
                        e.getProcedure().getDataMap()
                });

        if (node == null) {
            return;
        }

        Procedure procedure = e.getProcedure();
        DefaultMutableTreeNode currentNode = new DefaultMutableTreeNode(procedure, false);
        positionNode(node, currentNode, Comparators.forDataMapChildren());
        view.navigateTo(currentNode);
    }

    @Override
    public void procedureChanged(ProcedureEvent e) {
        if (e.isNameChange()) {
            Object[] path = new Object[]{
                    parent.getProject().getRootNode(),
                    e.getProcedure().getDataMap(), e.getProcedure()
            };

            updateNode(path);
            positionNode(path, Comparators.forDataMapChildren());
            navigateTo(path);
        }
    }

    @Override
    public void procedureRemoved(ProcedureEvent e) {
        removeNode(new Object[]{
                parent.getProject().getRootNode(),
                e.getProcedure().getDataMap(), e.getProcedure()
        });
    }

    @Override
    public void queryAdded(QueryEvent e) {

        DefaultMutableTreeNode node = view.getProjectModel().getNodeForObjectPath(
                new Object[]{
                        e.getDomain() != null
                                ? e.getDomain()
                                : (DataChannelDescriptor) parent
                                .getProject()
                                .getRootNode(), e.getDataMap()
                });

        if (node == null) {
            return;
        }

        QueryDescriptor query = e.getQuery();
        DefaultMutableTreeNode currentNode = new DefaultMutableTreeNode(query, false);
        positionNode(node, currentNode, Comparators.forDataMapChildren());
        view.navigateTo(currentNode);
    }

    @Override
    public void queryChanged(QueryEvent e) {

        if (e.isNameChange()) {
            Object[] path = new Object[]{
                    e.getDomain() != null
                            ? e.getDomain()
                            : (DataChannelDescriptor) parent.getProject().getRootNode(),
                    e.getQuery().getDataMap(), e.getQuery()
            };

            updateNode(path);
            positionNode(path, Comparators.forDataMapChildren());
            navigateTo(path);
        }
    }

    @Override
    public void queryRemoved(QueryEvent e) {
        removeNode(new Object[]{
                e.getDomain() != null ? e.getDomain() : (DataChannelDescriptor) parent
                        .getProject()
                        .getRootNode(), e.getDataMap(), e.getQuery()
        });
    }

    @Override
    public void domainChanged(DomainEvent e) {

        Object[] path = new Object[]{
                e.getDomain()
        };

        updateNode(path);

        if (e.isNameChange()) {
            positionNode(path, Comparators.forNamedObjects());
            navigateTo(path);
        }
    }

    @Override
    public void dataNodeChanged(DataNodeEvent e) {

        DefaultMutableTreeNode node = view.getProjectModel().getNodeForObjectPath(
                new Object[]{
                        e.getDomain() != null
                                ? e.getDomain()
                                : (DataChannelDescriptor) parent
                                .getProject()
                                .getRootNode(), e.getDataNode()
                });

        if (node != null) {

            if (e.isNameChange()) {
                positionNode((DefaultMutableTreeNode) node.getParent(), node,
                        Comparators.forDataDomainChildren());
                view.navigateTo(node);
            } else {

                view.getProjectModel().nodeChanged(node);

                DataChannelDescriptor domain = (DataChannelDescriptor) parent.getProject().getRootNode();

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
                            positionNode(node, newMapNode, Comparators.forNamedObjects());
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
                            view.removeNode(child);
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

        DefaultMutableTreeNode node = view.getProjectModel().getNodeForObjectPath(
                new Object[]{
                        e.getDomain() != null
                                ? e.getDomain()
                                : (DataChannelDescriptor) parent.getProject().getRootNode()
                });

        if (node == null) {
            return;
        }

        DataNodeDescriptor dataNode = e.getDataNode();
        DefaultMutableTreeNode currentNode = ProjectTreeFactory.wrapProjectNode(dataNode);
        positionNode(node, currentNode, Comparators.forDataDomainChildren());
        view.navigateTo(currentNode);
    }

    @Override
    public void dataNodeRemoved(DataNodeEvent e) {
        if (e.getSource() == this) {
            return;
        }

        removeNode(new Object[]{
                e.getDomain() != null ? e.getDomain() : (DataChannelDescriptor) parent
                        .getProject()
                        .getRootNode(), e.getDataNode()
        });
    }

    @Override
    public void dataMapChanged(DataMapEvent e) {

        Object[] path = new Object[]{
                e.getDomain() != null ? e.getDomain() : (DataChannelDescriptor) parent
                        .getProject()
                        .getRootNode(), e.getDataMap()
        };

        updateNode(path);

        if (e.isNameChange()) {
            parent.updateEntityResolver();
            positionNode(path, Comparators.forDataDomainChildren());
            navigateTo(path);
        }
    }

    @Override
    public void dataMapAdded(DataMapEvent e) {
        DataChannelDescriptor dataChannelDescriptor = e.getDomain() != null ? e.getDomain() :
                (DataChannelDescriptor) parent.getProject().getRootNode();
        DefaultMutableTreeNode domainNode = view.getProjectModel().getNodeForObjectPath(
                new Object[]{
                        dataChannelDescriptor
                });

        DefaultMutableTreeNode newMapNode = ProjectTreeFactory.wrapProjectNode(e
                .getDataMap());

        parent.getEntityResolver().addDataMap(e.getDataMap());

        positionNode(domainNode, newMapNode, Comparators.forDataDomainChildren());
        if (Application.getInstance().getFrameController().getDbImportController().isGlobalImport()) {
            view.setSelected(newMapNode);
        } else {
            view.navigateTo(newMapNode);
        }

        for (DataNodeDescriptor dataNode : new ArrayList<>(dataChannelDescriptor.getNodeDescriptors())) {
            for (String dataMapName : dataNode.getDataMapNames()) {
                if (e.getDataMap().getName().equals(dataMapName)) {
                    parent.fireDataNodeEvent(new DataNodeEvent(this, dataNode));
                }
            }
        }
    }

    @Override
    public void dataMapRemoved(DataMapEvent e) {
        DataMap map = e.getDataMap();
        DataChannelDescriptor dataChannelDescriptor = (DataChannelDescriptor) Application
                .getInstance()
                .getProject()
                .getRootNode();
        removeNode(new Object[]{
                dataChannelDescriptor, map
        });

        parent.getEntityResolver().removeDataMap(e.getDataMap());

        // Clean up map from the nodes
        for (DataNodeDescriptor dataNode : new ArrayList<>(dataChannelDescriptor.getNodeDescriptors())) {
            removeNode(new Object[]{
                    dataChannelDescriptor, dataNode, map
            });
        }
    }

    @Override
    public void objEntityChanged(EntityEvent e) {
        entityChanged(e);
    }

    @Override
    public void objEntityAdded(EntityEvent e) {
        entityAdded(e);
    }

    @Override
    public void objEntityRemoved(EntityEvent e) {
        entityRemoved(e);
    }

    @Override
    public void dbEntityChanged(EntityEvent e) {
        entityChanged(e);
    }

    @Override
    public void dbEntityAdded(EntityEvent e) {
        entityAdded(e);
    }

    @Override
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
    private void entityChanged(EntityEvent e) {
        if (e.isNameChange()) {
            Object[] path = new Object[]{
                    e.getDomain() != null
                            ? e.getDomain()
                            : (DataChannelDescriptor) parent.getProject().getRootNode(),
                    e.getEntity().getDataMap(), e.getEntity()
            };

            updateNode(path);
            positionNode(path, Comparators.forDataMapChildren());
            navigateTo(path);
        }
    }

    /**
     * Event handler for ObjEntity and DbEntity additions. Adds a tree node for the entity
     * and make it selected.
     */
    private void entityAdded(EntityEvent e) {

        Entity<?, ?, ?> entity = e.getEntity();

        DefaultMutableTreeNode mapNode = view.getProjectModel().getNodeForObjectPath(
                new Object[]{
                        e.getDomain() != null
                                ? e.getDomain()
                                : (DataChannelDescriptor) parent
                                .getProject()
                                .getRootNode(), e.getEntity().getDataMap()
                });

        if (mapNode == null) {
            return;
        }

        DefaultMutableTreeNode currentNode = new DefaultMutableTreeNode(entity, false);
        positionNode(mapNode, currentNode, Comparators.forDataMapChildren());
        view.navigateTo(currentNode);
    }

    /**
     * Event handler for ObjEntity and DbEntity removals. Removes a tree node for the
     * entity and selects its sibling.
     */
    private void entityRemoved(EntityEvent e) {
        if (e.getSource() == this) {
            return;
        }

        // remove from DataMap tree
        removeNode(new Object[]{
                e.getDomain() != null ? e.getDomain() : (DataChannelDescriptor) parent
                        .getProject()
                        .getRootNode(), e.getEntity().getDataMap(), e.getEntity()
        });
    }

    @Override
    public void embeddableAdded(EmbeddableEvent e, DataMap map) {
        Embeddable embeddable = e.getEmbeddable();

        DefaultMutableTreeNode mapNode = view.getProjectModel().getNodeForObjectPath(
                new Object[]{
                        e.getDomain() != null
                                ? e.getDomain()
                                : (DataChannelDescriptor) parent
                                .getProject()
                                .getRootNode(), map
                });

        if (mapNode == null) {
            return;
        }

        DefaultMutableTreeNode currentNode = new DefaultMutableTreeNode(embeddable, false);
        positionNode(mapNode, currentNode, Comparators.forDataMapChildren());
        view.navigateTo(currentNode);
    }

    @Override
    public void embeddableChanged(EmbeddableEvent e, DataMap map) {
        if (e.isNameChange()) {
            Object[] path = new Object[]{
                    e.getDomain() != null
                            ? e.getDomain()
                            : (DataChannelDescriptor) parent.getProject().getRootNode(),
                    map, e.getEmbeddable()
            };

            updateNode(path);
            positionNode(path, Comparators.forDataMapChildren());
            navigateTo(path);
        }
    }

    @Override
    public void embeddableRemoved(EmbeddableEvent e, DataMap map) {
        if (e.getSource() == this) {
            return;
        }

        // remove from DataMap tree
        removeNode(new Object[]{
                e.getDomain() != null ? e.getDomain() : (DataChannelDescriptor) parent
                        .getProject()
                        .getRootNode(), map, e.getEmbeddable()
        });
    }

    /**
     * Processes node selection regardless of whether a new node was selected, or an
     * already selected node was clicked again. Normally called from event listener
     * methods.
     */
    private void processSelection(TreePath path) {
        if (path == null) {
            return;
        }

        DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) path.getLastPathComponent();

        Object[] data = getUserObjects(currentNode);
        if (data.length == 0) {
            // this should clear the right-side panel
            parent.displayDomain(new DomainDisplayEvent(
                    this,
                    (DataChannelDescriptor) parent.getProject().getRootNode()));

            return;
        }

        Object obj = data[data.length - 1];
        if (obj instanceof DataChannelDescriptor) {
            parent.displayDomain(new DomainDisplayEvent(
                    this,
                    (DataChannelDescriptor) obj));
        } else if (obj instanceof DataMap) {
            if (data.length == 2) {
                parent.displayDataMap(new DataMapDisplayEvent(
                        this,
                        (DataMap) obj,
                        (DataChannelDescriptor) parent.getProject().getRootNode(),
                        (DataNodeDescriptor) data[data.length - 2]));
            } else if (data.length == 1) {
                parent.displayDataMap(new DataMapDisplayEvent(
                        this,
                        (DataMap) obj,
                        (DataChannelDescriptor) parent.getProject().getRootNode()));
            }
        } else if (obj instanceof DataNodeDescriptor) {
            if (data.length == 1) {
                parent.displayDataNode(new DataNodeDisplayEvent(
                        this,
                        (DataChannelDescriptor) parent.getProject().getRootNode(),
                        (DataNodeDescriptor) obj));
            }
        } else if (obj instanceof Entity) {

            EntityDisplayEvent e = (data.length == 3)

                    ? new EntityDisplayEvent(
                    this,
                    (Entity) obj, (DataMap) data[data.length - 2],
                    (DataNodeDescriptor) data[data.length - 3],
                    (DataChannelDescriptor) parent.getProject().getRootNode())

                    : new EntityDisplayEvent(
                    this,
                    (Entity) obj, (DataMap) data[data.length - 2],
                    (DataChannelDescriptor) parent.getProject().getRootNode());


            e.setUnselectAttributes(true);

            if (obj instanceof ObjEntity) {
                parent.displayObjEntity(e);
            } else if (obj instanceof DbEntity) {
                parent.displayDbEntity(e);
            }
        } else if (obj instanceof Embeddable) {
            EmbeddableDisplayEvent e = new EmbeddableDisplayEvent(
                    this,
                    (Embeddable) obj,
                    (DataMap) data[data.length - 2],
                    (DataChannelDescriptor) parent.getProject().getRootNode());
            parent.displayEmbeddable(e);
        } else if (obj instanceof Procedure) {
            ProcedureDisplayEvent e = new ProcedureDisplayEvent(
                    this,
                    (Procedure) obj,
                    (DataMap) data[data.length - 2],
                    (DataChannelDescriptor) parent.getProject().getRootNode());
            parent.displayProcedure(e);
        } else if (obj instanceof QueryDescriptor) {
            QueryDisplayEvent e = new QueryDisplayEvent(
                    this,
                    (QueryDescriptor) obj,
                    (DataMap) data[data.length - 2],
                    (DataChannelDescriptor) parent.getProject().getRootNode());
            parent.displayQuery(e);
        }

        view.scrollPathToVisible(path);
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

        DefaultMutableTreeNode node = view.getProjectModel().getNodeForObjectPath(path);

        if (node == null) {
            return;
        }

        view.navigateTo(node);
    }

    private void updateNode(Object[] path) {
        if (path == null) {
            return;
        }

        DefaultMutableTreeNode node = view.getProjectModel().getNodeForObjectPath(path);
        if (node != null) {
            view.updateNode(node);
        }
    }

    private void removeNode(Object[] path) {
        if (path == null) {
            return;
        }

        DefaultMutableTreeNode node = view.getProjectModel().getNodeForObjectPath(path);
        if (node != null) {
            view.removeNode(node);
        }
    }

    private void positionNode(Object[] path, Comparator<ConfigurationNode> comparator) {
        if (path == null) {
            return;
        }

        DefaultMutableTreeNode node = view.getProjectModel().getNodeForObjectPath(path);
        if (node == null) {
            return;
        }

        positionNode(null, node, comparator);
    }

    private void positionNode(
            MutableTreeNode parent,
            DefaultMutableTreeNode treeNode,
            Comparator<ConfigurationNode> comparator) {

        view.removeTreeSelectionListener(treeSelectionListener);
        try {
            view.getProjectModel().positionNode(parent, treeNode, comparator);
        } finally {
            view.addTreeSelectionListener(treeSelectionListener);
        }
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
     * Creates and returns a menu item associated with the given action type.
     */
    private JMenuItem buildMenu(Class<? extends Action> actionType) {
        ModelerAbstractAction action = (ModelerAbstractAction) parent
                .getApplication()
                .getActionManager()
                .getAction(actionType);
        return action.buildMenu();
    }

    // Filter all disabled actions in popupMenu, but skip Cut-Copy-Paste block. It should always exist.
    private void popupMenuFilter() {
        Action cutAction = parent.getApplication().getActionManager().getAction(CutAction.class);
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
            int closestRow = view.getClosestRowForLocation(e.getX(), e.getY());
            Rectangle closestRowBounds = view.getRowBounds(closestRow);
            if (e.getY() >= closestRowBounds.getY()
                    && e.getY() < closestRowBounds.getY() + closestRowBounds.getHeight()
                    && !view.isRowSelected(closestRow)) {
                view.setSelectionRow(closestRow);
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
                popup.show(view, e.getX(), e.getY());
            }
        }
    }
}
