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


package org.apache.cayenne.modeler.editor.datanode;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.cayenne.modeler.util.CayenneWidgetFactory;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 */
public class DBCPDataSourceView extends JPanel {

    protected JTextField propertiesFile;

    public DBCPDataSourceView() {

        propertiesFile = CayenneWidgetFactory.createUndoableTextField();

        // assemble
        FormLayout layout = new FormLayout("right:80dlu, 3dlu, fill:200dlu", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.appendSeparator("Apache DBCP Configuration");

        builder.append("DBCP Properties File:", propertiesFile);

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);
    }

    public JTextField getPropertiesFile() {
        return propertiesFile;
    }
}
