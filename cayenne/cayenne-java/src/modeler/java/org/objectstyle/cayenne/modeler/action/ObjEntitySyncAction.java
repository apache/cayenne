/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.modeler.action;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.event.EntityEvent;
import org.objectstyle.cayenne.map.event.MapEvent;
import org.objectstyle.cayenne.modeler.Application;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.dialog.objentity.EntitySyncController;
import org.objectstyle.cayenne.modeler.event.EntityDisplayEvent;
import org.objectstyle.cayenne.modeler.util.CayenneAction;
import org.objectstyle.cayenne.project.ProjectPath;
import org.objectstyle.cayenne.util.EntityMergeSupport;

/**
 * Action that synchronizes a given ObjEntity with the current state of the underlying
 * DbEntity.
 * 
 * @author Andrei Adamchik
 */
public class ObjEntitySyncAction extends CayenneAction {

    public static String getActionName() {
        return "Sync ObjEntity with DbEntity";
    }

    public ObjEntitySyncAction(Application application) {
        super(getActionName(), application);
    }

    public String getIconName() {
        return "icon-sync.gif";
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_U, Toolkit
                .getDefaultToolkit()
                .getMenuShortcutKeyMask());
    }

    /**
     * @see org.objectstyle.cayenne.modeler.util.CayenneAction#performAction(ActionEvent)
     */
    public void performAction(ActionEvent e) {
        synchObjEntity();
    }

    protected void synchObjEntity() {
        ProjectController mediator = getProjectController();
        ObjEntity entity = mediator.getCurrentObjEntity();

        if (entity != null && entity.getDbEntity() != null) {
            EntityMergeSupport merger = new EntitySyncController(Application
                    .getInstance()
                    .getFrameController(), entity).createMerger();

            if (merger == null) {
                return;
            }

            if (merger.synchronizeWithDbEntity(entity)) {
                mediator
                        .fireObjEntityEvent(new EntityEvent(this, entity, MapEvent.CHANGE));
                mediator.fireObjEntityDisplayEvent(new EntityDisplayEvent(
                        this,
                        entity,
                        entity.getDataMap(),
                        mediator.getCurrentDataDomain()));
            }
        }
    }

    /**
     * Returns <code>true</code> if path contains a ObjEntity object.
     */
    public boolean enableForPath(ProjectPath path) {
        if (path == null) {
            return false;
        }

        return path.firstInstanceOf(ObjEntity.class) != null;
    }
}
