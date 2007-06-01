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
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.objectstyle.cayenne.modeler.CayenneModelerFrame;
import org.objectstyle.cayenne.modeler.util.CayenneDialog;
import org.objectstyle.cayenne.modeler.util.PanelFactory;

/**
 * @author Andrei Adamchik
 */
public class DbLoaderMergeDialog extends CayenneDialog {

    protected DbLoaderHelper helper;
    protected JCheckBox rememberSelection;
    protected JLabel message;
    protected JButton overwriteButton;
    protected JButton skipButton;
    protected JButton stopButton;

    public DbLoaderMergeDialog(CayenneModelerFrame owner) {
        super(owner);
        init();
        initController();
    }

    private void init() {
        // create widgets
        this.rememberSelection = new JCheckBox("Remember my decision for other entities.");
        this.rememberSelection.setSelected(true);

        this.overwriteButton = new JButton("Overwrite");
        this.skipButton = new JButton("Skip");
        this.stopButton = new JButton("Stop");
        this.message = new JLabel("DataMap already contains this table. Overwrite?");

        // assemble
        JPanel messagePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        messagePanel.add(message);

        JPanel checkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        checkPanel.add(rememberSelection);

        JPanel buttons = PanelFactory.createButtonPanel(new JButton[] {
                skipButton, overwriteButton, stopButton
        });

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(messagePanel, BorderLayout.NORTH);
        contentPane.add(checkPanel, BorderLayout.CENTER);
        contentPane.add(buttons, BorderLayout.SOUTH);

        setModal(true);
        setResizable(false);
        setSize(250, 150);
        setTitle("DbEntity Already Exists");
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    }

    private void initController() {
        overwriteButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateModel(true, false);
            }
        });

        skipButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateModel(false, false);
            }
        });

        stopButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateModel(false, true);
            }
        });
    }

    private void updateModel(boolean overwrite, boolean stop) {
        if (helper != null) {
            helper.setOverwritePreferenceSet(rememberSelection.isSelected());
            helper.setOverwritingEntities(overwrite);
            helper.setStoppingReverseEngineering(stop);
        }

        this.hide();
    }

    public void initFromModel(DbLoaderHelper helper, String tableName) {
        this.helper = helper;
        this.message.setText("DataMap already contains table '"
                + tableName
                + "'. Overwrite?");

        validate();
        pack();
    }
}