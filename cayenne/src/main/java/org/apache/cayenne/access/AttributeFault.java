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

package org.apache.cayenne.access;

import org.apache.cayenne.Fault;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.reflect.AttributeProperty;

/**
 * @since 4.2
 */
public class AttributeFault extends Fault {

    private final AttributeProperty property;

    public AttributeFault(AttributeProperty property) {
        this.property = property;
    }

    @Override
    public Object resolveFault(Persistent sourceObject, String attributeName) {
        return ObjectSelect
                .columnQuery(sourceObject.getClass(), PropertyFactory.createBase(attributeName, property.getAttribute().getJavaClass()))
                .where(ExpressionFactory.matchAllDbExp(sourceObject.getObjectId().getIdSnapshot(), Expression.EQUAL_TO))
                .selectOne(sourceObject.getObjectContext());
    }

}
