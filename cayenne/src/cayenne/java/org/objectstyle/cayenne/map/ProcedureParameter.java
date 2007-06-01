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
package org.objectstyle.cayenne.map;

import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.util.Util;
import org.objectstyle.cayenne.util.XMLEncoder;
import org.objectstyle.cayenne.util.XMLSerializable;

/**
 * A descriptor for the StoredProcedure parameter.
 * 
 * @author Andrei Adamchik
 */
public class ProcedureParameter extends MapObject implements XMLSerializable {
    public static final int IN_OUT_PARAMETER = 3;
    public static final int IN_PARAMETER = 1;
    public static final int OUT_PARAMETER = 2;

    protected int direction = -1;

    // The length of CHAR or VARCHAR or max num of digits for DECIMAL.
    protected int maxLength = -1;

    // The number of digits after period for DECIMAL.
    protected int precision = -1;
    protected int type = TypesMapping.NOT_DEFINED;

    /**
     * Constructor for ProcedureParam.
     */
    public ProcedureParameter() {
        super();
    }

    public ProcedureParameter(String name) {
        super(name);
    }

    public ProcedureParameter(String name, int type, int direction) {
        super(name);
        setType(type);
        setDirection(direction);
    }

    /**
     * Prints itself as XML to the provided PrintWriter.
     * 
     * @since 1.1
     */
    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<procedure-parameter name=\"" + Util.encodeXmlAttribute(getName()) + '\"');

        String type = TypesMapping.getSqlNameByType(getType());
        if (type != null) {
            encoder.print(" type=\"" + type + '\"');
        }

        if (getMaxLength() > 0) {
            encoder.print(" length=\"");
            encoder.print(getMaxLength());
            encoder.print('\"');
        }

        if (getPrecision() > 0) {
            encoder.print(" precision=\"");
            encoder.print(getPrecision());
            encoder.print('\"');
        }

        int direction = getDirection();
        if (direction == ProcedureParameter.IN_PARAMETER) {
            encoder.print(" direction=\"in\"");
        }
        else if (direction == ProcedureParameter.IN_OUT_PARAMETER) {
            encoder.print(" direction=\"in_out\"");
        }
        else if (direction == ProcedureParameter.OUT_PARAMETER) {
            encoder.print(" direction=\"out\"");
        }

        encoder.println("/>");
    }

    /**
     * Returns the direction of this parameter. Possible values 
     * can be IN_PARAMETER, OUT_PARAMETER, IN_OUT_PARAMETER, VOID_PARAMETER.
     */
    public int getDirection() {
        return direction;
    }

    /** Returns the procedure that holds this parameter. */
    public Procedure getEntity() {
        return (Procedure) getParent();
    }

    public int getMaxLength() {
        return maxLength;
    }

    public int getPrecision() {
        return precision;
    }

    public int getType() {
        return type;
    }

    /**
     * @return <code>true</code> if this is IN or INOUT parameter.
     */
    public boolean isInParameter() {
        return direction == IN_PARAMETER || direction == IN_OUT_PARAMETER;
    }

    /**
     * @return <code>true</code> if this is OUT or INOUT parameter.
     */
    public boolean isOutParam() {
        return direction == OUT_PARAMETER || direction == IN_OUT_PARAMETER;
    }

    /**
     * Sets the direction of this parameter. Acceptable values of direction are
     * defined as int constants in ProcedureParam class. If an attempt is
     * made to set an invalid attribute's direction, an IllegalArgumentException
     * is thrown by this method.
     */
    public void setDirection(int direction) {
        if (direction != IN_PARAMETER
            && direction != OUT_PARAMETER
            && direction != IN_OUT_PARAMETER) {
            throw new IllegalArgumentException("Unknown parameter type: " + direction);
        }

        this.direction = direction;
    }

    public void setMaxLength(int i) {
        maxLength = i;
    }

    public void setPrecision(int i) {
        precision = i;
    }

    public void setType(int i) {
        type = i;
    }

    /** Returns the procedure that holds this parameter. */
    public Procedure getProcedure() {
        return (Procedure) getParent();
    }

    /** Sets the procedure that holds this parameter. */
    public void setProcedure(Procedure procedure) {
        setParent(procedure);
    }
}
