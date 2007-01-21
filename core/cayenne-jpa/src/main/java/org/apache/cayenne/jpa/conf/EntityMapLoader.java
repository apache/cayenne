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

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.spi.PersistenceUnitInfo;

import org.apache.cayenne.jpa.JpaProviderException;
import org.apache.cayenne.jpa.map.JpaClassDescriptor;
import org.apache.cayenne.jpa.map.JpaEntityMap;

/**
 * Loads JPA mapping information from all sources per JPA specification.
 * <h3>Specification Documentation, Chapter 6.2.1.6</h3>
 * <p>
 * The set of managed persistence classes that are managed by a persistence unit is
 * defined by using one or more of the following:
 * <ul>
 * <li>One or more object/relational mapping XML files
 * <li>One or more jar files that will be searched for classes
 * <li>An explicit list of the classes
 * <li>The annotated managed persistence classes contained in the root of the persistence
 * unit (unless the exclude-unlisted-classes element is specified) [Java SE doesn't have
 * to support that].
 * </ul>
 * <p>
 * The result is undefined if multiple mapping files (including any orm.xml file)
 * referenced within a single persistence unit contain overlapping mapping information for
 * any given class.
 * </p>
 * <p>
 * The resulting set of entities managed by the persistence unit [and contained in the
 * returned entity map] is the union of these sources, with the mapping metadata
 * annotations (or annotation defaults) for any given class being overridden by the XML
 * mapping information file if there are both annotations as well as XML mappings for that
 * class. The minimum portable level of overriding is at the level of the persistent field
 * or property.
 * </p>
 * 
 * @author Andrus Adamchik
 */
public class EntityMapLoader {

    static final String DESCRIPTOR_LOCATION = "META-INF/orm.xml";

    protected EntityMapLoaderContext context;
    protected Map<String, JpaClassDescriptor> descriptors;

    /**
     * Creates an EntityMapLoader for the persistence unit, merging entity information
     * from all locations supported by the JPA specification.
     */
    public EntityMapLoader(PersistenceUnitInfo persistenceUnit) {
        loadEntityMap(persistenceUnit);
    }

    /**
     * Returns an entity map with entity
     */
    public JpaEntityMap getEntityMap() {
        return context.getEntityMap();
    }

    /**
     * Loads a combined entity map including managed class descriptors from all supported
     * locations.
     */
    protected void loadEntityMap(PersistenceUnitInfo persistenceUnit)
            throws JpaProviderException {

        this.context = new EntityMapLoaderContext(persistenceUnit);

        try {
            loadFromAnnotations(persistenceUnit);
            updateFromXML(persistenceUnit);
            updateFromDefaults();
        }
        catch (JpaProviderException e) {
            throw e;
        }
        catch (Exception e) {
            throw new JpaProviderException("Error loading ORM descriptors", e);
        }
    }

    /**
     * Updates missing values with spec-compilant defaults.
     */
    protected void updateFromDefaults() {
        new EntityMapDefaultsProcessor().applyDefaults(context);
    }

    /**
     * Updates mapping with data loaded from XML. JPA specification gives some leeway in
     * processing conflicting mapping files. Cayenne provider strategy is "last mapping
     * file overrides all".
     * <h3>Specification Documentation, Chapter 6.2.1.6</h3>
     * <p>
     * An <em>orm.xml</em> file may be specified in the META-INF directory in the root
     * of the persistence unit or in the META-INF directory of any jar file referenced by
     * the persistence.xml. Alternatively, or in addition, other mapping files maybe
     * referenced by the mapping-file elements of the persistence-unit element, and may be
     * present anywhere on the classpath. An orm.xml file or other mapping file is loaded
     * as a resource by the persistence provider.
     */
    protected void updateFromXML(PersistenceUnitInfo unit) throws IOException {

        EntityMapMergeProcessor merger = new EntityMapMergeProcessor(context);

        Set loadedLocations = new HashSet();
        EntityMapXMLLoader loader = new EntityMapXMLLoader(unit.getClassLoader(), false);

        // 1. load from the standard file called orm.xml
        loadedLocations.add(DESCRIPTOR_LOCATION);
        Enumeration<URL> standardDescriptors = unit.getClassLoader().getResources(
                DESCRIPTOR_LOCATION);

        while (standardDescriptors.hasMoreElements()) {
            JpaEntityMap map = loader.getEntityMap(standardDescriptors.nextElement());
            merger.mergeOverride(map);
        }

        // 2. load from orm.xml within the jars
        // TODO: andrus, 4/20/2006 - load from the jar files

        // 3. load from explicitly specified descriptors
        for (String descriptor : unit.getMappingFileNames()) {

            // avoid loading duplicates, such as META-INF/orm.xml that is also explicitly
            // mentioned in the unit...
            if (loadedLocations.add(descriptor)) {

                Enumeration<URL> mappedDescriptors = unit.getClassLoader().getResources(
                        descriptor);
                while (mappedDescriptors.hasMoreElements()) {
                    JpaEntityMap map = loader.getEntityMap(mappedDescriptors
                            .nextElement());
                    merger.mergeOverride(map);
                }
            }
        }
    }

    /**
     * Loads JpaEntityMap based on metadata annotations of persistent classes.
     */
    protected void loadFromAnnotations(PersistenceUnitInfo persistenceUnit) {

        if (!persistenceUnit.getManagedClassNames().isEmpty()) {

            // must use Unit class loader to prevent loading an un-enahnced class in the
            // app ClassLoader.
            ClassLoader loader = persistenceUnit.getClassLoader();
            EntityMapAnnotationLoader annotationLoader = new EntityMapAnnotationLoader(
                    context);

            for (String className : persistenceUnit.getManagedClassNames()) {

                Class managedClass;
                try {
                    managedClass = Class.forName(className, true, loader);
                }
                catch (ClassNotFoundException e) {
                    throw new JpaProviderException("Class not found: " + className, e);
                }

                annotationLoader.loadClassMapping(managedClass);
            }
        }
    }

    public EntityMapLoaderContext getContext() {
        return context;
    }
}
