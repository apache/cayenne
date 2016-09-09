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
package org.apache.cayenne.test.resource;

import static org.junit.Assert.assertNotNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class ResourceUtil {

	/**
	 * Copies resources to a file, thus making it available to the caller as
	 * File.
	 */
	public static void copyResourceToFile(String resourceName, File file) {
		URL in = getResource(resourceName);

		if (!copyResourceToFile(in, file)) {
			throw new RuntimeException("Error copying resource to file : " + file);
		}
	}

	/**
	 * Returns a guaranteed non-null resource for a given name.
	 */
	public static URL getResource(Class<?> relativeTo, String name) {
		URL in = relativeTo.getResource(name);
		assertNotNull("Resource not found: " + name, in);
		return getResource(in);
	}

	/**
	 * Returns a guaranteed non-null resource for a given name.
	 */
	public static URL getResource(String name) {
		URL in = Thread.currentThread().getContextClassLoader().getResource(name);
		assertNotNull("Resource not found: " + name, in);
		return getResource(in);
	}

	/**
	 * Returns a guaranteed non-null resource for a given name.
	 */
	private static URL getResource(URL classloaderUrl) {

		if (classloaderUrl == null) {
			throw new NullPointerException("null URL");
		}

		// Fix for the issue described at
		// https://issues.apache.org/struts/browse/SB-35
		// Basically, spaces in filenames make maven cry.
		try {
			return new URL(classloaderUrl.toExternalForm().replaceAll(" ", "%20"));
		} catch (MalformedURLException e) {
			throw new RuntimeException("Error constructing URL.", e);
		}
	}

	public static boolean copyResourceToFile(URL from, File to) {
		int bufSize = 8 * 1024;
		try (BufferedInputStream urlin = new BufferedInputStream(from.openConnection().getInputStream(), bufSize);) {

			try (BufferedOutputStream fout = new BufferedOutputStream(new FileOutputStream(to), bufSize);) {
				copyPipe(urlin, fout, bufSize);
			}
		} catch (IOException ioex) {
			return false;
		} catch (SecurityException sx) {
			return false;
		}
		return true;
	}

	private static void copyPipe(InputStream in, OutputStream out, int bufSizeHint) throws IOException {
		int read = -1;
		byte[] buf = new byte[bufSizeHint];
		while ((read = in.read(buf, 0, bufSizeHint)) >= 0) {
			out.write(buf, 0, read);
		}
		out.flush();
	}

}
