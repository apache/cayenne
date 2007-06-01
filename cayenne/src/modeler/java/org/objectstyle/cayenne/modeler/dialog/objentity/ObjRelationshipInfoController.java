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
package org.objectstyle.cayenne.modeler.dialog.objentity;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.map.event.RelationshipEvent;
import org.objectstyle.cayenne.modeler.Application;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.dialog.ResolveDbRelationshipDialog;
import org.objectstyle.cayenne.project.NamedObjectFactory;
import org.scopemvc.controller.basic.BasicController;
import org.scopemvc.core.Control;
import org.scopemvc.core.ControlException;

/**
 * @since 1.1
 * @author Andrei Adamchik
 */
public class ObjRelationshipInfoController extends BasicController {

    static final Logger logObj = Logger.getLogger(ObjRelationshipInfoController.class);

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