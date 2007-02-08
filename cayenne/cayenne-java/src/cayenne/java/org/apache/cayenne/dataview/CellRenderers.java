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

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.text.*;

public class CellRenderers {
  public FormatRenderer createFormatTableCellRenderer(
      Format format,
      String nullText,
      String invalidText,
      int alignment) {
    FormatRenderer renderer = new FormatRenderer(format, nullText, invalidText);
    if (alignment >= 0)
      renderer.setHorizontalAlignment(alignment);
    return renderer;
  }

  public BooleanRenderer createBooleanTableCellRenderer() {
    BooleanRenderer renderer = new BooleanRenderer();
    return renderer;
  }

  public ObjectRenderer createDefaultTableCellRenderer(int alignment) {
    ObjectRenderer renderer = new ObjectRenderer();
    if (alignment >= 0)
      renderer.setHorizontalAlignment(alignment);
    return renderer;
  }

  public TableCellRenderer createTableCellRenderer(ObjEntityViewField field) {
    TableCellRenderer renderer = null;
    Format format = field.getDisplayFormat();
    int dataType = field.getDataType().getValue();
    int alignment;
    String nullText = "";
    String invalidText = "";

    switch (dataType) {
      case DataTypeEnum.INTEGER_TYPE_VALUE:
      case DataTypeEnum.DOUBLE_TYPE_VALUE:
      case DataTypeEnum.MONEY_TYPE_VALUE:
      case DataTypeEnum.PERCENT_TYPE_VALUE:
        alignment = JLabel.RIGHT;
        break;
      default:
        alignment = JLabel.LEFT;
        break;
    }

    if (format != null) {
      renderer = createFormatTableCellRenderer(
          format, nullText, invalidText, alignment);
    } else {
      if (dataType == DataTypeEnum.BOOLEAN_TYPE_VALUE)
        renderer = createBooleanTableCellRenderer();
      else
        renderer = createDefaultTableCellRenderer(alignment);
    }

    return renderer;
  }

  public ListCellRenderer createFormatListCellRenderer(
      Format format,
      String nullText,
      String invalidText,
      int alignment) {
    FormatListCellRenderer renderer = new FormatListCellRenderer(
        format, nullText, invalidText);
    if (alignment >= 0)
      renderer.setHorizontalAlignment(alignment);
    return renderer;
  }

  public ListCellRenderer createListCellRenderer(ObjEntityViewField field) {
    ListCellRenderer renderer = null;
    Format format = field.getDisplayFormat();
    int dataType = field.getDataType().getValue();
    int alignment;
    String nullText = "";
    String invalidText = "";

    switch (dataType) {
      case DataTypeEnum.INTEGER_TYPE_VALUE:
      case DataTypeEnum.DOUBLE_TYPE_VALUE:
      case DataTypeEnum.MONEY_TYPE_VALUE:
      case DataTypeEnum.PERCENT_TYPE_VALUE:
        alignment = JLabel.RIGHT;
      default:
        alignment = JLabel.LEFT;
        break;
    }

    if (format != null) {
      renderer = createFormatListCellRenderer(
          format, nullText, invalidText, alignment);
    } else {
      renderer = new DefaultListCellRenderer();
      ((DefaultListCellRenderer)renderer).setHorizontalAlignment(alignment);
    }

    return renderer;
  }

  public void installRenderers(JTable table) {
    TableModel m = table.getModel();
    if (!(m instanceof DOTableModel))
      return;
    DOTableModel model = (DOTableModel)m;
    TableColumnModel columnModel = table.getColumnModel();
    int columnCount = model.getColumnCount();
    for (int i = 0; i < columnCount; i++) {
      ObjEntityViewField field = model.getField(i);
      TableCellRenderer renderer = createTableCellRenderer(field);
      TableColumn column = columnModel.getColumn(i);
      column.setCellRenderer(renderer);
    }
  }

  public void installRenderer(JList list, ObjEntityViewField field) {
    ListCellRenderer renderer = createListCellRenderer(field);
    list.setCellRenderer(renderer);
  }

  public void installRenderer(JComboBox comboBox, ObjEntityViewField field) {
    ListCellRenderer renderer = createListCellRenderer(field);
    comboBox.setRenderer(renderer);
  }

  public class BooleanRenderer extends JCheckBox implements TableCellRenderer {
    public BooleanRenderer() {
      super();
      setHorizontalAlignment(JLabel.CENTER);
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column) {
      if (isSelected) {
        setForeground(table.getSelectionForeground());
        super.setBackground(table.getSelectionBackground());
      }
      else {
        setForeground(table.getForeground());
        setBackground(table.getBackground());
      }
      setSelected((value != null && ((Boolean)value).booleanValue()));
      return this;
    }
  }

  public class ObjectRenderer extends DefaultTableCellRenderer {
  }

  public class FormatRenderer extends DefaultTableCellRenderer {
    protected Format formatter;
    protected String nullText = "";
    protected String invalidText = "";

    public FormatRenderer() {
      this(null, null, null);
    }

    public FormatRenderer(Format formatter) {
      this(formatter, null, null);
    }

    public FormatRenderer(Format formatter, String nullText) {
      this(formatter, nullText, null);
    }

    public FormatRenderer(Format formatter, String nullText, String invalidText) {
      setFormatter(formatter);
      setNullText(nullText);
      setInvalidText(invalidText);
    }

    public void setFormatter(Format formatter) {
      this.formatter = formatter;
    }
    public Format getFormatter() {
      return formatter;
    }
    public void setNullText(String nullText) {
      this.nullText = (nullText != null ? nullText : this.nullText);
    }
    public String getNullText() {
      return nullText;
    }
    public void setInvalidText(String invalidText) {
      this.invalidText = (invalidText != null ? invalidText : this.invalidText);
    }
    public String getInvalidText() {
      return invalidText;
    }

    public void setValue(Object value) {
      String text;
      try {
        if (value == null)
          text = nullText;
        else if (formatter==null)
          text = value.toString();
        else
          text = formatter.format(value);
      }
      catch (Exception ex) {
        text = invalidText;
      }
      setText(text);
    }
  }

  public class FormatListCellRenderer extends DefaultListCellRenderer {
    protected Format formatter;
    protected String nullText = "";
    protected String invalidText = "";

    public FormatListCellRenderer() {
      this(null, null, null);
    }

    public FormatListCellRenderer(Format formatter) {
      this(formatter, null, null);
    }

    public FormatListCellRenderer(Format formatter, String nullText) {
      this(formatter, nullText, null);
    }

    public FormatListCellRenderer(Format formatter, String nullText, String invalidText) {
      setFormatter(formatter);
      setNullText(nullText);
      setInvalidText(invalidText);
    }

    public void setFormatter(Format formatter) {
      this.formatter = formatter;
    }
    public Format getFormatter() {
      return formatter;
    }
    public void setNullText(String nullText) {
      this.nullText = (nullText != null ? nullText : this.nullText);
    }
    public String getNullText() {
      return nullText;
    }
    public void setInvalidText(String invalidText) {
      this.invalidText = (invalidText != null ? invalidText : this.invalidText);
    }
    public String getInvalidText() {
      return invalidText;
    }

    public Component getListCellRendererComponent(
        JList list,
        Object value,
        int index,
        boolean isSelected,
        boolean cellHasFocus) {
      String text;
      try {
        if (value == null)
          text = nullText;
        else if (formatter==null)
          text = value.toString();
        else
          text = formatter.format(value);
      }
      catch (Exception ex) {
        text = invalidText;
      }
      return super.getListCellRendererComponent(
          list, text, index, isSelected, cellHasFocus);
    }
  }

}
