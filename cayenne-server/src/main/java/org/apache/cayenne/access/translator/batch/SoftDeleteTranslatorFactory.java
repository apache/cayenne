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
package org.apache.cayenne.access.translator.batch;

import java.sql.Types;

import org.apache.cayenne.access.translator.batch.BatchTranslator;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.DeleteBatchQuery;

/**
 * Implementation of {link #BatchTranslator}, which uses 'soft' delete
 * (runs UPDATE and sets 'deleted' field to true instead-of running SQL DELETE)
 * 
 * @since 3.2
 */
public class SoftDeleteTranslatorFactory extends DefaultBatchTranslatorFactory {
    /**
     * Default name of 'deleted' field
     */
    public static final String DEFAULT_DELETED_FIELD_NAME = "DELETED";

    /**
     * Name of 'deleted' field
     */
    private String deletedFieldName;

    public SoftDeleteTranslatorFactory() {
        this(DEFAULT_DELETED_FIELD_NAME);
    }

    public SoftDeleteTranslatorFactory(String deletedFieldName) {
        this.deletedFieldName = deletedFieldName;
    }

    @Override
    protected BatchTranslator deleteTranslator(DeleteBatchQuery query, DbAdapter adapter, String trimFunction) {

        DbAttribute attr = query.getDbEntity().getAttribute(deletedFieldName);
        boolean needsSoftDelete = attr != null && attr.getType() == Types.BOOLEAN;

        return needsSoftDelete ? new SoftDeleteBatchTranslator(query, adapter, trimFunction, deletedFieldName) : super
                .deleteTranslator(query, adapter, trimFunction);
    }

    /**
     * @return name of 'deleted' field
     */
    public String getDeletedFieldName() {
        return deletedFieldName;
    }
}
