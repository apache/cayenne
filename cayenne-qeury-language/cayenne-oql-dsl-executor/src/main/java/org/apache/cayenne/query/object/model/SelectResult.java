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

import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.query.object.model.visitor.ObjectQueryVisitable;
import org.apache.cayenne.query.object.model.visitor.ObjectQueryVisitor;

import javax.annotation.concurrent.Immutable;

/**
* @since 4.0
*/
@Immutable
public interface SelectResult extends ObjectQueryVisitable {

    @Immutable
    class SelectFrom implements SelectResult {
        public final From from;

        public SelectFrom(From from) {
            this.from = from;
        }

        @Override
        public <R> R accept(ObjectQueryVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    /**
     * property or scalar expression that can be used in select clause
     * */
    interface SelectValue extends SelectResult {}

    @Immutable
    class Attribute implements SelectValue {
        public final From from;
        public final ObjAttribute attr;

        public Attribute(From from, ObjAttribute attr) {
            this.from = from;
            this.attr = attr;
        }

        @Override
        public <R> R accept(ObjectQueryVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }
}
