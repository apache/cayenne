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
package org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.mvc.ChildController;
import org.apache.cayenne.modeler.ui.ModelerController;
import org.apache.cayenne.modeler.ui.dbloadresult.DbLoadResultDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;
import java.util.concurrent.ConcurrentMap;

public class DbImportController extends ChildController<ModelerController> {

    private static final String DIALOG_TITLE = "Db Import Result";

    private DbLoadResultDialog dbLoadResultDialog;
    private boolean globalImport;

    public DbImportController(ModelerController parent) {
        super(parent);
    }

    @Override
    public Component getView() {
        return dbLoadResultDialog;
    }

    public DbLoadResultDialog createDialog() {
        if(dbLoadResultDialog == null) {
            dbLoadResultDialog = new DbLoadResultDialog(application, DIALOG_TITLE);
        }
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

    public void resetDialog() {
        ConcurrentMap<DataMap, JTable> tableMap = dbLoadResultDialog.getTableForMap();
        for(DataMap dataMap : tableMap.keySet()) {
            clearTable(dataMap);
        }

        dbLoadResultDialog.getTableForMap().clear();
        dbLoadResultDialog.removeListenersFromButtons();
        dbLoadResultDialog.getTablePanel().removeAll();
    }

    public void clearTable(DataMap dataMap) {
        JTable table = dbLoadResultDialog.getTableForMap().get(dataMap);
        if(table == null) {
            return;
        }
        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
        int rowCount = tableModel.getRowCount();
        for (int i = rowCount - 1; i >= 0; i--) {
            tableModel.removeRow(i);
        }
    }
}
