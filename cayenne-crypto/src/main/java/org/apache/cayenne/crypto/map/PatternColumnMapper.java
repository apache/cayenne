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
package org.apache.cayenne.crypto.map;

import java.util.regex.Pattern;

import org.apache.cayenne.map.DbAttribute;

/**
 * A {@link ColumnMapper} that decides on whether a column is encrypted by
 * matching its name against a preset pattern. Only column name is inspected.
 * Table name is ignored.
 * 
 * @since 3.2
 */
public class PatternColumnMapper implements ColumnMapper {

    private Pattern columnNamePattern;

    public PatternColumnMapper(String columnNamePattern) {
        this.columnNamePattern = Pattern.compile(columnNamePattern);
    }

    @Override
    public boolean isEncrypted(DbAttribute column) {
        return columnNamePattern.matcher(column.getName()).find();
    }

}
