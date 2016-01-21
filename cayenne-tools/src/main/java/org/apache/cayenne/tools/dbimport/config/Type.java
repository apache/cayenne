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
package org.apache.cayenne.tools.dbimport.config;

import org.apache.cayenne.util.ToStringBuilder;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 * @since 4.0.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Type {

    @XmlElement(name = "jdbc")
    private String jdbc;

    @XmlElement(name = "java")
    private String java;

    @XmlElement(name = "length")
    private Integer length;

    @XmlElement(name = "precision")
    private Integer precision;

    @XmlElement(name = "scale")
    private Integer scale;

    @XmlElement(name = "notNull")
    private Boolean notNull;

    public String getJdbc() {
        return jdbc;
    }

    public void setJdbc(String jdbc) {
        this.jdbc = jdbc;
    }

    public String getJava() {
        return java;
    }

    public void setJava(String java) {
        this.java = java;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public Integer getPrecision() {
        return precision;
    }

    public void setPrecision(Integer precision) {
        this.precision = precision;
    }

    public Integer getScale() {
        return scale;
    }

    public void setScale(Integer scale) {
        this.scale = scale;
    }

    public Boolean getNotNull() {
        return notNull;
    }

    public void setNotNull(Boolean notNull) {
        this.notNull = notNull;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Type type = (Type) o;

        if (jdbc != null ? !jdbc.equals(type.jdbc) : type.jdbc != null) {
            return false;
        }
        if (!length.equals(type.length)) {
            return false;
        }
        if (!notNull.equals(type.notNull)) {
            return false;
        }
        if (!precision.equals(type.precision)) {
            return false;
        }
        if (!scale.equals(type.scale)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = jdbc != null ? jdbc.hashCode() : 0;
        result = 31 * result + length.hashCode();
        result = 31 * result + precision.hashCode();
        result = 31 * result + scale.hashCode();
        result = 31 * result + notNull.hashCode();
        return result;
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("jdbc", jdbc)
                .append("java", java)
                .append("length", length)
                .append("precision", precision)
                .append("scale", scale)
                .append("notNull", notNull)
                .toString();
    }
}
