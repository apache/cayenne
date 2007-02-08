/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.xml;

import java.util.Collection;
import java.util.Iterator;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.property.PropertyUtils;
import org.w3c.dom.Element;

/**
 * A flyweight wrapper for serializing with XML mapping. This object is NOT thread-safe.
 * 
 * @since 1.2
 * @author Andrus Adamchik
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
            Collection c = (Collection) object;
            if (!c.isEmpty()) {

                // push the first node, and create the rest as peers.
                Iterator it = c.iterator();
                encodeObject(encoder, it.next(), true);
                while (it.hasNext()) {
                    encodeObject(encoder, it.next(), false);
                }
            }
        }
        else {
            encodeObject(encoder, this.object, true);
        }
    }

    public void decodeFromXML(XMLDecoder decoder) {
        throw new CayenneRuntimeException("Decoding is not supported by this object");
    }

    void encodeObject(XMLEncoder encoder, Object object, boolean push) {
        encoder.setRoot(descriptor.getAttribute("xmlTag"), null, push);

        Iterator it = XMLUtil.getChildren(descriptor).iterator();
        while (it.hasNext()) {

            Element property = (Element) it.next();
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