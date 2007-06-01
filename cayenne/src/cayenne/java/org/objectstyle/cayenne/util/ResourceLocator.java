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
package org.objectstyle.cayenne.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.Predicate;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.objectstyle.cayenne.conf.Configuration;

/** 
  * Utility class to find resources. (Resources are usually files).
  * Lookup is done using a preconfigured strategy.
  * 
  * @author Andrei Adamchik
  */
public class ResourceLocator {
	private static Logger logObj;

	// Create a Predicate that will enable logging only when
	// Configuration.isLoggingConfigured() returns true.
	// The passed predicate argument is ignored.
	static {
		Predicate p = new Predicate() {
			public boolean evaluate(Object o) {
				return Configuration.isLoggingConfigured();
			}
		};

		logObj = new PredicateLogger(ResourceLocator.class, p);
	}

	/** properties for enabling/disabling certain lookup strategies */
	protected boolean skipAbsolutePath;
	protected boolean skipClasspath;
	protected boolean skipCurrentDirectory;
	protected boolean skipHomeDirectory;

	/** additional lookup paths (as Strings) */
	protected List additionalClassPaths;
	protected List additionalFilesystemPaths;

	/** ClassLoader used for resource loading */
	protected ClassLoader classLoader;

	/**
	 * Returns a resource as InputStream if it is found in CLASSPATH or
	 * <code>null</code> otherwise. Lookup is normally performed
	 * in all JAR and ZIP files and directories available to the ClassLoader.
	 */
	public static InputStream findResourceInClasspath(String name) {
		try {
			URL url = findURLInClasspath(name);
			if (url != null) {
				logObj.debug("resource found in classpath: " + url);
				return url.openStream();
			} else {
				logObj.debug("resource not found in classpath: " + name);
				return null;
			}
		} catch (IOException ioex) {
			return null;
		}
	}

	/**
	 * Returns a resource as InputStream if it is found in the filesystem or
		 * <code>null</code> otherwise. Lookup is first performed relative
		 * to the user's home directory (as defined by "user.home" system property),
		 * and then relative to the current directory.
		 */
	public static InputStream findResourceInFileSystem(String name) {
		try {
			File file = findFileInFileSystem(name);
			if (file != null) {
				logObj.debug("resource found in file system: " + file);
				return new FileInputStream(file);
			} else {
				logObj.debug("resource not found in file system: " + name);
				return null;
			}
		} catch (IOException ioex) {
			return null;
		}
	}

	/**
	 *  Looks up a file in the filesystem. 
	 *  First looks in the user home directory, then in the current directory.
	 *  
	 *  @return file object matching the name, or null if file can not be found
	 *  or if it is not readable.
	 * 
	 *  @see #findFileInHomeDirectory(String)
	 *  @see #findFileInCurrentDirectory(String)
	 */
	public static File findFileInFileSystem(String name) {
		File file = findFileInHomeDirectory(name);

		if (file == null) {
			file = findFileInCurrentDirectory(name);
		}

		if (file != null) {
			logObj.debug("file found in file system: " + file);
		} else {
			logObj.debug("file not found in file system: " + name);
		}

		return file;
	}

	/**
	 * Looks up a file in the user home directory.
	 *  
	 * @return file object matching the name, or <code>null</code>
	 * if <code>file</code> cannot be found or is not readable.
	 */
	public static File findFileInHomeDirectory(String name) {
		// look in home directory
		String homeDirPath =
			System.getProperty("user.home") + File.separator + name;

		try {

			File file = new File(homeDirPath);
			if (file.exists() && file.canRead()) {
				logObj.debug("file found in home directory: " + file);
			} else {
				file = null;
				logObj.debug("file not found in home directory: " + name);
			}

			return file;
		} catch (SecurityException se) {
			logObj.debug("permission denied reading file: " + homeDirPath, se);
			return null;
		}
	}

	/**
	 * Looks up a file in the current directory.
	 *  
	 * @return file object matching the name, or <code>null</code>
	 * if <code>file</code> can not be found is not readable.
	 */
	public static File findFileInCurrentDirectory(String name) {
		// look in the current directory
		String currentDirPath =
			System.getProperty("user.dir") + File.separator + name;

		try {

			File file = new File(currentDirPath);

			if (file.exists() && file.canRead()) {
				logObj.debug("file found in current directory: " + file);
			} else {
				logObj.debug("file not found in current directory: " + name);
				file = null;
			}

			return file;
		} catch (SecurityException se) {
			logObj.debug(
				"permission denied reading file: " + currentDirPath,
				se);
			return null;
		}
	}

	/**
	 * Looks up the URL for the named resource using this class' ClassLoader.
	 */
	public static URL findURLInClasspath(String name) {
		ClassLoader classLoader = ResourceLocator.class.getClassLoader();
		if (classLoader == null) {
			classLoader = ClassLoader.getSystemClassLoader();
		}
		return findURLInClassLoader(name, classLoader);
	}

	/**
	 * Looks up the URL for the named resource using the specified ClassLoader.
	 */
	public static URL findURLInClassLoader(String name, ClassLoader loader) {
		URL url = loader.getResource(name);

		if (url != null) {
			logObj.debug("URL found with classloader: " + url);
		} else {
			logObj.debug("URL not found with classloader: " + name);
		}

		return url;
	}

	/** 
	 * Returns a base URL as a String from which this class was loaded.
	 * This is normally a JAR or a file URL, but it is ClassLoader dependent.
	 */
	public static String classBaseUrl(Class aClass) {
		String pathToClass = aClass.getName().replace('.', '/') + ".class";
		ClassLoader classLoader = aClass.getClassLoader();
		
		if (classLoader == null) {
		   classLoader = ClassLoader.getSystemClassLoader();
	    }
	    
		URL selfUrl = classLoader.getResource(pathToClass);
		
		if (selfUrl == null) {
			return null;
		}

		String urlString = selfUrl.toExternalForm();
		return urlString.substring(
			0,
			urlString.length() - pathToClass.length());
	}

	/**
	 * Creates new ResourceLocator with default lookup policy including
	 * user home directory, current directory and CLASSPATH.
	 */
	public ResourceLocator() {
		super();
		this.setClassLoader(this.getClass().getClassLoader());
		this.additionalClassPaths = new ArrayList();
		this.additionalFilesystemPaths = new ArrayList();
	}

	/**
	 * Returns an InputStream on the found resource using the lookup strategy
	 * configured for this ResourceLocator or <code>null</code> if no
	 * readable resource can be found for the given name.
	 */
	public InputStream findResourceStream(String name) {
		URL url = findResource(name);
		if (url == null) {
			return null;
		}

		try {
			return url.openStream();
		} catch (IOException ioex) {
			logObj.debug("Error reading URL, ignoring", ioex);
			return null;
		}
	}

	/** 
	 * Returns a resource URL using the lookup strategy configured for this
	 * Resourcelocator or <code>null</code> if no readable resource can be
	 * found for the given name. 
	 */
	public URL findResource(String name) {
		if (!willSkipAbsolutePath()) {
			File f = new File(name);
			if (f.isAbsolute() && f.exists()) {
				logObj.debug("File found at absolute path: " + name);
				try {
					return f.toURL();
				} catch (MalformedURLException ex) {
					// ignoring
					logObj.debug("Malformed url, ignoring.", ex);
				}
			}
			else {
				logObj.debug("No file at absolute path: " + name);
			}
		}

		if (!willSkipHomeDirectory()) {
			File f = findFileInHomeDirectory(name);
			if (f != null) {

				try {
					return f.toURL();
				} catch (MalformedURLException ex) {
					// ignoring
					logObj.debug("Malformed url, ignoring", ex);
				}
			}
		}

		if (!willSkipCurrentDirectory()) {
			File f = findFileInCurrentDirectory(name);
			if (f != null) {

				try {
					return f.toURL();
				} catch (MalformedURLException ex) {
					// ignoring
					logObj.debug("Malformed url, ignoring", ex);
				}
			}
		}

		if (!additionalFilesystemPaths.isEmpty()) {
			logObj.debug(
				"searching additional paths: "
					+ this.additionalFilesystemPaths);
			Iterator pi = this.additionalFilesystemPaths.iterator();
			while (pi.hasNext()) {
				File f = new File((String) pi.next(), name);
				logObj.debug("searching for: " + f.getAbsolutePath());
				if (f.exists()) {
					try {
						return f.toURL();
					} catch (MalformedURLException ex) {
						// ignoring
						logObj.debug("Malformed URL, ignoring.", ex);
					}
				}
			}
		}

		if (!willSkipClasspath()) {
			URL url = findURLInClassLoader(name, classLoader);
			if (url != null) {
				return url;
			}

			if (!this.additionalClassPaths.isEmpty()) {
				logObj.debug(
					"searching additional classpaths: "
						+ this.additionalClassPaths);
				Iterator cpi = this.additionalClassPaths.iterator();
				while (cpi.hasNext()) {
					String fullName = cpi.next() + "/" + name;
					logObj.debug("searching for: " + fullName);
					url = findURLInClassLoader(fullName, classLoader);
					if (url != null) {
						return url;
					}
				}
			}
		}

		return null;
	}

	/**
	 * Returns a directory resource URL using the lookup strategy configured for
	 * this ResourceLocator or <code>null</code> if no readable resource can be
	 * found for the given name. The returned resource is assumed to be a directory,
	 * so the returned URL will be in a directory format (with "/" at the end).
	 */
	public URL findDirectoryResource(String name) {
		URL url = findResource(name);
		if (url == null) {
			return null;
		}

		try {
			String urlSt = url.toExternalForm();
			return (urlSt.endsWith("/")) ? url : new URL(urlSt + "/");
		} catch (MalformedURLException ex) {
			// ignoring...
			logObj.debug("Malformed URL, ignoring.", ex);
			return null;
		}
	}

	/**
	 * Returns true if no lookups are performed in the user home directory.
	 */
	public boolean willSkipHomeDirectory() {
		return skipHomeDirectory;
	}

	/**
	 * Sets "skipHomeDirectory" property.
	 */
	public void setSkipHomeDirectory(boolean skipHomeDir) {
		this.skipHomeDirectory = skipHomeDir;
	}

	/**
	 * Returns true if no lookups are performed in the current directory.
	 */
	public boolean willSkipCurrentDirectory() {
		return skipCurrentDirectory;
	}

	/**
	 * Sets "skipCurrentDirectory" property.
	 */
	public void setSkipCurrentDirectory(boolean skipCurDir) {
		this.skipCurrentDirectory = skipCurDir;
	}

	/**
	 * Returns true if no lookups are performed in the classpath.
	 */
	public boolean willSkipClasspath() {
		return skipClasspath;
	}

	/**
	 * Sets "skipClasspath" property.
	 */
	public void setSkipClasspath(boolean skipClasspath) {
		this.skipClasspath = skipClasspath;
	}

	/**
	 * Returns the ClassLoader associated with this ResourceLocator.
	 */
	public ClassLoader getClassLoader() {
		return classLoader;
	}

	/** 
	 * Sets ClassLoader used to locate resources. If <code>null</code> is
	 * passed, the ClassLoader of the ResourceLocator class will be used.
	 */
	public void setClassLoader(ClassLoader classLoader) {
		if (classLoader == null) {
			classLoader = this.getClass().getClassLoader();
			if (classLoader == null) {
				classLoader = ClassLoader.getSystemClassLoader();
			}
		}

		this.classLoader = classLoader;
	}

	/**
	 * Returns true if no lookups are performed using path as absolute path.
	 */
	public boolean willSkipAbsolutePath() {
		return skipAbsolutePath;
	}

	/**
	 * Sets "skipAbsolutePath" property.
	 */
	public void setSkipAbsolutePath(boolean skipAbsPath) {
		this.skipAbsolutePath = skipAbsPath;
	}

	/**
	 * Adds a custom path for class path lookups.
	 * Format should be "my/package/name" <i>without</i> leading "/".
	 */
	public void addClassPath(String customPath) {
		this.additionalClassPaths.add(customPath);
	}

	/**
	 * Adds the given String as a custom path for filesystem lookups.
	 * The path can be relative or absolute and is <i>not</i> checked
	 * for existence.
	 * 
	 * @throws IllegalArgumentException if <code>path</code> is <code>null</code>.
	 */
	public void addFilesystemPath(String path) {
		if (path != null) {
			this.additionalFilesystemPaths.add(path);
		} else {
			throw new IllegalArgumentException("Path must not be null.");
		}
	}

	/**
	 * Adds the given directory as a path for filesystem lookups.
	 * The directory is checked for existence.
	 * 
	 * @throws IllegalArgumentException if <code>path</code> is <code>null</code>,
	 * not a directory or not readable.
	 */
	public void addFilesystemPath(File path) {
		if (path != null && path.isDirectory()) {
			this.addFilesystemPath(path.getPath());
		} else {
			throw new IllegalArgumentException(
				"Path '" + path + "' is not a directory.");
		}
	}

	/**
	 * Custom logger that can be dynamically turned on/off by evaluating a Predicate.
	 */
	protected static class PredicateLogger extends Logger {
		private Logger _target;
		private Predicate _predicate;

		private PredicateLogger(String name) {
			super(name);
		}

		public PredicateLogger(Class clazz, Predicate condition) {
			this(clazz.getName(), condition);
		}

		public PredicateLogger(String name, Predicate condition) {
			this(name);
			_target = Logger.getLogger(name);
			_predicate = condition;
		}

		public void debug(Object arg0, Throwable arg1) {
			this.log(Level.DEBUG, arg0, arg1);
		}

		public void debug(Object arg0) {
			this.log(Level.DEBUG, arg0);
		}

		public void info(Object arg0, Throwable arg1) {
			this.log(Level.INFO, arg0, arg1);
		}

		public void info(Object arg0) {
			this.log(Level.INFO, arg0);
		}

		public void warn(Object arg0, Throwable arg1) {
			this.log(Level.WARN, arg0, arg1);
		}

		public void warn(Object arg0) {
			this.log(Level.WARN, arg0);
		}

		public void error(Object arg0, Throwable arg1) {
			this.log(Level.ERROR, arg0, arg1);
		}

		public void error(Object arg0) {
			this.log(Level.ERROR, arg0);
		}

		public void fatal(Object arg0, Throwable arg1) {
			this.log(Level.FATAL, arg0, arg1);
		}

		public void fatal(Object arg0) {
			this.log(Level.FATAL, arg0);
		}

		public void log(Priority arg0, Object arg1, Throwable arg2) {
			if (_predicate.evaluate(arg1)) {
				_target.log(arg0, arg1);
			}
		}

		public void log(Priority arg0, Object arg1) {
			if (_predicate.evaluate(arg1)) {
				_target.log(arg0, arg1);
			}
		}

	}

}
