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

package org.apache.cayenne.modeler.editor.cgen.templateeditor;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.JButton;
import javax.swing.JTextField;
import java.awt.BorderLayout;

/**
 * since 4.3
 */
public class FindAndReplaceView extends FindView {

    private JTextField replaceWithField;
    private JButton replaceButton;
    private JButton replaceAllButton;

    public FindAndReplaceView() {
        initComponents();
        buildView();
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        this.replaceWithField = new JTextField();
        this.replaceButton = new JButton("Replace");
        this.replaceAllButton = new JButton("Replace all");
    }

    @Override
    protected void buildView() {
        this.setTitle("Find and replace dialog");
        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(new FormLayout(COLUMN_SPECS, ROW_SPECS));
        builder.setDefaultDialogBorder();

        builder.addLabel("Find what:", cc.xy(1, 1));
        builder.add(searchField, cc.xyw(3, 1, 3));
        builder.addLabel("Replace with:", cc.xy(1, 3));
        builder.add(replaceWithField, cc.xyw(3, 3, 3));
        builder.add(regexCB, cc.xy(1, 5));
        builder.add(matchCaseCB, cc.xy(3, 5));
        builder.add(wholeWordCB, cc.xy(5, 5));

        builder.add(nextButton, cc.xy(7, 1));
        builder.add(replaceButton, cc.xy(7, 3));
        builder.add(replaceAllButton, cc.xy(7, 5));

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
    }

    public JTextField getReplaceWithField() {
        return replaceWithField;
    }

    public JButton getReplaceButton() {
        return replaceButton;
    }

    public JButton getReplaceAllButton() {
        return replaceAllButton;
    }


}
