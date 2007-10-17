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

import org.apache.cayenne.map.DerivedDbEntity;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.apache.cayenne.project.ProjectPath;

/**
 * @author Andrus Adamchik
 */
public class DerivedEntitySyncAction extends CayenneAction {

	public static String getActionName() {
		return "Reset Derived Entity";
	}

    /**
     * Constructor for DerivedEntitySyncAction.
     * @param name
     */
    public DerivedEntitySyncAction(Application application) {
        super(getActionName(), application);
    }

    /**
     * @see org.apache.cayenne.modeler.util.CayenneAction#performAction(ActionEvent)
     */
    public void performAction(ActionEvent e) {
        DerivedDbEntity ent = (DerivedDbEntity) getProjectController().getCurrentDbEntity();

        if (ent != null && ent.getParentEntity() != null) {
            ent.resetToParentView();
            ProjectUtil.cleanObjMappings(getProjectController().getCurrentDataMap());

            // fire a chain of "remove/add" events for entity
            // this seems to be the only way to refresh the view
            getProjectController().fireObjEntityEvent(
                new EntityEvent(this, ent, MapEvent.REMOVE));
            getProjectController().fireObjEntityEvent(new EntityEvent(this, ent, MapEvent.ADD));
        }
    }

    /**
    * Returns <code>true</code> if path contains a DerivedDbEntity object.
    */
    public boolean enableForPath(ProjectPath path) {
        if (path == null) {
            return false;
        }

        return path.firstInstanceOf(DerivedDbEntity.class) != null;
    }
}
