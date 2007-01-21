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

import java.util.Collection;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.ResolveDbRelationshipDialog;
import org.apache.cayenne.project.NamedObjectFactory;
import org.scopemvc.controller.basic.BasicController;
import org.scopemvc.core.Control;
import org.scopemvc.core.ControlException;

/**
 * @since 1.1
 * @author Andrus Adamchik
 */
public class ObjRelationshipInfoController extends BasicController {

    public static final String SAVE_CONTROL = "cayenne.modeler.mapObjRelationship.save.button";
    public static final String CANCEL_CONTROL = "cayenne.modeler.mapObjRelationship.cancel.button";
    public static final String NEW_TOONE_CONTROL = "cayenne.modeler.mapObjRelationship.newtoone.button";
    public static final String NEW_TOMANY_CONTROL = "cayenne.modeler.mapObjRelationship.newtomany.button";

    protected ProjectController mediator;

    public ObjRelationshipInfoController(ProjectController mediator,
            ObjRelationship relationship) {

        this.mediator = mediator;
        Collection objEntities = mediator.getCurrentDataMap().getNamespace().getObjEntities();
        ObjRelationshipInfoModel model = new ObjRelationshipInfoModel(
                relationship,
                objEntities);
        setModel(model);
    }

    /**
     * Creates and runs the classpath dialog.
     */
    public void startup() {
        setView(new ObjRelationshipInfoDialog());
        super.startup();
    }

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
        cancelEditing();

        ObjRelationshipInfoModel model = (ObjRelationshipInfoModel) getModel();
        DbEntity source = model.getStartEntity();
        DbEntity target = model.getEndEntity();

        EntityRelationshipsModel selectedPathComponent = model.getSelectedPathComponent();
        if (selectedPathComponent != null) {
            source = (DbEntity) selectedPathComponent.getSourceEntity();
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
            if (selectedPathComponent == null) {
                selectedPathComponent = (EntityRelationshipsModel) model
                        .getDbRelationshipPath()
                        .get(0);
                model.setSelectedPathComponent(selectedPathComponent);
            }

            selectedPathComponent.setRelationshipName(dbRelationship.getName());
        }

        dialog.dispose();
    }

    protected void cancelEditing() {
        ((ObjRelationshipInfoDialog) getView()).cancelTableEditing();
    }
}
