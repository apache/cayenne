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
package org.apache.cayenne.reflect;

import org.apache.cayenne.Persistent;

/**
 * An accessor of a property of a {@link Persistent} object through the Persistent API, without disturbing the fault
 * status of the object.
 *
 * @since 6.0
 */
public final class PersistentAccessor implements Accessor {

    private final String name;

    public PersistentAccessor(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getValue(Object object) throws PropertyException {
        try {
            return ((Persistent) object).readPropertyDirectly(name);
        } catch (ClassCastException e) {
            throw new PropertyException("Object is not a Persistent: '" + object.getClass().getName() + "'", this,
                    object, e);
        } catch (Throwable th) {
            throw new PropertyException("Error reading Persistent property: " + name, this, object, th);
        }
    }

    @Override
    public void setValue(Object object, Object newValue) throws PropertyException {
        try {
            ((Persistent) object).writePropertyDirectly(name, newValue);
        } catch (ClassCastException e) {
            throw new PropertyException("Object is not a Persistent: '" + object.getClass().getName() + "'", this,
                    object, e);
        } catch (Throwable th) {
            throw new PropertyException("Error writing Persistent property: " + name, this, object, th);
        }
    }
}
