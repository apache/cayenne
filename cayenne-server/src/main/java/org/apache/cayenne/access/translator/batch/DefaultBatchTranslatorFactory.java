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

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.query.DeleteBatchQuery;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.query.UpdateBatchQuery;

/**
 * Default implementation of {@link BatchTranslatorFactory}.
 * 
 * @since 3.2
 */
public class DefaultBatchTranslatorFactory implements BatchTranslatorFactory {

    @Override
    public BatchTranslator deleteTranslator(DeleteBatchQuery query, DbAdapter adapter) {
        return new DeleteBatchTranslator(query, adapter);
    }

    @Override
    public BatchTranslator insertTranslator(InsertBatchQuery query, DbAdapter adapter) {
        return new InsertBatchTranslator(query, adapter);
    }

    @Override
    public BatchTranslator updateTranslator(UpdateBatchQuery query, DbAdapter adapter) {
        return new UpdateBatchTranslator(query, adapter);
    }

}
