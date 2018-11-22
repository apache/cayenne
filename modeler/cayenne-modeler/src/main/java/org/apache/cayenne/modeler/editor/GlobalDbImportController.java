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
package org.apache.cayenne.modeler.editor;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.dialog.db.load.DbLoadResultDialog;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.util.concurrent.ConcurrentMap;

/**
 * @since 4.1
 */
public class GlobalDbImportController {

    private static final String DIALOG_TITLE = "Reverse Engineering Result";

    private DbLoadResultDialog dbLoadResultDialog;
    private boolean globalImport;

    public GlobalDbImportController() {
        this.dbLoadResultDialog = new DbLoadResultDialog(DIALOG_TITLE);
    }

    public DbLoadResultDialog createDialog() {
        return dbLoadResultDialog;
    }

    public void showDialog() {
        dbLoadResultDialog.pack();
        dbLoadResultDialog.setVisible(true);
    }

    public void setGlobalImport(boolean globalImport) {
        this.globalImport = globalImport;
    }

    public boolean isGlobalImport() {
        return globalImport;
    }

    public void checkImport(DataMap dataMap) {
        dbLoadResultDialog.getTableForMap().remove(dataMap);
    }

    public void resetDialog() {
        ConcurrentMap<DataMap, JTable> tableMap = dbLoadResultDialog.getTableForMap();
        for(DataMap dataMap : tableMap.keySet()) {
            JTable table = tableMap.get(dataMap);
            DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
            int rowCount = tableModel.getRowCount();
            for (int i = rowCount - 1; i >= 0; i--) {
                tableModel.removeRow(i);
            }
        }

        dbLoadResultDialog.getTableForMap().clear();
        dbLoadResultDialog.getTablePanel().removeAll();
    }
}
