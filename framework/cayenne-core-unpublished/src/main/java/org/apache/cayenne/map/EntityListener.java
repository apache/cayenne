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
package org.apache.cayenne.map;

import java.io.Serializable;

import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

/**
 * A mapping descriptor of an entity listener class that declares one or more
 * callback methods to be notified of the entity events.
 * 
 * @since 3.0
 * @deprecated since 3.2, as EntityResolver no longer loads listeners from its
 *             DataMaps.
 */
@Deprecated
public class EntityListener implements Serializable, XMLSerializable {

    protected String className;
    protected CallbackMap callbacks;

    public EntityListener() {
        this(null);
    }

    public EntityListener(String className) {
        this.className = className;
        this.callbacks = new CallbackMap();
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Returns an object that stores callback methods of this listener.
     * 
     * @return callback map
     */
    public CallbackMap getCallbackMap() {
        return callbacks;
    }

    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<entity-listener class=\"");
        encoder.print(className);
        encoder.println("\">");
        encoder.indent(1);

        getCallbackMap().encodeCallbacksAsXML(encoder);

        encoder.indent(-1);
        encoder.println("</entity-listener>");
    }
}
