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
package org.apache.cayenne.modeler.editor;

import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.ObjEntityDisplayListener;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.CallbackTypeSelectionEvent;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.CallbackMap;


/**
 * Tab for editing callback methods on an ObjEntity
 *
 * @author Vasil Tarasevich
 * @version 1.0 Oct 28, 2007
 */
public class ObjEntityCallbackMethodsTab extends AbstractCallbackMethodsTab implements
        ObjEntityDisplayListener {

    /**
     * constructor
     * @param mediator mediator instance
     */
    public ObjEntityCallbackMethodsTab(ProjectController mediator) {
        super(mediator);
    }

    /**
     * Processes selection of a new ObjEntity
     */
    public void currentObjEntityChanged(EntityDisplayEvent e) {
        if (e.getSource() == this) {
            return;
        }

        ObjEntity entity = (ObjEntity) e.getEntity();
        // Important: process event even if this is the same entity,
        // since the inheritance structure might have changed
        if (entity != null) {
            mediator.fireCallbackTypeSelectionEvent(
                new CallbackTypeSelectionEvent(
                    e.getSource(),
                    (CallbackType)callbackTypeCombo.getItemAt(0))
            );

            rebuildTable();
        }

        // if an entity was selected on a tree,
        // unselect currently selected row
        if (e.isUnselectAttributes()) {
            table.clearSelection();
        }
    }


    /**
     * listeners initialization
     */
    protected void initController() {
        super.initController();
        mediator.addObjEntityDisplayListener(this);
    }


    /**
     * @return CallbackMap with callback methods
     */
    protected CallbackMap getCallbackMap() {
        if (mediator.getCurrentObjEntity() != null) {
            return mediator.getCurrentObjEntity().getCallbackMap();
        }
        return null;
    }
}

