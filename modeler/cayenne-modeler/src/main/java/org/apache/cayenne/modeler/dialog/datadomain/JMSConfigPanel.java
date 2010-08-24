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

package org.apache.cayenne.modeler.dialog.datadomain;

import java.awt.BorderLayout;

import org.scopemvc.view.swing.SPanel;
import org.scopemvc.view.swing.STextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 */
public class JMSConfigPanel extends SPanel {

    public JMSConfigPanel() {
        super();
        initView();
    }

    protected void initView() {
        setLayout(new BorderLayout());

        STextField topicFactory = new STextField(30);
        topicFactory.setSelector(JMSConfigModel.TOPIC_FACTORY_SELECTOR);

        // type form
        FormLayout layout = new FormLayout("right:150, 3dlu, left:200", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();
        builder.appendSeparator("JMS Settings");

        // "1" at the end would enforce spanning the text field to
        // the full width
        builder.append("Connection Factory Name:", topicFactory, 1);

        add(builder.getPanel(), BorderLayout.NORTH);
    }

}
