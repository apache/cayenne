/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.modeler.editor;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.Relationship;

import javax.swing.table.AbstractTableModel;
import java.util.Map;

/**
 * A table model for the Prefetch table.
 */
class PrefetchModel extends AbstractTableModel {

    private final Map<String, Integer> prefetchMap;

    private final Object root;

    private final String[] prefetches;

    PrefetchModel(Map<String, Integer> prefetchMap, Object root) {
        this.prefetchMap = prefetchMap;
        this.prefetches = prefetchMap.keySet().toArray(new String[0]);
        this.root = root;
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public int getRowCount() {
        return (prefetches != null) ? prefetches.length : 0;
    }

    @Override
    public Object getValueAt(int row, int column) {
        switch (column) {
            case 0:
                return prefetches[row];
            case 1:
                return isToMany(prefetches[row]) ? Boolean.TRUE : Boolean.FALSE;
            case 2:
                return getPrefetchTypeString(prefetchMap.get(prefetches[row]));
            default:
                throw new IndexOutOfBoundsException("Invalid column: " + column);
        }
    }

    @Override
    public Class getColumnClass(int column) {
        switch (column) {
            case 0:
                return String.class;
            case 1:
                return Boolean.class;
            case 2:
                return String.class;
            default:
                throw new IndexOutOfBoundsException("Invalid column: " + column);
        }
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return "Prefetch Path";
            case 1:
                return "To Many";
            case 2:
                return "Prefetch Type";
            default:
                throw new IndexOutOfBoundsException("Invalid column: " + column);
        }
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return column == 2;
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        switch (column) {
            case 2:
                prefetchMap.put(prefetches[row], getPrefetchType((String)value));
                break;
        }
    }

    static int getPrefetchType(String semantics) {

        //case 2: disjoint isn't use for SQLTemplate prefetch
        switch (semantics){
            case "Joint" :
                return 1;
            case "Disjoint":
                return 2;
            case "Disjoint by id":
                return 3;
            default: return 0;
        }
    }

    private static String getPrefetchTypeString(int semantics) {
        switch (semantics){
            case 1 :
                return SelectQueryPrefetchTab.JOINT_PREFETCH_SEMANTICS;
            case 2:
                return SelectQueryPrefetchTab.DISJOINT_PREFETCH_SEMANTICS;
            case 3:
                return SelectQueryPrefetchTab.DISJOINT_BY_ID_PREFETCH_SEMANTICS;
        }
        return SelectQueryPrefetchTab.UNDEFINED_SEMANTICS;
    }

    private boolean isToMany(String prefetch) {
        // totally invalid path would result in ExpressionException
        try {
            Expression exp = ExpressionFactory.exp(prefetch);
            Object object = exp.evaluate(root);
            return object instanceof Relationship && ((Relationship) object).isToMany();
        } catch (ExpressionException e) {
            return false;
        }
    }
}
