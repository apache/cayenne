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

package org.apache.cayenne.modeler.dialog;

import java.awt.HeadlessException;

import org.apache.cayenne.modeler.CayenneModelerFrame;

/**
 * Dialog for CayenneModeler warnings.
 * 
 * @author Andrei Adamchik
 */
public class WarningDialog extends ErrorDebugDialog {

    public WarningDialog(CayenneModelerFrame owner, String title, Throwable throwable,
            boolean detailed) throws HeadlessException {
        super(owner, title, throwable, detailed);
    }

    protected String infoHTML() {
        return "<font face='Arial,Helvetica' size='+1' color='blue'>"
                + getTitle()
                + "</font>";
    }
}
