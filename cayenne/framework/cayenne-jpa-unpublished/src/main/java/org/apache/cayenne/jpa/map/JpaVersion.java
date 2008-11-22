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
package org.apache.cayenne.jpa.map;

import java.sql.Types;

import javax.persistence.TemporalType;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.util.TreeNodeChild;
import org.apache.cayenne.util.XMLEncoder;

public class JpaVersion extends JpaAttribute {

    protected JpaColumn column;
    protected TemporalType temporal;

    @TreeNodeChild
    public JpaColumn getColumn() {
        return column;
    }

    @Override
    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<version");
        if (name != null) {
            encoder.print(" name=\"" + name + "\"");
        }
        encoder.println('>');
        encoder.indent(1);

        if (column != null) {
            column.encodeAsXML(encoder);
        }

        if (temporal != null) {
            encoder.println("<temporal>" + temporal.name() + "</temporal>");
        }

        encoder.indent(-1);
        encoder.println("</version>");
    }

    /**
     * Returns default JDBC mapping for this basic attribute.
     */
    public int getDefaultJdbcType() {

        if (getTemporal() != null) {

            if (TemporalType.TIMESTAMP == getTemporal()) {
                return Types.TIMESTAMP;
            }
            else if (TemporalType.DATE == getTemporal()) {
                return Types.DATE;
            }
            else {
                return Types.TIME;
            }
        }
        else {
            return TypesMapping.getSqlTypeByJava(getPropertyDescriptor().getType());
        }
    }

    public void setColumn(JpaColumn column) {
        this.column = column;
    }

    public TemporalType getTemporal() {
        return temporal;
    }

    public void setTemporal(TemporalType temporal) {
        this.temporal = temporal;
    }
}
