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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JTextField;
import java.awt.BorderLayout;

/**
 * @since 5.0
 */
public class FindView extends JDialog {

    protected JTextField searchField;
    protected JCheckBox regexCB;
    protected JCheckBox matchCaseCB;
    protected JCheckBox wholeWordCB;
    protected JButton nextButton;
    private JButton prevButton;
    protected static final String COLUMN_SPECS = "left:pref, 3dlu, left:pref,3dlu, 110dlu, 3dlu, fill:p:grow";
    protected static final String ROW_SPECS = "4 * (p, 3dlu)";

    public FindView() {
        initComponents();
        buildView();
    }

    protected void initComponents() {
        this.searchField = new JTextField();
        this.regexCB = new JCheckBox("Regex");
        this.matchCaseCB = new JCheckBox("Match Case");
        this.wholeWordCB = new JCheckBox("Whole word");
        this.nextButton = new JButton("Find Next");
        this.prevButton = new JButton("Find Previous");
    }

    protected void buildView() {
        this.setTitle("Find dialog");
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

    public JTextField getSearchField() {
        return searchField;
    }

    public JCheckBox getRegexCB() {
        return regexCB;
    }

    public JCheckBox getMatchCaseCB() {
        return matchCaseCB;
    }

    public JButton getNextButton() {
        return nextButton;
    }

    public JButton getPrevButton() {
        return prevButton;
    }

    public JCheckBox getWholeWordCB() {
        return wholeWordCB;
    }
}
