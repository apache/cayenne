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

import org.apache.cayenne.configuration.event.ProcedureParameterEvent;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.CayenneTableModel;
import org.apache.cayenne.modeler.util.ProjectUtil;

import javax.swing.JOptionPane;
import java.util.ArrayList;

public class ProcedureParameterTableModel extends CayenneTableModel<ProcedureParameter> {

    public static final int PARAMETER_NUMBER    = 0;
    public static final int PARAMETER_NAME      = 1;
    public static final int PARAMETER_DIRECTION = 2;
    public static final int PARAMETER_TYPE      = 3;
    public static final int PARAMETER_LENGTH    = 4;
    public static final int PARAMETER_PRECISION = 5;

    public static final String IN_PARAMETER     = "IN";
    public static final String OUT_PARAMETER    = "OUT";
    public static final String IN_OUT_PARAMETER = "INOUT";

    public static final String[] PARAMETER_DIRECTION_NAMES = { "", IN_PARAMETER, OUT_PARAMETER, IN_OUT_PARAMETER };

    private static final int[] PARAMETER_INDEXES = {
            PARAMETER_NUMBER,
            PARAMETER_NAME,
            PARAMETER_DIRECTION,
            PARAMETER_TYPE,
            PARAMETER_LENGTH,
            PARAMETER_PRECISION
    };

    private static final String[] PARAMETER_NAMES = { "No.", "Name", "Direction", "Type", "Max Length", "Precision" };

    protected Procedure procedure;

    public ProcedureParameterTableModel(Procedure procedure, ProjectController mediator, Object eventSource) {
        super(mediator, eventSource, new ArrayList<>(procedure.getCallParameters()));
        this.procedure = procedure;
    }

    /**
     * Returns procedure parameter at the specified row.
     * Returns NULL if row index is outside the valid range.
     */
    public ProcedureParameter getParameter(int row) {
        return (row >= 0 && row < objectList.size()) ? objectList.get(row) : null;
    }

    @Override
    public void setUpdatedValueAt(Object newVal, int rowIndex, int columnIndex) {
        ProcedureParameter parameter = getParameter(rowIndex);
        if (parameter == null) {
            return;
        }

        String value = (String)newVal;
        ProcedureParameterEvent event = new ProcedureParameterEvent(eventSource, parameter);
        switch (columnIndex) {
            case PARAMETER_NAME :
                event.setOldName(parameter.getName());
                setParameterName(value, parameter);
                fireTableCellUpdated(rowIndex, columnIndex);
                break;
            case PARAMETER_DIRECTION :
                setParameterDirection(value, parameter);
                break;
            case PARAMETER_TYPE :
                setParameterType(value, parameter);
                break;
            case PARAMETER_LENGTH :
                setMaxLength(value, parameter);
                break;
            case PARAMETER_PRECISION :
                setPrecision(value, parameter);
                break;
        }
        mediator.fireProcedureParameterEvent(event);
    }

    protected void setPrecision(String newVal, ProcedureParameter parameter) {
        if (newVal == null || newVal.trim().length() <= 0) {
            parameter.setPrecision(-1);
        } else {
            try {
                parameter.setPrecision(Integer.parseInt(newVal));
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                    null,
                    "Invalid precision (" + newVal + "), only numbers are allowed.",
                    "Invalid Precision Value",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    protected void setMaxLength(String newVal, ProcedureParameter parameter) {
        if (newVal == null || newVal.trim().length() <= 0) {
            parameter.setMaxLength(-1);
        } else {
            try {
                parameter.setMaxLength(Integer.parseInt(newVal));
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                        null,
                        "Invalid Max Length (" + newVal + "), only numbers are allowed",
                        "Invalid Maximum Length",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    protected void setParameterType(String newVal, ProcedureParameter parameter) {
        parameter.setType(TypesMapping.getSqlTypeByName(newVal));
    }

    protected void setParameterDirection(String direction, ProcedureParameter parameter) {
        if (ProcedureParameterTableModel.IN_PARAMETER.equals(direction)) {
            parameter.setDirection(ProcedureParameter.IN_PARAMETER);
        } else if (ProcedureParameterTableModel.OUT_PARAMETER.equals(direction)) {
            parameter.setDirection(ProcedureParameter.OUT_PARAMETER);
        } else if (ProcedureParameterTableModel.IN_OUT_PARAMETER.equals(direction)) {
            parameter.setDirection(ProcedureParameter.IN_OUT_PARAMETER);
        }
    }

    protected void setParameterName(String newVal, ProcedureParameter parameter) {
        ProjectUtil.setProcedureParameterName(parameter, newVal.trim());
    }

    @Override
    public Class getElementsClass() {
        return ProcedureParameter.class;
    }

    @Override
    public int getColumnCount() {
        return PARAMETER_INDEXES.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ProcedureParameter parameter = getParameter(rowIndex);

        if (parameter == null) {
            return "";
        }

        switch (columnIndex) {
            case PARAMETER_NUMBER :
                return getParameterNumber(rowIndex, parameter);
            case PARAMETER_NAME :
                return getParameterName(parameter);
            case PARAMETER_DIRECTION :
                return getParameterDirection(parameter);
            case PARAMETER_TYPE :
                return getParameterType(parameter);
            case PARAMETER_LENGTH :
                return getParameterLength(parameter);
            case PARAMETER_PRECISION :
                return getParameterPrecision(parameter);
            default :
                return "";
        }
    }

    protected String getParameterNumber(int rowIndex, ProcedureParameter parameter) {
        boolean hasReturnValue = parameter.getProcedure().isReturningValue();

        if (hasReturnValue) {
            return (rowIndex == 0) ? "R" : "" + rowIndex;
        } else {
            return "" + (rowIndex + 1);
        }
    }

    protected String getParameterPrecision(ProcedureParameter parameter) {
        return (parameter.getPrecision() >= 0)
            ? String.valueOf(parameter.getPrecision())
            : "";
    }

    protected String getParameterLength(ProcedureParameter parameter) {
        return (parameter.getMaxLength() >= 0)
            ? String.valueOf(parameter.getMaxLength())
            : "";
    }

    protected String getParameterType(ProcedureParameter parameter) {
        return TypesMapping.getSqlNameByType(parameter.getType());
    }

    protected String getParameterDirection(ProcedureParameter parameter) {
        int direction = parameter.getDirection();
        return PARAMETER_DIRECTION_NAMES[direction == -1 ? 0 : direction];
    }

    protected String getParameterName(ProcedureParameter parameter) {
        return parameter.getName();
    }

    @Override
    public String getColumnName(int col) {
        return PARAMETER_NAMES[col];
    }

    @Override
    public Class<?> getColumnClass(int col) {
        return String.class;
    }

    /**
     * Suppressed ordering operations defined in a superclass.
     * Since stored procedure parameters are positional,
     * no reordering is allowed.
     */
    public void orderList() {
        // NOOP
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return col != PARAMETER_NUMBER;
    }

    @Override
    public boolean isColumnSortable(int sortCol) {
        return false;
    }

    @Override
    public void sortByColumn(int sortCol, boolean isAscent) {
    }
}
