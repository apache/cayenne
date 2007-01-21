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

import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;

public class FieldComponentFactory {

  public FieldComponentFactory() {
  }

  public JComponent createFieldEditComponent(ObjEntityViewField field) {
    CellRenderers cellRenderers = new CellRenderers();
    JComponent editor = null;
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
      JComboBox comboBox = new JComboBox(comboData);
      comboBox.setRenderer(comboRenderer);
      editor = comboBox;
    } else if (format != null) {
      if (format instanceof MapFormat) {
        MapFormat mapFormat = (MapFormat)format;
        ComboBoxModel comboData =
          new DefaultComboBoxModel((mapFormat).getValues());
        ListCellRenderer comboRenderer =
          cellRenderers.createFormatListCellRenderer(
          mapFormat, mapFormat.getNullFormat(), null, -1);
        JComboBox comboBox = new JComboBox(comboData);
        comboBox.setRenderer(comboRenderer);
        editor = comboBox;
      } else {
        JFormattedTextField textField = new JFormattedTextField(format);
        if (alignment >= 0)
          textField.setHorizontalAlignment(alignment);
        if (format instanceof DecimalFormat)
          textField.setToolTipText(((DecimalFormat)format).toPattern());
        else if (format instanceof SimpleDateFormat)
          textField.setToolTipText(((SimpleDateFormat)format).toPattern());
        editor = textField;
      }
    } else {
      if (dataType == DataTypeEnum.BOOLEAN_TYPE_VALUE) {
        JCheckBox checkBox = new JCheckBox();
        editor = checkBox;
      } else {
        JTextField textField = new JTextField();
        if (alignment >= 0)
          textField.setHorizontalAlignment(alignment);
        editor = textField;
      }
    }

    return editor;
  }
}
