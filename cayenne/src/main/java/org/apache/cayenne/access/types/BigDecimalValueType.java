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

package org.apache.cayenne.access.types;

import java.math.BigDecimal;

/**
 * @since 4.2
 */
public class BigDecimalValueType implements ValueObjectType<BigDecimal, BigDecimal> {

    @Override
    public Class<BigDecimal> getTargetType() {
        return BigDecimal.class;
    }

    @Override
    public Class<BigDecimal> getValueType() {
        return BigDecimal.class;
    }

    @Override
    public BigDecimal toJavaObject(BigDecimal value) {
        return value;
    }

    @Override
    public BigDecimal fromJavaObject(BigDecimal object) {
        return object;
    }

    @Override
    public String toCacheKey(BigDecimal object) {
        return object.toString();
    }

    @Override
    public boolean equals(BigDecimal value1, BigDecimal value2) {
        //noinspection NumberEquality
        if(value1 == value2) {
            return true;
        }
        if(value1 == null || value2 == null) {
            return false;
        }
        return value1.compareTo(value2) == 0;
    }
}
