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

import java.awt.event.ActionEvent;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.db.DataSourceWizard;

/**
 * @since 4.1
 */
public class GetDbConnectionAction extends DBConnectionAwareAction {

    public static final String DIALOG_TITLE = "Configure Connection to Database";
    private static final String ACTION_NAME = "Configure Connection";
    private static final String ICON_NAME = "icon-dbi-config.png";

    public GetDbConnectionAction(final Application application) {
        super(ACTION_NAME, application);
    }

    public String getIconName() {
        return ICON_NAME;
    }

    @Override
    public void performAction(final ActionEvent e) {
        DataSourceWizard connectWizard = getDataSourceWizard(DIALOG_TITLE, new String[]{"Continue", "Cancel"});
        if (connectWizard == null) {
            return;
        }
        saveConnectionInfo(connectWizard);
    }
}
