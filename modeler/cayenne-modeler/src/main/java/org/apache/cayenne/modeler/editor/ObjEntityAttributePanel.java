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

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.map.event.AttributeEvent;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.ObjAttributeListener;
import org.apache.cayenne.map.event.ObjEntityListener;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.ActionManager;
import org.apache.cayenne.modeler.action.CopyAttributeRelationshipAction;
import org.apache.cayenne.modeler.action.CutAttributeRelationshipAction;
import org.apache.cayenne.modeler.action.PasteAction;
import org.apache.cayenne.modeler.action.RemoveAttributeRelationshipAction;
import org.apache.cayenne.modeler.dialog.objentity.ObjAttributeInfoDialog;
import org.apache.cayenne.modeler.editor.wrapper.ObjAttributeWrapper;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.ObjEntityDisplayListener;
import org.apache.cayenne.modeler.event.ProjectOnSaveEvent;
import org.apache.cayenne.modeler.event.ProjectOnSaveListener;
import org.apache.cayenne.modeler.event.TablePopupHandler;
import org.apache.cayenne.modeler.pref.TableColumnPreferences;
import org.apache.cayenne.modeler.util.CayenneTable;
import org.apache.cayenne.modeler.util.CayenneTableModel;
import org.apache.cayenne.modeler.util.EntityTreeFilter;
import org.apache.cayenne.modeler.util.EntityTreeModel;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.modeler.util.PanelFactory;
import org.apache.cayenne.modeler.util.UIUtil;
import org.apache.cayenne.modeler.util.combo.AutoCompletion;
import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.commons.lang.StringUtils;

import javax.swing.AbstractCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Detail view of the ObjEntity attributes.
 */
public class ObjEntityAttributePanel extends JPanel implements ObjEntityDisplayListener,
        ObjEntityListener, ObjAttributeListener, ProjectOnSaveListener {

    protected ProjectController mediator;
    protected CayenneTable table;
    private TableColumnPreferences tablePreferences;
    private ObjEntityAttributeRelationshipTab parentPanel;
    private boolean enabledResolve;//for JBottom "resolve" in ObjEntityAttrRelationshipTab

    private ActionListener resolver;

    /**
     * By now popup menu item is made similar to toolbar button. (i.e. all functionality
     * is here) This should be probably refactored as Action.
     */
    protected JMenuItem resolveMenu;

    public ObjEntityAttributePanel(ProjectController mediator, ObjEntityAttributeRelationshipTab parentPanel) {
        this.mediator = mediator;
        this.parentPanel = parentPanel;

        initView();
        initController();
    }

    private void initView() {
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
        table.setDefaultRenderer(String.class, new CellRenderer());
        tablePreferences = new TableColumnPreferences(
                ObjAttributeTableModel.class,
                "objEntity/attributeTable");

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
        mediator.addObjAttributeListener(this);

        resolver = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                if (row < 0) {
                    return;
                }

                ObjAttributeTableModel model = (ObjAttributeTableModel) table.getModel();

                // ... show dialog...
                new ObjAttributeInfoDialog(mediator, row, model).startupAction();

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

        table.getSelectionModel().addListSelectionListener(new ObjAttributeListSelectionListener());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        mediator.getApplication().getActionManager().setupCutCopyPaste(
                table,
                CutAttributeRelationshipAction.class,
                CopyAttributeRelationshipAction.class);
    }

    public void initComboBoxes(ObjAttributeTableModel model) {
        List<String> embeddableNames = new ArrayList<String>();
        List<String> typeNames = new ArrayList<String>();

        Iterator it = ((DataChannelDescriptor) mediator.getProject().getRootNode())
                .getDataMaps()
                .iterator();
        while (it.hasNext()) {
            DataMap dataMap = (DataMap) it.next();
            Iterator<Embeddable> embs = dataMap.getEmbeddables().iterator();
            while (embs.hasNext()) {
                Embeddable emb = embs.next();
                embeddableNames.add(emb.getClassName());
            }
        }

        String[] registeredTypes = ModelerUtil.getRegisteredTypeNames();
        for (int i = 0; i < registeredTypes.length; i++) {
            typeNames.add(registeredTypes[i]);
        }
        typeNames.addAll(embeddableNames);

        TableColumn typeColumn = table.getColumnModel().getColumn(
                ObjAttributeTableModel.OBJ_ATTRIBUTE_TYPE);

        JComboBox javaTypesCombo = Application.getWidgetFactory().createComboBox(
                typeNames.toArray(),
                false);
        AutoCompletion.enable(javaTypesCombo, false, true);
        typeColumn.setCellEditor(Application.getWidgetFactory().createCellEditor(
                javaTypesCombo));
    }

    /**
     * Selects a specified attribute.
     */
    public void selectAttributes(ObjAttribute[] attrs) {
        ObjAttributeTableModel model = (ObjAttributeTableModel) table.getModel();

        List<ObjAttributeWrapper> listAttrs = model.getObjectList();
        int[] newSel = new int[attrs.length];

        parentPanel.updateActions(attrs);

        // search for attributes to select from attributes that model has
        for (int i = 0; i < attrs.length; i++) {
            for (int j = 0; j < listAttrs.size(); j++) {
                if (listAttrs.get(j).getValue() == attrs[i]) {
                    newSel[i] = j;
                    break;
                }
            }
        }

        table.select(newSel);
    }

    public void objAttributeChanged(AttributeEvent e) {
        ObjAttributeTableModel model = (ObjAttributeTableModel) table.getModel();

        if (!model.isValid()) {
            model.resetModel();
        }

        model.fireTableDataChanged();

        int ind = -1;
        List<ObjAttributeWrapper> list = model.getObjectList();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getValue() == e.getAttribute()) {
                ind = i;
            }
        }

        table.select(ind);
    }

    public void objAttributeAdded(AttributeEvent e) {
        ObjAttributeTableModel model = (ObjAttributeTableModel) table.getModel();

        if (!model.isValid()) {
            model.resetModel();
        }

        model.addRow(new ObjAttributeWrapper((ObjAttribute) e.getAttribute()));
        model.fireTableDataChanged();

        int ind = -1;
        List<ObjAttributeWrapper> list = model.getObjectList();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getValue() == e.getAttribute()) {
                ind = i;
            }
        }

        table.select(ind);
    }

    public void objAttributeRemoved(AttributeEvent e) {
        ObjAttributeTableModel model = (ObjAttributeTableModel) table.getModel();
        int ind = -1;
        List<ObjAttributeWrapper> list = model.getObjectList();

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getValue() == e.getAttribute()) {
                ind = i;
            }
        }

        if (!model.isValid()) {
            model.resetModel();
        }

        if (ind >= 0) {
            model.removeRow(list.get(ind));
            model.fireTableDataChanged();
            table.select(ind);
        }
    }

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

    protected void rebuildTable(ObjEntity entity) {
        if (table.getEditingRow() != -1 && table.getEditingColumn() != -1) {
            TableCellEditor cellEditor = table.getCellEditor(table.getEditingRow(), table.getEditingColumn());
            cellEditor.stopCellEditing();
        }
        ObjAttributeTableModel model = new ObjAttributeTableModel(entity, mediator, this);
        table.setModel(model);
        table.setRowHeight(25);
        table.setRowMargin(3);
        setUpTableStructure(model);
    }

    protected void setUpTableStructure(ObjAttributeTableModel model) {
        int inheritanceColumnWidth = 30;

        Map<Integer, Integer> minSizes = new HashMap<Integer, Integer>();
        Map<Integer, Integer> maxSizes = new HashMap<Integer, Integer>();

        minSizes.put(ObjAttributeTableModel.INHERITED, inheritanceColumnWidth);
        maxSizes.put(ObjAttributeTableModel.INHERITED, inheritanceColumnWidth);

        initComboBoxes(model);

        table.getColumnModel().getColumn(3).setCellRenderer(new JTableDbAttributeComboBoxRenderer());
        table.getColumnModel().getColumn(3).setCellEditor(new JTableDbAttributeComboBoxEditor());

        tablePreferences.bind(
                table,
                minSizes,
                maxSizes,
                null,
                ObjAttributeTableModel.OBJ_ATTRIBUTE,
                true);
    }

    /**
     * Refreshes attributes view for the updated entity
     */
    public void objEntityChanged(EntityEvent e) {
        if (e.getSource() == this) {
            return;
        }

        if (!(table.getModel() instanceof ObjAttributeTableModel)) {
            // probably means this panel hasn't been loaded yet...
            return;
        }

        ObjAttributeTableModel model = (ObjAttributeTableModel) table.getModel();
        if (model.getDbEntity() != ((ObjEntity) e.getEntity()).getDbEntity()) {
            model.resetDbEntity();
            setUpTableStructure(model);
        }
    }

    public void objEntityAdded(EntityEvent e) {
        if (e.getSource() == this) {
            return;
        }

        this.rebuildTable((ObjEntity) e.getEntity());
    }

    public void objEntityRemoved(EntityEvent e) {
    }

    // custom renderer used for inherited attributes highlighting
    final class CellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column);

            ObjAttributeTableModel model = (ObjAttributeTableModel) table.getModel();
            column = table.getColumnModel().getColumn(column).getModelIndex();
            ObjAttribute attribute = model.getAttribute(row).getValue();
            if (column != ObjAttributeTableModel.INHERITED) {

                if (!model.isCellEditable(row, column)) {
                    setForeground(Color.GRAY);
                } else {
                    setForeground(isSelected && !hasFocus ? table
                            .getSelectionForeground() : table.getForeground());
                }

                if (attribute.isInherited()) {
                    Font font = getFont();
                    Font newFont = font.deriveFont(Font.ITALIC);
                    setFont(newFont);
                }
                setIcon(null);
            } else {
                if (attribute.isInherited()) {
                    ImageIcon objEntityIcon = ModelerUtil.buildIcon("icon-override.gif");
                    setIcon(objEntityIcon);
                }
                setText("");
            }

            return this;
        }
    }

    private void resetTableModel() {
        CayenneTableModel model = table.getCayenneModel();
        if (model != null && !model.isValid()) {
            model.resetModel();
            model.fireTableDataChanged();
        }
    }

    @Override
    public void beforeSaveChanges(ProjectOnSaveEvent e) {
        resetTableModel();
    }

    private class ObjAttributeListSelectionListener implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent e) {
            ObjAttribute[] attrs = new ObjAttribute[0];

            if (!e.getValueIsAdjusting() && !((ListSelectionModel) e.getSource()).isSelectionEmpty()) {

                parentPanel.getRelationshipPanel().table.getSelectionModel().clearSelection();
                if (parentPanel.getRelationshipPanel().table.getCellEditor() != null)
                    parentPanel.getRelationshipPanel().table.getCellEditor().stopCellEditing();
                Application.getInstance().getActionManager().getAction(RemoveAttributeRelationshipAction.class).setCurrentSelectedPanel(parentPanel.getAttributePanel());
                Application.getInstance().getActionManager().getAction(CutAttributeRelationshipAction.class).setCurrentSelectedPanel(parentPanel.getAttributePanel());
                Application.getInstance().getActionManager().getAction(CopyAttributeRelationshipAction.class).setCurrentSelectedPanel(parentPanel.getAttributePanel());
                parentPanel.getResolve().removeActionListener(parentPanel.getRelationshipPanel().getResolver());
                parentPanel.getResolve().removeActionListener(getResolver());
                parentPanel.getResolve().addActionListener(getResolver());
                parentPanel.getResolve().setToolTipText("Edit Attribute");
                parentPanel.getResolve().setEnabled(true);

                if (table.getSelectedRow() >= 0) {
                    ObjAttributeTableModel model = (ObjAttributeTableModel) table.getModel();

                    int[] sel = table.getSelectedRows();
                    attrs = new ObjAttribute[sel.length];

                    for (int i = 0; i < sel.length; i++) {
                        attrs[i] = model.getAttribute(sel[i]).getValue();
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

            mediator.setCurrentObjAttributes(attrs);
            parentPanel.updateActions(attrs);
        }
    }

    public boolean isEnabledResolve() {
        return enabledResolve;
    }

    public ActionListener getResolver() {
        return resolver;
    }


    private static final class JTableDbAttributeComboBoxRenderer extends DefaultTableCellRenderer {

        public JTableDbAttributeComboBoxRenderer() {
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            if (value instanceof DbAttribute){
                JLabel jLabel = new  JLabel(ModelerUtil.getObjectName(value));
                jLabel.setFont(new Font("Verdana", Font.PLAIN , 12));
                return jLabel;
            }
            if (value !=null){
                JLabel jLabel = new JLabel(value.toString());
                jLabel.setFont(new Font("Verdana", Font.PLAIN , 12));
                return jLabel;
            }
            return new JLabel("");
        }
    }

    private final static class JTableDbAttributeComboBoxEditor extends AbstractCellEditor implements TableCellEditor {

        private static final int DB_ATTRIBUTE_PATH_COLUMN = 3;

        private int row;
        private JComboBox dbAttributePathCombo;
        private EntityTreeModel treeModel;
        private int previousEmbededLevel = 0;
        private ObjAttributeTableModel model;

        private JTableDbAttributeComboBoxEditor() {
        }

        @Override
        public Object getCellEditorValue() {
             return model.getValueAt(row,DB_ATTRIBUTE_PATH_COLUMN);
        }

        @Override
        public Component getTableCellEditorComponent(final JTable table, Object o, boolean b, int i, int i1) {
            this.model = (ObjAttributeTableModel) table.getModel();
            row = i;
            treeModel = createTreeModelForComboBoxBrowser(row);
            if (treeModel == null)
                return new JLabel("You need select table to this ObjectEntity");
            initializeCombo(model , row ,table);

            String dbAttributePath = ((JTextComponent) (dbAttributePathCombo).
                    getEditor().getEditorComponent()).getText();
            previousEmbededLevel =  StringUtils.countMatches(dbAttributePath,".");
            return dbAttributePathCombo;
        }

        private void initializeCombo(final ObjAttributeTableModel model, final int row, final JTable table){
            String dbAttributePath = model.getAttribute(row).getValue().getDbAttributePath();
            Object currentNode;
            if (dbAttributePath == null){
                //case if it is new attribute or for some reason dbAttributePath is null
                currentNode = getCurrentNode(dbAttributePath);
                dbAttributePath = "";

            }else{
                //case if  dbAttributePath isn't null and we must change it to find auto completion list
                String[] pathStrings = dbAttributePath.split(Pattern.quote("."));
                String lastStringInPath = pathStrings[pathStrings.length - 1];
                dbAttributePath = dbAttributePath.replaceAll(lastStringInPath + "$", "");
                currentNode = getCurrentNode(dbAttributePath);
            }
            List<String> nodeChildren = getChildren(currentNode , dbAttributePath);
            dbAttributePathCombo = Application.getWidgetFactory().createComboBox(
                    nodeChildren,
                    false);
            dbAttributePathCombo.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
                private void enterPressed(){
                    String dbAttributePath = ((JTextComponent) (dbAttributePathCombo).
                            getEditor().getEditorComponent()).getText();
                    Object currentNode = getCurrentNode(dbAttributePath);
                    if (currentNode instanceof DbAttribute) {
                        // in this case choose is made.. we save data

                        if (table.getCellEditor() != null) {
                            table.getCellEditor().stopCellEditing();
                            model.getAttribute(row).setDbAttributePath(dbAttributePath);
                            model.setUpdatedValueAt(dbAttributePath, row, DB_ATTRIBUTE_PATH_COLUMN);
                        }
                    }else if (currentNode instanceof DbRelationship) {
                        // in this case we add dot  to pathString (if it is missing) and show variants for currentNode

                        if (dbAttributePath.charAt(dbAttributePath.length()-1) != '.') {
                            dbAttributePath = dbAttributePath + ".";
                            previousEmbededLevel =  StringUtils.countMatches(dbAttributePath,".");
                            ((JTextComponent) (dbAttributePathCombo).
                                    getEditor().getEditorComponent()).setText(dbAttributePath);
                        }
                        List<String> currentNodeChildren = new ArrayList<>();
                        currentNodeChildren.add(dbAttributePath + "");
                        currentNodeChildren.addAll(getChildren(getCurrentNode(dbAttributePath), dbAttributePath));
                        dbAttributePathCombo.setModel(new DefaultComboBoxModel(currentNodeChildren.toArray()));
                        dbAttributePathCombo.showPopup();
                        dbAttributePathCombo.setPopupVisible(true);
                    }
                }

                @Override
                public void keyReleased(KeyEvent event) {
                    if(event.getKeyCode() == KeyEvent.VK_ENTER){
                        enterPressed();
                        return;
                    }
                    parseDbAttributeString(event.getKeyChar());
                }
            });
            AutoCompletion.enable(dbAttributePathCombo, false, true);
            ((JTextComponent) (dbAttributePathCombo).
                    getEditor().getEditorComponent()).setText(model.getAttribute(row).getValue().getDbAttributePath());
            return;
        }

        private void parseDbAttributeString(char lastEnteredCharacter){
            String dbAttributePath = ((JTextComponent) (dbAttributePathCombo).
                    getEditor().getEditorComponent()).getText();

            if (dbAttributePath.equals("")){
                List<String> currentNodeChildren = new ArrayList<>();
                currentNodeChildren.add("");
                currentNodeChildren.addAll(getChildren(getCurrentNode(dbAttributePath),""));
                dbAttributePathCombo.setModel(new DefaultComboBoxModel(currentNodeChildren.toArray()));
                return;
            }

            if (lastEnteredCharacter == '.') {
                processDotEntered();
                return;
            }
            int currentEmbededLevel =  StringUtils.countMatches(dbAttributePath,".");
            if (previousEmbededLevel != currentEmbededLevel){
                previousEmbededLevel = currentEmbededLevel;
                List<String> currentNodeChildren = new ArrayList<>();
                String[] pathStrings = dbAttributePath.split(Pattern.quote("."));
                String lastStringInPath = pathStrings[pathStrings.length - 1];
                String saveDbAttributePath = dbAttributePath;
                dbAttributePath = dbAttributePath.replaceAll(lastStringInPath + "$", "");
                currentNodeChildren.addAll(getChildren(getCurrentNode(dbAttributePath), dbAttributePath));
                dbAttributePathCombo.setModel(new DefaultComboBoxModel(currentNodeChildren.toArray()));
                ((JTextComponent) (dbAttributePathCombo).
                        getEditor().getEditorComponent()).setText(saveDbAttributePath);
                return;
            }
        }

        private void processDotEntered(){
            String dbAttributePath = ((JTextComponent) (dbAttributePathCombo).
                    getEditor().getEditorComponent()).getText();
            if (dbAttributePath.equals(".")){
                List<String> currentNodeChildren = new ArrayList<>();
                currentNodeChildren.add("");
                currentNodeChildren.addAll(getChildren(getCurrentNode(""),""));
                dbAttributePathCombo.setModel(new DefaultComboBoxModel(currentNodeChildren.toArray()));
                dbAttributePathCombo.showPopup();
                dbAttributePathCombo.setPopupVisible(true);
                return;
            }else {
                char secondFromEndCharacter = dbAttributePath.charAt(dbAttributePath.length()-2);
                if(secondFromEndCharacter == '.') {
                    // two dots entered one by one , we replace it by one dot
                    ((JTextComponent) (dbAttributePathCombo).
                            getEditor().getEditorComponent()).setText(dbAttributePath.substring(0,dbAttributePath.length()-1));
                    return;
                }else{
                    String[] pathStrings = dbAttributePath.split(Pattern.quote("."));
                    String lastStringInPath = pathStrings[pathStrings.length - 1];

                    //we will check if lastStringInPath is correct name of DbAttribute or DbRelationship
                    //for appropriate previous node in path. if it is not we won't add entered dot to dbAttributePath
                    String dbAttributePathForPreviousNode;
                    if (pathStrings.length == 1){
                        //previous root is treeModel.getRoot()
                        dbAttributePathForPreviousNode = null;
                    }else {
                        dbAttributePathForPreviousNode = dbAttributePath.replace("."+lastStringInPath,"");
                    }
                    List<String> potentialVariantsToChoose = getChildren(getCurrentNode(dbAttributePathForPreviousNode),"");
                    if (potentialVariantsToChoose.contains(lastStringInPath)){
                        List<String> currentNodeChildren = new ArrayList<>();
                        currentNodeChildren.add(dbAttributePath + "");
                        currentNodeChildren.addAll(getChildren(getCurrentNode(dbAttributePath), dbAttributePath));
                        dbAttributePathCombo.setModel(new DefaultComboBoxModel(currentNodeChildren.toArray()));
                        dbAttributePathCombo.showPopup();
                        dbAttributePathCombo.setPopupVisible(true);
                    }else{
                        ((JTextComponent) (dbAttributePathCombo).
                                getEditor().getEditorComponent()).setText(dbAttributePath.substring(0,dbAttributePath.length()-1));
                    }
                }
            }
            previousEmbededLevel =  StringUtils.countMatches(dbAttributePath,".");
            return;
        }

        /**
         * find current node by dbAttributePath
         * @param dbAttributePath
         * @return last node in dbAttributePath which matches DbRelationship or DbAttribute
         */
        private final Object getCurrentNode(String dbAttributePath) {
            try {
                //case for new attribute
                if(dbAttributePath == null){
                    return treeModel.getRoot();
                }
                String[] pathStrings = dbAttributePath.split(Pattern.quote("."));
                Object root = treeModel.getRoot();
                for (int  i = 0 ; i < pathStrings.length ; i ++) {
                    String rootChildText = pathStrings[i];
                    for (int j = 0; j < treeModel.getChildCount(root); j++) {
                        Object child = treeModel.getChild(root, j);
                        String objectName = ModelerUtil.getObjectName(child);
                        if (objectName.equals(rootChildText)) {
                            root = child;
                            break;
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
         * @param dbAttributePath string which will be added to each child to make right autocomplete
         * @return list with children , which will be used to autocomplete
         */
        private final List<String> getChildren(Object node , String dbAttributePath){
            List<String> currentNodeChildren = new ArrayList<>();
            for(int j = 0 ; j <  treeModel.getChildCount(node) ; j++){
                Object child = treeModel.getChild(node, j);
                String objectName = ModelerUtil.getObjectName(child);
                currentNodeChildren.add(dbAttributePath+objectName);
            }
            return currentNodeChildren;
        }

        /**
         * @param attributeIndexInTable index of attribute for which now we will create cell editor
         * @return treeModel for nessesary for us attribute
         */
        private EntityTreeModel createTreeModelForComboBoxBrowser(int attributeIndexInTable){
            ObjAttribute attribute = model.getAttribute(attributeIndexInTable).getValue();
            Entity firstEntity = null;
            if (attribute.getDbAttribute() == null) {

                if (attribute.getParent() instanceof ObjEntity) {
                    DbEntity dbEnt = ((ObjEntity) attribute.getParent()).getDbEntity();

                    if (dbEnt != null) {
                        Collection<DbAttribute> attributes = dbEnt.getAttributes();
                        Collection<DbRelationship> rel = dbEnt.getRelationships();

                        if (attributes.size() > 0) {
                            Iterator<DbAttribute> iterator = attributes.iterator();
                            firstEntity = iterator.next().getEntity();
                        } else if (rel.size() > 0) {
                            Iterator<DbRelationship> iterator = rel.iterator();
                            firstEntity = iterator.next().getSourceEntity();
                        }
                    }
                }
            } else {
                firstEntity = getFirstEntity(attribute);
            }

            if (firstEntity != null) {
                EntityTreeModel treeModel = new EntityTreeModel(firstEntity);
                treeModel.setFilter(new EntityTreeFilter() {

                    public boolean attributeMatch(Object node, Attribute attr) {
                        if (!(node instanceof Attribute)) {
                            return true;
                        }
                        return false;
                    }

                    public boolean relationshipMatch(Object node, Relationship rel) {
                        if (!(node instanceof Relationship)) {
                            return true;
                        }

                        /**
                         * We do not allow A->B->A chains, where relationships
                         * are to-one
                         */
                        DbRelationship prev = (DbRelationship) node;
                        return !(!rel.isToMany() && prev.getReverseRelationship() == rel);
                    }
                });
                return treeModel;
            }
            return null;
        }
        private Entity getFirstEntity(ObjAttribute attribute) {
            Iterator<CayenneMapEntry> it = attribute.getDbPathIterator();
            Entity firstEnt = attribute.getDbAttribute().getEntity();
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
    }
}
