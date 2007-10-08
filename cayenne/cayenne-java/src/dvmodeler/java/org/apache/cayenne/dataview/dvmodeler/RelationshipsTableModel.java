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

import java.util.*;
import javax.swing.table.*;

/**
 *
 * @author Nataliya Kholodna
 * @version 1.0
 */

public class RelationshipsTableModel extends AbstractTableModel {
  private List objRelationships = new ArrayList();
  private String[] columnNames= {"Name",
                                 "Source",
                                 "Target",
                                 "To Many"};


  public RelationshipsTableModel() {
  }

  /*Setting another relationships in model*/
  public void setObjRelationships(List relationships){
    objRelationships = relationships;
    fireTableStructureChanged();
  }

  /*Returns the number of rows in the model. */
  public int getRowCount(){
    return objRelationships.size();
  }

  /*Returns the number of columns in the model. */
  public int getColumnCount(){
      return columnNames.length;
  }

  /*Returns a default name for the column */
  public String getColumnName(int col) {
    return columnNames[col];
  }

  /*Returns Object.class regardless of columnIndex.*/
  public Class getColumnClass(int c) {
    return getValueAt(0, c).getClass();
  }

  /*Returns the value for the cell at column index and row index. */
  public Object getValueAt(int row, int column){
    DVObjRelationship relationship = (DVObjRelationship)objRelationships.get(row);
    switch (column){
      case 0:
        return relationship.getName();
      case 1:
        return relationship.getSourceObjEntity();
      case 2:
        return relationship.getTargetObjEntity();
      case 3:
        return Boolean.valueOf(relationship.isToMany());

      default: return null;
    }

  }
}
