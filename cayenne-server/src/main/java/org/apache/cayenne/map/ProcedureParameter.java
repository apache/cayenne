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

package org.apache.cayenne.map;

import java.io.Serializable;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

/**
 * A descriptor for the Procedure parameter.
 */
public class ProcedureParameter implements ConfigurationNode, CayenneMapEntry,
        XMLSerializable, Serializable {

    public static final int IN_OUT_PARAMETER = 3;
    public static final int IN_PARAMETER = 1;
    public static final int OUT_PARAMETER = 2;

    protected String name;
    protected Procedure procedure;

    protected int direction = -1;

    // The length of CHAR or VARCHAR or max num of digits for DECIMAL.
    protected int maxLength = -1;

    // The number of digits after period for DECIMAL.
    protected int precision = -1;
    protected int type = TypesMapping.NOT_DEFINED;

    /**
     * Creates unnamed ProcedureParameter.
     */
    public ProcedureParameter() {
    }

    public ProcedureParameter(String name) {
        setName(name);
    }

    public ProcedureParameter(String name, int type, int direction) {
        this(name);
        setType(type);
        setDirection(direction);
    }
    
    public <T> T acceptVisitor(ConfigurationNodeVisitor<T> visitor) {
        return visitor.visitProcedureParameter(this);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getParent() {
        return getProcedure();
    }

    public void setParent(Object parent) {
        if (parent != null && !(parent instanceof Procedure)) {
            throw new IllegalArgumentException("Expected null or Procedure, got: " + parent);
        }

        setProcedure((Procedure) parent);
    }

    /**
     * Prints itself as XML to the provided PrintWriter.
     * 
     * @since 1.1
     */
    @Override
    public void encodeAsXML(XMLEncoder encoder, ConfigurationNodeVisitor delegate) {
        encoder.start("procedure-parameter")
                .attribute("name", getName())
                .attribute("type", TypesMapping.getSqlNameByType(getType()))
                .attribute("length", getMaxLength() > 0 ? getMaxLength() : 0)
                .attribute("precision", getPrecision() > 0 ? getPrecision() : 0);

        int direction = getDirection();
        if (direction == ProcedureParameter.IN_PARAMETER) {
            encoder.attribute("direction", "in");
        } else if (direction == ProcedureParameter.IN_OUT_PARAMETER) {
            encoder.attribute("direction", "in_out");
        } else if (direction == ProcedureParameter.OUT_PARAMETER) {
            encoder.attribute("direction", "out");
        }

        delegate.visitProcedureParameter(this);
        encoder.end();
    }

    /**
     * Returns the direction of this parameter. Possible values can be IN_PARAMETER,
     * OUT_PARAMETER, IN_OUT_PARAMETER, VOID_PARAMETER.
     */
    public int getDirection() {
        return direction;
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
     * Sets the direction of this parameter. Acceptable values of direction are defined as
     * int constants in ProcedureParam class. If an attempt is made to set an invalid
     * attribute's direction, an IllegalArgumentException is thrown by this method.
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

    /**
     * Returns the procedure that holds this parameter.
     */
    public Procedure getProcedure() {
        return procedure;
    }

    /**
     * Sets the procedure that holds this parameter.
     */
    public void setProcedure(Procedure procedure) {
        this.procedure = procedure;
    }
}
