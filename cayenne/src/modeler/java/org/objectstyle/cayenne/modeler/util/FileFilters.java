/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.modeler.util;

import java.io.File;

import javax.swing.filechooser.FileFilter;

import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.project.DataMapFile;

/**
 * A collection of common file filters used by CayenneModeler JFileChoosers.
 * 
 * @since 1.1
 * @author Andrei Adamchik
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
