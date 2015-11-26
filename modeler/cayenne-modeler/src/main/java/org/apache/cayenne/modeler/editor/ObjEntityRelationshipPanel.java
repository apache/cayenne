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
package org.apache.cayenne.modeler.editor;

import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.DeleteRule;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.ObjEntityListener;
import org.apache.cayenne.map.event.ObjRelationshipListener;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.ActionManager;
import org.apache.cayenne.modeler.action.CopyAttributeRelationshipAction;
import org.apache.cayenne.modeler.action.CutAttributeRelationshipAction;
import org.apache.cayenne.modeler.action.PasteAction;
import org.apache.cayenne.modeler.action.RemoveAttributeRelationshipAction;
import org.apache.cayenne.modeler.dialog.objentity.ObjRelationshipInfo;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.ObjEntityDisplayListener;
import org.apache.cayenne.modeler.event.TablePopupHandler;
import org.apache.cayenne.modeler.pref.TableColumnPreferences;
import org.apache.cayenne.modeler.util.CayenneTable;
import org.apache.cayenne.modeler.util.CellRenderers;
import org.apache.cayenne.modeler.util.EntityTreeFilter;
import org.apache.cayenne.modeler.util.EntityTreeModel;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.modeler.util.PanelFactory;
import org.apache.cayenne.modeler.util.UIUtil;
import org.apache.cayenne.modeler.util.combo.AutoCompletion;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Displays ObjRelationships for the edited ObjEntity.
 */
public class ObjEntityRelationshipPanel extends JPanel implements ObjEntityDisplayListener,
        ObjEntityListener, ObjRelationshipListener {

    private static Log logObj = LogFactory.getLog(ObjEntityRelationshipPanel.class);

    private static final Object[] deleteRules = new Object[]{
            DeleteRule.deleteRuleName(DeleteRule.NO_ACTION),
            DeleteRule.deleteRuleName(DeleteRule.NULLIFY),
            DeleteRule.deleteRuleName(DeleteRule.CASCADE),
            DeleteRule.deleteRuleName(DeleteRule.DENY),
    };

    protected ProjectController mediator;
    protected CayenneTable table;
    private TableColumnPreferences tablePreferences;
    private ActionListener resolver;
    private ObjEntityAttributeRelationshipTab parentPanel;
    private boolean enabledResolve;//for JBottom "resolve" in ObjEntityAttrRelationshipTab

    /**
     * By now popup menu item is made similar to toolbar button. (i.e. all functionality
     * is here) This should be probably refactored as Action.
     */
    protected JMenuItem resolveMenu;

    public ObjEntityRelationshipPanel(ProjectController mediator, ObjEntityAttributeRelationshipTab parentPanel) {
        this.mediator = mediator;
        this.parentPanel = parentPanel;

        init();
        initController();
    }

    private void init() {
        this.setLayout(new BorderLayout());

        ActionManager actionManager = Application.getInstance().getActionManager();

        table = new CayenneTable(){
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component component = super.prepareRenderer(renderer, row, column);
                int rendererWidth = component.getPreferredSize().width;
                TableColumn tableColumn = getColumnModel().getColumn(column);
                tableColumn.setPreferredWidth(Math.max(rendererWidth + getIntercellSpacing().width, tableColumn.getPreferredWidth()));
                return component;
            }
        };
        table.setDefaultRenderer(String.class, new StringRenderer());
        table.setDefaultRenderer(ObjEntity.class, new EntityRenderer());
        tablePreferences = new TableColumnPreferences(
                ObjRelationshipTableModel.class,
                "objEntity/relationshipTable");

        /**
         * Create and install a popup
         */
        Icon ico = ModelerUtil.buildIcon("icon-info.gif");
        resolveMenu = new JMenuItem("Database Mapping", ico);

        JPopupMenu popup = new JPopupMenu();
        popup.add(resolveMenu);
        popup.add(actionManager.getAction(RemoveAttributeRelationshipAction.class).buildMenu());

        popup.addSeparator();
        popup.add(actionManager.getAction(CutAttributeRelationshipAction.class).buildMenu());
        popup.add(actionManager.getAction(CopyAttributeRelationshipAction.class).buildMenu());
        popup.add(actionManager.getAction(PasteAction.class).buildMenu());

        TablePopupHandler.install(table, popup);
        add(PanelFactory.createTablePanel(table, null), BorderLayout.CENTER);
    }

    private void initController() {
        mediator.addObjEntityDisplayListener(this);
        mediator.addObjEntityListener(this);
        mediator.addObjRelationshipListener(this);

        resolver = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                if (row < 0) {
                    return;
                }

                ObjRelationshipTableModel model = (ObjRelationshipTableModel) table
                        .getModel();
                new ObjRelationshipInfo(mediator, model.getRelationship(row)).startupAction();

                /**
                 * This is required for a table to be updated properly
                 */
                table.cancelEditing();

                // need to refresh selected row... do this by unselecting/selecting the
                // row
                table.getSelectionModel().clearSelection();
                table.select(row);
                enabledResolve = false;
            }
        };
        resolveMenu.addActionListener(resolver);

        table.getSelectionModel().addListSelectionListener(new ObjRelationshipListSelectionListener());

        mediator.getApplication().getActionManager().setupCutCopyPaste(
                table,
                CutAttributeRelationshipAction.class,
                CopyAttributeRelationshipAction.class);
    }

    /**
     * Selects a specified relationship in the relationships table.
     */
    public void selectRelationships(ObjRelationship[] rels) {
        ObjRelationshipTableModel model = (ObjRelationshipTableModel) table.getModel();

        List listRels = model.getObjectList();
        int[] newSel = new int[rels.length];

        parentPanel.updateActions(rels);

        for (int i = 0; i < rels.length; i++) {
            newSel[i] = listRels.indexOf(rels[i]);
        }

        table.select(newSel);
    }

    /**
     * Loads obj relationships into table.
     */
    public void currentObjEntityChanged(EntityDisplayEvent e) {
        if (e.getSource() == this) {
            return;
        }

        ObjEntity entity = (ObjEntity) e.getEntity();

        // Important: process event even if this is the same entity,
        // since the inheritance structure might have changed
        if (entity != null) {
            rebuildTable(entity);
        }

        // if an entity was selected on a tree,
        // unselect currently selected row
        if (e.isUnselectAttributes()) {
            table.clearSelection();
        }
    }

    /**
     * Creates a list of ObjEntity names.
     */
    private Object[] createObjEntityComboModel() {
        DataMap map = mediator.getCurrentDataMap();

        // this actually happens per CAY-221... can't reproduce though
        if (map == null) {
            logObj.warn("createObjEntityComboModel:: Null DataMap.");
            return new Object[0];
        }

        if (map.getNamespace() == null) {
            logObj.warn("createObjEntityComboModel:: Null DataMap namespace - " + map);
            return new Object[0];
        }

        Collection objEntities = map.getNamespace().getObjEntities();
        return objEntities.toArray();
    }

    public void objEntityChanged(EntityEvent e) {
    }

    public void objEntityAdded(EntityEvent e) {
        reloadEntityList(e);
    }

    public void objEntityRemoved(EntityEvent e) {
        reloadEntityList(e);
    }

    public void objRelationshipChanged(RelationshipEvent e) {
        table.select(e.getRelationship());
    }

    public void objRelationshipAdded(RelationshipEvent e) {
        rebuildTable((ObjEntity) e.getEntity());
        table.select(e.getRelationship());
    }

    public void objRelationshipRemoved(RelationshipEvent e) {
        ObjRelationshipTableModel model = (ObjRelationshipTableModel) table.getModel();
        int ind = model.getObjectList().indexOf(e.getRelationship());
        model.removeRow(e.getRelationship());
        table.select(ind);
    }

    /**
     * Refresh the list of ObjEntity targets. Also refresh the table in case some
     * ObjRelationships were deleted.
     */
    private void reloadEntityList(EntityEvent e) {
        if (e.getSource() != this) {
            return;
        }

        // If current model added/removed, do nothing.
        ObjEntity entity = mediator.getCurrentObjEntity();
        if (entity == e.getEntity() || entity == null) {
            return;
        }

        TableColumn col = table.getColumnModel().getColumn(
                ObjRelationshipTableModel.REL_TARGET);
        DefaultCellEditor editor = (DefaultCellEditor) col.getCellEditor();

        JComboBox combo = (JComboBox) editor.getComponent();
        combo.setRenderer(CellRenderers.entityListRendererWithIcons(entity.getDataMap()));

        ObjRelationshipTableModel model = (ObjRelationshipTableModel) table.getModel();
        model.fireTableDataChanged();
    }

    protected void rebuildTable(ObjEntity entity) {
        final ObjRelationshipTableModel model = new ObjRelationshipTableModel(
                entity,
                mediator,
                this);

        model.addTableModelListener(new TableModelListener() {

            public void tableChanged(TableModelEvent e) {
                if (table.getSelectedRow() >= 0) {
                    ObjRelationship rel = model.getRelationship(table.getSelectedRow());
                    if (((ObjEntity) rel.getSourceEntity()).getDbEntity() != null) {
                        enabledResolve = true;
                    } else
                        enabledResolve = false;

                    resolveMenu.setEnabled(enabledResolve);
                }
            }
        });

        table.setModel(model);
        table.setRowHeight(25);
        table.setRowMargin(3);

        TableColumn col = table.getColumnModel().getColumn(
                ObjRelationshipTableModel.REL_TARGET_PATH);
        col.setCellEditor(new JTableTargetPathComboBoxEditor());

        col = table.getColumnModel().getColumn(ObjRelationshipTableModel.REL_DELETE_RULE);
        JComboBox deleteRulesCombo = Application.getWidgetFactory().createComboBox(
                deleteRules,
                false);
        deleteRulesCombo.setEditable(false);
        deleteRulesCombo.setSelectedIndex(0); // Default to the first value
        col.setCellEditor(Application.getWidgetFactory().createCellEditor(
                deleteRulesCombo));

        col = table.getColumnModel().getColumn(ObjRelationshipTableModel.REL_COLLECTION_TYPE);

        col.setCellEditor(new JTableCollectionTypeComboBoxEditor());
        col.setCellRenderer(new JTableCollectionTypeComboBoxRenderer());

        col = table.getColumnModel().getColumn(ObjRelationshipTableModel.REL_MAP_KEY);

        col.setCellEditor(new JTableMapKeyComboBoxEditor());
        col.setCellRenderer(new JTableMapKeyComboBoxRenderer());

        tablePreferences.bind(
                table,
                null,
                null,
                null,
                ObjRelationshipTableModel.REL_NAME,
                true);
    }

    class EntityRenderer extends StringRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            Object oldValue = value;
            value = CellRenderers.asString(value);

            super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column);

            setIcon(CellRenderers.iconForObject(oldValue));
            return this;
        }
    }

    class StringRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            // center cardinality column
            int align = column == ObjRelationshipTableModel.REL_SEMANTICS
                    ? JLabel.CENTER
                    : JLabel.LEFT;
            super.setHorizontalAlignment(align);

            super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column);

            ObjRelationshipTableModel model = (ObjRelationshipTableModel) table
                    .getModel();
            ObjRelationship relationship = model.getRelationship(row);

            if (relationship != null
                    && relationship.getSourceEntity() != model.getEntity()) {
                setForeground(Color.GRAY);
            } else {
                setForeground(isSelected && !hasFocus
                        ? table.getSelectionForeground()
                        : table.getForeground());
            }

            return this;
        }
    }

    private class ObjRelationshipListSelectionListener implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent e) {
            ObjRelationship[] rels = new ObjRelationship[0];

            if (!e.getValueIsAdjusting() && !((ListSelectionModel) e.getSource()).isSelectionEmpty()) {

                parentPanel.getAttributePanel().table.getSelectionModel().clearSelection();
                if (parentPanel.getAttributePanel().table.getCellEditor() != null)
                    parentPanel.getAttributePanel().table.getCellEditor().stopCellEditing();
                Application.getInstance().getActionManager().getAction(RemoveAttributeRelationshipAction.class).setCurrentSelectedPanel(parentPanel.getRelationshipPanel());
                Application.getInstance().getActionManager().getAction(CutAttributeRelationshipAction.class).setCurrentSelectedPanel(parentPanel.getRelationshipPanel());
                Application.getInstance().getActionManager().getAction(CopyAttributeRelationshipAction.class).setCurrentSelectedPanel(parentPanel.getRelationshipPanel());
                parentPanel.getResolve().removeActionListener(parentPanel.getAttributePanel().getResolver());
                parentPanel.getResolve().removeActionListener(getResolver());
                parentPanel.getResolve().addActionListener(getResolver());
                parentPanel.getResolve().setToolTipText("Edit Relationship");
                parentPanel.getResolve().setEnabled(true);

                if (table.getSelectedRow() >= 0) {
                    ObjRelationshipTableModel model = (ObjRelationshipTableModel) table.getModel();

                    int[] sel = table.getSelectedRows();
                    rels = new ObjRelationship[sel.length];

                    for (int i = 0; i < sel.length; i++) {
                        rels[i] = model.getRelationship(sel[i]);
                    }

                    if (sel.length == 1) {
                        UIUtil.scrollToSelectedRow(table);
                    }

                    enabledResolve = true;
                } else {
                    enabledResolve = false;
                }
                resolveMenu.setEnabled(enabledResolve);
            }

            mediator.setCurrentObjRelationships(rels);
            parentPanel.updateActions(rels);
        }
    }

    public boolean isEnabledResolve() {
        return enabledResolve;
    }

    public ActionListener getResolver() {
        return resolver;
    }

    private final static class JTableCollectionTypeComboBoxEditor extends AbstractCellEditor implements TableCellEditor{

        static final String COLLECTION_TYPE_MAP = "java.util.Map";
        static final String COLLECTION_TYPE_SET = "java.util.Set";
        static final String COLLECTION_TYPE_COLLECTION = "java.util.Collection";
        static final String DEFAULT_COLLECTION_TYPE = "java.util.List";
        private static final int REL_COLLECTION_TYPE_COLUMN = 3;

        private ObjRelationshipTableModel model;
        private int row;

        public JTableCollectionTypeComboBoxEditor() {
        }

        @Override
        public Component getTableCellEditorComponent(final JTable table, Object value, boolean isSelected, final int row, final int column) {
            this.model = (ObjRelationshipTableModel) table.getModel();
            this.row = row;

            final JComboBox collectionTypeCombo = Application.getWidgetFactory().createComboBox(
                    new Object[]{
                            COLLECTION_TYPE_MAP,
                            COLLECTION_TYPE_SET,
                            COLLECTION_TYPE_COLLECTION,
                            DEFAULT_COLLECTION_TYPE
                    },
                    false);
            if(model.getRelationship(row).isToMany()){
                collectionTypeCombo.setEnabled(true);
                collectionTypeCombo.setSelectedItem( model.getRelationship(row).getCollectionType());
            }else{
                JLabel labelIfToOneRelationship = new JLabel();
                labelIfToOneRelationship.setEnabled(false);
                return labelIfToOneRelationship;
            }
            collectionTypeCombo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Object selected = collectionTypeCombo.getSelectedItem();
                    model.setUpdatedValueAt(selected,row,REL_COLLECTION_TYPE_COLUMN);
                    table.repaint();
                }
            });
            return collectionTypeCombo;
        }

        @Override
        public Object getCellEditorValue() {
            return model.getValueAt(row,REL_COLLECTION_TYPE_COLUMN);
        }
    }

    private final static class JTableCollectionTypeComboBoxRenderer implements TableCellRenderer {

        private ObjRelationshipTableModel model;

        public JTableCollectionTypeComboBoxRenderer() {
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            this.model = (ObjRelationshipTableModel) table.getModel();
            JLabel labelIfToOneRelationship = new JLabel();
            labelIfToOneRelationship.setEnabled(false);
            JLabel labelIfToManyRelationship = new JLabel((String) value);
            labelIfToManyRelationship.setEnabled(true);
            labelIfToManyRelationship.setFont(new Font("Verdana", Font.PLAIN , 12));
            if (value == null)
                return labelIfToOneRelationship;

            if (model.getRelationship(row).isToMany()) {
                return labelIfToManyRelationship;
            }else{
                return labelIfToOneRelationship;
            }

        }
    }

    private final static class JTableMapKeyComboBoxEditor extends AbstractCellEditor implements TableCellEditor {

        private static final String DEFAULT_MAP_KEY = "ID (default)";
        private static final String COLLECTION_TYPE_MAP = "java.util.Map";
        private static final int REL_MAP_KEY_COLUMN = 4;

        private List<String> mapKeys = new ArrayList<>() ;
        private ObjRelationshipTableModel model;
        private int row;

        private JTableMapKeyComboBoxEditor() {
        }

        private void initMapKeys() {
            mapKeys.clear();
            mapKeys.add(DEFAULT_MAP_KEY);
            /**
             * Object target can be null when selected target DbEntity has no
             * ObjEntities
             */
            ObjEntity objectTarget = model.getRelationship(row).getTargetEntity();
            if (objectTarget == null) {
                return ;
            }
            for (ObjAttribute attribute : objectTarget.getAttributes()) {
                mapKeys.add(attribute.getName());
            }
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, final int row, final int column) {
            this.model = (ObjRelationshipTableModel) table.getModel();
            this.row = row;
            initMapKeys();
            final JComboBox mapKeysComboBox =  Application.getWidgetFactory().createComboBox(
                  mapKeys,
                    false);
            if ((model.getRelationship(row).getCollectionType() == null)
                    ||(!model.getRelationship(row).getCollectionType().equals(COLLECTION_TYPE_MAP))){
                JComboBox jComboBox = new JComboBox();
                jComboBox.setFocusable(false);
                jComboBox.setEnabled(false);
                return jComboBox;
            }else{
                mapKeysComboBox.setFocusable(true);
                mapKeysComboBox.setEnabled(true);
            }
            mapKeysComboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Object selected = mapKeysComboBox.getSelectedItem();
                    model.setUpdatedValueAt(selected,row,REL_MAP_KEY_COLUMN);
                }
            });
            mapKeysComboBox.setSelectedItem(model.getRelationship(row).getMapKey());
            return mapKeysComboBox;
        }

        @Override
        public Object getCellEditorValue() {
            return model.getValueAt(row,REL_MAP_KEY_COLUMN);
        }
    }

    private final static class JTableMapKeyComboBoxRenderer implements TableCellRenderer{

        static final String DEFAULT_MAP_KEY = "ID (default)";
        static final String COLLECTION_TYPE_MAP = "java.util.Map";

        private ObjRelationshipTableModel model;

        public JTableMapKeyComboBoxRenderer() {
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            this.model = (ObjRelationshipTableModel) table.getModel();
            if ( (model.getRelationship(row).getCollectionType() == null)
                ||(!model.getRelationship(row).getCollectionType().equals(COLLECTION_TYPE_MAP))){
                JComboBox jComboBox = new JComboBox();
                jComboBox.setFocusable(false);
                jComboBox.setEnabled(false);
                return jComboBox;
            }
            if (model.getRelationship(row).getMapKey() == null){
                model.getRelationship(row).setMapKey(DEFAULT_MAP_KEY);
            }
            JLabel jLabel  = new JLabel(model.getRelationship(row).getMapKey());
            jLabel.setFont(new Font("Verdana", Font.PLAIN , 12));
            return jLabel;
        }
    }

    private static final class JTableTargetPathComboBoxEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {

        static final int REL_TARGET_PATH_COLUMN = 2;

        private ObjRelationshipTableModel model;
        private int row;
        private JComboBox dbRelationshipPathCombo;
        private EntityTreeModel treeModel;
        private int previousEmbededLevel = 0;
        private static int enterPressedCount = 0;

        public JTableTargetPathComboBoxEditor() {
        }

        @Override
        public Object getCellEditorValue() {
            return model.getValueAt(row,REL_TARGET_PATH_COLUMN);
        }

        @Override
        public Component getTableCellEditorComponent(final JTable table, Object value, boolean isSelected, final int row, int column) {
            this.model = (ObjRelationshipTableModel) table.getModel();
            this.row = row;
            treeModel = createTreeModelForComboBoxBrowser(row);
            if (treeModel == null)
                return new JLabel("You need select table to this ObjectEntity");
            initializeCombo(model , row , table);

            String dbRelationshipPath = ((JTextComponent) (dbRelationshipPathCombo).
                    getEditor().getEditorComponent()).getText();
            previousEmbededLevel =  dbRelationshipPath.split(Pattern.quote(".")).length;
            return dbRelationshipPathCombo;
        }

        private void initializeCombo(final ObjRelationshipTableModel model, final int row, final JTable table){
            String dbRelationshipPath = model.getRelationship(row).getDbRelationshipPath();
            Object currentNode;
            if (dbRelationshipPath == null){
                //case if it is new attribute or for some reason dbRelationshipPath is null
                currentNode = getCurrentNode(dbRelationshipPath);
                dbRelationshipPath = "";

            }else{
                //case if  dbRelationshipPath isn't null and we must change it to find auto completion list
                String[] pathStrings = dbRelationshipPath.split(Pattern.quote("."));
                String lastStringInPath = pathStrings[pathStrings.length - 1];
                dbRelationshipPath = dbRelationshipPath.replaceAll(lastStringInPath + "$", "");
                currentNode = getCurrentNode(dbRelationshipPath);
            }
            List<String> nodeChildren = getChildren(currentNode , dbRelationshipPath);
            dbRelationshipPathCombo = Application.getWidgetFactory().createComboBox(
                    nodeChildren,
                    false);
            dbRelationshipPathCombo.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
                private void enterPressed(){
                    String dbRelationshipPath = ((JTextComponent) (dbRelationshipPathCombo).
                            getEditor().getEditorComponent()).getText();
                    Object currentNode = getCurrentNode(dbRelationshipPath);
                    String[] pathStrings = dbRelationshipPath.split(Pattern.quote("."));
                    String lastStringInPath = pathStrings[pathStrings.length - 1];

                    if (lastStringInPath.equals(ModelerUtil.getObjectName(currentNode))) {
                        if (enterPressedCount == 1) {
                            //it is second time enter pressed.. so we will save input data
                            enterPressedCount = 0;
                            if (currentNode instanceof DbRelationship) {

                                if (table.getCellEditor() != null) {

                                    table.getCellEditor().stopCellEditing();
                                    model.getRelationship(row).setDbRelationshipPath(dbRelationshipPath);

                                    //we need object target to save it in model
                                    DbEntity lastEntity = ((DbRelationship) currentNode).getTargetEntity();
                                    Collection<ObjEntity> objEntities = ((DbRelationship) currentNode).getTargetEntity().
                                            getDataMap().getMappedEntities(lastEntity);
                                    ObjEntity objectTarget = objEntities.size() == 0 ? null : objEntities.iterator().next();
                                    model.getRelationship(row).setTargetEntityName(objectTarget);
                                }
                            }
                            table.repaint();
                        } else {
                            enterPressedCount = 1;
                        }
                    }
                }

                @Override
                public void keyReleased(KeyEvent event) {
                    if (event.getKeyCode() == KeyEvent.VK_ENTER) {
                        enterPressed();
                        return;
                    }
                    parseDbRelationshipString(event.getKeyChar());
                }
            });
            AutoCompletion.enable(dbRelationshipPathCombo, false, true);
            dbRelationshipPathCombo.setEditable(true);
            ((JTextComponent) (dbRelationshipPathCombo).
                    getEditor().getEditorComponent()).setText(model.getRelationship(row).getDbRelationshipPath());
            dbRelationshipPathCombo.setSelectedItem(model.getRelationship(row).getDbRelationshipPath());
            dbRelationshipPathCombo.addActionListener(this);
            return;
        }

        private void parseDbRelationshipString(char lastEnteredCharacter){
            String dbRelationshipPath = ((JTextComponent) (dbRelationshipPathCombo).
                    getEditor().getEditorComponent()).getText();

            enterPressedCount = 0;

            if (dbRelationshipPath.equals("")){
                List<String> currentNodeChildren = new ArrayList<>();
                currentNodeChildren.add("");
                currentNodeChildren.addAll(getChildren(getCurrentNode(dbRelationshipPath),""));
                dbRelationshipPathCombo.setModel(new DefaultComboBoxModel(currentNodeChildren.toArray()));
                return;
            }

            if (lastEnteredCharacter == '.') {
                processDotEntered();
                previousEmbededLevel  =  StringUtils.countMatches(dbRelationshipPath,".");
                return;
            }

            int currentEmbededLevel =  StringUtils.countMatches(dbRelationshipPath,".");
            if (previousEmbededLevel != currentEmbededLevel){
                previousEmbededLevel = currentEmbededLevel;
                List<String> currentNodeChildren = new ArrayList<>();
                String[] pathStrings = dbRelationshipPath.split(Pattern.quote("."));
                String lastStringInPath = pathStrings[pathStrings.length - 1];
                String saveDbRelationshipPath = dbRelationshipPath;
                dbRelationshipPath = dbRelationshipPath.replaceAll(lastStringInPath + "$", "");
                currentNodeChildren.add("");
                currentNodeChildren.addAll(getChildren(getCurrentNode(dbRelationshipPath), dbRelationshipPath));
                dbRelationshipPathCombo.setModel(new DefaultComboBoxModel(currentNodeChildren.toArray()));
                ((JTextComponent) (dbRelationshipPathCombo).
                        getEditor().getEditorComponent()).setText(saveDbRelationshipPath);
                return;
            }
        }

        private void processDotEntered(){
            String dbAttributePath = ((JTextComponent) (dbRelationshipPathCombo).
                    getEditor().getEditorComponent()).getText();
            if (dbAttributePath.equals(".")){
                List<String> currentNodeChildren = new ArrayList<>();
                currentNodeChildren.add("");
                currentNodeChildren.addAll(getChildren(getCurrentNode(""),""));
                dbRelationshipPathCombo.setModel(new DefaultComboBoxModel(currentNodeChildren.toArray()));
                dbRelationshipPathCombo.showPopup();
                dbRelationshipPathCombo.setPopupVisible(true);
                return;
            }else {
                char secondFromEndCharacter = dbAttributePath.charAt(dbAttributePath.length()-2);
                if(secondFromEndCharacter == '.') {
                    // two dots entered one by one , we replace it by one dot
                    ((JTextComponent) (dbRelationshipPathCombo).
                            getEditor().getEditorComponent()).setText(dbAttributePath.substring(0,dbAttributePath.length()-1));
                    return;
                }else{
                    String[] pathStrings = dbAttributePath.split(Pattern.quote("."));
                    String lastStringInPath = pathStrings[pathStrings.length - 1];

                    //we will check if lastStringInPath is correct name of DbAttribute or DbRelationship
                    //for appropriate previous node in path. if it is not we won't add entered dot to dbAttributePath
                    String dbAttributePathForPreviousNode;
                    if (pathStrings.length == 1){
                        dbAttributePathForPreviousNode = null;
                    }else {
                        dbAttributePathForPreviousNode = dbAttributePath.replace("."+lastStringInPath,"");
                    }
                    List<String> potentialVariantsToChoose = getChildren(getCurrentNode(dbAttributePathForPreviousNode),"");
                    if (potentialVariantsToChoose.contains(lastStringInPath)){
                        List<String> currentNodeChildren = new ArrayList<>();
                        currentNodeChildren.add(dbAttributePath + "");
                        currentNodeChildren.addAll(getChildren(getCurrentNode(dbAttributePath), dbAttributePath));
                        dbRelationshipPathCombo.setModel(new DefaultComboBoxModel(currentNodeChildren.toArray()));
                        dbRelationshipPathCombo.showPopup();
                        dbRelationshipPathCombo.setPopupVisible(true);
                    }else{
                        ((JTextComponent) (dbRelationshipPathCombo).
                                getEditor().getEditorComponent()).setText(dbAttributePath.substring(0,dbAttributePath.length()-1));
                    }
                }
            }
            previousEmbededLevel =  StringUtils.countMatches(dbAttributePath,".");
            return;
        }

        /**
         * find current node by dbRelationshipPath
         * @param dbRelationshipPath
         * @return last node in dbRelationshipPath which matches DbRelationship
         */
        private final Object getCurrentNode(String dbRelationshipPath) {
            try {
                //case for new relationship
                if(dbRelationshipPath == null){
                    return treeModel.getRoot();
                }
                String[] pathStrings = dbRelationshipPath.split(Pattern.quote("."));
                Object root = treeModel.getRoot();
                for (int  i = 0 ; i < pathStrings.length ; i ++) {
                    String rootChildText = pathStrings[i];
                    for (int j = 0; j < treeModel.getChildCount(root); j++) {
                        Object child = treeModel.getChild(root, j);
                        if (child instanceof DbRelationship) {
                            String relationshipName = ModelerUtil.getObjectName(child);
                            if (relationshipName.equals(rootChildText)) {
                                root = child;
                                break;
                            }
                        }
                    }
                }
                return root;
            }catch (Exception e){
                return treeModel.getRoot();
            }
        }

        /**
         * @param node for which we will find children
         * @param dbRelationshipPath string which will be added to each child to make right autocomplete
         * @return list with children , which will be used to autocomplete
         */
        private final List<String> getChildren(Object node , String dbRelationshipPath){
            List<String> currentNodeChildren = new ArrayList<>();
            for(int j = 0 ; j <  treeModel.getChildCount(node) ; j++){
                Object child = treeModel.getChild(node, j);
                String relationshipName = ModelerUtil.getObjectName(child);
                currentNodeChildren.add(dbRelationshipPath + relationshipName);
            }
            return currentNodeChildren;
        }

        /**
         * @param relationshipIndexInTable index of attribute for which now we will create cell editor
         * @return treeModel for nessesary for us attribute
         */
        private EntityTreeModel createTreeModelForComboBoxBrowser(int relationshipIndexInTable){
            if (model.getRelationship(relationshipIndexInTable).
                    getSourceEntity().getDbEntity() == null)
                return null;
            EntityTreeModel treeModel = new EntityTreeModel(model.getRelationship(relationshipIndexInTable).
                    getSourceEntity().getDbEntity());
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
                     * We do not allow A->B->A chains, where relationships are
                     * to-one
                     */
                    DbRelationship prev = (DbRelationship) node;
                    return !(!rel.isToMany() && prev.getReverseRelationship() == rel);
                }

            });
            return treeModel;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            model.getRelationship(row).setMapKey(null);

            //for some reason dbRelationshipPathCombo don't load selected item text, so we made it by hand
            if (dbRelationshipPathCombo.getSelectedIndex() != (-1)){
                ((JTextComponent) (dbRelationshipPathCombo).
                        getEditor().getEditorComponent()).setText(dbRelationshipPathCombo.getSelectedItem().toString());
            }


        }
    }
}