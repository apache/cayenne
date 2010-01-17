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
package org.apache.cayenne.modeler.dialog.validator;


import org.apache.cayenne.modeler.CayenneModelerFrame;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.CayenneDialog;
import org.apache.cayenne.modeler.util.CayenneWidgetFactory;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.apache.cayenne.modeler.util.CayenneTableModel;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;


import javax.swing.table.TableColumn;
import javax.swing.JTable;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.JDialog;
import java.util.List;
import java.util.LinkedList;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.FlowLayout;
import java.awt.BorderLayout;


import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.builder.PanelBuilder;

/**
 * Dialog for resolving name collision.
 * 
 */
public class DuplicatedAttributesDialog extends CayenneDialog {
    
    protected static DuplicatedAttributesDialog instance;
    
    static final String DELETE_ACTION = "delete";
    static final String RENAME_ACTION = "rename";
    
    public static final String CANCEL_RESULT = "cancel";
    public static final String PROCEEDED_RESULT = "proceeded";
    
    static String result = CANCEL_RESULT;
    
    protected List<DuplicatedAttributeInfo> duplicatedAttributes;
    
    protected ObjEntity superEntity;
    protected ObjEntity entity;
    
    protected JTable attributesTable;
    protected JButton cancelButton;
    protected JButton proceedButton;

    public static synchronized void showDialog(
            CayenneModelerFrame editor,
            List<ObjAttribute> duplicatedAttributes,
            ObjEntity superEntity,
            ObjEntity entity) {

        if (instance == null) {
            instance = new DuplicatedAttributesDialog(editor);
            instance.centerWindow();
        }

        instance.setSuperEntity(superEntity);
        instance.setEntity(entity);
        instance.setDuplicatedAttributes(duplicatedAttributes);
        instance.updateTable();
        instance.setVisible(true);
    }

    protected DuplicatedAttributesDialog(CayenneModelerFrame editor) {
        super(editor, "Duplicated Attributes", true);

        result = CANCEL_RESULT;
        
        initView();
        initController();
    }

    private void initView() {
        cancelButton = new JButton("Cancel");
        proceedButton = new JButton("Continue");

        attributesTable = new JTable();

        // assemble
        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(new FormLayout(
                "fill:200dlu:grow",
                "pref, 3dlu, top:40dlu:grow"));

        builder.setDefaultDialogBorder();

        builder
                .addLabel(
                        "Select actions for duplicated attributes:",
                        cc.xy(1, 1));
        builder.add(new JScrollPane(attributesTable), cc.xy(1, 3));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancelButton);
        buttons.add(proceedButton);
        

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);

        // TODO: use preferences
        setSize(450, 350);
    }

    private void initController() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                result = CANCEL_RESULT;
                setVisible(false);
                dispose();
            }
        });

        proceedButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                applyChanges();
                result = PROCEEDED_RESULT;
                setVisible(false);
                dispose();
            }
        });
    }

    public static String getResult() {
        return result;
    }

    private void updateTable() {
        TableColumn actionColumn = attributesTable.getColumnModel().getColumn(DuplicatedAttributeTableModel.ACTION);
        JComboBox actionsCombo = CayenneWidgetFactory.createComboBox(new String[]{DELETE_ACTION, RENAME_ACTION}, false);
        actionColumn.setCellEditor(CayenneWidgetFactory.createCellEditor(actionsCombo));
    }

    private void applyChanges() {
        for (DuplicatedAttributeInfo attributeInfo : duplicatedAttributes) {
            if (attributeInfo.getAction().equals(DELETE_ACTION)) {
                entity.removeAttribute(attributeInfo.getName());
            }
            if (attributeInfo.getAction().equals(RENAME_ACTION)) {
                ProjectUtil.setAttributeName(entity.getAttribute(attributeInfo.getName()), attributeInfo.getNewName());
            }
        }
    }
    
    public void setDuplicatedAttributes(List<ObjAttribute> attributes) {
        if (duplicatedAttributes == null) {
            duplicatedAttributes = new LinkedList<DuplicatedAttributeInfo>();
        }
        
        duplicatedAttributes.clear();
        
        for (ObjAttribute attribute : attributes) {
            DuplicatedAttributeInfo attributeInfo = new DuplicatedAttributeInfo(attribute.getName(), attribute.getType(),
                    ((ObjAttribute) superEntity.getAttribute(attribute.getName())).getType(), DELETE_ACTION);
            duplicatedAttributes.add(attributeInfo);
        }
        
        attributesTable.setModel(new DuplicatedAttributeTableModel(getMediator(), this, duplicatedAttributes));

    }

    public void setSuperEntity(ObjEntity superEntity) {
        this.superEntity = superEntity;
    }

    public void setEntity(ObjEntity entity) {
        this.entity = entity;
    }

    class DuplicatedAttributeTableModel extends CayenneTableModel {
        
        static final int ATTRIBUTE_NAME = 0;
        static final int PARENT_TYPE = 1;
        static final int TYPE = 2;
        static final int ACTION = 3;

        /**
         * Constructor for CayenneTableModel.
         */
        public DuplicatedAttributeTableModel(ProjectController mediator, Object eventSource, List objectList) {
            super(mediator, eventSource, objectList);
        }

        public void setUpdatedValueAt(Object newValue, int row, int column) {
            DuplicatedAttributeInfo attributeInfo = duplicatedAttributes.get(row);
            if(column == ATTRIBUTE_NAME) {
                attributeInfo.setNewName(newValue.toString());
                attributeInfo.setAction(RENAME_ACTION);
                //TODO: add warn if new valuew equals the old one or name equals to another attribute name.
                this.fireTableDataChanged();
            }

            if (column == ACTION) {
                attributeInfo.setAction(newValue.toString());
            }
            
        }

        public Class<?> getElementsClass() {
            return DuplicatedAttributeInfo.class;
        }

        public int getColumnCount() {
            return 4;
        }

        public Object getValueAt(int row, int col) {
            DuplicatedAttributeInfo attributeInfo = duplicatedAttributes.get(row);
            switch (col) {
                case ATTRIBUTE_NAME:
                    return attributeInfo.getNewName();
                case PARENT_TYPE:
                    return attributeInfo.getParentType();
                case TYPE:
                    return attributeInfo.getType();
                case ACTION:
                    return attributeInfo.getAction();
                
            }
            return "";
        }
        
        

        public boolean isCellEditable(int row, int column) {
            if (column == ACTION || column == ATTRIBUTE_NAME) {
                return true;
            }
            return false;
        }

        public String getColumnName(int column) {
            switch (column) {
                case ATTRIBUTE_NAME:
                    return "Name";
                case PARENT_TYPE:
                    return "Type in super entity";
                case TYPE :
                    return "Type";
                case ACTION:
                    return "Action";
            }
            return " ";
        }

        public Class getColumnClass(int column) {
          return String.class;
        }
    }

    public class DuplicatedAttributeInfo {
        private String name;
        private String newName;
        private String type;
        private String parentType;
        private String action;

        DuplicatedAttributeInfo(String name, String type, String parentType, String action) {
            this.name = name;
            this.newName = name;
            this.type = type;
            this.parentType = parentType;
            this.action = action;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public String getParentType() {
            return parentType;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getNewName() {
            return newName;
        }

        public void setNewName(String newName) {
            this.newName = newName;
        }
    }
    
}
