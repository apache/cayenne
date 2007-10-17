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

import javax.persistence.Basic;
import javax.persistence.EnumType;
import javax.persistence.FetchType;
import javax.persistence.TemporalType;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.util.TreeNodeChild;
import org.apache.cayenne.util.XMLEncoder;

public class JpaBasic extends JpaAttribute {

    protected FetchType fetch = FetchType.EAGER;
    protected boolean optional;
    protected JpaColumn column;
    protected boolean lob;
    protected TemporalType temporal;
    protected EnumType enumerated;

    public JpaBasic() {

    }

    public JpaBasic(Basic basic) {
        this.fetch = basic.fetch();
        this.optional = basic.optional();
    }

    @Override
    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<basic");
        if (name != null) {
            encoder.print(" name=\"" + name + "\"");
        }

        if (fetch != null) {
            encoder.print(" fetch=\"" + fetch.name() + "\"");
        }

        encoder.print(" optional=\"" + optional + "\"");

        if (!lob && temporal == null && enumerated == null && column == null) {
            encoder.println("/>");
        }
        else {

            encoder.println('>');
            encoder.indent(1);
            
            if(column != null) {
                column.encodeAsXML(encoder);
            }

            if (lob) {
                encoder.println("<lob/>");
            }

            if (temporal != null) {
                encoder.println("<temporal>" + temporal.name() + "</temporal>");
            }

            if (enumerated != null) {
                encoder.println("<enumerated>" + enumerated.name() + "</enumerated>");
            }

            encoder.indent(-1);
            encoder.println("</basic>");
        }
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
        else if (getEnumerated() != null) {
            return (getEnumerated() == EnumType.ORDINAL) ? Types.INTEGER : Types.VARCHAR;
        }
        else {
            return TypesMapping.getSqlTypeByJava(getPropertyDescriptor().getType());
        }
    }

    public FetchType getFetch() {
        return fetch;
    }

    public void setFetch(FetchType fetchType) {
        this.fetch = fetchType;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    @TreeNodeChild
    public JpaColumn getColumn() {
        return column;
    }

    public void setColumn(JpaColumn column) {
        this.column = column;
    }

    public EnumType getEnumerated() {
        return enumerated;
    }

    public void setEnumerated(EnumType enumerated) {
        this.enumerated = enumerated;
    }

    public boolean isLob() {
        return lob;
    }

    public void setLob(boolean lob) {
        this.lob = lob;
    }

    public void setLobTrue(Object value) {
        setLob(true);
    }

    public TemporalType getTemporal() {
        return temporal;
    }

    public void setTemporal(TemporalType temporal) {
        this.temporal = temporal;
    }
}
