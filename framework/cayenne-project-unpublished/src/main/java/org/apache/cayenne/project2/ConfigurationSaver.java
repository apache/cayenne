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
package org.apache.cayenne.project2;

import java.io.PrintWriter;

import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.util.XMLEncoder;

/**
 * @since 3.1
 */
class ConfigurationSaver implements ConfigurationNodeVisitor<Void> {

    private PrintWriter printWriter;

    ConfigurationSaver(PrintWriter printWriter) {
        this.printWriter = printWriter;
    }

    public Void visitDataChannelDescriptor(DataChannelDescriptor node) {
        XMLEncoder encoder = new XMLEncoder(printWriter, "\t");
        encoder.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        node.encodeAsXML(encoder);
        return null;
    }

    public Void visitDataMap(DataMap node) {
        XMLEncoder encoder = new XMLEncoder(printWriter, "\t");
        encoder.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        node.encodeAsXML(encoder);
        return null;
    }
}
