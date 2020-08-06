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

package org.apache.cayenne.modeler.action.dbimport;

import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeColumn;
import org.apache.cayenne.modeler.Application;

/**
 * @since 4.1
 */
public class AddExcludeColumnAction extends AddPatternParamAction {

    private static final String ACTION_NAME = "Exclude Column";
    private static final String ICON_NAME = "icon-dbi-excludeColumn.png";

    public AddExcludeColumnAction(Application application) {
        super(ACTION_NAME, application);
        insertableNodeClass = ExcludeColumn.class;
    }

    public String getIconName() {
        return ICON_NAME;
    }
}
