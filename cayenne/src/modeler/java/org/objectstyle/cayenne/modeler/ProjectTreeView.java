/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.modeler;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.map.event.DataMapEvent;
import org.objectstyle.cayenne.map.event.DataMapListener;
import org.objectstyle.cayenne.map.event.DataNodeEvent;
import org.objectstyle.cayenne.map.event.DataNodeListener;
import org.objectstyle.cayenne.map.event.DbEntityListener;
import org.objectstyle.cayenne.map.event.DomainEvent;
import org.objectstyle.cayenne.map.event.DomainListener;
import org.objectstyle.cayenne.map.event.EntityEvent;
import org.objectstyle.cayenne.map.event.ObjEntityListener;
import org.objectstyle.cayenne.map.event.ProcedureEvent;
import org.objectstyle.cayenne.map.event.ProcedureListener;
import org.objectstyle.cayenne.map.event.QueryEvent;
import org.objectstyle.cayenne.map.event.QueryListener;
import org.objectstyle.cayenne.modeler.event.DataMapDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DataMapDisplayListener;
import org.objectstyle.cayenne.modeler.event.DataNodeDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DataNodeDisplayListener;
import org.objectstyle.cayenne.modeler.event.DbEntityDisplayListener;
import org.objectstyle.cayenne.modeler.event.DomainDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DomainDisplayListener;
import org.objectstyle.cayenne.modeler.event.EntityDisplayEvent;
import org.objectstyle.cayenne.modeler.event.ObjEntityDisplayListener;
import org.objectstyle.cayenne.modeler.event.ProcedureDisplayEvent;
import org.objectstyle.cayenne.modeler.event.ProcedureDisplayListener;
import org.objectstyle.cayenne.modeler.event.QueryDisplayEvent;
import org.objectstyle.cayenne.modeler.event.QueryDisplayListener;
import org.objectstyle.cayenne.modeler.util.CellRenderers;
import org.objectstyle.cayenne.modeler.util.Comparators;
import org.objectstyle.cayenne.project.Project;
import org.objectstyle.cayenne.query.Query;

/**
 * Panel displaying Cayenne project as a tree.
 */
public class ProjectTreeView extends JTree implements DomainDisplayListener,
        DomainListener, DataMapDisplayListener, DataMapListener, DataNodeDisplayListener,
        DataNodeListener, ObjEntityListener, ObjEntityDisplayListener, DbEntityListener,
        DbEntityDisplayListener, QueryListener, QueryDisplayListener, ProcedureListener,
        ProcedureDisplayListener {

    private static final Logger logObj = Logger.getLogger(ProjectTreeView.class);

    protected ProjectController mediator;
    protected TreeSelectionListener treeSelectionListener;

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
                processSelection(e.getPath());
            }
        };

        addTreeSelectionListener(treeSelectionListener);

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
        mediator.addProcedureListener(this);
        mediator.addProcedureDisplayListener(this);
        mediator.addQueryListener(this);
        mediator.addQueryDisplayListener(this);
    }

    private void initFromModel(Project project) {
        // build model
        ProjectTreeModel model = new ProjectTreeModel(project);
        setRootVisible(false);
        setModel(model);

        // expand top level
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
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
        if (e.getSource() == this) {
            return;
        }

        showNode(new Object[] {
            e.getDomain()
        });
    }

    public void currentDataNodeChanged(DataNodeDisplayEvent e) {
        if (e.getSource() == this || !e.isDataNodeChanged())
            return;

        showNode(new Object[] {
                e.getDomain(), e.getDataNode()
        });
    }

    public void currentProcedureChanged(ProcedureDisplayEvent e) {
        if (e.getSource() == this || !e.isProcedureChanged())
            return;

        showNode(new Object[] {
                e.getDomain(), e.getDataMap(), e.getProcedure()
        });
    }

    public void currentDataMapChanged(DataMapDisplayEvent e) {
        if (e.getSource() == this || !e.isDataMapChanged())
            return;

        showNode(new Object[] {
                e.getDomain(), e.getDataMap()
        });
    }

    public void currentObjEntityChanged(EntityDisplayEvent e) {
        currentEntityChanged(e);
    }

    public void currentDbEntityChanged(EntityDisplayEvent e) {
        currentEntityChanged(e);
    }

    protected void currentEntityChanged(EntityDisplayEvent e) {
        if (e.getSource() == this || !e.isEntityChanged()) {
            return;
        }
        showNode(new Object[] {
                e.getDomain(), e.getDataMap(), e.getEntity()
        });
    }

    public void procedureAdded(ProcedureEvent e) {

        DefaultMutableTreeNode node = getProjectModel().getNodeForObjectPath(
                new Object[] {
                        mediator.getCurrentDataDomain(), mediator.getCurrentDataMap()
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
                    mediator.getCurrentDataDomain(), mediator.getCurrentDataMap(),
                    e.getProcedure()
            };

            updateNode(path);
            positionNode(path, Comparators.getDataMapChildrenComparator());
            showNode(path);
        }
    }

    public void procedureRemoved(ProcedureEvent e) {

        removeNode(new Object[] {
                mediator.getCurrentDataDomain(), mediator.getCurrentDataMap(),
                e.getProcedure()
        });
    }

    public void queryAdded(QueryEvent e) {

        DefaultMutableTreeNode node = getProjectModel().getNodeForObjectPath(
                new Object[] {
                        mediator.getCurrentDataDomain(), mediator.getCurrentDataMap()
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
                    mediator.getCurrentDataDomain(), mediator.getCurrentDataMap(),
                    e.getQuery()
            };

            updateNode(path);
            positionNode(path, Comparators.getDataMapChildrenComparator());
            showNode(path);
        }
    }

    public void queryRemoved(QueryEvent e) {
        removeNode(new Object[] {
                mediator.getCurrentDataDomain(), mediator.getCurrentDataMap(),
                e.getQuery()
        });
    }

    public void currentQueryChanged(QueryDisplayEvent e) {
        showNode(new Object[] {
                e.getDomain(), e.getDataMap(), e.getQuery()
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
                        mediator.getCurrentDataDomain(), e.getDataNode()
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
                    mediator.getCurrentDataDomain()
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
                mediator.getCurrentDataDomain(), e.getDataNode()
        });
    }

    public void dataMapChanged(DataMapEvent e) {

        Object[] path = new Object[] {
                mediator.getCurrentDataDomain(), e.getDataMap()
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
                    mediator.getCurrentDataDomain()
                });

        DefaultMutableTreeNode newMapNode = ProjectTreeModel.wrapProjectNode(e
                .getDataMap());
        positionNode(domainNode, newMapNode, Comparators.getDataDomainChildrenComparator());
        showNode(newMapNode);
    }

    public void dataMapRemoved(DataMapEvent e) {
        DataMap map = e.getDataMap();
        DataDomain domain = mediator.getCurrentDataDomain();

        removeNode(new Object[] {
                domain, map
        });

        // Clean up map from the nodes
        Iterator nodes = new ArrayList(domain.getDataNodes()).iterator();
        while (nodes.hasNext()) {
            removeNode(new Object[] {
                    domain, nodes.next(), map
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
                    mediator.getCurrentDataDomain(), mediator.getCurrentDataMap(),
                    e.getEntity()
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
                        mediator.getCurrentDataDomain(), mediator.getCurrentDataMap()
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
                mediator.getCurrentDataDomain(), mediator.getCurrentDataMap(),
                e.getEntity()
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
}