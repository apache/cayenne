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

package org.apache.cayenne.modeler.ui.project.editor.datamap.cgen.templateeditor;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.modeler.Application;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

public class TemplateEditorFindAndReplaceDialog extends TemplateEditorFindDialog {

    private static final String REPLACE = "replace";
    private static final String REPLACE_ALL = "replaceAll";

    private final JTextField replaceWithField;
    private final JButton replaceButton;
    private final JButton replaceAllButton;

    public TemplateEditorFindAndReplaceDialog(Application app, TemplateEditor templateEditor) {
        super(app, templateEditor, "Find and replace dialog");
        this.replaceWithField = new JTextField();
        this.replaceButton = new JButton("Replace");
        this.replaceAllButton = new JButton("Replace all");

        rebuildLayout();
        initReplaceBindings();
    }

    private void rebuildLayout() {
        getContentPane().removeAll();

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

    private void initReplaceBindings() {
        replaceButton.setActionCommand(REPLACE);
        replaceButton.addActionListener(this::onAction);
        replaceAllButton.setActionCommand(REPLACE_ALL);
        replaceAllButton.addActionListener(this::onAction);
    }

    @Override
    protected void onAction(ActionEvent e) {
        SearchContext context = buildSearchContext(true);
        if (context == null) {
            return;
        }
        context.setReplaceWith(replaceWithField.getText());

        switch (e.getActionCommand()) {
            case FIND_NEXT: {
                boolean found = SearchEngine.find(templateEditor.getEditingTemplatePane(), context).wasFound();
                if (!found) {
                    JOptionPane.showMessageDialog(this, TEXT_NOT_FOUND_MSG);
                }
                break;
            }
            case REPLACE: {
                boolean found = SearchEngine.replace(templateEditor.getEditingTemplatePane(), context).wasFound();
                if (!found) {
                    JOptionPane.showMessageDialog(this, TEXT_NOT_FOUND_MSG);
                }
                break;
            }
            case REPLACE_ALL: {
                boolean found = SearchEngine.replaceAll(templateEditor.getEditingTemplatePane(), context).wasFound();
                if (!found) {
                    JOptionPane.showMessageDialog(this, TEXT_NOT_FOUND_MSG);
                }
                break;
            }
            default:
                break;
        }
    }
}
