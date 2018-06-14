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

package org.apache.cayenne.modeler.editor.cgen;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.pref.DataMapDefaults;

import javax.swing.*;
import java.awt.*;

public class StandardPanelComponent extends JComponent {

    private DataMap dataMap;
    private DataMapDefaults preferences;
    private JLabel dataMapName;
    private JTextField superclassPackage;
    private DefaultFormBuilder builder;

    public StandardPanelComponent() {
        super();
        dataMapName = new JLabel();
        dataMapName.setFont(dataMapName.getFont().deriveFont(1));
        superclassPackage = new JTextField();

        FormLayout layout = new FormLayout(
                "right:77dlu, 3dlu, fill:200:grow, 3dlu", "");
        builder = new DefaultFormBuilder(layout);
        builder.append(dataMapName);
        builder.nextLine();
        builder.append("Superclass Package:", superclassPackage);

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);
    }

    public DataMap getDataMap() {
        return dataMap;
    }

    public void setDataMap(DataMap dataMap) {
        this.dataMap = dataMap;
    }

    public DataMapDefaults getPreferences() {
        return preferences;
    }

    public void setPreferences(DataMapDefaults preferences) {
        this.preferences = preferences;
    }

    public JLabel getDataMapName() {
        return dataMapName;
    }

    public JTextField getSuperclassPackage() {
        return superclassPackage;
    }

}
