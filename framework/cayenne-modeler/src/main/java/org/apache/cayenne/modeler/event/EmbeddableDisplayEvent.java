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
package org.apache.cayenne.modeler.event;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;

public class EmbeddableDisplayEvent extends DataMapDisplayEvent {

    protected Embeddable embeddable;
    protected boolean embeddableChanged = true;

    protected boolean mainTabFocus;

    public EmbeddableDisplayEvent(Object src, Embeddable embeddable, DataMap dataMap,
            DataDomain dataDomain) {
        super(src, dataMap, dataDomain);
        this.embeddable = embeddable;
        setDataMapChanged(false);
    }

    public Embeddable getEmbeddable() {
        return embeddable;
    }

    public boolean isMainTabFocus() {
        return mainTabFocus;
    }

    public void setMainTabFocus(boolean mainTabFocus) {
        this.mainTabFocus = mainTabFocus;
    }

    public boolean isEmbeddableChanged() {
        return embeddableChanged;
    }

    public void setEmbeddableChanged(boolean temp) {
        this.embeddableChanged = temp;
    }
}
