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

package org.apache.cayenne.modeler.dialog.templateeditor;

import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.util.Util;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * since 4.3
 */
public class FindController extends CayenneController implements ActionListener {

    private  FindView view;
    protected TemplateEditorView parentView;
    private  JButton nextButton;
    private  JButton prevButton;
    protected static final String TEXT_NOT_FOUND_MSG = "Text not found";

    public FindController(TemplateEditorController parent) {
        super(parent);
        this.parentView = (TemplateEditorView) parent.getView();
        initComponents();
        initListeners();
    }

    protected void initComponents() {
        this.view = new FindView();
        this.nextButton = this.view.getNextButton();
        this.prevButton = this.view.getPrevButton();
    }

    @Override
    public Component getView() {
        return view;
    }

    protected void initListeners() {
        nextButton.setActionCommand("FindNext");
        nextButton.addActionListener(this);
        prevButton.setActionCommand("FindPrev");
        prevButton.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        // "FindNext" => search forward, "FindPrev" => search backward
        String command = e.getActionCommand();
        boolean forward = "FindNext".equals(command);

        SearchContext context = getSearchContext(forward, view);
        if (context == null) return;

        boolean found = SearchEngine.find(parentView.getEditingTemplatePane(), context).wasFound();
        if (!found) {
            JOptionPane.showMessageDialog(view, TEXT_NOT_FOUND_MSG);
        }
    }

    protected SearchContext getSearchContext(boolean forward, FindView view) {
        SearchContext context = new SearchContext();
        String text = view.getSearchField().getText();
        if (Util.isEmptyString(text)) {
            return null;
        }
        context.setSearchFor(text);
        context.setMatchCase(view.getMatchCaseCB().isSelected());
        context.setRegularExpression(view.getRegexCB().isSelected());
        context.setWholeWord(view.getWholeWordCB().isSelected());
        context.setSearchForward(forward);
        return context;
    }

    /**
     * Pops up a dialog and blocks current thread until the dialog is closed.
     */
    public void startupAction() {
        view.setModal(true);
        view.pack();
        view.setResizable(false);
        makeCloseableOnEscape();
        centerView();
        view.setVisible(true);
    }
}
