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

import java.awt.Component;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.CayenneController;

/**
 */
public class DataNodeEditor extends CayenneController {

    protected JTabbedPane view;

    public DataNodeEditor(ProjectController parent) {
        super(parent);
        
        this.view = new JTabbedPane();
        view.addTab("Main", new JScrollPane(new MainDataNodeEditor(parent,this).getView()));
        view.addTab("Adapter", new AdapterEditor(parent).getView());
        view.addTab("Password Encoder", new PasswordEncoderEditor(parent).getView()) ;
    }

    public Component getView() {
        return view;
    }
    
    public JTabbedPane getTabComponent() {
        return view;
    }
}
