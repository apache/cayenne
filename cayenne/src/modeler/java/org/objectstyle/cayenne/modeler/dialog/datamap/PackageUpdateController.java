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
package org.objectstyle.cayenne.modeler.dialog.datamap;

import java.util.Iterator;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.event.EntityEvent;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.util.Util;
import org.scopemvc.core.Control;
import org.scopemvc.core.ControlException;
import org.scopemvc.view.swing.SPanel;

/**
 * @author Andrei Adamchik
 */
public class PackageUpdateController extends DefaultsPreferencesController {

    public static final String ALL_CONTROL = "cayenne.modeler.datamap.defaultprefs.package.radio";
    public static final String UNINIT_CONTROL = "cayenne.modeler.datamap.defaultprefs.packagenull.radio";

    public PackageUpdateController(ProjectController mediator, DataMap dataMap) {
        super(mediator, dataMap);
    }

    /**
     * Creates and runs the package update dialog.
     */
    public void startup() {
        SPanel view = new DefaultsPreferencesDialog(ALL_CONTROL, UNINIT_CONTROL);
        view.setTitle("Update ObjEntities Java Package");
        setView(view);
        super.startup();
    }

    protected void doHandleControl(Control control) throws ControlException {
        if (control.matchesID(UPDATE_CONTROL)) {
            updatePackage();
        }
        else {
            super.doHandleControl(control);
        }
    }

    protected void updatePackage() {
        boolean doAll = ((DefaultsPreferencesModel) getModel()).isAllEntities();
        String defaultPackage = dataMap.getDefaultPackage();
        if (Util.isEmptyString(defaultPackage)) {
            defaultPackage = "";
        }
        else if (!defaultPackage.endsWith(".")) {
            defaultPackage = defaultPackage + '.';
        }

        Iterator it = dataMap.getObjEntities().iterator();
        while (it.hasNext()) {
            ObjEntity entity = (ObjEntity) it.next();
            if (doAll || Util.isEmptyString(entity.getClassName())) {
                String oldName = entity.getClassName();
                String className = extractClassName(Util.isEmptyString(oldName) ? entity
                        .getName() : oldName);
                String newName = defaultPackage + className;

                if (!Util.nullSafeEquals(newName, oldName)) {
                    entity.setClassName(newName);

                    // any way to batch events, a big change will flood the app with
                    // entity events..?
                    mediator.fireDbEntityEvent(new EntityEvent(this, entity));
                }
            }
        }

        shutdown();
    }

    protected String extractClassName(String name) {
        if (name == null) {
            return "";
        }

        int dot = name.lastIndexOf('.');
        return (dot < 0) ? name : (dot + 1 < name.length())
                ? name.substring(dot + 1)
                : "";
    }
}