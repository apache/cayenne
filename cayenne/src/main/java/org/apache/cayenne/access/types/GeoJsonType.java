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

import org.apache.cayenne.value.GeoJson;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;

/**
 * @since 4.2
 */
public class GeoJsonType implements ExtendedType<GeoJson> {

    private CharType delegate;

    public GeoJsonType() {
        this.delegate = new CharType(true, false);
    }

    @Override
    public String getClassName() {
        return GeoJson.class.getName();
    }

    @Override
    public void setJdbcObject(PreparedStatement statement, GeoJson geometry, int pos, int type, int scale) throws Exception {
        String value = geometry != null ? geometry.getGeometry() : null;
        delegate.setJdbcObject(statement, value, pos, Types.VARCHAR, scale);
    }

    @Override
    public GeoJson materializeObject(ResultSet rs, int index, int type) throws Exception {
        String value = delegate.materializeObject(rs, index, Types.VARCHAR);
        return value != null ? new GeoJson(value) : null;
    }

    @Override
    public GeoJson materializeObject(CallableStatement rs, int index, int type) throws Exception {
        String value = delegate.materializeObject(rs, index, Types.VARCHAR);
        return value != null ? new GeoJson(value) : null;
    }

    @Override
    public String toString(GeoJson value) {
        return value != null ? value.getGeometry() : null;
    }
}