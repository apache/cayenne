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

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import javax.persistence.spi.PersistenceUnitInfo;

import org.apache.cayenne.jpa.JpaProviderException;
import org.apache.cayenne.jpa.map.JpaClassDescriptor;
import org.apache.cayenne.jpa.map.JpaEntity;
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

    public EntityMapLoaderContext getContext() {
        return context;
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
            updateInheritanceHierarchy();
            updateFromDefaults();
        }
        catch (JpaProviderException e) {
            throw e;
        }
        catch (Exception e) {
            throw new JpaProviderException("Error loading ORM descriptors", e);
        }
    }

    protected void updateInheritanceHierarchy() {

        JpaEntityMap map = getEntityMap();

        for (JpaEntity entity : map.getEntities()) {

            Class<?> superclass = entity
                    .getClassDescriptor()
                    .getManagedClass()
                    .getSuperclass();

            while (superclass != null && !superclass.getName().equals("java.lang.Object")) {

                JpaEntity superEntity = map.getEntity(superclass.getName());
                if (superEntity != null) {
                    entity.setSuperEntity(superEntity);
                    break;
                }

                superclass = superclass.getSuperclass();
            }
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

        Set<String> loadedLocations = new HashSet<String>();
        EntityMapXMLLoader loader = new EntityMapXMLLoader(
                context.getTempClassLoader(),
                false);

        // 1. load from the standard file called orm.xml
        loadedLocations.add(DESCRIPTOR_LOCATION);
        Enumeration<URL> standardDescriptors = context.getTempClassLoader().getResources(
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

                Enumeration<URL> mappedDescriptors = context
                        .getTempClassLoader()
                        .getResources(descriptor);
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

        Map<String, Class<?>> managedClassMap = new HashMap<String, Class<?>>();

        // must use Unit class loader to prevent loading an un-enahnced class in the
        // app ClassLoader
        ClassLoader loader = context.getTempClassLoader();

        // load explicitly mentioned classes
        Collection<String> explicitClasses = persistenceUnit.getManagedClassNames();
        if (explicitClasses != null) {
            for (String className : explicitClasses) {

                Class<?> managedClass;
                try {
                    managedClass = Class.forName(className, true, loader);
                }
                catch (ClassNotFoundException e) {
                    throw new JpaProviderException("Class not found: " + className, e);
                }

                managedClassMap.put(className, managedClass);
            }
        }

        // now detect potential managed classes from PU root and extra jars
        if (!persistenceUnit.excludeUnlistedClasses()) {
            Collection<String> implicitClasses = listImplicitClasses(persistenceUnit);
            for (String className : implicitClasses) {

                if (managedClassMap.containsKey(className)) {
                    continue;
                }

                Class<?> managedClass;
                try {
                    managedClass = Class.forName(className, true, loader);
                }
                catch (ClassNotFoundException e) {
                    throw new JpaProviderException("Class not found: " + className, e);
                }

                if (managedClass.getAnnotation(Entity.class) != null
                        || managedClass.getAnnotation(MappedSuperclass.class) != null
                        || managedClass.getAnnotation(Embeddable.class) != null) {
                    managedClassMap.put(className, managedClass);
                }
            }
        }

        if (!managedClassMap.isEmpty()) {

            EntityMapAnnotationLoader annotationLoader = new EntityMapAnnotationLoader(
                    context);
            for (Class<?> managedClass : managedClassMap.values()) {
                annotationLoader.loadClassMapping(managedClass);
            }
        }
    }

    /**
     * Returns a collection of all classes that are located in the unit root and unit
     * extra jars.
     */
    protected Collection<String> listImplicitClasses(PersistenceUnitInfo persistenceUnit) {

        Collection<String> classes = new ArrayList<String>();
        URL rootURL = persistenceUnit.getPersistenceUnitRootUrl();
        if (rootURL != null) {
            if ("file".equals(rootURL.getProtocol())) {
                locateClassesInFolder(rootURL, classes);
            }
            else {
                locateClassesInJar(rootURL, classes);
            }
        }

        for (URL jarURL : persistenceUnit.getJarFileUrls()) {
            if (jarURL != null) {
                // that's unlikely ... but we can handle it just in case...
                if ("file".equals(jarURL.getProtocol())) {
                    locateClassesInFolder(jarURL, classes);
                }
                else {
                    locateClassesInJar(jarURL, classes);
                }
            }
        }

        return classes;
    }

    private void locateClassesInFolder(URL dirURL, Collection<String> classes) {
        File root;
        try {
            root = new File(dirURL.toURI());
        }
        catch (URISyntaxException e) {
            throw new JpaProviderException("Error converting url to file: " + dirURL, e);
        }

        locateClassesInFolder(root, root.getAbsolutePath().length() + 1, classes);
    }

    private void locateClassesInFolder(
            File folder,
            int rootPathLength,
            Collection<String> classes) {

        File[] files = folder.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    locateClassesInFolder(files[i], rootPathLength, classes);
                }
                else {
                    String name = files[i].getName();
                    if (name.endsWith(".class")) {

                        int suffixLen = ".class".length();

                        String absPath = files[i].getAbsolutePath();
                        if (absPath.length() > rootPathLength + suffixLen) {
                            classes.add(absPath.substring(
                                    rootPathLength,
                                    absPath.length() - suffixLen).replace('/', '.'));
                        }
                    }
                }
            }
        }
    }

    private void locateClassesInJar(URL jarURL, Collection<String> classes) {

        try {
            JarURLConnection connection = (JarURLConnection) jarURL.openConnection();
            JarFile jar = connection.getJarFile();
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry e = entries.nextElement();
                if (e.isDirectory()) {
                    continue;
                }

                String name = e.getName();
                if (name.endsWith(".class")) {
                    int suffixLen = ".class".length();
                    classes.add(name.substring(0, name.length() - suffixLen).replace(
                            '/',
                            '.'));
                }
            }
        }
        catch (Exception e) {
            throw new JpaProviderException("Error reading jar contents: " + jarURL, e);
        }
    }
}
