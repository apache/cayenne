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
package org.apache.cayenne.query;

import java.util.Map;

import org.apache.cayenne.map.EntityResult;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * A "compiled" version of a {@link EntityResult} descriptor.
 * 
 * @since 3.0
 */
public interface EntityResultSegment {

    ClassDescriptor getClassDescriptor();

    /**
     * Returns a map of ResultSet labels keyed by column paths. Note that ordering of
     * fields in the returned map is generally undefined and should not be relied upon
     * when processing query result sets.
     */
    Map<String, String> getFields();

    /**
     * Performs a reverse lookup of the column path for a given ResultSet label.
     */
    String getColumnPath(String resultSetLabel);

    /**
     * Returns a zero-based column index of the first column of this segment in the
     * ResultSet.
     */
    int getColumnOffset();
}
