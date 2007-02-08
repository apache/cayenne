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

package org.apache.cayenne.dataview;

import java.text.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.text.*;
import javax.swing.border.*;

public class CellEditors {
  protected Border editStateBorder = new LineBorder(Color.black);
  protected Border invalidStateBorder = new LineBorder(Color.red);

  public FormattedFieldEditor createFormattedFieldEditor(
      JFormattedTextField.AbstractFormatter formatter, int alignment) {
    JFormattedTextField field = new JFormattedTextField(formatter);
    if (alignment >= 0)
      field.setHorizontalAlignment(alignment);
    return new FormattedFieldEditor(field);
  }

  public FormattedFieldEditor createFormattedFieldEditor(
      Format formatter, int alignment) {
    JFormattedTextField field = new JFormattedTextField(formatter);
    if (alignment >= 0)
      field.setHorizontalAlignment(alignment);
    return new FormattedFieldEditor(field);
  }

  public FormattedFieldEditor createFormattedFieldEditor(
      String mask, int alignment) throws ParseException {
    MaskFormatter formatter = new MaskFormatter(mask);
    return createFormattedFieldEditor(formatter, alignment);
  }

  public TextFieldEditor createTextFieldEditor(int alignment) {
    JTextField field = new JTextField();
    if (alignment >= 0)
      field.setHorizontalAlignment(alignment);
    return new TextFieldEditor(field);
  }

  public CheckBoxEditor createCheckBoxEditor() {
    JCheckBox checkBox = new JCheckBox();
    return new CheckBoxEditor(checkBox);
  }

  public ComboBoxEditor createComboBoxEditor(
      ComboBoxModel model, ListCellRenderer renderer) {
    JComboBox comboBox = new JComboBox(model);
    if (renderer != null)
      comboBox.setRenderer(renderer);
    return new ComboBoxEditor(comboBox);
  }

  public ButtonEditor createButtonEditor(JButton button) {
    return new ButtonEditor(button);
  }

  public SpinnerEditor createSpinnerEditor(
      SpinnerModel model, Format format) {
    JSpinner spinner = new JSpinner(model);
    if (format != null) {
      JFormattedTextField field = ((JSpinner.DefaultEditor)spinner.getEditor()).getTextField();
      JFormattedTextField.AbstractFormatter formatter = field.getFormatter();
      if (formatter instanceof InternationalFormatter)
        ((InternationalFormatter)formatter).setFormat(format);
    }
    return new SpinnerEditor(spinner);
  }

  public TableCellEditor createTableCellEditor(ObjEntityViewField field) {
    CellRenderers cellRenderers = new CellRenderers();
    TableCellEditor editor = null;
    Format format = field.getEditFormat();
    int dataType = field.getDataType().getValue();
    boolean lookup = field.isLookup();
    int alignment;

    switch (dataType) {
      case DataTypeEnum.INTEGER_TYPE_VALUE:
      case DataTypeEnum.DOUBLE_TYPE_VALUE:
      case DataTypeEnum.MONEY_TYPE_VALUE:
      case DataTypeEnum.PERCENT_TYPE_VALUE:
        alignment = JTextField.RIGHT;
        break;
      default:
        alignment = JTextField.LEFT;
        break;
    }

    if (lookup) {
      ComboBoxModel comboData =
          new DefaultComboBoxModel(field.getLookupValues());
      ListCellRenderer comboRenderer =
          cellRenderers.createListCellRenderer(field);
      editor = createComboBoxEditor(comboData, comboRenderer);
    } else if (format != null) {
      if (format instanceof MapFormat) {
        MapFormat mapFormat = (MapFormat)format;
        ComboBoxModel comboData =
          new DefaultComboBoxModel((mapFormat).getValues());
        ListCellRenderer comboRenderer =
          cellRenderers.createFormatListCellRenderer(
          mapFormat, mapFormat.getNullFormat(), null, -1);
        editor = createComboBoxEditor(comboData, comboRenderer);
      } else {
        editor = createFormattedFieldEditor(format, alignment);
      }
    } else {
      if (dataType == DataTypeEnum.BOOLEAN_TYPE_VALUE)
        editor = createCheckBoxEditor();
      else
        editor = createTextFieldEditor(alignment);
    }

    return editor;
  }

  public void installEditors(JTable table) {
    TableModel m = table.getModel();
    if (!(m instanceof DOTableModel))
      return;
    DOTableModel model = (DOTableModel)m;
    TableColumnModel columnModel = table.getColumnModel();
    int columnCount = model.getColumnCount();
    for (int i = 0; i < columnCount; i++) {
      ObjEntityViewField field = model.getField(i);
      TableCellEditor editor = createTableCellEditor(field);
      TableColumn column = columnModel.getColumn(i);
      column.setCellEditor(editor);
    }
  }

  public class FormattedFieldEditor extends DefaultCellEditor {
    public FormattedFieldEditor(final JFormattedTextField field) {
      super(field);
      field.removeActionListener(delegate);
      this.clickCountToStart = 2;
      delegate = new EditorDelegate() {
        public void setValue(Object value) {
          field.setValue(value);
        }
        public Object getCellEditorValue() {
          return field.getValue();
        }
        public boolean stopCellEditing() {
          if (field.isEditValid()) {
            try {field.commitEdit();}
            catch (ParseException ex) {}
          } else {
            field.setBorder(invalidStateBorder);
            return false;
          }
          return super.stopCellEditing();
        }
      };
      field.addActionListener(delegate);
    }
    public Component getTableCellEditorComponent(JTable table, Object value,
                         boolean isSelected,
                         int row, int column) {
      delegate.setValue(value);
      if (((JFormattedTextField)editorComponent).isEditValid())
        editorComponent.setBorder(editStateBorder);
      else
        editorComponent.setBorder(invalidStateBorder);
      return editorComponent;
    }
  }

  public class TextFieldEditor extends DefaultCellEditor {
    protected Border editStateBorder = new LineBorder(Color.black);
    public TextFieldEditor(JTextField field) {
      super(field);
      field.setBorder(editStateBorder);
    }
  }

  public class CheckBoxEditor extends DefaultCellEditor {
    public CheckBoxEditor(JCheckBox checkBox) {
      super(checkBox);
      checkBox.setBorder(editStateBorder);
      checkBox.setHorizontalAlignment(JCheckBox.CENTER);
    }
  }

  public class ComboBoxEditor extends DefaultCellEditor {
    public ComboBoxEditor(JComboBox comboBox) {
      super(comboBox);
      comboBox.setBorder(editStateBorder);
    }
  }

  public class SpinnerEditor extends DefaultCellEditor {
    public SpinnerEditor(final JSpinner spinner) {
      super(new JTextField());
      editorComponent = spinner;
      spinner.setBorder(editStateBorder);
      spinner.getEditor().setBorder(null);
      final JFormattedTextField field =
          ((JSpinner.DefaultEditor)spinner.getEditor()).getTextField();
      field.setBorder(null);
      delegate = new EditorDelegate() {
        public void setValue(Object value) {
          spinner.setValue(value);
        }
        public Object getCellEditorValue() {
          return spinner.getValue();
        }
        public boolean stopCellEditing() {
          if (field.isEditValid()) {
            try {field.commitEdit();}
            catch (ParseException ex) {}
          } else {
            field.setBorder(invalidStateBorder);
            return false;
          }
          return super.stopCellEditing();
        }
      };
      field.addActionListener(delegate);
    }
//    public Component getTableCellEditorComponent(JTable table, Object value,
//        boolean isSelected, int row, int column) {
//      JSpinner spinner = (JSpinner)editorComponent;
//      spinner.setValue(value);
//      return spinner;
//    }
//    public Object getCellEditorValue() {
//      JSpinner spinner = (JSpinner)editorComponent;
//      return spinner.getValue();
//    }
  }

  public class ButtonEditor extends DefaultCellEditor {
    protected Object currentValue;

    public ButtonEditor(JButton button) {
      super(new JCheckBox());
      editorComponent = button;
      setClickCountToStart(1);
      button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          fireEditingStopped();
        }
      });
      button.setBorder(editStateBorder);
      button.setFocusPainted(false);
    }
    protected void fireEditingStopped() {
      super.fireEditingStopped();
    }
    public Object getCellEditorValue() {
      return currentValue;
    }
    public Component getTableCellEditorComponent(JTable table,
        Object value,
        boolean isSelected,
        int row,
        int column) {
      ((JButton)editorComponent).setText(String.valueOf(value));
      currentValue = value;
      return editorComponent;
    }
  }
}
