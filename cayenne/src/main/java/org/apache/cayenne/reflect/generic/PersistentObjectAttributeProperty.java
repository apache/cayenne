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
package org.apache.cayenne.reflect.generic;

import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.PropertyException;
import org.apache.cayenne.reflect.PropertyVisitor;

class PersistentObjectAttributeProperty extends PersistentObjectBaseProperty implements
        AttributeProperty {

    protected final ObjAttribute attribute;

    /**
     * @since 4.2
     */
    protected final ValueComparisonStrategy<Object> valueComparisonStrategy;

    public PersistentObjectAttributeProperty(ObjAttribute attribute, ValueComparisonStrategy<Object> valueComparisonStrategy) {
        this.attribute = attribute;
        this.valueComparisonStrategy = valueComparisonStrategy;
    }

    @Override
    public String getName() {
        return attribute.getName();
    }

    public ObjAttribute getAttribute() {
        return attribute;
    }

    @Override
    public void injectValueHolder(Object object) throws PropertyException {
    }

    @Override
    public boolean visit(PropertyVisitor visitor) {
        return visitor.visitAttribute(this);
    }

    @Override
    public boolean equals(Object value1, Object value2) {
        return valueComparisonStrategy.equals(value1, value2);
    }
}
