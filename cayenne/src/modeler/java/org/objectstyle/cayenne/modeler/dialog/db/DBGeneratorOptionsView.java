/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.modeler.dialog.db;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Wizard for generating the database from the data map.
 */
public class DBGeneratorOptionsView extends JDialog {

    protected JTextArea sql;
    protected JButton generateButton;
    protected JButton cancelButton;
    protected JButton saveSqlButton;
    protected JCheckBox dropTables;
    protected JCheckBox createTables;
    protected JCheckBox createFK;
    protected JCheckBox createPK;
    protected JCheckBox dropPK;
    protected Component tables;
    protected JTabbedPane tabs;

    public DBGeneratorOptionsView(Component tables) {
        // create widgets
        this.generateButton = new JButton("Generate");
        this.cancelButton = new JButton("Close");
        this.saveSqlButton = new JButton("Save SQL");
        this.dropTables = new JCheckBox("Drop Tables");
        this.createTables = new JCheckBox("Create Tables");
        this.createFK = new JCheckBox("Create FK Support");
        this.createPK = new JCheckBox("Create Primary Key Support");
        this.dropPK = new JCheckBox("Drop Primary Key Support");
        this.tables = tables;
        this.tabs = new JTabbedPane(JTabbedPane.TOP);
        this.sql = new JTextArea();
        sql.setEditable(false);
        sql.setLineWrap(true);
        sql.setWrapStyleWord(true);

        // assemble...
        JPanel optionsPane = new JPanel(new GridLayout(3, 2));
        optionsPane.add(dropTables);
        optionsPane.add(createTables);
        optionsPane.add(new JLabel());
        optionsPane.add(createFK);
        optionsPane.add(dropPK);
        optionsPane.add(createPK);

        JPanel sqlTextPanel = new JPanel(new BorderLayout());
        sqlTextPanel.add(new JScrollPane(
                sql,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(new FormLayout(
                "fill:min(50dlu;pref):grow",
                "p, 3dlu, p, 9dlu, p, 3dlu, fill:40dlu:grow"));
        builder.setDefaultDialogBorder();
        builder.addSeparator("Options", cc.xywh(1, 1, 1, 1));
        builder.add(optionsPane, cc.xy(1, 3, "left,fill"));
        builder.addSeparator("Generated SQL", cc.xywh(1, 5, 1, 1));
        builder.add(sqlTextPanel, cc.xy(1, 7));

        tabs.addTab("SQL Options", builder.getPanel());
        tabs.addTab("Tables", new JScrollPane(
                tables,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));

        // we need the right preferred size so that dialog "pack()" produces decent
        // default size...
        tabs.setPreferredSize(new Dimension(450, 350));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(saveSqlButton);
        buttons.add(Box.createHorizontalStrut(20));
        buttons.add(cancelButton);
        buttons.add(generateButton);

        Container contentPane = this.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(tabs, BorderLayout.CENTER);
        contentPane.add(buttons, BorderLayout.SOUTH);
    }

    public JButton getCancelButton() {
        return cancelButton;
    }

    public JTabbedPane getTabs() {
        return tabs;
    }

    public JCheckBox getCreateFK() {
        return createFK;
    }

    public JCheckBox getCreatePK() {
        return createPK;
    }

    public JCheckBox getCreateTables() {
        return createTables;
    }

    public JCheckBox getDropPK() {
        return dropPK;
    }

    public JCheckBox getDropTables() {
        return dropTables;
    }

    public JButton getGenerateButton() {
        return generateButton;
    }

    public JButton getSaveSqlButton() {
        return saveSqlButton;
    }

    public JTextArea getSql() {
        return sql;
    }
}