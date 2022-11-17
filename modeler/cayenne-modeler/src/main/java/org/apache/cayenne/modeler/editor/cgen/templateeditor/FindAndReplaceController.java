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

import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @since 5.0
 */
public class FindAndReplaceController extends FindController implements ActionListener {

    private  FindAndReplaceView view;
    private  JButton nextButton;
    private  JButton replaceButton;
    private  JButton replaceAllButton;
    private static final String REPLACE = "replace";
    private static final String REPLACE_ALL = "replaceAll";

    public FindAndReplaceController(TemplateEditorController parent) {
        super(parent);
        initComponents();
        initListeners();
    }

    @Override
    protected void initComponents() {
        this.view = new FindAndReplaceView();
        this.nextButton = view.getNextButton();
        this.replaceButton = view.getReplaceButton();
        this.replaceAllButton = view.getReplaceAllButton();
    }

    @Override
    protected void initListeners() {
        nextButton.setActionCommand(FIND_NEXT);
        nextButton.addActionListener(this);
        replaceButton.setActionCommand(REPLACE);
        replaceButton.addActionListener(this);
        replaceAllButton.setActionCommand(REPLACE_ALL);
        replaceAllButton.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        SearchContext context = getSearchContext(true,view);
        if (context == null) return;
        switch (command) {
            case FIND_NEXT: {
                boolean found = SearchEngine.find(parentView.getEditingTemplatePane(), context).wasFound();
                if (!found) {
                    JOptionPane.showMessageDialog(view, TEXT_NOT_FOUND_MSG);
                }
                break;
            }
            case REPLACE: {
                boolean found = SearchEngine.replace(parentView.getEditingTemplatePane(), context).wasFound();
                if (!found) {
                    JOptionPane.showMessageDialog(view, TEXT_NOT_FOUND_MSG);
                }
                break;
            }
            case REPLACE_ALL: {
                boolean found = SearchEngine.replaceAll(parentView.getEditingTemplatePane(), context).wasFound();
                if (!found) {
                    JOptionPane.showMessageDialog(view, TEXT_NOT_FOUND_MSG);
                }
                break;
            }
            default:
                break;
        }
    }

    @Override
    protected SearchContext getSearchContext(boolean forward, FindView view) {
        SearchContext context = super.getSearchContext(forward,view);
        context.setReplaceWith(this.view.getReplaceWithField().getText());
        return context;
    }

    @Override
    public Component getView() {
        return view;
    }

    /**
     * Pops up a dialog and blocks current thread until the dialog is closed.
     */
    @Override
    public void startupAction() {
        view.setModal(true);
        view.pack();
        view.setResizable(false);
        makeCloseableOnEscape();
        centerView();
        view.setVisible(true);
    }
}
