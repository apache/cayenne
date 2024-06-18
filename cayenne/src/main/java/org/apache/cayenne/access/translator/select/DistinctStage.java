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

import java.sql.Types;

/**
 * @since 4.2
 */
class DistinctStage implements TranslationStage {

    private static final int[] UNSUPPORTED_DISTINCT_TYPES = {
            Types.BLOB,
            Types.CLOB,
            Types.NCLOB,
            Types.LONGVARCHAR,
            Types.LONGNVARCHAR,
            Types.LONGVARBINARY
    };

    static boolean isUnsupportedForDistinct(int type) {
        for (int unsupportedDistinctType : UNSUPPORTED_DISTINCT_TYPES) {
            if (unsupportedDistinctType == type) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void perform(TranslatorContext context) {
        // explicit suppressing of distinct
        if(context.getMetadata().isSuppressingDistinct()) {
            context.setDistinctSuppression(true);
            return;
        }

        // query forcing distinct or query have joins (qualifier or prefetch)
        if(!context.getQuery().isDistinct() && !context.getTableTree().hasToManyJoin()) {
            return;
        }

        // unsuitable jdbc type for distinct clause
        for(ResultNodeDescriptor node : context.getResultNodeList()) {
            // TODO: make it per adapter rather than one-for-all
            if(isUnsupportedForDistinct(node.getJdbcType())) {
                context.setDistinctSuppression(true);
                return;
            }
        }
        context.getSelectBuilder().distinct();
    }
}
