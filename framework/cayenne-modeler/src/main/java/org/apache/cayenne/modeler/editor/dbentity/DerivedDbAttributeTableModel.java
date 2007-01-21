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

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DerivedDbAttribute;
import org.apache.cayenne.map.event.AttributeEvent;
import org.apache.cayenne.modeler.ProjectController;

/**
 * Table model for derived DbAttributes.
 * 
 * @author Andrus Adamchik
 */
public class DerivedDbAttributeTableModel extends DbAttributeTableModel {
	// Column indices
	private static final int DB_ATTRIBUTE_NAME = 0;
	private static final int DB_ATTRIBUTE_SPEC = 1;
	private static final int DB_ATTRIBUTE_TYPE = 2;
	private static final int DB_ATTRIBUTE_GROUPBY = 3;
	private static final int DB_ATTRIBUTE_PRIMARY_KEY = 4;
	private static final int DB_ATTRIBUTE_MANDATORY = 5;

	
	/**
	 * Constructor for DerivedDbAttributeTableModel.
	 * @param entity
	 * @param mediator
	 * @param eventSource
	 */
	public DerivedDbAttributeTableModel(
			DbEntity entity,
			ProjectController mediator,
			Object eventSource) {
		super(entity, mediator, eventSource);
	}

	/**
	 * @see javax.swing.table.TableModel#getColumnClass(int)
	 */
	public Class getColumnClass(int col) {
		switch (col) {
			case DB_ATTRIBUTE_PRIMARY_KEY :
			case DB_ATTRIBUTE_MANDATORY :
			case DB_ATTRIBUTE_GROUPBY:
				return Boolean.class;
			default :
				return String.class;
		}
	}

    public int mandatoryColumnInd() {
    	return DB_ATTRIBUTE_MANDATORY;
    }
    
    public int nameColumnInd() {
    	return DB_ATTRIBUTE_NAME;
    }
    
    public int typeColumnInd() {
    	return DB_ATTRIBUTE_TYPE;
    }

	/**
	 * @see javax.swing.table.TableModel#getColumnCount()
	 */
	public int getColumnCount() {
		return 6;
	}


	/**
	 * @see javax.swing.table.TableModel#getColumnName(int)
	 */
	public String getColumnName(int col) {
		switch(col) {
			case DB_ATTRIBUTE_NAME: return "Name";
			case DB_ATTRIBUTE_SPEC: return "Spec";
			case DB_ATTRIBUTE_TYPE: return "Type";
			case DB_ATTRIBUTE_GROUPBY: return "Group By";
			case DB_ATTRIBUTE_PRIMARY_KEY: return "PK";
			case DB_ATTRIBUTE_MANDATORY: return "Mandatory";
			default: return "";
		}
	}

	public Object getValueAt(int row, int column) {
		DerivedDbAttribute attr = (DerivedDbAttribute)getAttribute(row);

		if (attr == null) {
			return "";
		}

		switch (column) {
			case DB_ATTRIBUTE_NAME :
				return getAttributeName(attr);
			case DB_ATTRIBUTE_SPEC :
				return getSpec(attr);
			case DB_ATTRIBUTE_TYPE :
				return getAttributeType(attr);
			case DB_ATTRIBUTE_GROUPBY :
				return isGroupBy(attr);
			case DB_ATTRIBUTE_PRIMARY_KEY :
				return isPrimaryKey(attr);
			case DB_ATTRIBUTE_MANDATORY :
				return isMandatory(attr);
			default :
				return "";
		}
	}
	
	public void setUpdatedValueAt(Object newVal, int row, int col) {
		DerivedDbAttribute attr = (DerivedDbAttribute)getAttribute(row);
		if (attr == null) {
			return;
		}

		AttributeEvent e = new AttributeEvent(eventSource, attr, entity);

		switch (col) {
			case DB_ATTRIBUTE_NAME :
				e.setOldName(attr.getName());
				setAttributeName((String) newVal, attr);
				fireTableCellUpdated(row, col);
				break;
			case DB_ATTRIBUTE_SPEC :
				setSpec((String) newVal, attr);
				break;
			case DB_ATTRIBUTE_TYPE :
				setAttributeType((String) newVal, attr);
				break;
			case DB_ATTRIBUTE_GROUPBY :
				setGroupBy((Boolean) newVal, attr);
				break;
			case DB_ATTRIBUTE_PRIMARY_KEY :
				if(!setPrimaryKey((Boolean) newVal, attr, row)) {
					return;
				}

				break;
			case DB_ATTRIBUTE_MANDATORY :
				setMandatory((Boolean) newVal, attr);
				break;
		}

		mediator.fireDbAttributeEvent(e);
	}


	
	public String getSpec(DerivedDbAttribute attr) {
		return attr.getExpressionSpec();
	}

	public void setSpec(String newVal, DerivedDbAttribute attr) {
		attr.setExpressionSpec(newVal);
	}
	
	public Boolean isGroupBy(DerivedDbAttribute attr) {
		return attr.isGroupBy() ? Boolean.TRUE : Boolean.FALSE;
	}
	
	public void setGroupBy(Boolean newVal, DerivedDbAttribute attr) {
		attr.setGroupBy(newVal.booleanValue());
	}
	
	
	/**
	 * @see org.apache.cayenne.modeler.util.CayenneTableModel#getElementsClass()
	 */
	public Class getElementsClass() {
		return DerivedDbAttribute.class;
	}
}

