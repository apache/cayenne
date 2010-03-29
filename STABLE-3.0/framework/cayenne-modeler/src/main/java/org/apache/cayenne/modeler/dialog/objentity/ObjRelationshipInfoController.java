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
import java.util.List;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.ResolveDbRelationshipDialog;
import org.apache.cayenne.modeler.util.EntityTreeModel;
import org.apache.cayenne.modeler.util.MultiColumnBrowser;
import org.apache.cayenne.project.NamedObjectFactory;
import org.scopemvc.controller.basic.BasicController;
import org.scopemvc.core.Control;
import org.scopemvc.core.ControlException;

/**
 * @since 1.1
 */
public class ObjRelationshipInfoController extends BasicController implements
        TreeSelectionListener {

    public static final String SAVE_CONTROL = "cayenne.modeler.mapObjRelationship.save.button";
    public static final String CANCEL_CONTROL = "cayenne.modeler.mapObjRelationship.cancel.button";
    public static final String NEW_REL_CONTROL = "cayenne.modeler.mapObjRelationship.newrel.button";

    public static final String SELECT_PATH_CONTROL = "cayenne.modeler.mapObjRelationship.select.path.button";
    public static final String REVERT_PATH_CONTROL = "cayenne.modeler.mapObjRelationship.revert.path.button";
    public static final String CLEAR_PATH_CONTROL = "cayenne.modeler.mapObjRelationship.clear.path.button";

    protected ProjectController mediator;

    public ObjRelationshipInfoController(ProjectController mediator,
            ObjRelationship relationship) {

        this.mediator = mediator;
        ObjRelationshipInfoModel model = new ObjRelationshipInfoModel(relationship);
        setModel(model);
    }

    /**
     * Creates and runs the classpath dialog.
     */
    @Override
    public void startup() {
        /**
         * Some workaround: need to save target first, because even if it is null, first
         * item will be displayed in combobox. Also we do not want to have empty item in
         * the combobox.
         */
        ObjRelationshipInfoModel model = (ObjRelationshipInfoModel) getModel();
        ObjEntity target = model.getObjectTarget();

        ObjRelationshipInfoDialog view = new ObjRelationshipInfoDialog();
        setView(view);

        model.setObjectTarget(target);

        /**
         * Register auto-selection of the target
         */
        view.getPathBrowser().addTreeSelectionListener(this);

        view.initFromModel();
        super.startup();
    }

    @Override
    protected void doHandleControl(Control control) throws ControlException {
        if (control.matchesID(CANCEL_CONTROL)) {
            shutdown();
        }
        else if (control.matchesID(SAVE_CONTROL)) {
            saveMapping();
        }
        else if (control.matchesID(NEW_REL_CONTROL)) {
            createRelationship();
        }
        else if (control.matchesID(SELECT_PATH_CONTROL)) {
            selectPath();
        }
        else if (control.matchesID(REVERT_PATH_CONTROL)) {
            revertPath();
        }
        else if (control.matchesID(CLEAR_PATH_CONTROL)) {
            clearPath();
        }
    }

    /**
     * Saves selected path
     */
    protected void selectPath() {
        ObjRelationshipInfoModel model = (ObjRelationshipInfoModel) getModel();
        model.selectPath();
    }

    /**
     * Reverts current path to saved path
     */
    protected void revertPath() {
        ObjRelationshipInfoModel model = (ObjRelationshipInfoModel) getModel();
        ((ObjRelationshipInfoDialog) getView()).setSelectionPath(model
                .getSavedDbRelationships());
        model.setDbRelationships(model.getSavedDbRelationships());
    }

    /**
     * Clears paths and selections in browser
     */
    protected void clearPath() {
        ObjRelationshipInfoModel model = (ObjRelationshipInfoModel) getModel();
        ((ObjRelationshipInfoDialog) getView()).getPathBrowser().clearSelection();
        model.setDbRelationships(new ArrayList<DbRelationship>());
    }

    protected void saveMapping() {
        ObjRelationshipInfoModel model = (ObjRelationshipInfoModel) getModel();

        if (!model.getDbRelationships().equals(model.getSavedDbRelationships())) {
            if (JOptionPane.showConfirmDialog(
                    (Component) getView(),
                    "You have changed Db Relationship path. Do you want it to be saved?",
                    "Save ObjRelationship",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                selectPath();
            }
        }

        if (model.savePath()) {
            mediator.fireObjRelationshipEvent(new RelationshipEvent(Application
                    .getFrame(), model.getRelationship(), model
                    .getRelationship()
                    .getSourceEntity()));
        }
        shutdown();
    }

    /**
     * Creates a new relationship connecting currently selected source entity with
     * ObjRelationship target entity. User is allowed to edit the relationship, change its
     * name, and create joins.
     */
    protected void createRelationship() {
        ObjRelationshipInfoModel model = (ObjRelationshipInfoModel) getModel();

        DbRelationship dbRel = model.getLastRelationship();
        DbEntity source = dbRel != null ? (DbEntity) dbRel.getTargetEntity() : null;

        DbRelationshipTargetController targetController = new DbRelationshipTargetController(
                model.getStartEntity(),
                source);
        targetController.startup();

        if (!targetController.isSavePressed()) {
            return;
        }

        DbRelationshipTargetModel targetModel = (DbRelationshipTargetModel) targetController
                .getModel();

        DbRelationship dbRelationship = (DbRelationship) NamedObjectFactory
                .createRelationship(
                        targetModel.getSource(),
                        targetModel.getTarget(),
                        targetModel.isToMany());

        // note: NamedObjectFactory doesn't set source or target, just the name
        dbRelationship.setSourceEntity(targetModel.getSource());
        dbRelationship.setTargetEntity(targetModel.getTarget());
        dbRelationship.setToMany(targetModel.isToMany());
        targetModel.getSource().addRelationship(dbRelationship);

        // TODO: creating relationship outside of ResolveDbRelationshipDialog confuses it
        // to send incorrect event - CHANGE instead of ADD
        ResolveDbRelationshipDialog dialog = new ResolveDbRelationshipDialog(
                dbRelationship);

        dialog.setVisible(true);
        if (dialog.isCancelPressed()) {
            targetModel.getSource().removeRelationship(dbRelationship.getName());
        }
        else {
            MultiColumnBrowser pathBrowser = ((ObjRelationshipInfoDialog) getView())
                    .getPathBrowser();
            Object[] oldPath = targetModel.isSource1Selected() ? new Object[] {
                model.getStartEntity()
            } : pathBrowser.getSelectionPath().getPath();

            /**
             * Update the view
             */
            EntityTreeModel treeModel = (EntityTreeModel) pathBrowser.getModel();
            treeModel.invalidate();

            pathBrowser.setSelectionPath(new TreePath(new Object[] {
                model.getStartEntity()
            }));
            pathBrowser.repaint();

            Object[] path = new Object[oldPath.length + 1];
            System.arraycopy(oldPath, 0, path, 0, path.length - 1);

            path[path.length - 1] = dbRelationship;
            pathBrowser.setSelectionPath(new TreePath(path));
        }

        dialog.dispose();
    }

    public void valueChanged(TreeSelectionEvent e) {
        TreePath selectedPath = e.getPath();

        // first item in the path is Entity, so we must have
        // at least two elements to constitute a valid ordering path
        if (selectedPath == null || selectedPath.getPathCount() < 2) {
            return;
        }

        Relationship rel = (Relationship) selectedPath.getLastPathComponent();
        DbEntity target = (DbEntity) rel.getTargetEntity();

        ObjRelationshipInfoModel model = (ObjRelationshipInfoModel) getModel();

        /**
         * Initialize root with one of mapped ObjEntities.
         */
        Collection<ObjEntity> objEntities = target.getDataMap().getMappedEntities(target);

        List<DbRelationship> relPath = new Vector<DbRelationship>(selectedPath
                .getPathCount() - 1);
        for (int i = 1; i < selectedPath.getPathCount(); i++) {
            relPath.add((DbRelationship) selectedPath.getPathComponent(i));
        }
        model.setDbRelationships(relPath);
        model.setObjectTarget(objEntities.size() == 0 ? null : objEntities
                .iterator()
                .next());

        ((ObjRelationshipInfoDialog) getView()).updateCollectionChoosers();
    }

}
