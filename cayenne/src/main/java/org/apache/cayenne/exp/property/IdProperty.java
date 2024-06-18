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

package org.apache.cayenne.exp.property;

import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;

/**
 * Base class for properties mapped to PK
 * @since 4.2
 */
public interface IdProperty<E> extends Property<E> {

    default Expression eq(ObjectId value) {
        if(!getEntityName().equals(value.getEntityName())) {
            throw new CayenneRuntimeException("Can match IdProperty only with ObjectId for same entity");
        }
        Map<String, Object> idSnapshot = value.getIdSnapshot();
        Object pkValue;
        if(getAttributeName() == null) {
            if (idSnapshot.size() > 1) {
                throw new CayenneRuntimeException("Can't match IdProperty with compound PK");
            }
            pkValue = idSnapshot.values().iterator().next();
        } else {
            pkValue = idSnapshot.get(getAttributeName());
            if(pkValue == null && !idSnapshot.containsKey(getAttributeName())) {
                throw new CayenneRuntimeException("No PK attribute %s for ObjectId %s", getAttributeName(), value);
            }
        }
        return ExpressionFactory.matchExp(getExpression(), pkValue);
    }

    String getEntityName();

    String getAttributeName();

}
