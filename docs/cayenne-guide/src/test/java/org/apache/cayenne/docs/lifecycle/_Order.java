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
package org.apache.cayenne.docs.lifecycle;

import org.apache.cayenne.PersistentObject;

import java.util.Date;

/**
 * A simplified stand-in for a generated superclass, that is not a part of the docs test model. Allows the examples
 * that need an "Order" entity to compile.
 */
public abstract class _Order extends PersistentObject {

    protected Date createdOn;

    public void setCreatedOn(Date createdOn) {
        beforePropertyWrite("createdOn", this.createdOn, createdOn);
        this.createdOn = createdOn;
    }

    public Date getCreatedOn() {
        beforePropertyRead("createdOn");
        return this.createdOn;
    }
}
