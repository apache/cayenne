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

import org.apache.cayenne.util.EqualsBuilder;
import org.apache.cayenne.util.HashCodeBuilder;

import javax.xml.bind.annotation.*;
import java.util.Collection;
import java.util.LinkedList;

/**
 * @since 4.0.
 */
@XmlRootElement(name = "typeMapper")
@XmlAccessorType(XmlAccessType.FIELD)
public class TypeMapper {

    @XmlElement(name = "mapperClassName")
    private String mapperClassName;

    @XmlElement(name = "usePrimitives")
    private Boolean usePrimitives;

    @XmlElement(name = "type")
    private Collection<Type> types = new LinkedList<Type>();

    public String getMapperClassName() {
        return mapperClassName;
    }

    public void setMapperClassName(String mapperClassName) {
        this.mapperClassName = mapperClassName;
    }

    public Boolean getUsePrimitives() {
        return usePrimitives;
    }

    public void setUsePrimitives(Boolean usePrimitives) {
        this.usePrimitives = usePrimitives;
    }

    public Collection<Type> getTypes() {
        return types;
    }

    public void setTypes(Collection<Type> types) {
        this.types = types;
    }

    public void addType(Type type) {
        this.types.add(type);
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        TypeMapper rhs = (TypeMapper) obj;
        return new EqualsBuilder()
                .append(this.mapperClassName, rhs.mapperClassName)
                .append(this.usePrimitives, rhs.usePrimitives)
                .append(this.types, rhs.types)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(mapperClassName).append(usePrimitives).append(types).toHashCode();
    }

    @Override
    public String toString() {
        return "TypeMapper {mapperClassName=" + mapperClassName + ", usePrimitives=" + usePrimitives + ", types=" + types + '}';
    }
}
