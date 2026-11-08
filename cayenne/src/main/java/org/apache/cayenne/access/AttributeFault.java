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
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.query.ObjectSelect;

/**
 * The fault of a lazy attribute, resolved by reading the column of the object's row.
 *
 * @param attributeName the name of the attribute this fault stands for
 * @param valueClass    the class of the attribute value
 * @since 4.2
 */
public record AttributeFault(String attributeName, Class<?> valueClass) implements Fault {

    @Override
    public Object resolveFault(Persistent sourceObject) {
        return ObjectSelect
                .columnQuery(sourceObject.getClass(),
                        PropertyFactory.createBase(attributeName, valueClass))

                .where(ExpressionFactory.matchAllDbExp(sourceObject.getObjectId().getIdSnapshot()))
                .selectOne(sourceObject.getObjectContext());
    }

}
