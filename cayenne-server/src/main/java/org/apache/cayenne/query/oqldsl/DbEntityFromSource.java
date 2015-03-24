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
package org.apache.cayenne.query.oqldsl;

import de.jexp.jequel.expression.PathExpression;
import de.jexp.jequel.expression.StringPathExpression;
import de.jexp.jequel.sql.SqlDsl;
import de.jexp.jequel.sql.SqlDsl.SqlVisitor;
import de.jexp.jequel.sql.SqlModel;
import de.jexp.jequel.sql.SqlModel.FromSource;
import org.apache.cayenne.map.DbEntity;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;

/**
 * @since 4.0
 */
public class DbEntityFromSource<T extends FromSource> extends StringPathExpression implements FromSource<T> {

    protected DbEntityFromSource(DbEntity entity) {
        super(entity.getName());
    }

    @Override
    public List<PathExpression> columns() {
        throw new NotImplementedException("");
    }

    @Override
    public T as(String alias) {
        throw new NotImplementedException("");
    }

    @Override
    public <R> R accept(SqlVisitor<R> sqlVisitor) {
        return sqlVisitor.visit(this);
    }
}
