/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.modeler.dialog.db;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.modeler.util.CayenneDialog;

/**
 * @since 4.0
 */
public class DbActionOptionsDialog extends CayenneDialog {

    protected static final String WILDCARD_PATTERN = ".*";
    public static final int CANCEL = 0;
    public static final int SELECT = 1;

    private int choice;
    private JLabel schemaLabel;
    private JLabel catalogLabel;
    private JComboBox<String> catalogSelector;
    private JComboBox<String> schemaSelector;
    private JButton selectButton;
    private JButton cancelButton;

    public DbActionOptionsDialog(Frame owner, String title, Collection<String> catalogs, Collection<String> schemas,
                                 String currentCatalog, String currentSchema) throws HeadlessException {
        super(owner, title);
        init();
        initController();
        initFromModel(catalogs, schemas, currentCatalog, currentSchema);

        pack();
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setModal(true);
        centerWindow();
    }

    protected void init() {
        // create widgets...
        selectButton = new JButton("Continue");
        cancelButton = new JButton("Cancel");
        catalogSelector = new JComboBox<>();
        schemaSelector = new JComboBox<>();

        FormLayout layout = new FormLayout(
                "right:pref, 3dlu, fill:max(170dlu;pref):grow",
                "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        initForm(builder);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(selectButton);
        buttons.add(cancelButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    protected void initController() {
        selectButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                processSelect();
            }
        });
        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                processCancel();
            }
        });
    }

    protected void initFromModel(Collection<String> catalogs, Collection<String> schemas, String currentCatalog, String currentSchema) {
        this.choice = CANCEL;

        boolean showSchemaSelector = schemas != null && !schemas.isEmpty();
        schemaSelector.setVisible(showSchemaSelector);
        schemaLabel.setVisible(showSchemaSelector);
        if (showSchemaSelector) {
            schemaSelector.setModel(new DefaultComboBoxModel<>(schemas.toArray(new String[0])));
            if (currentSchema != null) {
                for (String schema : schemas) {
                    if (currentSchema.equalsIgnoreCase(schema)) {
                        schemaSelector.setSelectedItem(schema);
                        break;
                    }
                }
            }
        }

        boolean showCatalogSelector = catalogs != null && !catalogs.isEmpty();
        catalogSelector.setVisible(showCatalogSelector);
        catalogLabel.setVisible(showCatalogSelector);
        if (showCatalogSelector) {
            catalogSelector.setModel(new DefaultComboBoxModel<>(catalogs.toArray(new String[0])));
            if (currentCatalog != null && !currentCatalog.isEmpty()) {
                for (String catalog : catalogs) {
                    if (currentCatalog.equalsIgnoreCase(catalog)) {
                        catalogSelector.setSelectedItem(catalog);
                        break;
                    }
                }
            }
        }
    }

    protected void initForm(DefaultFormBuilder builder) {
        catalogLabel = builder.append("Select Catalog:", catalogSelector, true);
        schemaLabel = builder.append("Select Schema:", schemaSelector);
    }

    public int getChoice() {
        return choice;
    }

    private void processSelect() {
        choice = SELECT;
        setVisible(false);
    }

    private void processCancel() {
        choice = CANCEL;
        setVisible(false);
    }

    /**
     * Returns selected catalog.
     */
    public String getSelectedCatalog() {
        String catalog = (String) catalogSelector.getSelectedItem();
        return "".equals(catalog) ? null : catalog;
    }

    /**
     * Returns selected schema.
     */
    public String getSelectedSchema() {
        String schema = (String) schemaSelector.getSelectedItem();
        return "".equals(schema) ? null : schema;
    }
}
