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

package org.apache.cayenne.access;

import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;

import java.util.ArrayList;
import java.util.List;

class CollectingNamePropertyVisitor implements PropertyVisitor {
    private final List<String> properties;

    CollectingNamePropertyVisitor() {
        this.properties = new ArrayList<>();
    }

    @Override
    public boolean visitAttribute(final AttributeProperty property) {
        properties.add(property.getName());
        return true;
    }

    @Override
    public boolean visitToOne(final ToOneProperty property) {
        properties.add(property.getName());
        return true;
    }

    @Override
    public boolean visitToMany(final ToManyProperty property) {
        properties.add(property.getName());
        return true;
    }

    List<String> allProperties(ClassDescriptor classDescriptor) {
        classDescriptor.visitProperties(this);
        return properties;
    }
}
