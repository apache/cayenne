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

import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.map.DbAttribute;

/**
 * @since 4.2
 */
class ColumnDescriptorStage implements TranslationStage {

    @Override
    public void perform(TranslatorContext context) {
        int i = 0;
        for(ResultNodeDescriptor resultNode : context.getResultNodeList()) {
            context.getSelectBuilder().result(resultNode::getNode);

            if(!resultNode.isInDataRow()) {
                continue;
            }

            String name;
            DbAttribute attribute = resultNode.getDbAttribute();
            if(attribute != null) {
                name = attribute.getName();
            } else {
                // generated name
                name = "__c" + i++;
            }

            ColumnDescriptor descriptor = new ColumnDescriptor(name, resultNode.getJdbcType(), resultNode.getJavaType());
            descriptor.setAttribute(attribute);
            descriptor.setDataRowKey(resultNode.getDataRowKey());

            context.getColumnDescriptors().add(descriptor);
        }
    }
}
