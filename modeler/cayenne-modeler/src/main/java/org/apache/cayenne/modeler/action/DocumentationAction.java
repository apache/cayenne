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
import org.apache.cayenne.modeler.util.BrowserControl;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.util.LocalizedStringsHandler;

public class DocumentationAction extends CayenneAction {

    public static String getActionName() {
        return "Documentation";
    }

    public DocumentationAction(Application application) {
        super(getActionName(), application);
    }

    @Override
    public void performAction(ActionEvent e) {
        String url = "https://cayenne.apache.org/docs/";
        String majorVersion = getMajorVersion();
        if(!majorVersion.isEmpty()) {
            url = url + majorVersion + "/cayenne-guide/";
        }
        BrowserControl.displayURL(url);
    }

    private static String getMajorVersion() {
        String version = LocalizedStringsHandler.getString("cayenne.version");
        if(version.isEmpty()) {
            return "";
        }
        String majorVersion = version;
        int dash = version.indexOf('-');
        if (dash != -1) {
            // trim down snapshot part
            majorVersion = version.substring(0, dash);
        }
        // build a major version in form of 'X.Y'
        String[] components = majorVersion.split("\\.");
        switch (components.length) {
            case 0:
                return "";
            case 1:
                return components[0] + ".0";
            default:
                return String.join(".", components[0], components[1]);
        }
    }
}
