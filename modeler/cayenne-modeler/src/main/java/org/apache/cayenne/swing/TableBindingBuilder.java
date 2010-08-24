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


package org.apache.cayenne.swing;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTable;

/**
 * A builder for a JTable binding.
 * 
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
