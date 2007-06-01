/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.dataview;

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