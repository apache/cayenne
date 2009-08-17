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
package org.apache.cayenne.modeler.dialog.objentity;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.WindowConstants;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.editor.ObjAttributeTableModel;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.modeler.util.Comparators;
import org.apache.cayenne.modeler.util.EntityTreeFilter;
import org.apache.cayenne.modeler.util.EntityTreeModel;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.util.CayenneMapEntry;

public class ObjAttributeInfoDialog extends CayenneController implements
        TreeSelectionListener {

    private ObjAttributeTableModel model;
    private int row;
    protected ObjAttributeInfoDialogView view;
    protected ObjAttribute attribute;
    protected ObjAttribute attributeSaved;

    protected List attributesList;
    protected List attributesSavedList;

    protected List<DbEntity> relTargets;

    protected ObjEntity objectTarget;
    protected List<ObjEntity> objectTargets;

    protected List<String> mapKeys;
    protected ProjectController mediator;

    public ObjAttributeInfoDialog(ProjectController mediator, int row,
            ObjAttributeTableModel model) {
        super(mediator);
        this.view = new ObjAttributeInfoDialogView();
        this.mediator = mediator;
        this.model = model;
        this.row = row;
        initController(model.getAttribute(row));
    }

    @Override
    public Component getView() {
        return view;
    }

    /**
     * Starts options dialog.
     */
    public void startupAction() {
        view.pack();
        view.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        view.setModal(true);
        makeCloseableOnEscape();
        centerView();
        view.setVisible(true);

    }

    private void initController(ObjAttribute attr) {
        this.attribute = attr;
        this.attributeSaved = new ObjAttribute();
        attributeSaved.setDbAttributePath(attribute.getDbAttributePath());
        attributeSaved.setName(attribute.getName());
        attributeSaved.setEntity(attribute.getEntity());
        attributeSaved.setParent(attribute.getParent());
        attributeSaved.setType(attribute.getType());
        attributeSaved.setUsedForLocking(attribute.isUsedForLocking());

        relTargets = new ArrayList<DbEntity>(attribute
                .getEntity()
                .getDataMap()
                .getDbEntities());

        /**
         * Register auto-selection of the target
         */
        view.getPathBrowser().addTreeSelectionListener(this);
        this.objectTarget = (ObjEntity) attr.getEntity();
        if (objectTarget != null) {
            updateTargetCombo(objectTarget.getDbEntity());
        }

        view.getAttributeName().setText(attribute.getName());
        if (attribute.getDbAttributePath() != null) {
            if (attribute.getDbAttributePath().contains(".")) {
                String path = attribute.getDbAttributePath();
                view.getCurrentPathLabel().setText(path.replace(".", " -> "));
            }
            else {
                view.getCurrentPathLabel().setText(attribute.getDbAttributePath());
            }
        }
        else {
            view.getCurrentPathLabel().setText("");
        }
        view.getSourceEntityLabel().setText(attribute.getEntity().getName());
        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);
        builder.bindToAction(view.getCancelButton(), "closeAction()");
        builder.bindToAction(view.getSelectPathButton(), "setPath(true)");
        builder.bindToAction(view.getSaveButton(), "saveMapping()");

        /*
         * set filter for ObjAttributePathBrowser
         */
        if (view.getPathBrowser().getModel() == null) {
            Entity firstEntity = null;
            if (attribute.getDbAttribute() == null) {

                Iterator<CayenneMapEntry> it = attribute.getDbPathIterator();
                if (attribute.getParent() instanceof ObjEntity) {
                    DbEntity dbEnt = ((ObjEntity) attribute.getParent()).getDbEntity();

                    Collection<DbAttribute> attrib = dbEnt.getAttributes();
                    Collection<DbRelationship> rel = dbEnt.getRelationships();

                    if (attrib.size() > 0) {
                        Iterator<DbAttribute> iter = attrib.iterator();
                        firstEntity = iter.next().getEntity();
                    }
                    else if (rel.size() > 0) {
                        Iterator<DbRelationship> iter = rel.iterator();
                        firstEntity = iter.next().getSourceEntity();
                    }
                }
            }
            else {
                firstEntity = getFirstEntity();
            }

            if (firstEntity != null) {
                EntityTreeModel treeModel = new EntityTreeModel(firstEntity);
                treeModel.setFilter(new EntityTreeFilter() {

                    public boolean attributeMatch(Object node, Attribute attr) {
                        if (!(node instanceof Attribute)) {
                            return true;
                        }
                        return false;
                    }

                    public boolean relationshipMatch(Object node, Relationship rel) {
                        if (!(node instanceof Relationship)) {
                            return true;
                        }

                        /**
                         * We do not allow A->B->A chains, where relationships are to-one
                         */
                        DbRelationship prev = (DbRelationship) node;
                        return !(!rel.isToMany() && prev.getReverseRelationship() == rel);
                    }
                });
                view.getPathBrowser().setModel(treeModel);

            }
        }

        if (attribute.getDbAttribute() != null) {
            setSelectionPath();
        }
    }

    public void closeAction() {
        view.dispose();
    }

    public boolean setPath(boolean isChange) {
        StringBuilder attributePath = new StringBuilder();
        StringBuilder pathStr = new StringBuilder();
        TreePath path = view.getPathBrowser().getSelectionPath();

        if (path.getLastPathComponent() instanceof DbAttribute) {
            Object[] pathComponents = path.getPath();
            for (int i = 0; i < pathComponents.length; i++) {
                boolean attrOrRel = true;
                if (pathComponents[i] instanceof DbAttribute) {
                    pathStr.append(((DbAttribute) pathComponents[i]).getName());
                    attributePath.append(((DbAttribute) pathComponents[i]).getName());
                }
                else if (pathComponents[i] instanceof DbRelationship) {
                    pathStr.append(((DbRelationship) pathComponents[i]).getName());
                    attributePath.append(((DbRelationship) pathComponents[i]).getName());
                }
                else {
                    attrOrRel = false;
                }

                if (i != pathComponents.length - 1 && attrOrRel) {
                    pathStr.append(" -> ");
                    attributePath.append(".");
                }
            }
        }
        else {
            view.getCurrentPathLabel().setText("");
        }

        view.getCurrentPathLabel().setText(pathStr.toString());

        if (attribute.getDbAttributePath() != null) {
            if (!attribute.getDbAttributePath().equals(attributePath.toString())
                    || !attribute.getName().equals(view.getAttributeName().getText())) {
                attributeSaved.setDbAttributePath(attributePath.toString());
                attributeSaved.setName(view.getAttributeName().getText());
                if (!attribute.getDbAttributePath().equals(attributePath.toString()) && isChange) {
                    model.setUpdatedValueAt(attributeSaved.getDbAttributePath(), row, 3);
                }
                return true;
            }
        }
        else {
            if (attributePath.length() > 0
                    || !attribute.getName().equals(view.getAttributeName().getText())) {
                attributeSaved.setDbAttributePath(attributePath.toString());
                attributeSaved.setName(view.getAttributeName().getText());
                if (attributePath.length() > 0 && isChange) {
                    model.setUpdatedValueAt(attributeSaved.getDbAttributePath(), row, 3);
                }
                return true;
            }
        }
        return false;
    }

    public void saveMapping() {

        if (setPath(false)) {
            if (JOptionPane.showConfirmDialog(
                    (Component) getView(),
                    "You have changed Db Attribute path. Do you want it to be saved?",
                    "Save ObjAttribute",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                model.setUpdatedValueAt(attributeSaved.getName(), row, 1);
                model.setUpdatedValueAt(attributeSaved.getDbAttributePath(), row, 3);
            }
        }

        closeAction();
    }

    public void valueChanged(TreeSelectionEvent e) {

        TreePath selectedPath = e.getPath();

        // first item in the path is Entity, so we must have
        // at least two elements to constitute a valid ordering path
        if (selectedPath == null || selectedPath.getPathCount() < 2) {
            return;
        }

        DbEntity target = null;
        if (selectedPath.getLastPathComponent() instanceof Relationship) {
            Relationship rel = (Relationship) selectedPath.getLastPathComponent();
            target = (DbEntity) rel.getTargetEntity();

        }
        else if (selectedPath.getLastPathComponent() instanceof Attribute) {
            Attribute attr = (Attribute) selectedPath.getLastPathComponent();
            target = (DbEntity) attr.getEntity();
        }

        if (target != null) {

            /**
             * Initialize root with one of mapped ObjEntities.
             */
            Collection<ObjEntity> objEntities = target.getDataMap().getMappedEntities(
                    target);

            List<DbRelationship> relPath = new Vector<DbRelationship>(selectedPath
                    .getPathCount() - 1);
            for (int i = 1; i < selectedPath.getPathCount(); i++) {
                if (selectedPath.getLastPathComponent() instanceof Relationship) {
                    relPath.add((DbRelationship) selectedPath.getPathComponent(i));
                }
            }

            setObjectTarget(objEntities.size() == 0 ? null : objEntities
                    .iterator()
                    .next());
            if (objectTarget != null) {
                updateTargetCombo(objectTarget.getDbEntity());
            }
            else {
                updateTargetCombo(null);
            }
        }
    }

    public void setObjectTarget(ObjEntity objectTarget) {
        if (this.objectTarget != objectTarget) {
            this.objectTarget = objectTarget;
        }
    }

    protected void updateTargetCombo(DbEntity dbTarget) {
        if (dbTarget != null) {
            // copy those that have DbEntities mapped to dbTarget, and then sort

            view.getTargCombo().removeAllItems();
            this.objectTargets = new ArrayList<ObjEntity>();

            if (dbTarget != null) {
                objectTargets.addAll(dbTarget.getDataMap().getMappedEntities(dbTarget));
                Collections.sort(objectTargets, Comparators.getNamedObjectComparator());
            }

            for (ObjEntity obj : objectTargets) {
                view.getTargCombo().addItem(obj.getName());
            }
        }
        else {
            view.getTargCombo().addItem("");
        }
    }

    private Entity getFirstEntity() {
        Iterator<CayenneMapEntry> it = attribute.getDbPathIterator();
        Entity firstEnt = attribute.getDbAttribute().getEntity();
        boolean setEnt = false;

        while (it.hasNext()) {
            Object ob = it.next();
            if (ob instanceof DbRelationship) {
                if (!setEnt) {
                    firstEnt = ((DbRelationship) ob).getSourceEntity();
                    setEnt = true;
                }
            }
            else if (ob instanceof DbAttribute) {
                if (!setEnt) {
                    firstEnt = ((DbAttribute) ob).getEntity();
                }
            }
        }

        return firstEnt;
    }

    /**
     * Selects path in browser
     */
    public void setSelectionPath() {
        List list = new ArrayList();
        boolean isAttributeLast = false;
        Iterator<CayenneMapEntry> it = attribute.getDbPathIterator();
        while (it.hasNext()) {
            Object obj = it.next();
            list.add(obj);
            if (obj instanceof DbAttribute && !it.hasNext()) {
                isAttributeLast = true;
            }
        }
        if (isAttributeLast) {
            Object[] path = new Object[list.size() + 1];
            path[0] = getFirstEntity();
            System.arraycopy(list.toArray(), 0, path, 1, list.size());
            view.getPathBrowser().setSelectionPath(new TreePath(path));
            view.getSaveButton().setEnabled(true);
        }
    }
}
