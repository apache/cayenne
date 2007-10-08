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
 * This class defines table model for ObjEntity's attributes.
 *
 * @author Nataliya Kholodna
 * @version 1.0
 */

public class AttributesTableModel extends AbstractTableModel {
  private DVObjEntity objEntity;
  private String[] columnNames= {"Name", "Type"};


  public AttributesTableModel() {
  }
  /*Sets another objEntity*/
  public void setObjEntity(DVObjEntity objEntity){
    this.objEntity = objEntity;
    fireTableStructureChanged();
  }

  /*Reterns row count*/
  public int getRowCount(){
    if (objEntity != null){
      return objEntity.getObjAttributeCount();
    }else{
      return 0;
    }
  }

  /*Returns colomn count*/
  public int getColumnCount(){
      return columnNames.length;
  }

  /*Returns name of colomn whith index col*/
  public String getColumnName(int col) {
    return columnNames[col];
  }

  /*Returns colomn c values class*/
  public Class getColumnClass(int c) {
    return getValueAt(0, c).getClass();
  }
  /*Returns the value for the cell at column - index and row -index. */
  public Object getValueAt(int row, int column){
    DVObjAttribute attribute = objEntity.getObjAttribute(row);
    switch (column){
      case 0:
        return attribute.getName();
      case 1:
        return attribute.getType();
      default: return null;
    }
  }
}

