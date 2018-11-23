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

package org.apache.cayenne.modeler.editor.cgen.domain;

import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.editor.GeneratorsTab;
import org.apache.cayenne.modeler.editor.GeneratorsTabController;

import javax.swing.JOptionPane;

/**
 * @since 4.1
 */
public class CgenTab extends GeneratorsTab {

    public CgenTab(ProjectController projectController, GeneratorsTabController additionalTabController) {
        super(projectController, additionalTabController, "icon-gen_java.png", "Run class generation on selected datamaps.");
    }

    void showSuccessMessage() {
        JOptionPane.showMessageDialog(
                this,
                "Class generation finished");
    }

    void showErrorMessage(String msg) {
        JOptionPane.showMessageDialog(
                this,
                "Error generating classes - " + msg);
    }

}
