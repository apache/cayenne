/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cayenne.query.object.model;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.object.model.visitor.ObjectQueryVisitable;
import org.apache.cayenne.query.object.model.visitor.ObjectQueryVisitor;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.Collections;
import java.util.List;

/**
* @since 4.0
*/
@Immutable
public class Select implements ObjectQueryVisitable {

    @Nonnull
    private final List<SelectResult> selectResults;

    @Nonnull
    private final List<From> from;

    @Nonnull
    private final List<Expression> where;

    public Select(@Nonnull List<SelectResult> selectResults,
                  @Nonnull List<From> from,
                  @Nonnull List<Expression> where) {

        this.selectResults = Collections.unmodifiableList(selectResults);
        this.from = Collections.unmodifiableList(from);
        this.where = Collections.unmodifiableList(where);
    }


    @Nonnull
    public List<SelectResult> getSelectResults() {
        return selectResults;
    }

    @Nonnull
    public List<From> getFrom() {
        return from;
    }

    @Nonnull
    public List<Expression> getWhere() {
        return where;
    }

    @Override
    public <R> R accept(ObjectQueryVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
