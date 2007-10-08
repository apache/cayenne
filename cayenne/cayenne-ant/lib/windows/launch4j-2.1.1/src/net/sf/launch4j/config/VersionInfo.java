/*
 * Created on May 21, 2005
 */
package net.sf.launch4j.config;

import net.sf.launch4j.binding.IValidatable;
import net.sf.launch4j.binding.Validator;

/**
 * @author Copyright (C) 2005 Grzegorz Kowal
 */
public class VersionInfo implements IValidatable {
	public static final String VERSION_PATTERN = "(\\d+\\.){3}\\d+"; //$NON-NLS-1$
	private static final int MAX_LEN = 150;

	private String fileVersion;
	private String txtFileVersion;
	private String fileDescription;
	private String copyright;
	private String productVersion;
	private String txtProductVersion;
	private String productName;
	private String companyName;
	private String internalName;
	private String originalFilename;

	public void checkInvariants() {
		Validator.checkString(fileVersion, 20, VERSION_PATTERN,
				"versionInfo.fileVersion", Messages.getString("VersionInfo.file.version")); //$NON-NLS-1$ //$NON-NLS-2$
		Validator.checkString(txtFileVersion, 50,
				"versionInfo.txtFileVersion", Messages.getString("VersionInfo.txt.file.version")); //$NON-NLS-1$ //$NON-NLS-2$
		Validator.checkString(fileDescription, MAX_LEN,
				"versionInfo.fileDescription", Messages.getString("VersionInfo.file.description")); //$NON-NLS-1$ //$NON-NLS-2$
		Validator.checkString(copyright, MAX_LEN,
				"versionInfo.copyright", Messages.getString("VersionInfo.copyright")); //$NON-NLS-1$ //$NON-NLS-2$
		Validator.checkString(productVersion, 20, VERSION_PATTERN,
				"versionInfo.productVersion", Messages.getString("VersionInfo.product.version")); //$NON-NLS-1$ //$NON-NLS-2$
		Validator.checkString(txtProductVersion, 50,
				"versionInfo.txtProductVersion", Messages.getString("VersionInfo.txt.product.version")); //$NON-NLS-1$ //$NON-NLS-2$
		Validator.checkString(productName, MAX_LEN,
				"versionInfo.productName", Messages.getString("VersionInfo.product.name")); //$NON-NLS-1$ //$NON-NLS-2$
		Validator.checkOptString(companyName, MAX_LEN,
				"versionInfo.companyName", Messages.getString("VersionInfo.company.name")); //$NON-NLS-1$ //$NON-NLS-2$
		Validator.checkString(internalName, 50,
				"versionInfo.internalName", Messages.getString("VersionInfo.internal.name")); //$NON-NLS-1$ //$NON-NLS-2$
		Validator.checkTrue(!internalName.endsWith(".exe"), //$NON-NLS-1$
				"versionInfo.internalName", //$NON-NLS-1$
				Messages.getString("VersionInfo.internal.name.not.exe")); //$NON-NLS-1$
		Validator.checkString(originalFilename, 50,
				"versionInfo.originalFilename", Messages.getString("VersionInfo.original.filename")); //$NON-NLS-1$ //$NON-NLS-2$
		Validator.checkTrue(originalFilename.endsWith(".exe"), //$NON-NLS-1$
				"versionInfo.originalFilename", //$NON-NLS-1$
				Messages.getString("VersionInfo.original.filename.exe")); //$NON-NLS-1$
	}

	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	public String getCopyright() {
		return copyright;
	}

	public void setCopyright(String copyright) {
		this.copyright = copyright;
	}

	public String getFileDescription() {
		return fileDescription;
	}

	public void setFileDescription(String fileDescription) {
		this.fileDescription = fileDescription;
	}

	public String getFileVersion() {
		return fileVersion;
	}

	public void setFileVersion(String fileVersion) {
		this.fileVersion = fileVersion;
	}

	public String getInternalName() {
		return internalName;
	}

	public void setInternalName(String internalName) {
		this.internalName = internalName;
	}

	public String getOriginalFilename() {
		return originalFilename;
	}

	public void setOriginalFilename(String originalFilename) {
		this.originalFilename = originalFilename;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public String getProductVersion() {
		return productVersion;
	}

	public void setProductVersion(String productVersion) {
		this.productVersion = productVersion;
	}

	public String getTxtFileVersion() {
		return txtFileVersion;
	}

	public void setTxtFileVersion(String txtFileVersion) {
		this.txtFileVersion = txtFileVersion;
	}

	public String getTxtProductVersion() {
		return txtProductVersion;
	}

	public void setTxtProductVersion(String txtProductVersion) {
		this.txtProductVersion = txtProductVersion;
	}
}
