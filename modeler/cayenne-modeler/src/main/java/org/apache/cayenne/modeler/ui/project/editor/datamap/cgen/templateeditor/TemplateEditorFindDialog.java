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
import org.apache.cayenne.modeler.toolkit.AppDialog;
import org.apache.cayenne.util.Util;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

public class TemplateEditorFindDialog extends AppDialog {

    protected static final String TEXT_NOT_FOUND_MSG = "Text not found";
    protected static final String FIND_NEXT = "FindNext";
    private static final String FIND_PREV = "FindPrev";

    protected static final String COLUMN_SPECS = "left:pref, $lcgap, left:pref,$lcgap, 110dlu, $lcgap, fill:p:grow";
    protected static final String ROW_SPECS = "4 * (p, $rgap)";

    protected final TemplateEditor templateEditor;
    protected final JTextField searchField;
    protected final JCheckBox regexCB;
    protected final JCheckBox matchCaseCB;
    protected final JCheckBox wholeWordCB;
    protected final JButton nextButton;
    private final JButton prevButton;

    public TemplateEditorFindDialog(Application app, TemplateEditor templateEditor) {
        this(app, templateEditor, "Find dialog");
    }

    protected TemplateEditorFindDialog(Application app, TemplateEditor templateEditor, String title) {
        super(app, templateEditor, title, ModalityType.APPLICATION_MODAL);
        this.templateEditor = templateEditor;
        this.searchField = new JTextField();
        this.regexCB = new JCheckBox("Regex");
        this.matchCaseCB = new JCheckBox("Match Case");
        this.wholeWordCB = new JCheckBox("Whole word");
        this.nextButton = new JButton("Find Next");
        this.prevButton = new JButton("Find Previous");

        initLayout();
        initBindings();
        setResizable(false);
    }

    protected void initLayout() {
        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(new FormLayout(COLUMN_SPECS, ROW_SPECS));
        builder.setDefaultDialogBorder();

        builder.addLabel("Find what:", cc.xy(1, 1));
        builder.add(searchField, cc.xyw(3, 1, 3));
        builder.add(regexCB, cc.xy(1, 3));
        builder.add(matchCaseCB, cc.xy(3, 3));
        builder.add(wholeWordCB, cc.xy(5, 3));
        builder.add(nextButton, cc.xy(7, 1));
        builder.add(prevButton, cc.xy(7, 3));

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
    }

    protected void initBindings() {
        nextButton.setActionCommand(FIND_NEXT);
        nextButton.addActionListener(this::onAction);
        prevButton.setActionCommand(FIND_PREV);
        prevButton.addActionListener(this::onAction);
    }

    protected void onAction(ActionEvent e) {
        boolean forward = FIND_NEXT.equals(e.getActionCommand());
        SearchContext context = buildSearchContext(forward);
        if (context == null) {
            return;
        }

        boolean found = SearchEngine.find(templateEditor.getEditingTemplatePane(), context).wasFound();
        if (!found) {
            JOptionPane.showMessageDialog(this, TEXT_NOT_FOUND_MSG);
        }
    }

    protected SearchContext buildSearchContext(boolean forward) {
        String text = searchField.getText();
        if (Util.isEmptyString(text)) {
            return null;
        }
        SearchContext context = new SearchContext();
        context.setSearchFor(text);
        context.setMatchCase(matchCaseCB.isSelected());
        context.setRegularExpression(regexCB.isSelected());
        context.setWholeWord(wholeWordCB.isSelected());
        context.setSearchForward(forward);
        return context;
    }
}
