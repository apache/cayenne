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
package org.apache.cayenne.gen;

/**
 * Defines class generation template types.
 * 
 * @since 3.0
 */
public enum TemplateType {

    ENTITY_SINGLE_CLASS(false),

    ENTITY_SUPERCLASS(true),

    ENTITY_SUBCLASS(false),

    EMBEDDABLE_SINGLE_CLASS(false),

    EMBEDDABLE_SUPERCLASS(true),

    EMBEDDABLE_SUBCLASS(false),

    DATAMAP_SINGLE_CLASS(false),

    DATAMAP_SUPERCLASS(true),

    DATAMAP_SUBCLASS(false);

    private boolean superclass;

    private TemplateType(boolean superclass) {
        this.superclass = superclass;
    }

    public boolean isSuperclass() {
        return superclass;
    }
}
