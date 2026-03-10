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

import java.util.Map;

import org.apache.cayenne.query.EntityResultSegment;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * @since 3.0
 */
public class DefaultEntityResultSegment implements EntityResultSegment {

    private ClassDescriptor classDescriptor;
    private Map<String, String> fields;
    private int offset;
    private int columnCount;

    public DefaultEntityResultSegment(ClassDescriptor classDescriptor,
            Map<String, String> fields, int offset) {
        this(classDescriptor, fields, offset, fields != null ? fields.size() : 0);
    }

    public DefaultEntityResultSegment(ClassDescriptor classDescriptor,
            Map<String, String> fields, int offset, int columnCount) {
        this.classDescriptor = classDescriptor;
        this.fields = fields;
        this.offset = offset;
        this.columnCount = columnCount;
    }

    public ClassDescriptor getClassDescriptor() {
        return classDescriptor;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    @Override
    public int getColumnCount() {
        return columnCount;
    }

    public int getColumnOffset() {
        return offset;
    }

    public String getColumnPath(String resultSetLabel) {

        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (resultSetLabel.equals(entry.getValue())) {
                return entry.getKey();
            }
        }

        return null;
    }
}
