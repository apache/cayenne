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
import org.apache.cayenne.modeler.Application;

import java.awt.BorderLayout;

/**
 * @since 4.1
 */
public class StandardModePanel extends GeneratorControllerPanel {

    public StandardModePanel(CodeGeneratorControllerBase codeGeneratorControllerBase) {
        super(Application.getInstance().getFrameController().getProjectController(), codeGeneratorControllerBase);
        FormLayout layout = new FormLayout(
                "right:83dlu, 1dlu, fill:240:grow, 1dlu, left:100dlu, 100dlu", "");

        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.append("Output Directory:", outputFolder.getComponent(), selectOutputFolder);
        builder.nextLine();

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);
    }
}