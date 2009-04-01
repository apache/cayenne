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

package org.apache.cayenne.xml;

import java.util.Collection;
import java.util.Iterator;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.reflect.PropertyUtils;
import org.w3c.dom.Element;

/**
 * A flyweight wrapper for serializing with XML mapping. This object is NOT thread-safe.
 * 
 * @since 1.2
 */
class SerializableEntity implements XMLSerializable {

    Element descriptor;
    XMLMappingDescriptor descriptorMap;

    // same node can store more than one object during encoding
    transient Object object;

    public SerializableEntity(XMLMappingDescriptor descriptorMap, Element descriptor) {
        this.descriptor = descriptor;
        this.descriptorMap = descriptorMap;
    }

    String getName() {
        return descriptor.getAttribute("name");
    }

    Element getDescriptor() {
        return descriptor;
    }

    void setObject(Object object) {
        this.object = object;
    }

    public void encodeAsXML(XMLEncoder encoder) {
        if (object instanceof Collection) {
            Collection<?> c = (Collection<?>) object;
            if (!c.isEmpty()) {

                // push the first node, and create the rest as peers.
                Iterator<?> it = c.iterator();
                encodeObject(encoder, it.next(), true);
                while (it.hasNext()) {
                    encodeObject(encoder, it.next(), false);
                }
                
                // Make sure we pop the node we just pushed -- needed for fix to CAY-597.
                encoder.pop();
            }
        }
        else {
            encodeObject(encoder, this.object, true);
            
            // Needed for fix to CAY-597.  This makes sure we get back to the appropriate level in the DOM, rather than constantly re-rooting the tree.
            encoder.pop();
        }
    }

    public void decodeFromXML(XMLDecoder decoder) {
        throw new CayenneRuntimeException("Decoding is not supported by this object");
    }

    void encodeObject(XMLEncoder encoder, Object object, boolean push) {
        encoder.setRoot(descriptor.getAttribute("xmlTag"), null, push);

        for (Element property : XMLUtil.getChildren(descriptor)) {
            String xmlTag = property.getAttribute("xmlTag");
            String name = property.getAttribute("name");
            Object value = PropertyUtils.getProperty(object, name);

            if (value == null) {
                continue;
            }

            SerializableEntity relatedEntity = descriptorMap.getEntity(xmlTag);
            if (relatedEntity != null) {
                relatedEntity.setObject(value);
                relatedEntity.encodeAsXML(encoder);
            }
            else {
                encoder.encodeProperty(xmlTag, value, false);
            }
        }
    }
}
