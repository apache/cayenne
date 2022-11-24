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

package org.apache.cayenne.gen;

import java.util.Objects;

/**
 * @since 5.0
 */
public class CgenTemplate {
    private final String data;
    private final boolean isFile;
    private final TemplateType type;

    public CgenTemplate(String data, boolean isFile, TemplateType type) {
        this.data = Objects.requireNonNull(data);
        this.isFile = isFile;
        this.type = Objects.requireNonNull(type);
    }

    public String getData() {
        return data;
    }

    public boolean isFile() {
        return isFile;
    }

    public String getName() {
        if (isFile) {
            return getData();
        } else {
            return type.name();
        }
    }

    public TemplateType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CgenTemplate that = (CgenTemplate) o;
        return isFile == that.isFile && type == that.type && data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data, isFile, type);
    }
}
