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
package org.apache.cayenne.query.object.model.visitor;

import org.apache.cayenne.query.object.model.From;
import org.apache.cayenne.query.object.model.Select;
import org.apache.cayenne.query.object.model.SelectResult;

/**
 * This visitor aware about select query tree and can go thought all nodes.
 *
 * It should be used in order to apply
 *
 * @since 4.0
 */
public class TraverseVisitor<R> implements ObjectQueryVisitor<R> {
    @Override
    public R visit(Select select) {
        return null;
    }

    @Override
    public R visit(SelectResult.SelectFrom selectEntity) {
        return null;
    }

    @Override
    public R visit(SelectResult.SelectAttr selectAttr) {
        return null;
    }

    @Override
    public R visit(From.Entity from) {
        return null;
    }

    @Override
    public R visit(From.Relation from) {
        return null;
    }
}
