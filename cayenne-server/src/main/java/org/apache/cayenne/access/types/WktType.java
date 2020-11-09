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

import org.apache.cayenne.access.sqlbuilder.sqltree.ChildProcessor;
import org.apache.cayenne.access.sqlbuilder.sqltree.FunctionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.value.Wkt;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Optional;

/**
 * @since 4.2
 */
public class WktType implements ExtendedType<Wkt> {

    private CharType delegate;

    public WktType() {
        this.delegate = new CharType(true, false);
    }

    @Override
    public String getClassName() {
        return Wkt.class.getName();
    }

    @Override
    public void setJdbcObject(PreparedStatement statement, Wkt wkt, int pos, int type, int scale) throws Exception {
        String value = wkt != null ? wkt.getWkt() : null;
        delegate.setJdbcObject(statement, value, pos, Types.VARCHAR, scale);
    }

    @Override
    public Wkt materializeObject(ResultSet rs, int index, int type) throws Exception {
        String value = delegate.materializeObject(rs, index, Types.VARCHAR);
        return value != null ? new Wkt(value) : null;
    }

    @Override
    public Wkt materializeObject(CallableStatement rs, int index, int type) throws Exception {
        String value = delegate.materializeObject(rs, index, Types.VARCHAR);
        return value != null ? new Wkt(value) : null;
    }

    @Override
    public String toString(Wkt value) {
        return value != null ? value.getWkt() : null;
    }

}