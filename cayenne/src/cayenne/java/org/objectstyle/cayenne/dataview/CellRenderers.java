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