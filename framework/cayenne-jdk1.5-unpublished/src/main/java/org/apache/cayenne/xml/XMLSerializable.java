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

/**
 * Interface for Cayenne objects that can be saved to XML.
 * 
 * @since 1.2
 * @deprecated since 3.1 this XML serialization package is deprecated and will be removed
 *             in the following releases. It has a number of functional and performance
 *             limitations that make it impossible to evolve further. A replacement may be
 *             provided in an undefined future. For now we recommend the users to
 *             implement XML serialization of persistent objects based JAXB, XStream or
 *             other similar frameworks.
 */
@Deprecated
public interface XMLSerializable {

    /**
     * Encodes itself as XML using the provided XMLEncoder.
     * 
     * @param encoder The encoder object.
     */
    public void encodeAsXML(XMLEncoder encoder);

    /**
     * Decodes itself from XML using the provided XMLDecoder.
     * 
     * @param decoder The decoder object.
     */
    public void decodeFromXML(XMLDecoder decoder);
}
