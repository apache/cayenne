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

package org.apache.cayenne.gen;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.loader.FileResourceLoader;

/**
 * Special Velocity resource loader that allows loading files using absolute path and
 * current directory.
 * 
 * @author Andrus Adamchik
 * @deprecated since 1.2 as
 *             {@link org.apache.cayenne.gen.ClassGeneratorResourceLoader} superceeds
 *             this class.
 */
public class AbsFileResourceLoader extends FileResourceLoader {

    /**
     * Constructor for AbsFileResourceLoader.
     */
    public AbsFileResourceLoader() {
        super();
    }

    /**
     * Returns resource as InputStream. First calls super implementation. If resource
     * wasn't found, it attempts to load it from current directory or as an absolute path.
     * 
     * @see org.apache.velocity.runtime.resource.loader.ResourceLoader#getResourceStream(String)
     */
    public synchronized InputStream getResourceStream(String name)
            throws ResourceNotFoundException {

        // attempt to load using default configuration
        try {
            return super.getResourceStream(name);
        }
        catch (ResourceNotFoundException rnfex) {
            // attempt to load from current directory or as an absolute path
            try {
                File file = new File(name);
                return (file.canRead()) ? new BufferedInputStream(new FileInputStream(
                        file.getAbsolutePath())) : null;

            }
            catch (FileNotFoundException fnfe) {
                throw new ResourceNotFoundException(
                        "AbsFileResourceLoader Error: cannot find resource " + name);
            }
        }
    }

    /**
     * @see org.apache.velocity.runtime.resource.loader.ResourceLoader#init(ExtendedProperties)
     */
    public void init(ExtendedProperties arg0) {
        rsvc.info("AbsFileResourceLoader : initialization starting.");
        super.init(arg0);
    }

}
