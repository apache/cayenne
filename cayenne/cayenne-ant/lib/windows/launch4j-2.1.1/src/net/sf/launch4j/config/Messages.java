package net.sf.launch4j.config;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {
	private static final String BUNDLE_NAME = "net.sf.launch4j.config.messages"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
			.getBundle(BUNDLE_NAME);
	private static final MessageFormat FORMATTER = new MessageFormat("");

	private Messages() {
	}
	
	public static String getString(String key) {
		// TODO Auto-generated method stub
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
	
	public static String getString(String key, String arg0) {
		return getString(key, new Object[] {arg0});
	}

	public static String getString(String key, String arg0, String arg1) {
		return getString(key, new Object[] {arg0, arg1});
	}

	public static String getString(String key, String arg0, String arg1, String arg2) {
		return getString(key, new Object[] {arg0, arg1, arg2});
	}

	public static String getString(String key, Object[] args) {
		// TODO Auto-generated method stub
		try {
			FORMATTER.applyPattern(RESOURCE_BUNDLE.getString(key));
			return FORMATTER.format(args);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
