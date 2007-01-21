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

package org.apache.cayenne.modeler.editor.dbentity;

import java.util.ArrayList;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DerivedDbAttribute;
import org.apache.cayenne.map.DerivedDbEntity;
import org.apache.cayenne.modeler.ProjectController;

/**
 * @author Andrus Adamchik
 */
public class DerivedAttributeParamsTableModel extends DbAttributeTableModel {
	private static final int DB_ATTRIBUTE_NAME = 0;
	private static final int DB_ATTRIBUTE_TYPE = 1;

	protected DerivedDbAttribute derived;

	/**
	 * Constructor for DerivedAttributeParamsTableModel.
	 */
	public DerivedAttributeParamsTableModel(
		DerivedDbAttribute derived,
		ProjectController mediator,
		Object eventSource) {

		super(
			((DerivedDbEntity) derived.getEntity()).getParentEntity(),
			mediator,
			eventSource,
			new ArrayList(derived.getParams()));
		this.derived = derived;
	}

    /**
     * Returns <code>null</code> to disable ordering.
     */
	public String getOrderingKey() {
		return null;
	}
	
    public DbEntity getParentEntity() {
    	return ((DerivedDbEntity) derived.getEntity()).getParentEntity();
    }
    
	/**
	 * @see javax.swing.table.TableModel#getColumnCount()
	 */
	public int getColumnCount() {
		return 2;
	}

	public String getColumnName(int col) {
		switch(col) {
			case DB_ATTRIBUTE_NAME: return "Name";
			case DB_ATTRIBUTE_TYPE: return "Type";
			default: return "";
		}
	}
	
	public Object getValueAt(int row, int column) {
		DbAttribute attr = getAttribute(row);

		if (attr == null) {
			return "";
		}

		switch (column) {
			case DB_ATTRIBUTE_NAME :
				return getAttributeName(attr);
			case DB_ATTRIBUTE_TYPE :
				return getAttributeType(attr);
			default :
				return "";
		}
	}

	public void setValueAt(Object newVal, int row, int col) {
		if (col == nameColumnInd()) {
			replaceParameter(row, (String)newVal);
		}
	}

	/** Replaces parameter at index with the new attribute. */
	protected void replaceParameter(int ind, String attrName) {
		if (attrName != null) {
			objectList.set(ind, getParentEntity().getAttribute(attrName));
			fireTableDataChanged();
		}
	}

	public boolean isCellEditable(int row, int col) {
		return col == DB_ATTRIBUTE_NAME;
	}
}
