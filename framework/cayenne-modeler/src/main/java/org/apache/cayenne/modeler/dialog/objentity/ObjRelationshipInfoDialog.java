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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.tree.TreePath;

import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.modeler.util.EntityTreeFilter;
import org.apache.cayenne.modeler.util.EntityTreeModel;
import org.apache.cayenne.modeler.util.MultiColumnBrowser;
import org.apache.cayenne.modeler.util.PanelFactory;
import org.scopemvc.view.swing.SAction;
import org.scopemvc.view.swing.SButton;
import org.scopemvc.view.swing.SComboBox;
import org.scopemvc.view.swing.SLabel;
import org.scopemvc.view.swing.SListCellRenderer;
import org.scopemvc.view.swing.SPanel;
import org.scopemvc.view.swing.STextField;
import org.scopemvc.view.swing.SwingView;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A view of the dialog for mapping an ObjRelationship to one or more DbRelationships.
 * 
 * @since 1.1
 */
public class ObjRelationshipInfoDialog extends SPanel {

    static final Dimension BROWSER_CELL_DIM = new Dimension(130, 200);

    /**
     * Browser to select path for flattened relationship
     */
    protected MultiColumnBrowser pathBrowser;

    protected Component collectionTypeLabel;
    protected SComboBox collectionTypeCombo;
    protected Component mapKeysLabel;
    protected SComboBox mapKeysCombo;

    public ObjRelationshipInfoDialog() {
        init();
    }

    protected void init() {
        // create widgets
        SButton saveButton = new SButton(new SAction(
                ObjRelationshipInfoController.SAVE_CONTROL));
        saveButton.setEnabled(true);

        SButton cancelButton = new SButton(new SAction(
                ObjRelationshipInfoController.CANCEL_CONTROL));
        cancelButton.setEnabled(true);

        SButton newRelButton = new SButton(new SAction(
                ObjRelationshipInfoController.NEW_REL_CONTROL));
        newRelButton.setEnabled(true);

        SButton selectPathButton = new SButton(new SAction(
                ObjRelationshipInfoController.SELECT_PATH_CONTROL));
        selectPathButton.setEnabled(true);

        STextField relationshipName = new STextField(25);
        relationshipName.setSelector(ObjRelationshipInfoModel.RELATIONSHIP_NAME_SELECTOR);

        SLabel currentPathLabel = new SLabel();
        currentPathLabel.setSelector(ObjRelationshipInfoModel.CURRENT_PATH_SELECTOR);

        SLabel sourceEntityLabel = new SLabel();
        sourceEntityLabel
                .setSelector(ObjRelationshipInfoModel.SOURCE_ENTITY_NAME_SELECTOR);

        SComboBox targetCombo = new SComboBox();
        targetCombo.setSelector(ObjRelationshipInfoModel.OBJECT_TARGETS_SELECTOR);
        targetCombo.setSelectionSelector(ObjRelationshipInfoModel.OBJECT_TARGET_SELECTOR);
        SListCellRenderer renderer = (SListCellRenderer) targetCombo.getRenderer();
        renderer.setTextSelector("name");

        collectionTypeCombo = new SComboBox();
        collectionTypeCombo
                .setSelector(ObjRelationshipInfoModel.TARGET_COLLECTIONS_SELECTOR);
        collectionTypeCombo
                .setSelectionSelector(ObjRelationshipInfoModel.TARGET_COLLECTION_SELECTOR);

        mapKeysCombo = new SComboBox();
        mapKeysCombo.setSelector(ObjRelationshipInfoModel.MAP_KEYS_SELECTOR);
        mapKeysCombo.setSelectionSelector(ObjRelationshipInfoModel.MAP_KEY_SELECTOR);

        pathBrowser = new ObjRelationshipPathBrowser();
        pathBrowser.setPreferredColumnSize(BROWSER_CELL_DIM);
        pathBrowser.setDefaultRenderer();

        // enable/disable map keys for collection type selection
        collectionTypeCombo.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent action) {
                updateCollectionChoosers();
            }
        });

        // assemble
        setDisplayMode(SwingView.MODAL_DIALOG);
        setTitle("ObjRelationship Inspector");
        setLayout(new BorderLayout());

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(
                new FormLayout(
                        "right:max(50dlu;pref), 3dlu, fill:min(150dlu;pref), 3dlu, 300dlu, 3dlu, fill:min(120dlu;pref)",
                        "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, top:14dlu, 3dlu, top:p:grow"));
        builder.setDefaultDialogBorder();

        builder.addSeparator("ObjRelationship Information", cc.xywh(1, 1, 5, 1));
        builder.addLabel("Relationship:", cc.xy(1, 3));
        builder.add(relationshipName, cc.xywh(3, 3, 1, 1));

        builder.addLabel("Current Db Path:", cc.xy(1, 5));
        builder.add(currentPathLabel, cc.xywh(3, 5, 5, 1));

        builder.addLabel("Source:", cc.xy(1, 7));
        builder.add(sourceEntityLabel, cc.xywh(3, 7, 1, 1));
        builder.addLabel("Target:", cc.xy(1, 9));
        builder.add(targetCombo, cc.xywh(3, 9, 1, 1));
        collectionTypeLabel = builder.addLabel("Collection Type:", cc.xy(1, 11));
        builder.add(collectionTypeCombo, cc.xywh(3, 11, 1, 1));
        mapKeysLabel = builder.addLabel("Map Key:", cc.xy(1, 13));
        builder.add(mapKeysCombo, cc.xywh(3, 13, 1, 1));

        builder.addSeparator("Mapping to DbRelationships", cc.xywh(1, 15, 5, 1));

        JPanel buttonsPane = new JPanel(new FlowLayout(FlowLayout.LEADING));
        buttonsPane.add(selectPathButton);
        buttonsPane.add(newRelButton);

        builder.add(buttonsPane, cc.xywh(1, 17, 5, 1));
        builder.add(new JScrollPane(
                pathBrowser,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), cc.xywh(1, 19, 5, 3));

        add(builder.getPanel(), BorderLayout.CENTER);
        add(PanelFactory.createButtonPanel(new JButton[] {
                saveButton, cancelButton
        }), BorderLayout.SOUTH);
    }

    /**
     * @return relationship path browser
     */
    public MultiColumnBrowser getPathBrowser() {
        return pathBrowser;
    }

    void initFromModel() {
        // called too early in the cycle...
        if (!updateCollectionChoosers()) {
            return;
        }

        ObjRelationshipInfoModel model = (ObjRelationshipInfoModel) getController()
                .getModel();

        if (pathBrowser.getModel() == null) {
            EntityTreeModel treeModel = new EntityTreeModel(model.getStartEntity());
            treeModel.setFilter(new EntityTreeFilter() {

                public boolean attributeMatch(Object node, Attribute attr) {
                    // attrs not allowed here
                    return false;
                }

                public boolean relationshipMatch(Object node, Relationship rel) {
                    if (!(node instanceof Relationship)) {
                        return true;
                    }

                    /**
                     * We do not allow A->B->A chains, where relationships are to-one
                     */
                    DbRelationship prev = (DbRelationship) node;
                    return !(!rel.isToMany() && prev.getReverseRelationship() == rel);
                }

            });

            pathBrowser.setModel(treeModel);

            setSelectionPath(model.getSavedDbRelationships());
        }
    }

    /**
     * Selects path in browser
     */
    void setSelectionPath(List<DbRelationship> rels) {
        ObjRelationshipInfoModel model = (ObjRelationshipInfoModel) getController()
                .getModel();

        Object[] path = new Object[rels.size() + 1];
        path[0] = model.getStartEntity();

        System.arraycopy(rels.toArray(), 0, path, 1, rels.size());

        pathBrowser.setSelectionPath(new TreePath(path));
    }

    /**
     * Updates 'collection type' and 'map keys' comboboxes
     */
    boolean updateCollectionChoosers() {
        if (getController() == null || getController().getModel() == null) {
            return false;
        }

        ObjRelationshipInfoModel model = (ObjRelationshipInfoModel) getController()
                .getModel();

        boolean collectionTypeEnabled = model.isToMany();
        collectionTypeCombo.setEnabled(collectionTypeEnabled);
        collectionTypeLabel.setEnabled(collectionTypeEnabled);

        boolean mapKeysEnabled = collectionTypeEnabled
                && ObjRelationshipInfoModel.COLLECTION_TYPE_MAP
                        .equals(collectionTypeCombo.getSelectedItem());
        mapKeysCombo.setEnabled(mapKeysEnabled);
        mapKeysLabel.setEnabled(mapKeysEnabled);

        return true;
    }

}
