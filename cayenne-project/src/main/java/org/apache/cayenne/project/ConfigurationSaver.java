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
package org.apache.cayenne.project;

import java.io.PrintWriter;

import org.apache.cayenne.configuration.BaseConfigurationNodeVisitor;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.xml.ProjectVersion;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.project.extension.SaverDelegate;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

/**
 * @since 3.1
 */
class ConfigurationSaver extends BaseConfigurationNodeVisitor<Void> {

    private PrintWriter printWriter;
    private ProjectVersion version;
    private SaverDelegate delegate;

    ConfigurationSaver(PrintWriter printWriter, ProjectVersion version, SaverDelegate delegate) {
        this.printWriter = printWriter;
        this.version = version;
        this.delegate = delegate;
    }

    @Override
    public Void visitDataChannelDescriptor(DataChannelDescriptor node) {
        encodeNode(node);
        return null;
    }

    @Override
    public Void visitDataMap(DataMap node) {
        encodeNode(node);
        return null;
    }

    private void encodeNode(XMLSerializable node) {
        XMLEncoder encoder = new XMLEncoder(printWriter, "\t", version);
        encoder.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        delegate.setXMLEncoder(encoder);
        node.encodeAsXML(encoder, delegate);
    }
}
