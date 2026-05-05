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

package org.apache.cayenne.modeler.toolkit.filechooser;

import javax.swing.filechooser.FileFilter;
import java.io.File;

/**
 * A collection of common file filters used by CayenneModeler JFileChoosers.
 */
public class FileFilters {

    protected static final FileFilter applicationFilter = new ApplicationFileFilter();
    protected static final FileFilter dataMapFilter = new DataMapFileFilter();

    private static final String DATA_MAP_LOCATION_SUFFIX = ".map.xml";

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
     * Returns filter that checks if file has specified extension
     */
    public static FileFilter getExtensionFileFilter(String ext, String description) {
        return new ExtensionFileFilter(ext, description);
    }

    static final class ApplicationFileFilter extends FileFilter {

        /**
         * Accepts all directories and all cayenne.xml files.
         */
        public boolean accept(File f) {
            return f.isDirectory()
                    || (f.getName().startsWith("cayenne") && f.getName().endsWith(".xml"));
        }

        /**
         * Returns description of this filter.
         */
        public String getDescription() {
            return "Cayenne Applications (" + "cayenne*.xml" + ")";
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
            if (name.endsWith(DATA_MAP_LOCATION_SUFFIX)
                    && !name.equals(DATA_MAP_LOCATION_SUFFIX)) {
                return true;
            }

            return false;
        }

        /**
         * Returns description of this filter.
         */
        public String getDescription() {
            return "DataMaps (*" + DATA_MAP_LOCATION_SUFFIX + ")";
        }
    }

    /**
     * filter that checks if file has specified extension
     */
    static final class ExtensionFileFilter extends FileFilter {

        String ext;
        String description;

        ExtensionFileFilter(String ext, String description) {
            this.ext = ext;
            this.description = description;
        }

        @Override
        public boolean accept(File f) {
            return f.isDirectory() || f.getName().endsWith("." + ext);
        }

        @Override
        public String getDescription() {
            return description + "(." + ext + ")";
        }

    }
}
