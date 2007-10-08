package org.objectstyle.cayenne.swing;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTable;

/**
 * A builder for a JTable binding.
 * 
 * @author Andrei Adamchik
 */
public class TableBindingBuilder {

    protected BindingBuilder helper;
    protected List columns;

    public TableBindingBuilder(BindingFactory factory, Object context) {
        this(new BindingBuilder(factory, context));
    }

    public TableBindingBuilder(BindingBuilder helper) {
        this.helper = helper;
    }

    /**
     * Creates a binding using preconfigured parameters.
     */
    public ObjectBinding bindToTable(JTable table, String listBinding) {
        int width = (columns != null) ? columns.size() : 0;

        String[] headers = new String[width];
        BindingExpression[] expressions = new BindingExpression[width];
        Class[] classes = new Class[width];
        boolean[] editableState = new boolean[width];
        Object[] sampleLongValues = new Object[width];

        for (int i = 0; i < width; i++) {
            ColumnDescriptor descriptor = (ColumnDescriptor) columns.get(i);
            headers[i] = descriptor.header;
            expressions[i] = descriptor.expression;
            classes[i] = descriptor.columnClass;
            editableState[i] = descriptor.editable;
            sampleLongValues[i] = descriptor.sampleLongValue;
        }

        ObjectBinding binding = helper.getFactory().bindToTable(
                table,
                listBinding,
                headers,
                expressions,
                classes,
                editableState,
                sampleLongValues);
        return helper.initBinding(binding, helper.getDelegate());
    }

    /**
     * Adds a column to the table description.
     */
    public void addColumn(
            String header,
            String expression,
            Class columnClass,
            boolean editable) {

        this.addColumn(header, expression, columnClass, editable, null);
    }

    /**
     * Adds a column to the table description.
     * 
     * @param sampleLongValue if not null, this value rendered size is used to size the
     *            column.
     */
    public void addColumn(
            String header,
            String expression,
            Class columnClass,
            boolean editable,
            Object sampleLongValue) {

        if (columns == null) {
            columns = new ArrayList();
        }

        columns.add(new ColumnDescriptor(
                header,
                new BindingExpression(expression),
                columnClass,
                editable,
                sampleLongValue));
    }

    final class ColumnDescriptor {

        String header;
        BindingExpression expression;
        boolean editable;
        Class columnClass;
        Object sampleLongValue;

        ColumnDescriptor(String header, BindingExpression expression, Class columnClass,
                boolean editable, Object sampleLongValue) {
            this.header = header;
            this.expression = expression;
            this.editable = editable;
            this.columnClass = columnClass;
            this.sampleLongValue = sampleLongValue;
        }
    }
}