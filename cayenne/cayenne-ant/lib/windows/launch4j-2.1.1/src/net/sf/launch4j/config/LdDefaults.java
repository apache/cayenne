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
 * Created on Sep 3, 2005
 */
package net.sf.launch4j.config;

public class LdDefaults {
	private static final LdDefaults _instance = new LdDefaults();

	private final String[] _guiHeaderObjects;
	private final String[] _consoleHeaderObjects;
	private final String[] _libs;

	private LdDefaults() {
		_guiHeaderObjects = new String[] {"w32api/crt2.o",
				"head/guihead.o",
				"head/head.o"};
		_consoleHeaderObjects = new String[] {"w32api/crt2.o",
				"head/consolehead.o",
				"head/head.o"};
		_libs = new String[] {"w32api/libmingw32.a",
				"w32api/libgcc.a",
				"w32api/libmsvcrt.a",
				"w32api/libkernel32.a",
				"w32api/libuser32.a",
				"w32api/libadvapi32.a",
				"w32api/libshell32.a"};
	}

	public static LdDefaults getInstance() {
		return _instance;
	}

	public String[] getGuiHeaderObjects() {
		return _guiHeaderObjects;
	}
	
	public String[] getConsoleHeaderObjects() {
		return _consoleHeaderObjects;
	}
	
	public String[] getLibs() {
		return _libs;
	}
}
