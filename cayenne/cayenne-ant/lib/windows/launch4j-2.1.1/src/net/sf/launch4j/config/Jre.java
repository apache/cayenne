/*
 	launch4j :: Cross-platform Java application wrapper for creating Windows native executables
 	Copyright (C) 2005 Grzegorz Kowal

	This program is free software; you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation; either version 2 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program; if not, write to the Free Software
	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

/*
 * Created on Apr 21, 2005
 */
package net.sf.launch4j.config;

import net.sf.launch4j.binding.IValidatable;
import net.sf.launch4j.binding.Validator;

/**
 * @author Copyright (C) 2005 Grzegorz Kowal
 */
public class Jre implements IValidatable {
	public static final String VERSION_PATTERN = "(\\d\\.){2}\\d(_\\d+)?"; //$NON-NLS-1$
	public static final String PATH = "jrepath"; //$NON-NLS-1$
	public static final String MIN_VERSION = "javamin"; //$NON-NLS-1$
	public static final String MAX_VERSION = "javamax"; //$NON-NLS-1$
	public static final String ARGS = "jvmArgs"; //$NON-NLS-1$

	private String path;
	private String minVersion;
	private String maxVersion;
	private int initialHeapSize;
	private int maxHeapSize;
	private String args;

	public void checkInvariants() {
		Validator.checkOptString(minVersion, 10, VERSION_PATTERN,
				"jre.minVersion", Messages.getString("Jre.min.version")); //$NON-NLS-1$ //$NON-NLS-2$
		Validator.checkOptString(maxVersion, 10, VERSION_PATTERN,
				"jre.maxVersion", Messages.getString("Jre.max.version")); //$NON-NLS-1$ //$NON-NLS-2$
		if (Validator.isEmpty(path)) {
			Validator.checkFalse(Validator.isEmpty(minVersion),
					"jre.path", Messages.getString("Jre.specify.jre.path.min.version")); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			Validator.checkRelativeWinPath(path,
					"jre.path", Messages.getString("Jre.embedded.path.rel")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (!Validator.isEmpty(maxVersion)) {
			Validator.checkFalse(Validator.isEmpty(minVersion),
					"jre.minVersion", Messages.getString("Jre.specify.min.version")); //$NON-NLS-1$ //$NON-NLS-2$
			Validator.checkTrue(minVersion.compareTo(maxVersion) < 0,
					"jre.maxVersion", Messages.getString("Jre.max.greater.than.min")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		Validator.checkTrue(initialHeapSize >= 0, "jre.initialHeapSize", //$NON-NLS-1$
				Messages.getString("Jre.initial.heap")); //$NON-NLS-1$
		Validator.checkTrue(maxHeapSize == 0 || initialHeapSize <= maxHeapSize,
				"jre.maxHeapSize", Messages.getString("Jre.max.heap")); //$NON-NLS-1$ //$NON-NLS-2$
		Validator.checkOptString(args, 512, "jre.args", Messages.getString("Jre.jvm.args")); //$NON-NLS-1$ //$NON-NLS-2$
		if (!Validator.isEmpty(args)) {
			int quotes = 0;
			for (int i = 0; i < args.length(); i++) {
				if (args.charAt(i) == '"') {
					quotes++;
				}
			}
			if (quotes % 2 > 0) {
				Validator.signalViolation("jre.args",
						Messages.getString("Jre.jvm.args.unclosed.quotation"));
			}
			Validator.checkTrue(args.matches("[^%]*|([^%]*\"[^%\"]*%[^%\"]*%[^%\"]*\"[^%]*)*"),
				"jre.args", Messages.getString("Jre.jvm.args.variable"));
		}
	}

	/** JVM arguments: XOptions, system properties. */
	public String getArgs() {
		return args;
	}

	public void setArgs(String args) {
		this.args = args;
	}

	/** Max Java version (x.x.x) */
	public String getMaxVersion() {
		return maxVersion;
	}

	public void setMaxVersion(String maxVersion) {
		this.maxVersion = maxVersion;
	}

	/** Min Java version (x.x.x) */
	public String getMinVersion() {
		return minVersion;
	}

	public void setMinVersion(String minVersion) {
		this.minVersion = minVersion;
	}

	/** JRE path */
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	/** Initial heap size in MB */
	public int getInitialHeapSize() {
		return initialHeapSize;
	}

	public void setInitialHeapSize(int initialHeapSize) {
		this.initialHeapSize = initialHeapSize;
	}

	/** Max heap size in MB */
	public int getMaxHeapSize() {
		return maxHeapSize;
	}

	public void setMaxHeapSize(int maxHeapSize) {
		this.maxHeapSize = maxHeapSize;
	}
}
