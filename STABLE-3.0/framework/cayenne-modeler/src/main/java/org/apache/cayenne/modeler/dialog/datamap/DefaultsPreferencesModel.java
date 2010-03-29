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

package org.apache.cayenne.modeler.dialog.datamap;

import org.scopemvc.core.Selector;

/**
 * A model that describes entity update preferences when pushing DataMap defaults to the
 * entities. Used by multiple views.
 * 
 */
public class DefaultsPreferencesModel {

    /**
     * Selector for the boolean property that determines whether all Entities should be
     * included in a particular default update.
     */
    public static final Selector ALL_ENTITIES_SELECTOR = Selector
            .fromString("allEntities");

    /**
     * Selector for the boolean property that determines whether only Entities with
     * missing corresponding property should be included in a particular default update.
     */
    public static final Selector UNINITIALIZED_ENTITIES_SELECTOR = Selector
            .fromString("uninitializedEntities");

    protected boolean allEntities;

    public DefaultsPreferencesModel(boolean flag) {
        this.allEntities = flag;
    }

    public boolean isAllEntities() {
        return allEntities;
    }

    public void setAllEntities(boolean flag) {
        this.allEntities = flag;
    }

    public boolean isUninitializedEntities() {
        return !isAllEntities();
    }

    public void setUninitializedEntities(boolean flag) {
        setAllEntities(!flag);
    }
}
