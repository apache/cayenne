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
package org.objectstyle.cayenne.gen;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.loader.FileResourceLoader;

/**
 * Special resource loader that allows loading files
 * using absolute path and current directory.
 * 
 * @author Andrei Adamchik
 */
public class AbsFileResourceLoader extends FileResourceLoader {

	/**
	 * Constructor for AbsFileResourceLoader.
	 */
	public AbsFileResourceLoader() {
		super();
	}

	/**
	 * Returns resource as InputStream.
	 * First calls super implementation. If resource wasn't found, 
	 * it attempts to load it from current directory or as an absolute path.
	 * 
	 * @see org.apache.velocity.runtime.resource.loader.ResourceLoader#getResourceStream(String)
	 */
	public synchronized InputStream getResourceStream(String name)
		throws ResourceNotFoundException {

		// attempt to load using default configuration
		try {
			return super.getResourceStream(name);
		} catch (ResourceNotFoundException rnfex) {
			// attempt to load from current directory or as an absolute path
			try {
				File file = new File(name);
				return (file.canRead())
					? new BufferedInputStream(
						new FileInputStream(file.getAbsolutePath()))
					: null;

			} catch (FileNotFoundException fnfe) {
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
