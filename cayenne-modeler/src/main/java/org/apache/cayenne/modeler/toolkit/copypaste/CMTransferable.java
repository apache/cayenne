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
package org.apache.cayenne.modeler.toolkit.copypaste;

import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.configuration.EmptyConfigurationNodeVisitor;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.List;

/**
 * CMTransferable is a data holder of Cayenne object(s), like Entities, Attributes, Relationships etc.
 */
public class CMTransferable implements Transferable {

    public static final DataFlavor CAYENNE_FLAVOR = new DataFlavor(Serializable.class, "Cayenne Object");
    private static final DataFlavor[] FLAVORS = new DataFlavor[]{CAYENNE_FLAVOR, DataFlavor.stringFlavor};

    private final Object data;

    public CMTransferable(Object data) {
        this.data = data;
    }

    @Override
    public Object getTransferData(DataFlavor flavor) {

        if (flavor == CAYENNE_FLAVOR) {
            return data;
        } else {
            StringWriter out = new StringWriter();
            XMLEncoder encoder = new XMLEncoder(new PrintWriter(out), "\t");
            ConfigurationNodeVisitor visitor = new EmptyConfigurationNodeVisitor();

            encoder.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");

            if (data instanceof XMLSerializable) {
                ((XMLSerializable) data).encodeAsXML(encoder, visitor);
            } else if (data instanceof List) {
                for (Object o : (List<?>) data) {
                    ((XMLSerializable) o).encodeAsXML(encoder, visitor);
                }
            }

            return out.toString();
        }

    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return FLAVORS;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor == CAYENNE_FLAVOR || flavor == DataFlavor.stringFlavor;
    }
}
