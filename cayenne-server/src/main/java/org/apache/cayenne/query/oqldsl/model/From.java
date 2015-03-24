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
package org.apache.cayenne.query.oqldsl.model;

import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.oqldsl.model.visitor.ObjectQueryVisitable;
import org.apache.cayenne.query.oqldsl.model.visitor.ObjectQueryVisitor;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
* @since 4.0
*/
@Immutable
public interface From extends ObjectQueryVisitable {

    String name();

    ObjEntity entity();

    /**
     * Unique key of from clause within the query. It can be table name
     * or join expression see Relation implementation
     * */
    @Nonnull
    String tableExpression();

    @Immutable
    abstract class BaseFrom implements From {
        public final String name;

        protected BaseFrom(String name) {
            this.name = name;
        }

        public String name() {
            return name;
        }

        @Override
        public String toString() {
            return tableExpression() + " " + name();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof From)) {
                return false;
            }

            From from = (From) o;

            return name.equals(from.name()) && tableExpression().equals(from.tableExpression());
        }

        @Override
        public int hashCode() {
            return name.hashCode() + tableExpression().hashCode();
        }
    }

    @Immutable
    class Relation extends BaseFrom {
        public final From joinTo;
        public final ObjRelationship rel;

        public Relation(String name, From joinTo, ObjRelationship rel) {
            super(name);
            this.joinTo = joinTo;
            this.rel = rel;
        }

        @Override
        public String name() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ObjEntity entity() {
            return rel.getTargetEntity();
        }

        @Nonnull
        @Override
        public String tableExpression() {
            return joinTo.name() + "." + rel.getName();
        }

        @Override
        public <R> R accept(ObjectQueryVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    @Immutable
    class Entity extends BaseFrom {
        public final ObjEntity entity;

        public Entity(String name, ObjEntity entity) {
            super(name);
            this.entity = entity;
        }

        @Override
        public ObjEntity entity() {
            return entity;
        }

        @Nonnull
        @Override
        public String tableExpression() {
            return entity.getName();
        }

        @Override
        public <R> R accept(ObjectQueryVisitor<R> visitor) {
            return visitor.visit(this);
        }

    }

}
