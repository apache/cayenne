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

package org.apache.cayenne.exp.property;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.path.CayennePath;

/**
 * Property that represents object attribute mapped on {@link org.apache.cayenne.map.Embeddable} object.
 * @since 4.2
 */
public class EmbeddableProperty<E> extends BaseProperty<E> implements PathProperty<E> {

    /**
     * Constructs a new property with the given name and type
     *
     * @param path       of the property (will be used as alias for the expression)
     * @param type       of the property
     * @see PropertyFactory#createEmbeddable(String, Class)
     */
    protected EmbeddableProperty(CayennePath path, Expression exp, Class<? super E> type) {
        super(path, exp, type);
    }
}
