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

package org.apache.cayenne.jpa.conf;

import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;

import org.apache.cayenne.jpa.JpaUnit;
import org.xml.sax.InputSource;

/**
 * A class that locates persistent units in the environment and loads their definitions.
 * 
 */
public class UnitLoader {

    static final String DESCRIPTOR_LOCATION = "META-INF/persistence.xml";

    protected UnitDescriptorParser parser;

    public UnitLoader(boolean validateDescriptors) {
        try {
            this.parser = new UnitDescriptorParser(validateDescriptors);
        }
        catch (Exception e) {
            throw new RuntimeException("Error creating XML parser", e);
        }
    }

    /**
     * Loads PersistenceUnitInfo from standard locations. Returns null if no persistent
     * unit with requested name is found.
     * <p>
     * <i>Implementation note: the loader performs no local caching of unit data. It will
     * scan all available peristence unit descriptors every time this method is called.</i>
     * </p>
     */
    public JpaUnit loadUnit(String persistenceUnitName) {

        if (persistenceUnitName == null) {
            throw new IllegalArgumentException("Null persistenceUnitName");
        }

        // load descriptors
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            Enumeration<URL> descriptors = loader.getResources(DESCRIPTOR_LOCATION);
            while (descriptors.hasMoreElements()) {

                String descriptorURL = descriptors.nextElement().toExternalForm();

                // determine descriptor "root"

                String descriptorRoot = descriptorURL.substring(0, descriptorURL.length()
                        - DESCRIPTOR_LOCATION.length());

                Collection<JpaUnit> units = parser.getPersistenceUnits(new InputSource(
                        descriptorURL), new URL(descriptorRoot));

                for (JpaUnit unit : units) {
                    if (persistenceUnitName.equals(unit.getPersistenceUnitName())) {
                        return unit;
                    }
                }
            }
        }
        catch (Exception e) {
            // throw on bad unit
            throw new RuntimeException("Error loading persistence descriptors", e);
        }

        return null;
    }
}
