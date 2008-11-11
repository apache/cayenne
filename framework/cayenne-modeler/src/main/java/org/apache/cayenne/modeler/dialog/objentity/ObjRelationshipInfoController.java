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

import org.apache.cayenne.map.*;
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

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

/**
 * @since 1.1
 */
public class ObjRelationshipInfoController extends BasicController implements TreeSelectionListener {

    public static final String SAVE_CONTROL = "cayenne.modeler.mapObjRelationship.save.button";
    public static final String CANCEL_CONTROL = "cayenne.modeler.mapObjRelationship.cancel.button";
    public static final String NEW_TOONE_CONTROL = "cayenne.modeler.mapObjRelationship.newtoone.button";
    public static final String NEW_TOMANY_CONTROL = "cayenne.modeler.mapObjRelationship.newtomany.button";

    protected ProjectController mediator;

    public ObjRelationshipInfoController(ProjectController mediator,
            ObjRelationship relationship) {

        this.mediator = mediator;
        Collection<ObjEntity> objEntities = mediator.getCurrentDataMap().getNamespace().getObjEntities();
        ObjRelationshipInfoModel model = new ObjRelationshipInfoModel(
                relationship,
                objEntities);
        setModel(model);
    }
    
    /**
     * Creates and runs the classpath dialog.
     */
    @Override
    public void startup() {
        /**
         * Some workaround: need to save target first, because even if it is null,
         * first item will be displayed in combobox. Also we do not want to have empty item
         * in the combobox.
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
        else if (control.matchesID(NEW_TOONE_CONTROL)) {
            createRelationship(false);
        }
        else if (control.matchesID(NEW_TOMANY_CONTROL)) {
            createRelationship(true);
        }
    }

    protected void saveMapping() {
        ObjRelationshipInfoModel model = (ObjRelationshipInfoModel) getModel();

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
    protected void createRelationship(boolean toMany) {
        ObjRelationshipInfoModel model = (ObjRelationshipInfoModel) getModel();
        DbEntity source = model.getStartEntity();
        DbEntity target = model.getEndEntity();

        DbRelationship dbRel = model.getLastRelationship();
        if (dbRel != null) {
            source = (DbEntity) dbRel.getSourceEntity();
        }
        
        if (target == null) {
            JOptionPane.showMessageDialog((Component) getView(), "Please select target entity first.",
                    "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        DbRelationship dbRelationship = (DbRelationship) NamedObjectFactory
                .createRelationship(source, target, toMany);
        // note: NamedObjectFactory doesn't set source or target, just the name
        dbRelationship.setSourceEntity(source);
        dbRelationship.setTargetEntity(target);
        dbRelationship.setToMany(toMany);
        source.addRelationship(dbRelationship);

        // TODO: creating relationship outside of ResolveDbRelationshipDialog confuses it
        // to send incorrect event - CHANGE instead of ADD
        ResolveDbRelationshipDialog dialog = new ResolveDbRelationshipDialog(
                dbRelationship);

        dialog.setVisible(true);
        if (dialog.isCancelPressed()) {
            source.removeRelationship(dbRelationship.getName());
        }
        else {
            MultiColumnBrowser pathBrowser = ((ObjRelationshipInfoDialog) getView()).getPathBrowser();
            Object[] oldPath = pathBrowser.getSelectionPath() == null ?
                    new Object[0] : pathBrowser.getSelectionPath().getPath();
            
            /**
             * Update the view
             */
            EntityTreeModel treeModel = (EntityTreeModel) pathBrowser.getModel();
            treeModel.invalidateChildren(source);
            treeModel.invalidateChildren(target);
            
            Object[] path = new Object[Math.max(oldPath.length, 2)];
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
        model.setObjectTarget(objEntities.size() == 0 ? null : objEntities.iterator().next());
        
        List<DbRelationship> relPath = new Vector<DbRelationship>(selectedPath.getPathCount() - 1);
        for (int i = 1; i < selectedPath.getPathCount(); i++) {
            relPath.add((DbRelationship) selectedPath.getPathComponent(i));
        }
        model.setDbRelationships(relPath);
        
        ((ObjRelationshipInfoDialog) getView()).updateCollectionChoosers();
    }

}
