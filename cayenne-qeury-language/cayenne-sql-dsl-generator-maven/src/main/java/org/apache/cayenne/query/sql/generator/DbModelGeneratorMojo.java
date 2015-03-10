/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cayenne.query.sql.generator;

import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.configuration.XMLDataMapLoader;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.resource.URLResource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.net.MalformedURLException;

/**
 * Maven mojo to reverse engineer datamap from DB.
 *
 * @since 3.0
 *
 * @phase generate-sources
 * @goal cdbimport
 */
public class DbModelGeneratorMojo extends AbstractMojo {

    /**
     * DataMap XML file to use as a schema descriptor.
     *
     * @parameter map="map"
     * @required
     */
    private File map;

    /**
     * Destination directory for Java classes (ignoring their package names).
     *
     * @parameter destDir="destDir" default-value="${project.build.sourceDirectory}"
     */
    private File destDir;

    /**
     * Java package name of generated superclasses. Ignored unless
     * <code>makepairs</code> set to <code>true</code>. If omitted, each
     * superclass will be assigned the same package as subclass. Note that
     * having superclass in a different package would only make sense when
     * <code>usepkgpath</code> is set to <code>true</code>. Otherwise classes
     * from different packages will end up in the same directory.
     *
     * @parameter superPkg="superPkg"
     */
    private String superPkg;

    /**
     * Destination directory for Java classes (ignoring their package names).
     *
     * @parameter schemaInterface="schemaInterface"
     */
    private String schemaInterfaceName;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            XMLDataMapLoader dataMapLoader = new XMLDataMapLoader();
            DataMap dataMap = dataMapLoader.load(new URLResource(map.toURI().toURL()));

            DbModelGenerator generator = new DbModelGenerator();
            generator.generate(destDir, superPkg + "." + schemaInterfaceName, dataMap);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }


}
