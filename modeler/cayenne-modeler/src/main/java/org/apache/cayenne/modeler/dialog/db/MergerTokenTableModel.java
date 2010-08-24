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
package org.apache.cayenne.modeler.dialog.db;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.apache.cayenne.merge.MergeDirection;
import org.apache.cayenne.merge.MergerToken;

public class MergerTokenTableModel extends AbstractTableModel {

    public static final int COL_SELECT = 0;
    public static final int COL_DIRECTION = 1;
    public static final int COL_NAME = 2;
    public static final int COL_VALUE = 3;

    private MergerTokenSelectorController controller;

    private List<MergerToken> tokens;

    public MergerTokenTableModel(MergerTokenSelectorController controller) {
        this.controller = controller;
        this.tokens = controller.getSelectableTokens();
    }

    private MergerTokenSelectorController getController() {
        return controller;
    }

    public Class getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case COL_SELECT:
                return Boolean.class;
            case COL_DIRECTION:
                // TODO: correct?
                return String.class;
            case COL_NAME:
            case COL_VALUE:
                return String.class;
        }

        return null;
    }

    public int getColumnCount() {
        return 4;
    }

    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case COL_SELECT:
                return "";
            case COL_NAME:
                return "Operation";
            case COL_DIRECTION:
                return "Direction";
            case COL_VALUE:
                return "";
        }

        return null;
    }

    public int getRowCount() {
        return tokens.size();
    }

    public MergerToken getToken(int rowIndex) {
        return tokens.get(rowIndex);
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        MergerToken token = getToken(rowIndex);
        switch (columnIndex) {
            case COL_SELECT:
                return Boolean.valueOf(getController().isSelected(token));
            case COL_NAME:
                return token.getTokenName();
            case COL_DIRECTION:
                return token.getDirection();
            case COL_VALUE:
                return token.getTokenValue();
        }
        return null;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case COL_SELECT:
            case COL_DIRECTION:
                return true;
        }
        return false;
    }

    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        MergerToken token = getToken(rowIndex);
        switch (columnIndex) {
            case COL_SELECT:
                Boolean val = (Boolean) value;
                getController().select(token, val.booleanValue());
                break;
            case COL_DIRECTION:
                MergeDirection direction = (MergeDirection) value;
                getController().setDirection(token, direction);
                break;
        }
    }

}
