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
package org.objectstyle.cayenne.modeler.dialog.objentity;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.modeler.util.CayenneController;
import org.objectstyle.cayenne.util.EntityMergeSupport;

public class EntitySyncController extends CayenneController {

    protected DbEntity dbEntity;
    protected ObjEntity objEntity;
    protected EntitySyncDialog view;

    /**
     * Creates a controller for synchronizing all ObjEntities mapped to a given DbEntity.
     */
    public EntitySyncController(CayenneController parent, DbEntity dbEntity) {
        super(parent);
        this.dbEntity = dbEntity;
    }

    /**
     * Creates a controller for synchronizing a single ObjEntity with its parent DbEntity.
     */
    public EntitySyncController(CayenneController parent, ObjEntity objEntity) {
        this(parent, objEntity.getDbEntity());
        this.objEntity = objEntity;
    }

    public EntityMergeSupport createMerger() {
        Collection entities = getObjEntities();
        if (entities.isEmpty()) {
            return null;
        }

        final EntityMergeSupport merger = new EntityMergeSupport(dbEntity.getDataMap());

        // see if we need to remove meaningful attributes...
        boolean showDialog = false;
        Iterator it = entities.iterator();
        while (it.hasNext()) {
            ObjEntity entity = (ObjEntity) it.next();
            if (!merger.getMeaningfulFKs(entity).isEmpty()) {
                showDialog = true;
                break;
            }
        }

        return (showDialog) ? configureMerger(merger) : merger;
    }

    /**
     * Displays a nerger config dialog, returning a merger configured by the user. Returns
     * null if the dialog was canceled.
     */
    protected EntityMergeSupport configureMerger(final EntityMergeSupport merger) {

        final boolean[] cancel = new boolean[1];

        view = new EntitySyncDialog();

        view.getUpdateButton().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                merger.setRemoveMeaningfulFKs(view.getRemoveFKs().isSelected());
                view.dispose();
            }
        });

        view.getCancelButton().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                cancel[0] = true;
                view.dispose();
            }
        });

        view.pack();
        view.setModal(true);
        centerView();
        makeCloseableOnEscape();
        view.show();

        return cancel[0] ? null : merger;
    }

    public Component getView() {
        return view;
    }

    protected Collection getObjEntities() {
        return (objEntity != null) ? Collections.singleton(objEntity) : dbEntity
                .getDataMap()
                .getMappedEntities(dbEntity);
    }

}
