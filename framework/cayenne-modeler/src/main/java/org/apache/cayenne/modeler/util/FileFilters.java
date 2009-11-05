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

package org.apache.cayenne.modeler.util;

import java.io.File;

import javax.swing.filechooser.FileFilter;

import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.project.DataMapFile;

/**
 * A collection of common file filters used by CayenneModeler JFileChoosers.
 * 
 * @since 1.1
 */
public class FileFilters {

    protected static final FileFilter applicationFilter = new ApplicationFileFilter();
    protected static final FileFilter velotemplateFilter = new VelotemplateFileFilter();
    protected static final FileFilter eomodelFilter = new EOModelFileFilter();
    protected static final FileFilter eomodelSelectFilter = new EOModelSelectFilter();
    protected static final FileFilter dataMapFilter = new DataMapFileFilter();
    protected static final FileFilter classArchiveFilter = new JavaClassArchiveFilter();

    /**
     * Returns a FileFilter for java class archive files, such as JAR and ZIP.
     */
    public static FileFilter getClassArchiveFilter() {
        return classArchiveFilter;
    }

    /**
     * Returns a FileFilter used to select Cayenne Application project files.
     */
    public static FileFilter getApplicationFilter() {
        return applicationFilter;
    }

    /**
     * Returns a FileFilter used to select DataMap files.
     */
    public static FileFilter getDataMapFilter() {
        return dataMapFilter;
    }

    /**
     * Returns a FileFilter used to select Velocity template files. 
     * Filters files with ".vm" extension.
     */
    public static FileFilter getVelotemplateFilter() {
        return velotemplateFilter;
    }

    /**
     * Returns a FileFilter used to filter EOModels. This filter will only display
     * directories and index.eomodeld files.
     */
    public static FileFilter getEOModelFilter() {
        return eomodelFilter;
    }

    /**
     * Returns FileFilter that defines the rules for EOModel selection.
     * This filter will only allow selection of the following 
     * files/directories:
     * <ul>
     *   <li>Directories with name matching <code>*.eomodeld</code>
     *   that contain <code>index.eomodeld</code>.</li>
     *   <li><code>index.eomodeld</code> files contained within 
     *   <code>*.eomodeld</code> directory.</li>
     * </ul>
     */
    public static FileFilter getEOModelSelectFilter() {
        return eomodelSelectFilter;
    }

    static final class JavaClassArchiveFilter extends FileFilter {
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }

            String name = f.getName().toLowerCase();
            return (name.length() > 4 && (name.endsWith(".jar") || name.endsWith(".zip")));
        }

        public String getDescription() {
            return "Java Class Archive (*.jar,*.zip)";
        }
    }

    static final class VelotemplateFileFilter extends FileFilter {
        /**
         * Accepts all *.vm files.
         */
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }

            String name = f.getName();
            return (name.endsWith(".vm") && !name.equals(".vm"));
        }

        public String getDescription() {
            return "Velocity Templates (*.vm)";
        }
    }

    static final class ApplicationFileFilter extends FileFilter {

        /**
         * Accepts all directories and all cayenne.xml files.
         */
        public boolean accept(File f) {
            return f.isDirectory()
                || Configuration.DEFAULT_DOMAIN_FILE.equals(f.getName());
        }

        /**
         *  Returns description of this filter.
         */
        public String getDescription() {
            return "Cayenne Applications (" + Configuration.DEFAULT_DOMAIN_FILE + ")";
        }
    }

    static final class DataMapFileFilter extends FileFilter {

        /**
         * Accepts all directories and all *.map.xml files.
         */
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }

            String name = f.getName();
            if (name.endsWith(DataMapFile.LOCATION_SUFFIX)
                && !name.equals(DataMapFile.LOCATION_SUFFIX)) {
                return true;
            }

            return false;
        }

        /**
         *  Returns description of this filter.
         */
        public String getDescription() {
            return "DataMaps (*" + DataMapFile.LOCATION_SUFFIX + ")";
        }
    }

    static final class EOModelFileFilter extends FileFilter {
        static final String EOM_SUFFIX = ".eomodeld";
        static final String EOM_INDEX = "index" + EOM_SUFFIX;

        /**
         * Accepts all directories and <code>*.eomodeld/index.eomodeld</code> files.
         */
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }

            File parent = f.getParentFile();
            return parent != null
                && parent.getName().endsWith(EOM_SUFFIX)
                && EOM_INDEX.equals(f.getName());
        }

        public String getDescription() {
            return "*" + EOM_SUFFIX;
        }
    }

    static final class EOModelSelectFilter extends FileFilter {
        /**
         * Accepts all directories and <code>*.eomodeld/index.eomodeld</code> files.
         *
         * @see EOModelSelectFilter#accept(File)
         */
        public boolean accept(File f) {
            if (f.isDirectory()) {
                if (f.getName().endsWith(EOModelFileFilter.EOM_SUFFIX)
                    && new File(f, EOModelFileFilter.EOM_INDEX).exists()) {

                    return true;
                }
            }
            else if (f.isFile()) {
                File parent = f.getParentFile();
                if (parent != null
                    && parent.getName().endsWith(EOModelFileFilter.EOM_SUFFIX)
                    && EOModelFileFilter.EOM_INDEX.equals(f.getName())) {
                    return true;
                }
            }

            return false;
        }

        public String getDescription() {
            return "*" + EOModelFileFilter.EOM_SUFFIX;
        }
    }
}
