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

/**
 * Get result columns based on query, options are: <ol>
 *    <li> for column queries - defined set of columns
 *    <li> for paginated or nested queries - root pk columns
 *    <li> for queries with defined class descriptor - full column set including flattened
 *    <li> for everything else - all columns of the root db entity
 * </ol>
 *
 * @since 4.2
 */
class ColumnExtractorStage implements TranslationStage {

    @Override
    public void perform(TranslatorContext context) {
        ColumnExtractor extractor;

        if(context.getQuery().getColumns() != null && !context.getQuery().getColumns().isEmpty()) {
            extractor = new CustomColumnSetExtractor(context, context.getQuery().getColumns());
        } else if (context.getParentContext() != null || context.getMetadata().getPageSize() > 0) {
            if(context.getMetadata().getObjEntity() != null) {
                extractor = new IdColumnExtractor(context, context.getMetadata().getObjEntity());
            } else {
                extractor = new IdColumnExtractor(context, context.getMetadata().getDbEntity());
            }
        } else if (context.getMetadata().getClassDescriptor() != null) {
            extractor = new DescriptorColumnExtractor(context, context.getMetadata().getClassDescriptor());
        } else {
            extractor = new DbEntityColumnExtractor(context);
        }

        extractor.extract();
    }
}
