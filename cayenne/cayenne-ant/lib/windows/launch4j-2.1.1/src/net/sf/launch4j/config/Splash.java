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
public class Splash implements IValidatable {
	public static final String SPLASH_FILE = "splash"; //$NON-NLS-1$
	public static final String WAIT_FOR_TITLE = "waitForTitle"; //$NON-NLS-1$
	public static final String TIMEOUT = "splashTimeout"; //$NON-NLS-1$
	public static final String TIMEOUT_ERR = "splashTimeoutErr"; //$NON-NLS-1$

	private File file;
	private boolean waitForWindow = true;
	private int timeout = 60;
	private boolean timeoutErr = true;

	public void checkInvariants() {
		Validator.checkFile(file, "splash.file", Messages.getString("Splash.splash.file")); //$NON-NLS-1$ //$NON-NLS-2$
		Validator.checkRange(timeout, 1, 60 * 15, "splash.timeout", Messages.getString("Splash.splash.timeout")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/** Splash screen in BMP format. */
	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	/** Splash timeout in seconds. */
	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	/** Signal error on splash timeout. */
	public boolean isTimeoutErr() {
		return timeoutErr;
	}

	public void setTimeoutErr(boolean timeoutErr) {
		this.timeoutErr = timeoutErr;
	}

	/** Hide splash screen when the child process displayes the first window. */
	public boolean getWaitForWindow() {
		return waitForWindow;
	}

	public void setWaitForWindow(boolean waitForWindow) {
		this.waitForWindow = waitForWindow;
	}
}
