/*
 *    Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */
package org.apache.cayenne.dbsync.naming;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.map.ObjEntity;

/**
 * @since 4.0
 */
// TODO: fold CallbackMethod to org.apache.cayenne.map package and make it a ConfigurationNode
// then we can get rid off this fork...
class CallbackNameBuilder extends NameBuilder {

    public CallbackNameBuilder() {
        super(new CallbackNode());
    }

    @Override
    public String name() {
        String baseName = this.baseName != null
                ? this.baseName
                : "onEvent";

        return new DeduplicationVisitor(parent, baseName, dupesPattern).resolve(name -> {

            ObjEntity entity = (ObjEntity) parent;

            if (entity.getCallbackMethods().contains(name)) {
                return true;
            }

            if (name.startsWith("get")) {
                String conflictingProperty = NameUtil.uncapitalize(name.substring(3));

                // check if either attribute or relationship name matches...
                if (entity.getAttribute(conflictingProperty) != null
                        || entity.getRelationship(conflictingProperty) != null) {
                    return true;
                }
            }

            return false;
        });
    }

    static class CallbackNode implements ConfigurationNode {

        @Override
        public <T> T acceptVisitor(ConfigurationNodeVisitor<T> visitor) {
            return null;
        }
    }
}
