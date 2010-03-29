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
package org.apache.cayenne.modeler.dialog.objentity;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.CayenneTable;
import org.apache.cayenne.modeler.util.CayenneWidgetFactory;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.modeler.util.MultiColumnBrowser;
import org.apache.cayenne.modeler.util.PanelFactory;
import org.apache.cayenne.modeler.util.combo.AutoCompletion;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class ObjAttributeInfoDialogView extends JDialog {

    /**
     * // * Browser to select path for attribute //
     */
    protected MultiColumnBrowser pathBrowser;

    protected JButton cancelButton;
    protected JButton saveButton;
    protected JButton selectPathButton;

    protected JTextField attributeName;
    protected JLabel currentPathLabel;
    protected JLabel sourceEntityLabel;

    protected JComboBox type;
    protected JPanel typeManagerPane;

    protected CayenneTable overrideAttributeTable;

    ProjectController mediator;

    static final Dimension BROWSER_CELL_DIM = new Dimension(130, 200);
    
    static final String EMBEDDABLE_PANEL = "EMBEDDABLE_PANEL"; 
    static final String FLATTENED_PANEL = "FLATTENED_PANEL"; 

    public ObjAttributeInfoDialogView(final ProjectController mediator) {

        this.mediator = mediator;

        // create widgets
        this.cancelButton = new JButton("Cancel");
        this.saveButton = new JButton("Done");
        this.selectPathButton = new JButton("Select path");

        this.attributeName = new JTextField(25);
        this.currentPathLabel = new JLabel();
        this.sourceEntityLabel = new JLabel();

        this.type = CayenneWidgetFactory.createComboBox(ModelerUtil
                .getRegisteredTypeNames(), false);
        AutoCompletion.enable(type, false, true);
        type.getRenderer();

        overrideAttributeTable = new CayenneTable();

        saveButton.setEnabled(false);
        cancelButton.setEnabled(true);
        selectPathButton.setEnabled(false);

        setTitle("ObjAttribute Inspector");
        setLayout(new BorderLayout());

        CellConstraints cc = new CellConstraints();
        final PanelBuilder builder = new PanelBuilder(
                new FormLayout(
                        "right:max(50dlu;pref), 3dlu, 200dlu, 15dlu, right:max(30dlu;pref), 3dlu, 200dlu",
                        "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 6dlu, p, 6dlu, p, 3dlu, fill:p:grow"));
        builder.setDefaultDialogBorder();
        builder.addSeparator("ObjAttribute Information", cc.xywh(1, 1, 7, 1));

        builder.addLabel("Attribute:", cc.xy(1, 3));
        builder.add(attributeName, cc.xywh(3, 3, 1, 1));

        builder.addLabel("Current Db Path:", cc.xy(1, 5));
        builder.add(currentPathLabel, cc.xywh(3, 5, 5, 1));

        builder.addLabel("Source:", cc.xy(1, 7));
        builder.add(sourceEntityLabel, cc.xywh(3, 7, 1, 1));

        builder.addLabel("Type:", cc.xy(1, 9));
        builder.add(type, cc.xywh(3, 9, 1, 1));

        builder.addSeparator("Mapping to Attributes", cc.xywh(1, 11, 7, 1));

        typeManagerPane = new JPanel();
        typeManagerPane.setLayout(new CardLayout());

        final FormLayout fL = new FormLayout(
                "493dlu ",
                "p, 3dlu, fill:min(128dlu;pref):grow");

        // panel for Flattened attribute
        final PanelBuilder builderPathPane = new PanelBuilder(fL);

        JPanel buttonsPane = new JPanel(new FlowLayout(FlowLayout.LEADING));
        buttonsPane.add(selectPathButton);

        builderPathPane.add(buttonsPane, cc.xywh(1, 1, 1, 1));
        pathBrowser = new ObjAttributePathBrowser(selectPathButton, saveButton);
        pathBrowser.setPreferredColumnSize(BROWSER_CELL_DIM);
        pathBrowser.setDefaultRenderer();
        builderPathPane.add(new JScrollPane(
                pathBrowser,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), cc.xywh(1, 3, 1, 1));

        // panel for embeddable attribute
        final FormLayout fLEmb = new FormLayout(
                "493dlu ",
                "fill:min(140dlu;pref):grow");

        final PanelBuilder embeddablePane = new PanelBuilder(fLEmb);

        embeddablePane.add(new JScrollPane(overrideAttributeTable,
              JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
              JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
              cc.xywh(1, 1, 1, 1));

        typeManagerPane.add(builderPathPane.getPanel(), FLATTENED_PANEL);
        typeManagerPane.add(embeddablePane.getPanel(), EMBEDDABLE_PANEL);

        builder.add(typeManagerPane, cc.xywh(1, 13, 7, 1));

        add(builder.getPanel(), BorderLayout.CENTER);

        this.addComponentListener(new ComponentListener() {

            int height;

            public void componentHidden(ComponentEvent e) {
            }

            public void componentMoved(ComponentEvent e) {
            }

            public void componentResized(ComponentEvent e) {
                int delta = e.getComponent().getHeight() - height;
                if (delta < 0) {
                    fL.setRowSpec(3, new RowSpec("fill:min(10dlu;pref):grow"));
                    fLEmb.setRowSpec(1, new RowSpec("fill:min(10dlu;pref):grow"));
                }
            }

            public void componentShown(ComponentEvent e) {
                height = e.getComponent().getHeight();
            }
        });

        add(PanelFactory.createButtonPanel(new JButton[] {
                saveButton, cancelButton
        }), BorderLayout.SOUTH);

        type.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                boolean isType = false;
                String[] typeNames = ModelerUtil.getRegisteredTypeNames();
                for (int i = 0; i < typeNames.length; i++) {
                    if (type.getSelectedItem() == null || typeNames[i].equals(type.getSelectedItem().toString())) {
                        isType = true;
                    }
                }

                Iterator<Embeddable> embs = mediator.getEmbeddableNamesInCurRentDataDomain().iterator();
                ArrayList<String> embNames = new ArrayList<String>();
                while (embs.hasNext()) {
                embNames.add(embs.next().getClassName());
               }
                
                if (isType || !embNames.contains(type.getSelectedItem()) ) {
                    ((CardLayout) typeManagerPane.getLayout()).show(typeManagerPane, FLATTENED_PANEL);
                }
                else {
                    ((CardLayout) typeManagerPane.getLayout()).show(typeManagerPane, EMBEDDABLE_PANEL);
                    getCurrentPathLabel().setText("");
                    
                }
            }

        });
    }

    public CayenneTable getOverrideAttributeTable() {
        return overrideAttributeTable;
    }

    public JComboBox getType() {
        return type;
    }

    public MultiColumnBrowser getPathBrowser() {
        return pathBrowser;
    }

    public JButton getCancelButton() {
        return cancelButton;
    }

    public JButton getSaveButton() {
        return saveButton;
    }

    public JButton getSelectPathButton() {
        return selectPathButton;
    }

    public JTextField getAttributeName() {
        return attributeName;
    }

    public JLabel getCurrentPathLabel() {
        return currentPathLabel;
    }

    public JLabel getSourceEntityLabel() {
        return sourceEntityLabel;
    }
}
