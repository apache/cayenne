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


package org.apache.cayenne.modeler.action;

import java.awt.event.ActionEvent;

import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.event.AttributeEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.ConfirmRemoveDialog;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.apache.cayenne.project.ProjectPath;

/**
 * Removes currently selected attribute from either the DbEntity or ObjEntity.
 * 
 * @author Garry Watkins
 */
public class RemoveAttributeAction extends RemoveAction implements MultipleObjectsAction {

    private final static String ACTION_NAME = "Remove Attribute";
    
    /**
     * Name of action if multiple rels are selected
     */
    private final static String ACTION_NAME_MULTIPLE = "Remove Attributes";

    public static String getActionName() {
        return ACTION_NAME;
    }
    
    public String getActionName(boolean multiple) {
        return multiple ? ACTION_NAME_MULTIPLE : ACTION_NAME;
    }

    public RemoveAttributeAction(Application application) {
        super(ACTION_NAME, application);
    }

    /**
     * Returns <code>true</code> if last object in the path contains a removable
     * attribute.
     */
    @Override
    public boolean enableForPath(ProjectPath path) {
        if (path == null) {
            return false;
        }

        return path.getObject() instanceof Attribute;
    }

    @Override
    public void performAction(ActionEvent e, boolean allowAsking) {
        ConfirmRemoveDialog dialog = getConfirmDeleteDialog(allowAsking);

        ObjAttribute[] attrs = getProjectController().getCurrentObjAttributes();
        if (attrs != null && attrs.length > 0) {
            if ((attrs.length == 1 && dialog.shouldDelete("ObjAttribute", attrs[0].getName()))
                || (attrs.length > 1 && dialog.shouldDelete("selected ObjAttributes"))) {
                removeObjAttributes();
            }
        }
        else {
            DbAttribute[] dbAttrs = getProjectController().getCurrentDbAttributes();
            if (dbAttrs != null && dbAttrs.length > 0) {
                if ((dbAttrs.length == 1 && dialog.shouldDelete("DbAttribute", dbAttrs[0].getName()))
                    || (dbAttrs.length > 1 && dialog.shouldDelete("selected DbAttributes"))) {
                    removeDbAttributes();
                }
            }            
        }
    }

    protected void removeDbAttributes() {
        ProjectController mediator = getProjectController();
        DbEntity entity = mediator.getCurrentDbEntity();
        DbAttribute[] attribs = mediator.getCurrentDbAttributes();
        
        for (int i = 0; i < attribs.length; i++) {
            entity.removeAttribute(attribs[i].getName());

            AttributeEvent e = new AttributeEvent(
                    Application.getFrame(),
                    attribs[i],
                    entity,
                    MapEvent.REMOVE);
            mediator.fireDbAttributeEvent(e);
        }

        ProjectUtil.cleanObjMappings(mediator.getCurrentDataMap());
    }

    protected void removeObjAttributes() {
        ProjectController mediator = getProjectController();
        ObjEntity entity = mediator.getCurrentObjEntity();
        
        ObjAttribute[] attribs = mediator.getCurrentObjAttributes();
        
        for (int i = 0; i < attribs.length; i++) {
            entity.removeAttribute(attribs[i].getName());
            AttributeEvent e = new AttributeEvent(
                    Application.getFrame(),
                    attribs[i],
                    entity,
                    MapEvent.REMOVE);
            mediator.fireObjAttributeEvent(e);
        }
    }
}
