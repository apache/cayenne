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

/**
 * @since 4.0.
 */
public class ObjAttributeBuilder extends DefaultBuilder<ObjAttribute> {

    public ObjAttributeBuilder() {
        super(new ObjAttribute());
    }

    public ObjAttributeBuilder name() {
        return name(getRandomJavaName());
    }

    public ObjAttributeBuilder name(String name) {
        obj.setName(name);

        return this;
    }

    public ObjAttributeBuilder type(Class type) {
        obj.setType(type.getCanonicalName());

        return this;
    }

    public ObjAttributeBuilder dbPath(String path) {
        obj.setDbAttributePath(path);

        return this;
    }

    @Override
    public ObjAttribute build() {
        if (obj.getName() == null) {
            name();
        }

        return obj;
    }

    @Override
    public ObjAttribute random() {
        return build();
    }
}
