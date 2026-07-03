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

package org.apache.cayenne.access.flush.operation;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

import java.sql.Types;
import java.util.Objects;

/**
 * A {@link DeleteDbRowOpFactory} enabling "soft delete". For tables that contain a BOOLEAN column with the configured
 * name it produces {@link SoftDeleteDbRowOp}, so the row is deleted by an UPDATE setting that column to {@code true}.
 * Tables without such a column (or whose column of that name is not BOOLEAN) get a plain {@link DeleteDbRowOp} and
 * are deleted with a regular SQL DELETE.
 *
 * @since 5.0
 */
public class ConditionalSoftDeleteDbRowOpFactory implements DeleteDbRowOpFactory {

    private final String columnName;

    public ConditionalSoftDeleteDbRowOpFactory(String columnName) {
        this.columnName = Objects.requireNonNull(columnName);
    }

    @Override
    public DeleteDbRowOp createOp(Persistent object, DbEntity entity, ObjectId id) {
        DbAttribute attr = entity.getAttribute(columnName);
        return attr != null && attr.getType() == Types.BOOLEAN
                ? new SoftDeleteDbRowOp(object, entity, id, attr)
                : new DeleteDbRowOp(object, entity, id);
    }
}
