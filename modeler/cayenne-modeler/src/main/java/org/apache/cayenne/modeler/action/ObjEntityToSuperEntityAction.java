/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.modeler.action;

import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.Application;

/**
 * @since 4.0
 */
public class ObjEntityToSuperEntityAction extends BaseViewEntityAction {

    public static String getActionName() {
        return "View related SuperEntity";
    }

    public ObjEntityToSuperEntityAction(Application application) {
        super(getActionName(), application);
    }

    @Override
    protected ObjEntity getEntity() {
        ObjEntity objEntity = getProjectController().getCurrentObjEntity();
        if (objEntity == null) {
            return null;
        }
        return objEntity.getSuperEntity();
    }
}
