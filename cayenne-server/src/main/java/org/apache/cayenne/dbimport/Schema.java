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
package org.apache.cayenne.dbimport;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * @since 4.0.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Schema extends FilterContainer {

    @XmlAttribute(name = "name")
    private String name;

    public Schema() {
    }

    public Schema(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void set(String name) {
        setName(name);
    }

    public void addConfiguredName(AntNestedElement name) {
        setName(name.getName());
    }

    public void addText(String name) {
        if (name.trim().isEmpty()) {
            return;
        }

        setName(name);
    }

    @Override
    public StringBuilder toString(StringBuilder res, String prefix) {
        res.append(prefix).append("Schema: ").append(name).append("\n");

        return super.toString(res, prefix + "  ");
    }
}
