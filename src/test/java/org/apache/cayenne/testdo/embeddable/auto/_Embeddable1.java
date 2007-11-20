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
package org.apache.cayenne.testdo.embeddable.auto;

import org.apache.cayenne.Persistent;

public class _Embeddable1 {

    private Persistent owner;
    private String embeddedProperty;

    protected String embedded10;
    protected String embedded20;

    public String getEmbedded10() {
        return embedded10;
    }

    public void setEmbedded10(String embedded10) {
        propertyWillChange("embdedded10", this.embedded10, embedded10);
        this.embedded10 = embedded10;
    }

    public String getEmbedded20() {
        return embedded20;
    }

    public void setEmbedded20(String embedded20) {
        propertyWillChange("embdedded20", this.embedded20, embedded20);
        this.embedded20 = embedded20;
    }

    protected void propertyWillChange(String property, Object oldValue, Object newValue) {
        if (owner != null && owner.getObjectContext() != null) {
            owner.getObjectContext().propertyChanged(
                    owner,
                    embeddedProperty + "." + property,
                    oldValue,
                    newValue);
        }
    }
}
