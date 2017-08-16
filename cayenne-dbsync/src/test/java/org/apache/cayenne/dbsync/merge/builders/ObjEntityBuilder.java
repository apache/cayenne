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
package org.apache.cayenne.dbsync.merge.builders;

import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.util.Util;

/**
 * @since 4.0.
 */
public class ObjEntityBuilder extends DefaultBuilder<ObjEntity> {

    public ObjEntityBuilder() {
        super(new ObjEntity());
    }

    public ObjEntityBuilder name() {
        return name(getRandomJavaName());
    }

    public ObjEntityBuilder name(String name) {
        obj.setName(name);

        return this;
    }

    public ObjEntityBuilder attributes(ObjAttribute... attributes) {
        for (ObjAttribute attribute : attributes) {
            obj.addAttribute(attribute);
        }

        return this;
    }

    public ObjEntityBuilder attributes(ObjAttributeBuilder ... attributes) {
        for (ObjAttributeBuilder attribute : attributes) {
            obj.addAttribute(attribute.build());
        }

        return this;
    }

    public ObjEntityBuilder attributes(int numberUpTo) {
        for (int i = 0; i < numberUpTo; i++) {
            obj.addAttribute(new ObjAttributeBuilder().random());
        }

        return this;
    }


    @Override
    public ObjEntity build() {
        if (obj.getName() == null) {
            obj.setName(Util.capitalized(getRandomJavaName()));
        }

        return obj;
    }

    @Override
    public ObjEntity random() {
        if (dataFactory.chance(99)) {
            attributes(dataFactory.getNumberUpTo(20));
        }

        return build();
    }

    public ObjEntityBuilder clazz(String s) {
        obj.setClassName(s);

        return this;
    }

    public ObjEntityBuilder dbEntity(String table) {
        obj.setDbEntityName(table);

        return this;
    }
}
