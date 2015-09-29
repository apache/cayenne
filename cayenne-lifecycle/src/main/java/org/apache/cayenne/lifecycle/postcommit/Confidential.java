package org.apache.cayenne.lifecycle.postcommit;

/**
 * A singleton representing a confidential property value.
 * 
 * @since 4.0
 */
public class Confidential {

	private static final Confidential instance = new Confidential();

	public static Confidential getInstance() {
		return instance;
	}

	private Confidential() {
	}

	@Override
	public String toString() {
		return "*******";
	}
}
