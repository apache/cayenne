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

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.util.Util;

/**
 * @since 4.0.
 */
public class DbEntityBuilder extends DefaultBuilder<DbEntity> {

    public DbEntityBuilder() {
        super(new DbEntity());
    }

    public DbEntityBuilder name() {
        return name(getRandomJavaName());
    }

    public DbEntityBuilder name(String name) {
        obj.setName(name);

        return this;
    }

    public DbEntityBuilder attributes(DbAttribute ... attributes) {
        for (DbAttribute attribute : attributes) {
            obj.addAttribute(attribute);
        }

        return this;
    }

    public DbEntityBuilder attributes(DbAttributeBuilder ... attributes) {
        for (DbAttributeBuilder attribute : attributes) {
            obj.addAttribute(attribute.build());
        }

        return this;
    }

    public DbEntityBuilder attributes(int numberUpTo) {
        for (int i = 0; i < numberUpTo; i++) {
            try {
                obj.addAttribute(new DbAttributeBuilder().random());
            } catch (IllegalArgumentException e) {
                i--; // try again
            }
        }

        return this;
    }


    @Override
    public DbEntity build() {
        if (obj.getName() == null) {
            obj.setName(Util.capitalized(getRandomJavaName()));
        }

        return obj;
    }

    @Override
    public DbEntity random() {
        if (dataFactory.chance(99)) {
            attributes(dataFactory.getNumberUpTo(20));
        }

        return build();
    }
}
