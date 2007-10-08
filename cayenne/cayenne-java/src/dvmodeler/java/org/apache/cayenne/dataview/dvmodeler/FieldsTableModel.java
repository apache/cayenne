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


package org.apache.cayenne.dataview.dvmodeler;

import javax.swing.table.*;

/**
 *
 * @author Nataliya Kholodna
 * @version 1.0
 */

public class FieldsTableModel extends AbstractTableModel {
  private ObjEntityView objEntityView;
  private String[] columnNames= {"Name",
                   "Type",
                   "CalcType",
                   "Attribute/Relationship",
                   "Lookup"};

  public FieldsTableModel(ObjEntityView objEntityView) {
    this.objEntityView = objEntityView;
  }

  public FieldsTableModel() {
  }

  public void setObjEntityView(ObjEntityView objEntityView){
    this.objEntityView = objEntityView;
    fireTableStructureChanged();
  }


  public int getRowCount(){
    if (objEntityView != null){
      return objEntityView.getObjEntityViewFieldCount();
    }else{
      return 0;
    }
  }

  public int getColumnCount(){
      return columnNames.length;
  }

  public String getColumnName(int col) {
    return columnNames[col];
  }

  public Class getColumnClass(int c) {
    return getValueAt(0, c).getClass();
  }

  public Object getValueAt(int row, int column){
    ObjEntityViewField field = objEntityView.getObjEntityViewField(row);
    switch (column){
      case 0:
        return field.getName();
      case 1:
        return field.getDataType();
      case 2:
        return field.getCalcType();
      case 3: {
        String calcType = field.getCalcType();
        if (calcType.equals("nocalc") &&
            field.getObjAttribute() != null){
          return field.getObjAttribute().getName();
        } else if (calcType.equals("lookup") &&
                   field.getObjRelationship() != null) {
          return field.getObjRelationship().getName();
        } else
          return "";
      }
      case 4:{
        String calcType = field.getCalcType();
        if (calcType.equals("lookup")){
          return field.getLookup().toString();
        }else{
          return "";
        }
      }

      default: return null;
    }
  }
}
