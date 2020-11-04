/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */

package org.apache.cayenne.access.types;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;

import org.apache.cayenne.value.Json;

/**
 * @since 4.2
 */
public class JsonType implements ExtendedType<Json> {

    private final CharType delegate;
    private final boolean useRealType;

    public JsonType(CharType delegate, boolean useRealType) {
        this.delegate = delegate;
        this.useRealType = useRealType;
    }

    @Override
    public String getClassName() {
        return Json.class.getName();
    }

    @Override
    public void setJdbcObject(PreparedStatement statement, Json json, int pos, int type, int scale) throws Exception {
        String value = json != null ? json.getRawJson() : null;
        delegate.setJdbcObject(statement, value, pos, useRealType ? type : Types.OTHER, scale);
    }

    @Override
    public Json materializeObject(ResultSet rs, int index, int type) throws Exception {
        String value = delegate.materializeObject(rs, index, useRealType ? type : Types.OTHER);
        return value != null ? new Json(value) : null;
    }

    @Override
    public Json materializeObject(CallableStatement rs, int index, int type) throws Exception {
        String value = delegate.materializeObject(rs, index, useRealType ? type : Types.OTHER);
        return value != null ? new Json(value) : null;
    }

    @Override
    public String toString(Json value) {
        return value != null ? value.getRawJson() : null;
    }
}