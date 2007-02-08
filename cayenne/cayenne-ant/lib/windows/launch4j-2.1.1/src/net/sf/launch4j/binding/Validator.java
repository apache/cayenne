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
 * Created on 2004-01-30
 */
package net.sf.launch4j.binding;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

import net.sf.launch4j.Util;
import net.sf.launch4j.config.ConfigPersister;

/**
 * @author Copyright (C) 2004 Grzegorz Kowal
 */
public class Validator {
	public static final String ALPHANUMERIC_PATTERN = "[\\w]*?"; //$NON-NLS-1$
	public static final String ALPHA_PATTERN = "[\\w&&\\D]*?"; //$NON-NLS-1$
	public static final String NUMERIC_PATTERN = "[\\d]*?"; //$NON-NLS-1$
	public static final String PATH_PATTERN = "[\\w|[ .,:\\-/\\\\]]*?"; //$NON-NLS-1$

	private Validator() {}

	public static boolean isEmpty(String s) {
		return s == null || s.equals(""); //$NON-NLS-1$
	}

	public static void checkNotNull(Object o, String property, String name) {
		if (o == null) {
			signalViolation(property,
					Messages.getString("Validator.empty.field", name)); //$NON-NLS-1$
		}
	}

	public static void checkString(String s, int maxLength, String property, String name) {
		if (s == null || s.length() == 0) {
			signalViolation(property,
					Messages.getString("Validator.empty.field", name)); //$NON-NLS-1$
		}
		if (s.length() > maxLength) {
			signalLengthViolation(property, name, maxLength);
		}
	}

	public static void checkString(String s, int maxLength, String pattern,
			String property, String name) {
		checkString(s, maxLength, property, name);
		if (!s.matches(pattern)) {
			signalViolation(property,
					Messages.getString("Validator.invalid.data", name)); //$NON-NLS-1$
		}
	}

	public static void checkOptString(String s, int maxLength, String property, String name) {
		if (s == null || s.length() == 0) {
			return;
		}
		if (s.length() > maxLength) {
			signalLengthViolation(property, name, maxLength);
		}
	}

	public static void checkOptString(String s, int maxLength, String pattern,
			String property, String name) {
		if (s == null || s.length() == 0) {
			return;
		}
		if (s.length() > maxLength) {
			signalLengthViolation(property, name, maxLength);
		}
		if (!s.matches(pattern)) {
			signalViolation(property,
					Messages.getString("Validator.invalid.data", name));	//$NON-NLS-1$
		}
	}

	public static void checkRange(int value, int min, int max,
			String property, String name) {
		if (value < min || value > max) {
			signalViolation(property,
					Messages.getString("ValidatocheckFiler.must.be.in.range", //$NON-NLS-1$
					name, String.valueOf(min), String.valueOf(max)));
		}
	}

	public static void checkRange(char value, char min, char max,
			String property, String name) {
		if (value < min || value > max) {
			signalViolation(property,
					Messages.getString("Validator.must.be.in.range", //$NON-NLS-1$
					name, String.valueOf(min), String.valueOf(max)));
		}
	}

	public static void checkMin(int value, int min, String property, String name) {
		if (value < min) {
			signalViolation(property,
					Messages.getString("Validator.must.be.at.least", //$NON-NLS-1$
					name, String.valueOf(min)));
		}
	}

	public static void checkTrue(boolean condition, String property, String msg) {
		if (!condition) {
			signalViolation(property, msg);
		}
	}
	
	public static void checkFalse(boolean condition, String property, String msg) {
		if (condition) {
			signalViolation(property, msg);
		}
	}
	
	public static void checkElementsNotNullUnique(Collection c, String property, String msg) {
		if (c.contains(null)
				|| new HashSet(c).size() != c.size()) {
			signalViolation(property,
					Messages.getString("Validator.already.exists", msg)); //$NON-NLS-1$ 
		}
	}

	public static void checkElementsUnique(Collection c, String property, String msg) {
		if (new HashSet(c).size() != c.size()) {
			signalViolation(property,
					Messages.getString("Validator.already.exists", msg)); //$NON-NLS-1$
		}
	}

	public static void checkFile(File f, String property, String fileDescription) {
		File cfgPath = ConfigPersister.getInstance().getConfigPath();
		if (f == null || (!f.exists() && !Util.getAbsoluteFile(cfgPath, f).exists())) {
			signalViolation(property,
					Messages.getString("Validator.doesnt.exist", fileDescription)); //$NON-NLS-1$
		}
	}

	public static void checkOptFile(File f, String property, String fileDescription) {
		if (f != null && f.getPath().length() > 0) {
			checkFile(f, property, fileDescription);
		}
	}

	public static void checkRelativeWinPath(String path, String property, String msg) {
		if (path == null
				|| path.equals("")
				|| path.startsWith("/") //$NON-NLS-1$
				|| path.startsWith("\\") //$NON-NLS-1$
				|| path.indexOf(':') != -1) {
			signalViolation(property, msg);
		}
	}

	public static void signalLengthViolation(String property, String name, int maxLength) {
		signalViolation(property,
				Messages.getString("Validator.exceeds.max.length", //$NON-NLS-1$
						name, String.valueOf(maxLength)));
	}

	public static void signalViolation(String property, String msg)	{
		throw new InvariantViolationException(property, msg);
	}
}
