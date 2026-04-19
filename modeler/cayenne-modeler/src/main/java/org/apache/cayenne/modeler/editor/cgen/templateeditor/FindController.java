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

import org.apache.cayenne.modeler.mvc.ChildController;
import org.apache.cayenne.util.Util;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * @since 5.0
 */
public class FindController extends ChildController<TemplateEditorController> {

    private FindView view;
    protected TemplateEditorView parentView;
    private JButton nextButton;
    private JButton prevButton;
    protected static final String TEXT_NOT_FOUND_MSG = "Text not found";
    protected static final String FIND_NEXT = "FindNext";
    private static final String FIND_PREV = "FindPrev";

    public FindController(TemplateEditorController parent) {
        super(parent);
        this.parentView = parent.getView();
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
        nextButton.setActionCommand(FIND_NEXT);
        nextButton.addActionListener(this::actionPerformed);
        prevButton.setActionCommand(FIND_PREV);
        prevButton.addActionListener(this::actionPerformed);
    }


    protected void actionPerformed(ActionEvent e) {

        // "FindNext" => search forward, "FindPrev" => search backward
        String command = e.getActionCommand();
        boolean forward = FIND_NEXT.equals(command);

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
