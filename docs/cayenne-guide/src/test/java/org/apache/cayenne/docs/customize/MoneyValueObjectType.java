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
package org.apache.cayenne.docs.customize;

import org.apache.cayenne.access.types.ValueObjectType;

import java.math.BigDecimal;

// tag::content[]
public class MoneyValueObjectType implements ValueObjectType<Money, BigDecimal> {

    @Override
    public Class<BigDecimal> getTargetType() {
        return BigDecimal.class;
    }

    @Override
    public Class<Money> getValueType() {
        return Money.class;
    }

    @Override
    public Money toJavaObject(BigDecimal value) {
        return new Money(value);
    }

    @Override
    public BigDecimal fromJavaObject(Money object) {
        return object.getValue();
    }

    @Override
    public String toCacheKey(Money object) {
        return object.getValue().toString();
    }
}
// end::content[]
