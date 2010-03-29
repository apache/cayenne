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

package org.apache.cayenne.modeler.dialog.codegen;

import java.awt.BorderLayout;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class StandardModePanel extends GeneratorControllerPanel {

    public StandardModePanel() {

        PanelBuilder builder = new PanelBuilder(new FormLayout(
                "right:70dlu, 3dlu, fill:150dlu:grow, 3dlu, pref",
                "p, 3dlu, p"));
        builder.setDefaultDialogBorder();

        CellConstraints cc = new CellConstraints();

        builder.addLabel("Output Directory:", cc.xy(1, 1));
        builder.add(outputFolder, cc.xy(3, 1));
        builder.add(selectOutputFolder, cc.xy(5, 1));
        builder.addLabel("Superclass Package:", cc.xy(1, 3));
        builder.add(superclassPackage, cc.xy(3, 3));

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);
    }
}
