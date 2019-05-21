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

package org.apache.cayenne.configuration.xml;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataChannelDescriptorLoader;
import org.apache.cayenne.map.DataMap;
import org.xml.sax.XMLReader;

/**
 * @since 4.1
 */
public class LoaderContext {

    Collection<DataMapLoaderListener> dataMapListeners;

    Collection<DataChannelLoaderListener> dataChannelListeners;

    private XMLReader xmlReader;

    private HandlerFactory factory;

    public LoaderContext(XMLReader reader, HandlerFactory factory) {
        this.xmlReader = reader;
        this.factory = factory;
        dataMapListeners = new ArrayList<>();
        dataChannelListeners = new ArrayList<>();
    }

    public HandlerFactory getFactory() {
        return factory;
    }

    public XMLReader getXmlReader() {
        return xmlReader;
    }

    public void addDataMapListener(DataMapLoaderListener listener) {
        dataMapListeners.add(listener);
    }

    public void dataMapLoaded(DataMap dataMap) {
        for(DataMapLoaderListener listener : dataMapListeners) {
            listener.onDataMapLoaded(dataMap);
        }
    }

    public void addDataChannelListener(DataChannelLoaderListener listener) {
        dataChannelListeners.add(listener);
    }

    public void dataChannelLoaded(DataChannelDescriptor descriptor) {
        for(DataChannelLoaderListener listener : dataChannelListeners) {
            listener.onDataChannelLoaded(descriptor);
        }
    }

}
