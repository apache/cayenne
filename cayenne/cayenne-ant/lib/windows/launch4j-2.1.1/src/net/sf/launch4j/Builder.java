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
 * Created on 2005-04-24
 */
package net.sf.launch4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import net.sf.launch4j.binding.InvariantViolationException;
import net.sf.launch4j.config.Config;
import net.sf.launch4j.config.ConfigPersister;

/**
 * @author Copyright (C) 2005 Grzegorz Kowal
 */
public class Builder {
	private final Log _log;

	public Builder(Log log) {
		_log = log;
	}

	/**
	 * @return Output file path.
	 */
	public File build() throws BuilderException {
		final Config c = ConfigPersister.getInstance().getConfig();
		try {
			c.validate();
		} catch (InvariantViolationException e) {
			throw new BuilderException(e.getMessage());
		}
		File rc = null;
		File ro = null;
		File outfile = null;
		FileInputStream is = null;
		FileOutputStream os = null;
		final RcBuilder rcb = new RcBuilder();
		try {
			File basedir = Util.getJarBasedir();
			rc = rcb.build(c);
			ro = File.createTempFile("launch4j", "o"); //$NON-NLS-1$ //$NON-NLS-2$
			outfile = ConfigPersister.getInstance().getOutputFile();

			Cmd resCmd = new Cmd(basedir);
			resCmd.addExe("bin/windres") //$NON-NLS-1$
					.add(Util.WINDOWS_OS ? "--preprocessor=type" : "--preprocessor=cat") //$NON-NLS-1$ //$NON-NLS-2$
					.add("-J rc -O coff -F pe-i386") //$NON-NLS-1$
					.addAbsFile(rc)
					.addAbsFile(ro);
			_log.append(Messages.getString("Builder.compiling.resources")); //$NON-NLS-1$
			resCmd.exec(_log);

			Cmd ldCmd = new Cmd(basedir);
			ldCmd.addExe("bin/ld") //$NON-NLS-1$
					.add("-mi386pe") //$NON-NLS-1$
					.add("--oformat pei-i386") //$NON-NLS-1$
					.add((c.getHeaderType() == Config.GUI_HEADER)
							? "--subsystem windows" : "--subsystem console") //$NON-NLS-1$ //$NON-NLS-2$
					.add("-s")		// strip symbols //$NON-NLS-1$
					.addFiles(c.getHeaderObjects())
					.addAbsFile(ro)
					.addFiles(c.getLibs())
					.add("-o") //$NON-NLS-1$
					.addAbsFile(outfile);
			_log.append(Messages.getString("Builder.linking")); //$NON-NLS-1$
			ldCmd.exec(_log);

			if (!c.isDontWrapJar()) {
				_log.append(Messages.getString("Builder.wrapping")); //$NON-NLS-1$
				int len;
				byte[] buffer = new byte[1024];
				is = new FileInputStream(
						Util.getAbsoluteFile(ConfigPersister.getInstance().getConfigPath(),	c.getJar()));
				os = new FileOutputStream(outfile, true);
				while ((len = is.read(buffer)) > 0) {
					os.write(buffer, 0, len);
				}
			}
			_log.append(Messages.getString("Builder.success") + outfile.getPath()); //$NON-NLS-1$
			return outfile;
		} catch (IOException e) {
			Util.delete(outfile);
			_log.append(e.getMessage());
			throw new BuilderException(e);
		} catch (ExecException e) {
			Util.delete(outfile);
			String msg = e.getMessage(); 
			if (msg != null && msg.indexOf("windres") != -1) { //$NON-NLS-1$
				_log.append(Messages.getString("Builder.generated.resource.file")); //$NON-NLS-1$
				_log.append(rcb.getContent());
			}
			throw new BuilderException(e);
		} finally {
			Util.close(is);
			Util.close(os);
			Util.delete(rc);
			Util.delete(ro);
		}
	}
}

class Cmd {
	private final StringBuffer _sb = new StringBuffer();
	private final File _basedir;
	private final boolean _quote;

	public Cmd(File basedir) {
		_basedir = basedir;
		_quote = basedir.getPath().indexOf(' ') != -1;
	}

	public Cmd add(String s) {
		space();
		_sb.append(s);
		return this;
	}

	public Cmd addAbsFile(File file) {
		space();
		boolean quote = file.getPath().indexOf(' ') != -1;
		if (quote) {
			_sb.append('"');
		}
		_sb.append(file.getPath());
		if (quote) {
			_sb.append('"');
		}
		return this;
	}

	public Cmd addFile(String pathname) {
		space();
		if (_quote) {
			_sb.append('"');
		}
		_sb.append(new File(_basedir, pathname).getPath());
		if (_quote) {
			_sb.append('"');
		}
		return this;
	}

	public Cmd addExe(String pathname) {
		return addFile(Util.WINDOWS_OS ? pathname + ".exe" : pathname); //$NON-NLS-1$
	}

	public Cmd addFiles(String[] files) {
		for (int i = 0; i < files.length; i++) {
			addFile(files[i]);
		}
		return this;
	}

	private void space() {
		if (_sb.length() > 0) {
			_sb.append(' ');
		}
	}
	
	public String toString() {
		return _sb.toString();
	}

	public void exec(Log log) throws ExecException {
		Util.exec(_sb.toString(), log);
	}
}
