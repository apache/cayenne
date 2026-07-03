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
package org.apache.cayenne.crypto.batch;

import org.apache.cayenne.access.translator.BatchTranslator;
import org.apache.cayenne.access.translator.TranslatedBatch;
import org.apache.cayenne.crypto.transformer.BindingsTransformer;
import org.apache.cayenne.crypto.transformer.TransformerFactory;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.BatchQuery;

/**
 * @since 5.0
 */
public class CryptoBatchTranslator<T extends BatchQuery> implements BatchTranslator<T> {

    private final TransformerFactory transformerFactory;
    private final BatchTranslator<T> delegate;

    public CryptoBatchTranslator(
            @Inject BatchTranslator<T> delegate,
            @Inject TransformerFactory transformerFactory) {

        this.transformerFactory = transformerFactory;
        this.delegate = delegate;
    }

    @Override
    public TranslatedBatch translate(T query, DbAdapter adapter) {
        TranslatedBatch translated = delegate.translate(query, adapter);
        BindingsTransformer encryptor = transformerFactory.encryptor(translated.bindings());

        return encryptor != null
                ? new TranslatedBatch(translated.sql(), encryptor.transform(translated.bindings()))
                : translated;
    }
}
