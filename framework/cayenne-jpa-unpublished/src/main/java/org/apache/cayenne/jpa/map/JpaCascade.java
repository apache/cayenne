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
package org.apache.cayenne.jpa.map;

import java.util.Collection;
import java.util.LinkedHashSet;

import javax.persistence.CascadeType;

import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

public class JpaCascade implements XMLSerializable {

    protected Collection<CascadeType> cascades;

    public Collection<CascadeType> getCascades() {
        if (cascades == null) {
            cascades = new LinkedHashSet<CascadeType>(5);
        }

        return cascades;
    }

    public void encodeAsXML(XMLEncoder encoder) {
        if (cascades != null) {
            encoder.println("<cascades>");
            encoder.indent(1);
            for (CascadeType t : cascades) {
                switch (t) {
                    case ALL:
                        encoder.println("<cascade-all/>");
                        break;
                    case MERGE:
                        encoder.println("<cascade-merge/>");
                        break;
                    case PERSIST:
                        encoder.println("<cascade-persist/>");
                        break;
                    case REFRESH:
                        encoder.println("<cascade-refresh/>");
                        break;
                    case REMOVE:
                        encoder.println("<cascade-remove/>");
                        break;
                }
            }
            encoder.indent(-1);
            encoder.println("</cascades>");
        }
    }

    public void setCascadeAll(Object value) {
        getCascades().add(CascadeType.ALL);
    }

    public void setCascadeMerge(Object value) {
        getCascades().add(CascadeType.MERGE);
    }

    public void setCascadePersist(Object value) {
        getCascades().add(CascadeType.PERSIST);
    }

    public void setCascadeRefresh(Object value) {
        getCascades().add(CascadeType.REFRESH);
    }

    public void setCascadeRemove(Object value) {
        getCascades().add(CascadeType.REMOVE);
    }
}
