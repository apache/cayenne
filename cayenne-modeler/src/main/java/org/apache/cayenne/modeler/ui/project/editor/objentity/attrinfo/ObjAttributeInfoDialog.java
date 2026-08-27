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
package org.apache.cayenne.modeler.ui.project.editor.objentity.attrinfo;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.EmbeddedAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.event.display.ObjAttributeDisplayEvent;
import org.apache.cayenne.modeler.event.display.ObjEntityDisplayEvent;
import org.apache.cayenne.modeler.event.model.ObjAttributeEvent;
import org.apache.cayenne.modeler.event.model.ObjEntityEvent;
import org.apache.cayenne.modeler.toolkit.columnview.ColumnViewPanel;
import org.apache.cayenne.modeler.toolkit.valuetype.ValueTypes;
import org.apache.cayenne.modeler.toolkit.buttons.CMButtonPanel;
import org.apache.cayenne.modeler.toolkit.combobox.AutoCompletion;
import org.apache.cayenne.modeler.toolkit.combobox.CMComboBox;
import org.apache.cayenne.modeler.toolkit.table.CMTable;
import org.apache.cayenne.modeler.pref.adapters.CMTablePrefs;
import org.apache.cayenne.modeler.toolkit.ProjectDialog;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.ui.project.editor.objentity.properties.ObjAttributeTableModel;
import org.apache.cayenne.modeler.toolkit.tree.EntityTreeAttributeRelationshipFilter;
import org.apache.cayenne.modeler.toolkit.tree.EntityTreeModel;
import org.apache.cayenne.project.extension.info.ObjectInfo;
import org.apache.cayenne.util.CayenneMapEntry;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Modal "ObjAttribute inspector" — edits the source attribute's name, Java type, locking
 * and lazy flags, comment, and the path of DbAttributes/DbRelationships it maps to. When
 * the chosen Java type is an {@link Embeddable}, the path browser is replaced by an
 * embedded-attribute override table.
 */
public class ObjAttributeInfoDialog extends ProjectDialog implements TreeSelectionListener {

    private static final Dimension BROWSER_CELL_DIM = new Dimension(130, 200);

    static final String EMBEDDABLE_PANEL = "EMBEDDABLE_PANEL";
    static final String FLATTENED_PANEL = "FLATTENED_PANEL";

    private final ObjAttributeTableModel model;
    private final int row;
    private OverrideEmbeddableAttributeTableModel embeddableModel;

    private final JButton cancelButton;
    private final JButton saveButton;
    private final JButton selectPathButton;
    private final JTextField attributeName;
    private final JLabel currentPathLabel;
    private final JLabel sourceEntityLabel;
    private final JComboBox<String> typeComboBox;
    private final JPanel typeManagerPane;
    private final CMTable overrideAttributeTable;
    private final JCheckBox usedForLockingCheckBox;
    private final JCheckBox lazyCheckBox;
    private final JTextField commentField;
    private final ColumnViewPanel pathBrowser;

    private final Map<String, Embeddable> stringToEmbeddables = new HashMap<>();
    private final List<String> embeddableNames = new ArrayList<>();

    private ObjAttribute attribute;
    private ObjAttribute attributeSaved;
    private List<DbEntity> relTargets;
    private Object lastObjectType;

    public ObjAttributeInfoDialog(ProjectSession session, Window owner,
                                  int row, ObjAttributeTableModel model) {
        super(session, owner, "ObjAttribute Inspector", ModalityType.APPLICATION_MODAL);
        this.model = model;
        this.row = row;

        // create widgets
        this.cancelButton = new JButton("Cancel");
        this.saveButton = new JButton("Done");
        this.selectPathButton = new JButton("Select path");
        this.attributeName = new JTextField(25);
        this.currentPathLabel = new JLabel();
        this.sourceEntityLabel = new JLabel();

        this.typeComboBox = new CMComboBox<>(ValueTypes.getTypes());
        AutoCompletion.enable(typeComboBox, false, true, session::getSelectedDataMap);
        typeComboBox.getRenderer();

        this.usedForLockingCheckBox = new JCheckBox();
        this.lazyCheckBox = new JCheckBox();
        this.commentField = new JTextField();
        this.overrideAttributeTable = new CMTable();

        this.pathBrowser = new ObjAttributePathNavigatorPanel(selectPathButton, saveButton);
        this.pathBrowser.setPreferredColumnSize(BROWSER_CELL_DIM);
        this.pathBrowser.setDefaultRenderer();

        this.typeManagerPane = new JPanel();
        this.typeManagerPane.setLayout(new CardLayout());

        for (Embeddable emb : session.entityResolver().getEmbeddables()) {
            stringToEmbeddables.put(emb.getClassName(), emb);
            embeddableNames.add(emb.getClassName());
        }

        getRootPane().setDefaultButton(saveButton);
        saveButton.setEnabled(false);
        cancelButton.setEnabled(true);
        selectPathButton.setEnabled(false);
        setLayout(new BorderLayout());

        initLayout();
        initController(model.getAttribute(row));
    }

    public JButton getSaveButton() {
        return saveButton;
    }

    private void initLayout() {
        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(
                new FormLayout(
                        "right:max(50dlu;pref), $lcgap, 200dlu, 15dlu, right:max(30dlu;pref), $lcgap, 200dlu",
                        "p, $rgap, p, $rgap, p, $rgap, p, $rgap, p, $rgap, p, $rgap, p, $rgap, p, $lgap, p, $lgap, p, $rgap, fill:p:grow"));
        builder.setDefaultDialogBorder();
        builder.addSeparator("ObjAttribute Information", cc.xywh(1, 1, 7, 1));

        builder.addLabel("Entity:", cc.xy(1, 3));
        builder.add(sourceEntityLabel, cc.xywh(3, 3, 1, 1));
        builder.addLabel("Attribute Name:", cc.xy(1, 5));
        builder.add(attributeName, cc.xywh(3, 5, 1, 1));
        builder.addLabel("Current Db Path:", cc.xy(1, 7));
        builder.add(currentPathLabel, cc.xywh(3, 7, 5, 1));
        builder.addLabel("Java Type:", cc.xy(1, 9));
        builder.add(typeComboBox, cc.xywh(3, 9, 1, 1));
        builder.addLabel("Used for locking:", cc.xy(1, 11));
        builder.add(usedForLockingCheckBox, cc.xywh(3, 11, 1, 1));
        builder.addLabel("Lazy loading:", cc.xy(1, 13));
        builder.add(lazyCheckBox, cc.xywh(3, 13, 1, 1));
        builder.addLabel("Comment:", cc.xy(1, 15));
        builder.add(commentField, cc.xywh(3, 15, 1, 1));
        builder.addSeparator("Mapping to DbAttributes", cc.xywh(1, 17, 7, 1));

        FormLayout fL = new FormLayout(
                "493dlu",
                "p, $rgap, fill:min(128dlu;pref):grow");
        PanelBuilder builderPathPane = new PanelBuilder(fL);

        JPanel buttonsPane = new JPanel(new FlowLayout(FlowLayout.LEADING));
        buttonsPane.add(selectPathButton);
        builderPathPane.add(buttonsPane, cc.xywh(1, 1, 1, 1));
        builderPathPane.add(new JScrollPane(
                pathBrowser,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), cc.xywh(1, 3, 1, 1));

        FormLayout fLEmb = new FormLayout("493dlu", "fill:min(140dlu;pref):grow");
        PanelBuilder embeddablePane = new PanelBuilder(fLEmb);
        embeddablePane.add(new JScrollPane(overrideAttributeTable,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                cc.xywh(1, 1, 1, 1));

        typeManagerPane.add(builderPathPane.getPanel(), FLATTENED_PANEL);
        typeManagerPane.add(embeddablePane.getPanel(), EMBEDDABLE_PANEL);

        builder.add(typeManagerPane, cc.xywh(1, 19, 7, 1));

        add(builder.getPanel(), BorderLayout.CENTER);

        addComponentListener(new ComponentListener() {
            int height;

            public void componentHidden(ComponentEvent e) {}
            public void componentMoved(ComponentEvent e) {}

            public void componentResized(ComponentEvent e) {
                int delta = e.getComponent().getHeight() - height;
                if (delta < 0) {
                    fL.setRowSpec(3, RowSpec.decode("fill:min(10dlu;pref):grow"));
                    fLEmb.setRowSpec(1, RowSpec.decode("fill:min(10dlu;pref):grow"));
                }
            }

            public void componentShown(ComponentEvent e) {
                height = e.getComponent().getHeight();
            }
        });

        add(new CMButtonPanel(cancelButton, saveButton), BorderLayout.SOUTH);
    }

    private void initController(ObjAttribute attr) {
        for (String embeddableName : embeddableNames) {
            ((DefaultComboBoxModel<String>) typeComboBox.getModel()).addElement(embeddableName);
        }
        // need to register early — reacts to the later setSelectedItem(...) call
        typeComboBox.addActionListener(e -> {
            boolean isType = false;
            String[] typeNames = ValueTypes.getTypes();
            for (String typeName : typeNames) {
                if (typeComboBox.getSelectedItem() == null
                        || typeName.equals(typeComboBox.getSelectedItem().toString())) {
                    isType = true;
                }
            }

            if (isType || session.entityResolver()
                    .getEmbeddable((String) typeComboBox.getSelectedItem()) == null) {
                ((CardLayout) typeManagerPane.getLayout()).show(typeManagerPane, FLATTENED_PANEL);
            } else {
                ((CardLayout) typeManagerPane.getLayout()).show(typeManagerPane, EMBEDDABLE_PANEL);
                currentPathLabel.setText("");
            }
        });

        this.attribute = attr;

        if (attribute instanceof EmbeddedAttribute || embeddableNames.contains(attribute.getType())) {
            this.attributeSaved = new EmbeddedAttribute();
        } else {
            this.attributeSaved = new ObjAttribute();
        }

        copyObjAttribute(attributeSaved, attribute);

        relTargets = new ArrayList<>(attribute.getEntity().getDataMap().getDbEntities());

        // Register auto-selection of the target
        pathBrowser.addTreeSelectionListener(this);

        attributeName.setText(attribute.getName());
        if (attribute.getDbAttributePath() != null) {
            if (attribute.getDbAttributePath().length() > 1) {
                String path = attribute.getDbAttributePath().value();
                currentPathLabel.setText(path.replace(".", " -> "));
            } else {
                currentPathLabel.setText(attribute.getDbAttributePath().value());
            }
        } else {
            currentPathLabel.setText("");
        }
        sourceEntityLabel.setText(attribute.getEntity().getName());
        typeComboBox.setSelectedItem(attribute.getType());
        usedForLockingCheckBox.setSelected(attribute.isUsedForLocking());
        lazyCheckBox.setSelected(attribute.isLazy());
        commentField.setText(ObjectInfo.getFromMetaData(app.getMetaData(), attr, ObjectInfo.COMMENT));

        cancelButton.addActionListener(e -> dispose());
        selectPathButton.addActionListener(e -> setPath(true));
        saveButton.addActionListener(e -> saveMapping());

        // set filter for ObjAttributePathBrowser
        if (pathBrowser.getModel() == null) {
            DbEntity firstEntity = null;
            if (attribute.getDbAttribute() == null) {
                if (attribute.getParent() instanceof ObjEntity) {
                    DbEntity dbEnt = ((ObjEntity) attribute.getParent()).getDbEntity();
                    if (dbEnt != null) {
                        Collection<DbAttribute> attrib = dbEnt.getAttributes();
                        Collection<DbRelationship> rel = dbEnt.getRelationships();
                        if (!attrib.isEmpty()) {
                            firstEntity = attrib.iterator().next().getEntity();
                        } else if (!rel.isEmpty()) {
                            firstEntity = rel.iterator().next().getSourceEntity();
                        }
                    }
                }
            } else {
                firstEntity = getFirstEntity();
            }

            if (firstEntity != null) {
                EntityTreeModel treeModel = new EntityTreeModel(firstEntity);
                treeModel.setFilter(new EntityTreeAttributeRelationshipFilter());
                pathBrowser.setModel(treeModel);
            }
        }

        if (attribute.getDbAttribute() != null) {
            setSelectionPath();
        }

        typeComboBox.addItemListener(e -> {
            if (lastObjectType != null) {
                if (!lastObjectType.equals(e.getItemSelectable())) {
                    if (embeddableNames.contains(e.getItemSelectable().getSelectedObjects()[0].toString())) {
                        EmbeddedAttribute copyAttrSaved = new EmbeddedAttribute();
                        copyObjAttribute(copyAttrSaved, attributeSaved);
                        attributeSaved = copyAttrSaved;
                    } else {
                        if (attributeSaved instanceof EmbeddedAttribute) {
                            ObjAttribute copyAttrSaved = new ObjAttribute();
                            copyObjAttribute(copyAttrSaved, attributeSaved);
                            attributeSaved = copyAttrSaved;
                        }
                    }
                    attributeSaved.setType(e.getItemSelectable().getSelectedObjects()[0].toString());
                    rebuildTable();
                    setEnabledSaveButton();
                }
            }
        });

        attributeName.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {
                if (!attributeName.getText().equals(attribute.getName())) {
                    setEnabledSaveButton();
                }
            }
            public void keyReleased(KeyEvent e) {
                if (!attributeName.getText().equals(attribute.getName())) {
                    setEnabledSaveButton();
                }
            }
            public void keyTyped(KeyEvent e) {}
        });

        rebuildTable();
    }

    private void setEnabledSaveButton() {
        if (!attribute.getDbPathIterator().hasNext()) {
            saveButton.setEnabled(true);
        } else {
            boolean isAttributeLast = false;
            Iterator<CayenneMapEntry> it = attribute.getDbPathIterator();
            while (it.hasNext()) {
                Object obj = it.next();
                if (obj instanceof DbAttribute && !it.hasNext()) {
                    isAttributeLast = true;
                }
            }
            saveButton.setEnabled(isAttributeLast);
        }
    }

    private void setUpTableStructure() {
        DefaultTableCellRenderer renderer = new CellRenderer();

        TableColumn nameColumn = overrideAttributeTable.getColumnModel()
                .getColumn(OverrideEmbeddableAttributeTableModel.OBJ_ATTRIBUTE);
        nameColumn.setCellRenderer(renderer);

        TableColumn typeColumn = overrideAttributeTable.getColumnModel()
                .getColumn(OverrideEmbeddableAttributeTableModel.OBJ_ATTRIBUTE_TYPE);
        typeColumn.setCellRenderer(renderer);

        TableColumn dbAttrColumn = overrideAttributeTable.getColumnModel()
                .getColumn(OverrideEmbeddableAttributeTableModel.DB_ATTRIBUTE);
        dbAttrColumn.setCellRenderer(renderer);

        TableColumn dbAttrTypeColumn = overrideAttributeTable.getColumnModel()
                .getColumn(OverrideEmbeddableAttributeTableModel.DB_ATTRIBUTE_TYPE);
        dbAttrTypeColumn.setCellRenderer(renderer);

        new CMTablePrefs(app.getPrefsManager().uiNode("objEntity/overrideAttributeTable"))
                .bind(overrideAttributeTable, null,
                        OverrideEmbeddableAttributeTableModel.OBJ_ATTRIBUTE);

        initComboBoxes();
    }

    private void initComboBoxes() {
        if (attributeSaved != null) {
            DbEntity currentEnt = attributeSaved.getEntity().getDbEntity();
            if (currentEnt != null) {
                Collection<String> nameAttr = dbAttributeNames(currentEnt);
                embeddableModel.setCellEditor(nameAttr, overrideAttributeTable);
                embeddableModel.setComboBoxes(
                        nameAttr,
                        overrideAttributeTable.convertColumnIndexToView(
                                OverrideEmbeddableAttributeTableModel.DB_ATTRIBUTE));
            }
        }
    }

    private void rebuildTable() {
        String typeName = null;
        Collection<EmbeddableAttribute> embAttrTempCopy = new ArrayList<>();

        if (attributeSaved.getType() != null) {
            typeName = attributeSaved.getType();
        }
        if (embeddableNames.contains(typeName)) {
            Collection<EmbeddableAttribute> embAttrTemp = stringToEmbeddables.get(typeName).getAttributes();
            for (EmbeddableAttribute temp : embAttrTemp) {
                EmbeddableAttribute at = new EmbeddableAttribute();
                at.setDbAttributeName(temp.getDbAttributeName());
                at.setName(temp.getName());
                at.setType(temp.getType());
                at.setEmbeddable(temp.getEmbeddable());
                embAttrTempCopy.add(at);
            }
        }

        embeddableModel = new OverrideEmbeddableAttributeTableModel(session, this, embAttrTempCopy, attributeSaved);

        overrideAttributeTable.setModel(embeddableModel);
        overrideAttributeTable.setRowHeight(25);
        overrideAttributeTable.setRowMargin(3);

        setUpTableStructure();

        if (typeComboBox.getSelectedItem() == null) {
            lastObjectType = "";
        } else {
            lastObjectType = typeComboBox.getSelectedItem();
        }
    }

    public boolean setPath(boolean isChange) {
        if (isModified()) {
            if (typeComboBox.getSelectedItem() != null) {
                attributeSaved.setType(typeComboBox.getSelectedItem().toString());
            }
            attributeSaved.setName(attributeName.getText());
            attributeSaved.setUsedForLocking(usedForLockingCheckBox.isSelected());
            attributeSaved.setLazy(lazyCheckBox.isSelected());
            ObjectInfo.putToMetaData(app.getMetaData(),
                    attributeSaved,
                    ObjectInfo.COMMENT,
                    commentField.getText());
        }

        if (!(attributeSaved instanceof EmbeddedAttribute) || isRegisteredType(attributeSaved.getType())) {

            StringBuilder attributePath = new StringBuilder();
            StringBuilder pathStr = new StringBuilder();
            TreePath path = pathBrowser.getSelectionPath();
            if (attribute.getEntity().getDbEntity() != null && path != null) {
                if (path.getLastPathComponent() instanceof DbAttribute) {
                    Object[] pathComponents = path.getPath();
                    for (int i = 0; i < pathComponents.length; i++) {
                        boolean attrOrRel = true;
                        if (pathComponents[i] instanceof DbAttribute) {
                            pathStr.append(((DbAttribute) pathComponents[i]).getName());
                            attributePath.append(((DbAttribute) pathComponents[i]).getName());
                        } else if (pathComponents[i] instanceof DbRelationship) {
                            pathStr.append(((DbRelationship) pathComponents[i]).getName());
                            attributePath.append(((DbRelationship) pathComponents[i]).getName());
                        } else {
                            attrOrRel = false;
                        }

                        if (i != pathComponents.length - 1 && attrOrRel) {
                            pathStr.append(" -> ");
                            attributePath.append(".");
                        }
                    }
                }
            } else {
                currentPathLabel.setText("");
            }

            currentPathLabel.setText(pathStr.toString());

            if (attribute.getDbAttributePath() != null
                    && ((typeComboBox.getSelectedItem() != null && !embeddableNames.contains(typeComboBox.getSelectedItem().toString()))
                    || typeComboBox.getSelectedItem() == null)) {

                attributeSaved.setDbAttributePath(attributePath.toString());

                if (isChange) {
                    model.setUpdatedValueAt(attributeSaved.getDbAttributePath(), row, 2);
                }
                return true;

            } else {
                if (attributePath.length() > 0
                        || (attribute instanceof EmbeddedAttribute && !(attributeSaved instanceof EmbeddedAttribute))) {

                    attributeSaved.setDbAttributePath(attributePath.toString());
                    if (attributePath.length() == 0) {
                        model.setUpdatedValueAt(attributeSaved.getDbAttributePath(), row, 2);
                        return false;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isModified() {
        boolean isOverrideTableChange =
                ((OverrideEmbeddableAttributeTableModel) overrideAttributeTable.getModel())
                        .isAttributeOverrideChange();
        String comment = ObjectInfo.getFromMetaData(app.getMetaData(), attribute, ObjectInfo.COMMENT);
        return isOverrideTableChange
                || !attribute.getName().equals(attributeName.getText())
                || (attribute.getType() == null && typeComboBox.getSelectedItem() != null)
                || !Objects.equals(attribute.getType(), typeComboBox.getSelectedItem())
                || attribute.isUsedForLocking() != usedForLockingCheckBox.isSelected()
                || attribute.isLazy() != lazyCheckBox.isSelected()
                || !Objects.equals(comment, commentField.getText());
    }

    private void updateTable() {
        String comment = ObjectInfo.getFromMetaData(app.getMetaData(), attributeSaved, ObjectInfo.COMMENT);
        model.setUpdatedValueAt(attributeSaved.getName(), row, 0);
        model.setUpdatedValueAt(attributeSaved.getType(), row, 1);
        model.setUpdatedValueAt(attributeSaved.isUsedForLocking(), row, 4);
        model.setUpdatedValueAt(attributeSaved.isLazy(), row, 5);
        model.setUpdatedValueAt(comment, row, 6);
    }

    private void saveMapping() {
        if (setPath(false)) {
            if (JOptionPane.showConfirmDialog(this,
                    "You have changed Db Attribute path. Do you want it to be saved?", "Save ObjAttribute",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {

                if (attribute instanceof EmbeddedAttribute) {
                    changeAttributeObject();
                } else {
                    updateTable();
                }

                model.setUpdatedValueAt(attributeSaved.getDbAttributePath(), row, 2);
            } else {
                updateTable();
            }
        } else {
            if ((attributeSaved instanceof EmbeddedAttribute && !(attribute instanceof EmbeddedAttribute))
                    || (!(attributeSaved instanceof EmbeddedAttribute) && attribute instanceof EmbeddedAttribute)) {
                changeAttributeObject();
            } else {
                if (attributeSaved instanceof EmbeddedAttribute && embeddableModel.isAttributeOverrideChange()) {
                    Map<String, String> overrides = ((EmbeddedAttribute) attributeSaved).getAttributeOverrides();
                    Map<String, String> currentOverrAttr = getCurrentOverrideAttribute();

                    compareAndSetOverrideInEmbeddedAttribute(attributeSaved, overrides, currentOverrAttr);
                }

                updateTable();
                model.setUpdatedValueAt(attributeSaved.getDbAttributePath(), row, 2);
            }

            if (attributeSaved instanceof EmbeddedAttribute && attribute instanceof EmbeddedAttribute) {
                model.setUpdatedValueAt(attributeSaved.getDbAttributePath(), row, 2);
                if (embeddableModel.isAttributeOverrideChange()) {
                    Map<String, String> overrides = ((EmbeddedAttribute) attribute).getAttributeOverrides();
                    Map<String, String> currentOverrAttr = ((EmbeddedAttribute) attributeSaved).getAttributeOverrides();
                    compareAndSetOverrideInEmbeddedAttribute(attribute, overrides, currentOverrAttr);
                }
            }
        }
        dispose();
    }

    private void changeAttributeObject() {
        if (attributeSaved instanceof EmbeddedAttribute && embeddableModel.isAttributeOverrideChange()) {
            Map<String, String> overrides = ((EmbeddedAttribute) attributeSaved).getAttributeOverrides();
            Map<String, String> currentOverrAttr = getCurrentOverrideAttribute();
            compareAndSetOverrideInEmbeddedAttribute(attributeSaved, overrides, currentOverrAttr);
        }
        if (attributeSaved instanceof EmbeddedAttribute) {
            attributeSaved.setDbAttributePath((String) null);
            model.setUpdatedValueAt(attributeSaved.getDbAttributePath(), row, 2);
        }

        model.getEntity().removeAttribute(attribute.getName());
        model.getEntity().addAttribute(attributeSaved);

        session.fireObjEntityEvent(ObjEntityEvent.ofChange(this, model.getEntity()));

        DataChannelDescriptor domain = (DataChannelDescriptor) session.project().getRootNode();
        ObjEntityDisplayEvent event = new ObjEntityDisplayEvent(this, domain,
                session.getSelectedDataMap(), session.getSelectedObjEntity());
        session.displayObjEntity(event);

        session.fireObjAttributeEvent(ObjAttributeEvent.ofChange(this, attributeSaved, model.getEntity()));

        ObjAttributeDisplayEvent eventAttr = new ObjAttributeDisplayEvent(this, domain,
                session.getSelectedDataMap(), session.getSelectedObjEntity(), attributeSaved);
        session.displayObjAttribute(eventAttr);
    }

    private Map<String, String> getCurrentOverrideAttribute() {
        Map<String, String> currentEmbeddableOverride = new HashMap<>();
        Collection<EmbeddableAttribute> embList = embeddableModel.getEmbeddableList();
        Embeddable emb = stringToEmbeddables.get(attributeSaved.getType());
        for (EmbeddableAttribute e : embList) {
            if ((emb.getAttribute(e.getName()).getDbAttributeName() == null && e.getDbAttributeName() != null)
                    || (emb.getAttribute(e.getName()).getDbAttributeName() != null
                    && !emb.getAttribute(e.getName()).getDbAttributeName().equals(e.getDbAttributeName()))) {
                currentEmbeddableOverride.put(e.getName(), e.getDbAttributeName());
            }
        }
        return currentEmbeddableOverride;
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
        // intentionally empty — selection changes are handled inside ObjAttributePathBrowser
    }

    private DbEntity getFirstEntity() {
        Iterator<CayenneMapEntry> it = attribute.getDbPathIterator();
        DbEntity firstEnt = attribute.getDbAttribute().getEntity();
        boolean setEnt = false;

        while (it.hasNext()) {
            Object ob = it.next();
            if (ob instanceof DbRelationship) {
                if (!setEnt) {
                    firstEnt = ((DbRelationship) ob).getSourceEntity();
                    setEnt = true;
                }
            } else if (ob instanceof DbAttribute) {
                if (!setEnt) {
                    firstEnt = ((DbAttribute) ob).getEntity();
                }
            }
        }

        return firstEnt;
    }

    /**
     * Selects path in browser
     */
    private void setSelectionPath() {
        List<CayenneMapEntry> list = new ArrayList<>();
        boolean isAttributeLast = false;
        Iterator<CayenneMapEntry> it = attribute.getDbPathIterator();
        while (it.hasNext()) {
            CayenneMapEntry obj = it.next();
            list.add(obj);
            if (obj instanceof DbAttribute && !it.hasNext()) {
                isAttributeLast = true;
            }
        }
        if (isAttributeLast) {
            Object[] path = new Object[list.size() + 1];
            path[0] = getFirstEntity();
            System.arraycopy(list.toArray(), 0, path, 1, list.size());
            pathBrowser.setSelectionPath(new TreePath(path));
            saveButton.setEnabled(true);
        }
    }

    private boolean isRegisteredType(String typeName) {
        String[] typeNames = ValueTypes.getTypes();
        for (String nextTypeName : typeNames) {
            if (nextTypeName.equals(typeName)) {
                return true;
            }
        }
        return false;
    }

    private void copyObjAttribute(ObjAttribute attributeSaved, ObjAttribute attribute) {
        attributeSaved.setDbAttributePath(attribute.getDbAttributePath());
        attributeSaved.setName(attribute.getName());
        attributeSaved.setEntity(attribute.getEntity());
        attributeSaved.setParent(attribute.getParent());
        attributeSaved.setType(attribute.getType());
        attributeSaved.setUsedForLocking(attribute.isUsedForLocking());
        attributeSaved.setLazy(attribute.isLazy());
        String comment = ObjectInfo.getFromMetaData(app.getMetaData(), attribute, ObjectInfo.COMMENT);
        ObjectInfo.putToMetaData(app.getMetaData(), attributeSaved, ObjectInfo.COMMENT, comment);

        if (attributeSaved instanceof EmbeddedAttribute) {
            Map<String, String> attrOverrides = (attribute instanceof EmbeddedAttribute)
                    ? ((EmbeddedAttribute) attribute).getAttributeOverrides()
                    : new HashMap<>();
            if (!attrOverrides.isEmpty()) {
                for (Map.Entry<String, String> attrOv : attrOverrides.entrySet()) {
                    ((EmbeddedAttribute) attributeSaved).addAttributeOverride(attrOv.getKey(), attrOv.getValue());
                }
            }
        }
    }

    private void compareAndSetOverrideInEmbeddedAttribute(ObjAttribute attribute,
                                                          Map<String, String> overrides,
                                                          Map<String, String> currentOverrAttr) {
        ArrayList<String> keysForDelete = new ArrayList<>();
        ArrayList<String> keysForAdd = new ArrayList<>();

        for (Map.Entry<String, String> obj : overrides.entrySet()) {
            String key = obj.getKey();
            if (currentOverrAttr.get(key) == null || !(obj.getValue().equals(currentOverrAttr.get(key)))) {
                keysForDelete.add(key);
            }
        }

        for (Map.Entry<String, String> obj : currentOverrAttr.entrySet()) {
            String key = obj.getKey();
            if (overrides.get(key) == null || !(obj.getValue().equals(overrides.get(key)))) {
                keysForAdd.add(key);
            }
        }

        for (String aKeysForDelete : keysForDelete) {
            ((EmbeddedAttribute) attribute).removeAttributeOverride(aKeysForDelete);
        }
        for (String key : keysForAdd) {
            ((EmbeddedAttribute) attribute).addAttributeOverride(key, currentOverrAttr.get(key));
        }
    }

    private static Collection<String> dbAttributeNames(DbEntity entity) {
        Set<String> keys = entity.getAttributeMap().keySet();
        List<String> names = new ArrayList<>(keys.size() + 1);
        names.add("");
        names.addAll(keys);
        return names;
    }

    // custom renderer used for inherited attribute highlighting
    private static final class CellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            OverrideEmbeddableAttributeTableModel model = (OverrideEmbeddableAttributeTableModel) table.getModel();
            if (!model.isCellEditable(row, column)) {
                setForeground(Color.DARK_GRAY);
            } else {
                setForeground(isSelected && !hasFocus ? table.getSelectionForeground() : table.getForeground());
            }
            setBackground(isSelected && !hasFocus ? table.getSelectionBackground() : table.getBackground());

            return this;
        }
    }
}
