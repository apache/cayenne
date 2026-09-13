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

package org.apache.cayenne.access.translator.select;

import java.util.List;

import org.apache.cayenne.query.ResultSegment;

/**
 * Resolves the {@link org.apache.cayenne.map.SQLResult} collected by the column extractors into a list of
 * {@link ResultSegment}s with column offsets matching the translated SELECT column list. The result is a part of
 * the translation output and is never written back into the query.
 *
 * @since 4.2
 */
public class SQLResultStage implements TranslationStage {

    @Override
    public void perform(SelectTranslatorContext context) {
        if(context.getParentContext() != null || !context.needsResultSetMapping()) {
            return;
        }

        List<ResultSegment> resultSetMapping = context.getSqlResult().getResolvedComponents(context.getResolver());
        context.setResultSetMapping(resultSetMapping);
    }
}
