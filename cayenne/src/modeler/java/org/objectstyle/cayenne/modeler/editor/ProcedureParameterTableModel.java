/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.modeler.editor;

import java.util.ArrayList;

import javax.swing.JOptionPane;

import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.map.ProcedureParameter;
import org.objectstyle.cayenne.map.event.ProcedureParameterEvent;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.util.CayenneTableModel;
import org.objectstyle.cayenne.modeler.util.ProjectUtil;

/**
 * @author Andrei Adamchik
 */
public class ProcedureParameterTableModel extends CayenneTableModel {
    public static final int PARAMETER_NUMBER = 0;
    public static final int PARAMETER_NAME = 1;
    public static final int PARAMETER_DIRECTION = 2;
    public static final int PARAMETER_TYPE = 3;
    public static final int PARAMETER_LENGTH = 4;
    public static final int PARAMETER_PRECISION = 5;

    public static final String IN_PARAMETER = "IN";
    public static final String OUT_PARAMETER = "OUT";
    public static final String IN_OUT_PARAMETER = "INOUT";

    public static final String[] PARAMETER_DIRECTION_NAMES =
        new String[] { "", IN_PARAMETER, OUT_PARAMETER, IN_OUT_PARAMETER };

    private static final int[] PARAMETER_INDEXES =
        new int[] {
            PARAMETER_NUMBER,
            PARAMETER_NAME,
            PARAMETER_DIRECTION,
            PARAMETER_TYPE,
            PARAMETER_LENGTH,
            PARAMETER_PRECISION };

    private static final String[] PARAMETER_NAMES =
        new String[] { "No.", "Name", "Direction", "Type", "Max Length", "Precision" };

    protected Procedure procedure;

    public ProcedureParameterTableModel(
        Procedure procedure,
        ProjectController mediator,
        Object eventSource) {

        super(mediator, eventSource, new ArrayList(procedure.getCallParameters()));
        this.procedure = procedure;
    }

    /**
     * Returns procedure parameter at the specified row.
     * Returns NULL if row index is outside the valid range.
     */
    public ProcedureParameter getParameter(int row) {
        return (row >= 0 && row < objectList.size())
            ? (ProcedureParameter) objectList.get(row)
            : null;
    }

    public void setUpdatedValueAt(Object newVal, int rowIndex, int columnIndex) {
        ProcedureParameter parameter = getParameter(rowIndex);

        if (parameter == null) {
            return;
        }

        ProcedureParameterEvent event =
            new ProcedureParameterEvent(eventSource, parameter);
        switch (columnIndex) {
            case PARAMETER_NAME :
                event.setOldName(parameter.getName());
                setParameterName((String) newVal, parameter);
                fireTableCellUpdated(rowIndex, columnIndex);
                break;
            case PARAMETER_DIRECTION :
                setParameterDirection((String) newVal, parameter);
                break;
            case PARAMETER_TYPE :
                setParameterType((String) newVal, parameter);
                break;
            case PARAMETER_LENGTH :
                setMaxLength((String) newVal, parameter);
                break;
            case PARAMETER_PRECISION :
                setPrecision((String) newVal, parameter);
                break;
        }
        mediator.fireProcedureParameterEvent(event);
    }

    protected void setPrecision(String newVal, ProcedureParameter parameter) {
        if (newVal == null || newVal.trim().length() <= 0) {
            parameter.setPrecision(-1);
        }
        else {
            try {
                parameter.setPrecision(Integer.parseInt(newVal));
            }
            catch (NumberFormatException ex) {
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
        }
        else {
            try {
                parameter.setMaxLength(Integer.parseInt(newVal));
            }
            catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                    null,
                    "Invalid Max Length (" + newVal + "), only numbers are allowed",
                    "Invalid Maximum Length",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
    }

    protected void setParameterType(String newVal, ProcedureParameter parameter) {
        parameter.setType(TypesMapping.getSqlTypeByName(newVal));
    }

    protected void setParameterDirection(
        String direction,
        ProcedureParameter parameter) {
        if (ProcedureParameterTableModel.IN_PARAMETER.equals(direction)) {
            parameter.setDirection(ProcedureParameter.IN_PARAMETER);
        }
        else if (ProcedureParameterTableModel.OUT_PARAMETER.equals(direction)) {
            parameter.setDirection(ProcedureParameter.OUT_PARAMETER);
        }
        else if (ProcedureParameterTableModel.IN_OUT_PARAMETER.equals(direction)) {
            parameter.setDirection(ProcedureParameter.IN_OUT_PARAMETER);
        }
        else {
            parameter.setDirection(-1);
        }
    }

    protected void setParameterName(String newVal, ProcedureParameter parameter) {
        ProjectUtil.setProcedureParameterName(parameter, newVal.trim());
    }

    public Class getElementsClass() {
        return ProcedureParameter.class;
    }

    public int getColumnCount() {
        return PARAMETER_INDEXES.length;
    }

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

    protected Object getParameterNumber(int rowIndex, ProcedureParameter parameter) {
        boolean hasReturnValue = parameter.getProcedure().isReturningValue();

        if (hasReturnValue) {
            return (rowIndex == 0) ? "R" : "" + rowIndex;
        }
        else {
            return "" + (rowIndex + 1);
        }
    }

    protected Object getParameterPrecision(ProcedureParameter parameter) {
        return (parameter.getPrecision() >= 0)
            ? String.valueOf(parameter.getPrecision())
            : "";
    }

    protected Object getParameterLength(ProcedureParameter parameter) {
        return (parameter.getMaxLength() >= 0)
            ? String.valueOf(parameter.getMaxLength())
            : "";
    }

    protected Object getParameterType(ProcedureParameter parameter) {
        return TypesMapping.getSqlNameByType(parameter.getType());
    }

    protected Object getParameterDirection(ProcedureParameter parameter) {
        int direction = parameter.getDirection();
        switch (direction) {
            case ProcedureParameter.IN_PARAMETER :
                return ProcedureParameterTableModel.IN_PARAMETER;
            case ProcedureParameter.OUT_PARAMETER :
                return ProcedureParameterTableModel.OUT_PARAMETER;
            case ProcedureParameter.IN_OUT_PARAMETER :
                return ProcedureParameterTableModel.IN_OUT_PARAMETER;
            default :
                return "";
        }
    }

    protected Object getParameterName(ProcedureParameter parameter) {
        return parameter.getName();
    }

    public String getColumnName(int col) {
        return PARAMETER_NAMES[col];
    }

    public Class getColumnClass(int col) {
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

    public boolean isCellEditable(int row, int col) {
        return col != PARAMETER_NUMBER;
    }
}
