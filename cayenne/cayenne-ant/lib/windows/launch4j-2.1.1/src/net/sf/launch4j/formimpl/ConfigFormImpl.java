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
 * Created on May 10, 2005
 */
package net.sf.launch4j.formimpl;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.launch4j.FileChooserFilter;
import net.sf.launch4j.binding.Binding;
import net.sf.launch4j.binding.Bindings;
import net.sf.launch4j.binding.IValidatable;
import net.sf.launch4j.binding.Validator;
import net.sf.launch4j.config.Config;
import net.sf.launch4j.config.ConfigPersister;
import net.sf.launch4j.config.Splash;
import net.sf.launch4j.config.VersionInfo;
import net.sf.launch4j.form.ConfigForm;

/**
 * @author Copyright (C) 2005 Grzegorz Kowal
 */
public class ConfigFormImpl extends ConfigForm {
	private final JFileChooser _fileChooser = new JFileChooser();
	private final Bindings _bindings = new Bindings();

	public ConfigFormImpl() {
		_tab.setTitleAt(0, Messages.getString("tab.basic"));
		_tab.setTitleAt(1, Messages.getString("tab.header"));
		_tab.setTitleAt(2, Messages.getString("tab.jre"));
		_tab.setTitleAt(3, Messages.getString("tab.splash"));
		_tab.setTitleAt(4, Messages.getString("tab.version"));

		_logSeparator.setText(Messages.getString("log"));
		_outfileLabel.setText(Messages.getString("outfile"));
		_outfileField.setToolTipText(Messages.getString("outfileTip"));
		_errorTitleLabel.setText(Messages.getString("errorTitle"));
		_errorTitleField.setToolTipText(Messages.getString("errorTitleTip"));
		_customProcNameCheck.setText(Messages.getString("customProcName"));
		_stayAliveCheck.setText(Messages.getString("stayAlive"));
		_iconLabel.setText(Messages.getString("icon"));
		_iconField.setToolTipText(Messages.getString("iconTip"));
		_jarLabel.setText(Messages.getString("jar"));
		_jarField.setToolTipText(Messages.getString("jarTip"));
		_dontWrapJarCheck.setText(Messages.getString("dontWrapJar"));
		_jarArgsLabel.setText(Messages.getString("jarArgs"));
		_jarArgsField.setToolTipText(Messages.getString("jarArgsTip"));
		_optionsLabel.setText(Messages.getString("options"));
		_chdirLabel.setText(Messages.getString("chdir"));
		_chdirField.setToolTipText(Messages.getString("chdirTip"));

		_headerTypeLabel.setText(Messages.getString("headerType"));
		_guiHeaderRadio.setText(Messages.getString("gui"));
		_consoleHeaderRadio.setText(Messages.getString("console"));
		_headerObjectsCheck.setText(Messages.getString("objectFiles"));
		_libsCheck.setText(Messages.getString("libs"));
		_linkerOptionsSeparator.setText(Messages.getString("linkerOptions"));
		_jrePathLabel.setText(Messages.getString("jrePath"));
		_jrePathField.setToolTipText(Messages.getString("jrePathTip"));
		_jreMinLabel.setText(Messages.getString("jreMin"));
		_jreMaxLabel.setText(Messages.getString("jreMax"));
		_jvmArgsTextLabel.setText(Messages.getString("jvmArgs"));
		_jvmArgsTextArea.setToolTipText(Messages.getString("jvmArgsTip"));
		_initialHeapSizeLabel.setText(Messages.getString("initialHeapSize"));
		_maxHeapSizeLabel.setText(Messages.getString("maxHeapSize"));
		_addVarsLabel.setText(Messages.getString("addVariables"));
		_addVarsLabel.setToolTipText(Messages.getString("addVariablesTip"));
		_exeDirRadio.setToolTipText(Messages.getString("exeDirVarTip"));
		_exeFileRadio.setToolTipText(Messages.getString("exeFileVarTip"));
		_otherVarRadio.setText(Messages.getString("other"));
		_otherVarRadio.setToolTipText(Messages.getString("otherTip"));
		_otherVarField.setToolTipText(Messages.getString("otherVarTip"));
		_addVarButton.setText(Messages.getString("add"));
		_addVarButton.setToolTipText(Messages.getString("addVariablesTip"));

		_splashCheck.setText(Messages.getString("enableSplash"));
		_splashFileLabel.setText(Messages.getString("splashFile"));
		_splashFileField.setToolTipText(Messages.getString("splashFileTip"));
		_waitForWindowLabel.setText(Messages.getString("waitForWindow"));
		_waitForWindowCheck.setText(Messages.getString("waitForWindowText"));
		_timeoutLabel.setText(Messages.getString("timeout"));
		_timeoutField.setToolTipText(Messages.getString("timeoutTip"));
		_timeoutErrCheck.setText(Messages.getString("timeoutErr"));
		_timeoutErrCheck.setToolTipText(Messages.getString("timeoutErrTip"));

		_versionInfoCheck.setText(Messages.getString("addVersionInfo"));
		_fileVersionLabel.setText(Messages.getString("fileVersion"));
		_fileVersionField.setToolTipText(Messages.getString("fileVersionTip"));
		_addVersionInfoSeparator.setText(Messages.getString("additionalInfo"));
		_productVersionLabel.setText(Messages.getString("productVersion"));
		_productVersionField.setToolTipText(Messages.getString("productVersionTip"));
		_fileDescriptionLabel.setText(Messages.getString("fileDescription"));
		_fileDescriptionField.setToolTipText(Messages.getString("fileDescriptionTip"));
		_copyrightLabel.setText(Messages.getString("copyright"));
		_txtFileVersionLabel.setText(Messages.getString("txtFileVersion"));
		_txtFileVersionField.setToolTipText(Messages.getString("txtFileVersionTip"));
		_txtProductVersionLabel.setText(Messages.getString("txtProductVersion"));
		_txtProductVersionField.setToolTipText(Messages.getString("txtProductVersionTip"));
		_productNameLabel.setText(Messages.getString("productName"));
		_originalFilenameLabel.setText(Messages.getString("originalFilename"));
		_originalFilenameField.setToolTipText(Messages.getString("originalFilenameTip"));
		_internalNameLabel.setText(Messages.getString("internalName"));
		_internalNameField.setToolTipText(Messages.getString("internalNameTip"));
		_companyNameLabel.setText(Messages.getString("companyName"));

		_dontWrapJarCheck.addChangeListener(new DontWrapJarChangeListener());

		_outfileButton.addActionListener(new BrowseActionListener(
				_outfileField, new FileChooserFilter("Windows executables (.exe)", ".exe")));
		_jarButton.addActionListener(new BrowseActionListener(
				_jarField, new FileChooserFilter("Jar files", ".jar")));
		_iconButton.addActionListener(new BrowseActionListener(
				_iconField, new FileChooserFilter("Icon files (.ico)", ".ico")));
		_splashFileButton.addActionListener(new BrowseActionListener(
				_splashFileField, new FileChooserFilter("Bitmap files (.bmp)", ".bmp")));

		ActionListener al = new VariableActionListener();
		_exeDirRadio.addActionListener(al);
		_exeFileRadio.addActionListener(al);
		_otherVarRadio.addActionListener(al);
		_exeDirRadio.setSelected(true);
		al.actionPerformed(null);
		_addVarButton.addActionListener(new AddVariableActionListener());

		_guiHeaderRadio.addChangeListener(new HeaderTypeChangeListener());
		_headerObjectsCheck.addActionListener(new HeaderObjectsActionListener());
		_libsCheck.addActionListener(new LibsActionListener());

		_bindings.add("outfile", _outfileField)
				.add("dontWrapJar", _dontWrapJarCheck)
				.add("jar", _jarField)
				.add("icon", _iconField)
				.add("jarArgs", _jarArgsField)
				.add("errTitle", _errorTitleField)
				.add("chdir", _chdirField)
				.add("customProcName", _customProcNameCheck)
				.add("stayAlive", _stayAliveCheck)
				.add("headerType", new JRadioButton[] {_guiHeaderRadio, _consoleHeaderRadio})
				.add("headerObjects", "customHeaderObjects", _headerObjectsCheck, _headerObjectsTextArea)
				.add("libs", "customLibs", _libsCheck, _libsTextArea)
				.add("jre.path", _jrePathField)
				.add("jre.minVersion", _jreMinField)
				.add("jre.maxVersion", _jreMaxField)
				.add("jre.initialHeapSize", _initialHeapSizeField)
				.add("jre.maxHeapSize", _maxHeapSizeField)
				.add("jre.args", _jvmArgsTextArea)
				.addOptComponent("splash", Splash.class, _splashCheck)
				.add("splash.file", _splashFileField)
				.add("splash.waitForWindow", _waitForWindowCheck, true)
				.add("splash.timeout", _timeoutField, "60")
				.add("splash.timeoutErr", _timeoutErrCheck, true)
				.addOptComponent("versionInfo", VersionInfo.class, _versionInfoCheck)
				.add("versionInfo.fileVersion", _fileVersionField)
				.add("versionInfo.productVersion", _productVersionField)
				.add("versionInfo.fileDescription", _fileDescriptionField)
				.add("versionInfo.internalName", _internalNameField)
				.add("versionInfo.originalFilename", _originalFilenameField)
				.add("versionInfo.productName", _productNameField)
				.add("versionInfo.txtFileVersion", _txtFileVersionField)
				.add("versionInfo.txtProductVersion", _txtProductVersionField)
				.add("versionInfo.companyName", _companyNameField)
				.add("versionInfo.copyright", _copyrightField);
	}

	private class DontWrapJarChangeListener implements ChangeListener {
		public void stateChanged(ChangeEvent e) {
			boolean dontWrap = _dontWrapJarCheck.isSelected();
			_jarLabel.setText(dontWrap ? Messages.getString("jarPath")
					: Messages.getString("jar"));
			_jarField.setToolTipText(dontWrap ? Messages.getString("jarPathTip")
					: Messages.getString("jarTip"));
			_jarButton.setEnabled(!dontWrap);
		}
	}

	private class BrowseActionListener implements ActionListener {
		private final JTextField _field;
		private final FileChooserFilter _filter;

		public BrowseActionListener(JTextField field, FileChooserFilter filter) {
			_field = field;
			_filter = filter;
		}

		public void actionPerformed(ActionEvent e) {
			if (!_field.isEnabled()) {
				return;
			}
			_fileChooser.setFileFilter(_filter);
			_fileChooser.setSelectedFile(new File(""));
			int result = _field.equals(_outfileField)
					? _fileChooser.showSaveDialog(MainFrame.getInstance())
					: _fileChooser.showOpenDialog(MainFrame.getInstance());
			if (result == JFileChooser.APPROVE_OPTION) {
				_field.setText(_fileChooser.getSelectedFile().getPath());
			}
		}
	}
	
	private class HeaderTypeChangeListener implements ChangeListener {
		public void stateChanged(ChangeEvent e) {
			Config c = ConfigPersister.getInstance().getConfig();
			c.setHeaderType(_guiHeaderRadio.isSelected() ? Config.GUI_HEADER : Config.CONSOLE_HEADER);
			if (!_headerObjectsCheck.isSelected()) {
				Binding b = getBinding("headerObjects");
				b.put(c);
			}
		}
	}
	
	private class HeaderObjectsActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (!_headerObjectsCheck.isSelected()) {
				ConfigPersister.getInstance().getConfig().setHeaderObjects(null);
				Binding b = getBinding("headerObjects");
				b.put(ConfigPersister.getInstance().getConfig());
			}
		}
	}

	private class LibsActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (!_libsCheck.isSelected()) {
				ConfigPersister.getInstance().getConfig().setLibs(null);
				Binding b = getBinding("libs");
				b.put(ConfigPersister.getInstance().getConfig());
			}
		}
	}
	
	private class VariableActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			_otherVarField.setEnabled(_otherVarRadio.isSelected());
		}
	}
	
	private class AddVariableActionListener implements ActionListener {
		private final Color _validColor = _otherVarField.getBackground();

		public void actionPerformed(ActionEvent e) {
			final int pos = _jvmArgsTextArea.getCaretPosition();
			if (_exeDirRadio.isSelected()) {
				_jvmArgsTextArea.insert("-Dlaunch4j.exedir=\"%EXEDIR%\"\n", pos);
			} else if (_exeFileRadio.isSelected()) {
				_jvmArgsTextArea.insert("-Dlaunch4j.exefile=\"%EXEFILE%\"\n", pos);
			} else {
				final String var = _otherVarField.getText()
						.replaceAll("\"", "")
						.replaceAll("%", "");
				if (Validator.isEmpty(var)) {
					signalViolation("specifyVar");
					return;
				}
				final String prop = var.replaceAll(" ", ".")
						.replaceAll("_", ".")
						.toLowerCase();
				_jvmArgsTextArea.insert("-Denv." + prop + "=\"%" + var + "%\"\n", pos);
			}
		}
		
		private void signalViolation(String code) {
			_otherVarField.setBackground(Binding.INVALID_COLOR);
			MainFrame.getInstance().warn(Messages.getString(code));
			_otherVarField.setBackground(_validColor);
			_otherVarField.requestFocusInWindow();
		}
	}

	public void clear(IValidatable bean) {
		_bindings.clear(bean);
	}

	public void put(IValidatable bean) {
		_bindings.put(bean);
	}

	public void get(IValidatable bean) {
		_bindings.get(bean);
	}
	
	public boolean isModified() {
		return _bindings.isModified();
	}
	
	public JTextArea getLogTextArea() {
		return _logTextArea;
	}
	
	public Binding getBinding(String property) {
		return _bindings.getBinding(property);
	}
}
