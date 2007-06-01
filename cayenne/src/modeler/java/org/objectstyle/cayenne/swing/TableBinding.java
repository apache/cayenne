package org.objectstyle.cayenne.swing;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.collections.map.SingletonMap;

/**
 * A binding for a JTable.
 * 
 * @author Andrei Adamchik
 */
public class TableBinding extends BindingBase {

    /**
     * A variable exposed in the context of set/get cell value.
     */
    public static final String ITEM_VAR = "item";

    protected JTable table;
    protected String[] headers;
    protected BindingExpression[] columns;
    protected boolean[] editableState;
    protected Class[] columnClass;
    protected List list;

    public TableBinding(JTable table, String listBinding, String[] headers,
            BindingExpression[] columns, Class[] columnClass, boolean[] editableState) {

        super(listBinding);
        this.table = table;
        this.headers = headers;
        this.columns = columns;
        this.editableState = editableState;
        this.columnClass = columnClass;

        table.setModel(new BoundTableModel());
    }

    public void setContext(Object object) {
        super.setContext(object);

        this.list = updateList();
    }

    public Component getComponent() {
        return table;
    }

    public void updateView() {
        this.list = updateList();
        ((BoundTableModel) table.getModel()).fireTableDataChanged();
    }

    int getListSize() {
        return (list != null) ? list.size() : 0;
    }

    List updateList() {
        if (getContext() == null) {
            return null;
        }

        Object list = getValue();
        if (list == null) {
            return null;
        }

        if (list instanceof List) {
            return (List) list;
        }

        if (list instanceof Object[]) {
            Object[] objects = (Object[]) list;
            return Arrays.asList(objects);
        }

        if (list instanceof Collection) {
            return new ArrayList((Collection) list);
        }

        throw new BindingException("List expected, got - " + list);
    }

    final class BoundTableModel extends AbstractTableModel {

        // this map is used as "flywieght", providing on the spot context for Ognl
        // expression evaluation
        Map listContext = new SingletonMap(ITEM_VAR, null);

        public int getColumnCount() {
            return headers.length;
        }

        public int getRowCount() {
            return getListSize();
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return editableState[columnIndex];
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            Object item = list.get(rowIndex);
            listContext.put(ITEM_VAR, item);
            return columns[columnIndex].getValue(getContext(), listContext);
        }

        public String getColumnName(int column) {
            return headers[column];
        }

        public Class getColumnClass(int columnIndex) {
            return columnClass[columnIndex];
        }

        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            Object item = list.get(rowIndex);
            listContext.put(ITEM_VAR, item);
            columns[columnIndex].setValue(getContext(), listContext, value);
        }
    }
}