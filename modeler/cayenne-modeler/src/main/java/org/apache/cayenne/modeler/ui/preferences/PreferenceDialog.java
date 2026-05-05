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

package org.apache.cayenne.modeler.ui.preferences;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.toolkit.border.TopBorder;
import org.apache.cayenne.modeler.toolkit.AppDialog;
import org.apache.cayenne.modeler.toolkit.AppPanel;
import org.apache.cayenne.modeler.ui.preferences.classpath.ClasspathPrefsPanel;
import org.apache.cayenne.modeler.ui.preferences.dbconnector.DBConnectorPrefsPanel;
import org.apache.cayenne.modeler.ui.preferences.general.GeneralPrefsPanel;
import org.apache.cayenne.modeler.ui.preferences.more.MorePrefsPanel;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Window;

public class PreferenceDialog extends AppDialog {

    private static final String GENERAL_KEY = "General";
    private static final String DB_CONNECTORS_KEY = "DB Connectors";
    private static final String CLASSPATH_KEY = "Classpath";
    private static final String MORE_KEY = "More...";

    private static final String[] PREFERENCE_MENUS = new String[]{
            GENERAL_KEY, DB_CONNECTORS_KEY, CLASSPATH_KEY, MORE_KEY
    };

    private static final int LIST_CELL_LEFT_PAD = 8;
    private static final int LIST_CELL_RIGHT_PAD = 12;
    private static final int LIST_MIN_WIDTH = 120;
    private static final int LIST_HEIGHT = 400;

    // Session-only memory of which card the user last viewed. The dialog is
    // recreated on every open, so an instance field would always reset to General.
    private static String lastSelectedCard = GENERAL_KEY;

    private final JList<String> menu;
    private final JPanel leftContainer;
    private final CardLayout detailLayout;
    private final JPanel detailPanel;
    private final JButton saveButton;
    private final JButton cancelButton;

    private final GeneralPrefsPanel generalPrefs;
    private final ClasspathPrefsPanel classpathPrefs;
    private final DBConnectorPrefsPanel dbConnectorPrefs;
    private final MorePrefsPanel morePrefs;

    public PreferenceDialog(Application app, Window owner) {
        super(app, owner, "Edit Preferences", ModalityType.MODELESS);

        this.menu = new JList<>();
        this.detailLayout = new CardLayout();
        this.detailPanel = new JPanel(detailLayout);
        this.leftContainer = new JPanel(new BorderLayout());
        this.saveButton = new JButton("Save");
        this.cancelButton = new JButton("Cancel");

        this.generalPrefs = new GeneralPrefsPanel(app);
        // classpath BEFORE dbConnector — DBConnectorPreferences needs a reference to it
        this.classpathPrefs = new ClasspathPrefsPanel(app);
        this.dbConnectorPrefs = new DBConnectorPrefsPanel(app, classpathPrefs);
        this.morePrefs = new MorePrefsPanel(app);

        initLayout();
        initBindings();
        addCards();
    }

    public void showLastSelectedAction() {
        doShow(lastSelectedCard);
    }

    public void showClassPathEditorAction() {
        doShow(CLASSPATH_KEY);
    }

    public void showDBConnectorEditorAction(Object connectorKey) {
        if (connectorKey != null) {
            dbConnectorPrefs.editConnectorAction(connectorKey);
        }
        doShow(DB_CONNECTORS_KEY);
    }

    private void doShow(String cardKey) {
        AppPanel child = panelFor(cardKey);

        showCard(GENERAL_KEY);
        pack();
        centerOnOwner();
        makeCloseableOnEscape();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        showCard(cardKey);
        child.setEnabled(true);
        setVisible(true);
    }

    private AppPanel panelFor(String cardKey) {
        switch (cardKey) {
            case DB_CONNECTORS_KEY:
                return dbConnectorPrefs;
            case CLASSPATH_KEY:
                return classpathPrefs;
            case MORE_KEY:
                return morePrefs;
            case GENERAL_KEY:
            default:
                return generalPrefs;
        }
    }

    private void initLayout() {
        menu.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBorder(BorderFactory.createEmptyBorder(5, LIST_CELL_LEFT_PAD, 5, LIST_CELL_RIGHT_PAD));
                return this;
            }
        });
        menu.setFont(new JLabel().getFont().deriveFont(Font.BOLD, 12));

        JScrollPane scrollPane = new JScrollPane(menu);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        leftContainer.add(scrollPane);
        leftContainer.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY));

        JPanel center = new JPanel(new BorderLayout());
        center.setBorder(TopBorder.create());
        center.add(leftContainer, BorderLayout.WEST);
        center.add(detailPanel, BorderLayout.CENTER);

        getRootPane().setDefaultButton(saveButton);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancelButton);
        buttons.add(saveButton);
        buttons.setBorder(TopBorder.create());

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(center, BorderLayout.CENTER);
        contentPane.add(buttons, BorderLayout.SOUTH);

        menu.setListData(PREFERENCE_MENUS);
        sizeListToLabels();
    }

    private void initBindings() {
        menu.addListSelectionListener(e -> {
            String selection = menu.getSelectedValue();
            if (selection != null) {
                lastSelectedCard = selection;
                showCard(selection);
            }
        });
        cancelButton.addActionListener(e -> {
            dbConnectorPrefs.discard();
            dispose();
        });
        saveButton.addActionListener(e -> {
            dbConnectorPrefs.commit();
            generalPrefs.commit();
            classpathPrefs.commit();
            dispose();
        });
    }

    private void addCards() {
        detailPanel.add(generalPrefs, GENERAL_KEY);
        detailPanel.add(dbConnectorPrefs, DB_CONNECTORS_KEY);
        detailPanel.add(classpathPrefs, CLASSPATH_KEY);
        detailPanel.add(morePrefs, MORE_KEY);
    }

    private void showCard(String name) {
        detailLayout.show(detailPanel, name);
        menu.setSelectedValue(name, true);
    }

    private void sizeListToLabels() {
        FontMetrics fm = menu.getFontMetrics(menu.getFont());
        int maxText = 0;
        for (int i = 0; i < menu.getModel().getSize(); i++) {
            maxText = Math.max(maxText, fm.stringWidth(menu.getModel().getElementAt(i)));
        }
        int width = Math.max(LIST_MIN_WIDTH, maxText + LIST_CELL_LEFT_PAD + LIST_CELL_RIGHT_PAD);
        leftContainer.setPreferredSize(new Dimension(width, LIST_HEIGHT));
        leftContainer.setMinimumSize(new Dimension(width, 0));
    }
}
