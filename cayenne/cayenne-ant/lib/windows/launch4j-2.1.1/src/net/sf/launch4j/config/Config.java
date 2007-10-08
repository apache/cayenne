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

import java.io.File;

import net.sf.launch4j.binding.IValidatable;
import net.sf.launch4j.binding.Validator;

/**
 * @author Copyright (C) 2005 Grzegorz Kowal
 */
public class Config implements IValidatable {

	public static final String HEADER = "header"; //$NON-NLS-1$
	public static final String JAR = "jar"; //$NON-NLS-1$
	public static final String OUTFILE = "outfile"; //$NON-NLS-1$
	public static final String ERR_TITLE = "errTitle"; //$NON-NLS-1$
	public static final String JAR_ARGS = "jarArgs"; //$NON-NLS-1$
	public static final String CHDIR = "chdir"; //$NON-NLS-1$
	public static final String CUSTOM_PROC_NAME = "customProcName"; //$NON-NLS-1$
	public static final String STAY_ALIVE = "stayAlive"; //$NON-NLS-1$
	public static final String ICON = "icon"; //$NON-NLS-1$

	public static final int GUI_HEADER = 0;
	public static final int CONSOLE_HEADER = 1;

	private boolean dontWrapJar;
	private int headerType;
	private String[] headerObjects;
	private String[] libs;
	private File jar;
	private File outfile;

	// runtime configuration
	private String errTitle;
	private String jarArgs;
	private String chdir;
	private boolean customProcName;
	private boolean stayAlive;
	private File icon;
	private Jre jre;
	private Splash splash;
	private VersionInfo versionInfo;

	public void checkInvariants() {
		Validator.checkTrue(outfile != null && outfile.getPath().endsWith(".exe"), //$NON-NLS-1$
				"outfile", Messages.getString("Config.specify.output.exe")); //$NON-NLS-1$ //$NON-NLS-2$
		if (dontWrapJar) {
			Validator.checkRelativeWinPath(jar.getPath(), "jar", Messages.getString("Config.application.jar.path"));
		} else {
			Validator.checkFile(jar, "jar", Messages.getString("Config.application.jar")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (!Validator.isEmpty(chdir)) {
			Validator.checkRelativeWinPath(chdir,
					"chdir", Messages.getString("Config.chdir.relative")); //$NON-NLS-1$ //$NON-NLS-2$
			Validator.checkFalse(chdir.toLowerCase().equals("true") //$NON-NLS-1$
					|| chdir.toLowerCase().equals("false"), //$NON-NLS-1$
					"chdir", Messages.getString("Config.chdir.path")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		Validator.checkOptFile(icon, "icon", Messages.getString("Config.icon")); //$NON-NLS-1$ //$NON-NLS-2$
		Validator.checkOptString(jarArgs, 128, "jarArgs", Messages.getString("Config.jar.arguments")); //$NON-NLS-1$ //$NON-NLS-2$
		Validator.checkOptString(errTitle, 128, "errTitle", Messages.getString("Config.error.title")); //$NON-NLS-1$ //$NON-NLS-2$
		Validator.checkRange(headerType, GUI_HEADER, CONSOLE_HEADER,
				"headerType", Messages.getString("Config.invalid.header.type")); //$NON-NLS-1$ //$NON-NLS-2$
		Validator.checkTrue(headerType != CONSOLE_HEADER || splash == null,
				"headerType", Messages.getString("Config.splash.not.impl.by.console.hdr")); //$NON-NLS-1$ //$NON-NLS-2$
		jre.checkInvariants();
	}
	
	public void validate() {
		checkInvariants();
		if (splash != null) {
			splash.checkInvariants();
		}
		if (versionInfo != null) {
			versionInfo.checkInvariants();
		}
	}

	/** Change current directory to EXE location. */
	public String getChdir() {
		return chdir;
	}

	public void setChdir(String chdir) {
		this.chdir = chdir;
	}

	/** Constant command line arguments passed to application jar. */
	public String getJarArgs() {
		return jarArgs;
	}

	public void setJarArgs(String jarArgs) {
		this.jarArgs = jarArgs;
	}

	/** Optional, error message box title. */
	public String getErrTitle() {
		return errTitle;
	}

	public void setErrTitle(String errTitle) {
		this.errTitle = errTitle;
	}

	/** launch4j header file. */
	public int getHeaderType() {
		return headerType;
	}

	public void setHeaderType(int headerType) {
		this.headerType = headerType;
	}
	
	public boolean isCustomHeaderObjects() {
		return headerObjects != null && headerObjects.length > 0;
	}

	public String[] getHeaderObjects() {
		return isCustomHeaderObjects() ? headerObjects
				: headerType == GUI_HEADER
						? LdDefaults.getInstance().getGuiHeaderObjects()
						: LdDefaults.getInstance().getConsoleHeaderObjects();
	}

	public void setHeaderObjects(String[] headerObjects) {
		this.headerObjects = headerObjects;
	}

	public boolean isCustomLibs() {
		return libs != null && libs.length > 0;
	}

	public String[] getLibs() {
		return isCustomLibs() ? libs : LdDefaults.getInstance().getLibs();
	}

	public void setLibs(String[] libs) {
		this.libs = libs;
	}

	/** ICO file. */
	public File getIcon() {
		return icon;
	}

	public void setIcon(File icon) {
		this.icon = icon;
	}

	/** Jar to wrap. */
	public File getJar() {
		return jar;
	}

	public void setJar(File jar) {
		this.jar = jar;
	}

	/** JRE configuration */
	public Jre getJre() {
		return jre;
	}

	public void setJre(Jre jre) {
		this.jre = jre;
	}

	/** Output EXE file. */
	public File getOutfile() {
		return outfile;
	}

	public void setOutfile(File outfile) {
		this.outfile = outfile;
	}

	/** Custom process name as the output EXE file name. */
	public boolean isCustomProcName() {
		return customProcName;
	}

	public void setCustomProcName(boolean customProcName) {
		this.customProcName = customProcName;
	}

	/** Splash screen configuration. */
	public Splash getSplash() {
		return splash;
	}

	public void setSplash(Splash splash) {
		this.splash = splash;
	}

	/** Stay alive after launching the application. */
	public boolean isStayAlive() {
		return stayAlive;
	}

	public void setStayAlive(boolean stayAlive) {
		this.stayAlive = stayAlive;
	}
	
	public VersionInfo getVersionInfo() {
		return versionInfo;
	}

	public void setVersionInfo(VersionInfo versionInfo) {
		this.versionInfo = versionInfo;
	}

	public boolean isDontWrapJar() {
		return dontWrapJar;
	}

	public void setDontWrapJar(boolean dontWrapJar) {
		this.dontWrapJar = dontWrapJar;
	}
}
