/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.crypto.transformer.bytes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import org.apache.cayenne.crypto.CayenneCryptoException;

/**
 * @since 4.0
 */
class GzipEncryptor implements BytesEncryptor {

	static final int GZIP_THRESHOLD = 150;

	private BytesEncryptor delegate;

	public GzipEncryptor(BytesEncryptor delegate) {
		this.delegate = delegate;
	}

	@Override
	public byte[] encrypt(byte[] input, int outputOffset, byte[] flags) {

		boolean compressed = input.length >= GZIP_THRESHOLD;

		if (compressed) {
			try {
				input = gzip(input);
			} catch (IOException e) {
				// really not expecting an error here...
				throw new CayenneCryptoException("Error compressing input", e);
			}
		}

		flags[0] = Header.setCompressed(flags[0], compressed);
		return delegate.encrypt(input, outputOffset, flags);
	}

	static byte[] gzip(byte[] input) throws IOException {
		ByteArrayOutputStream zipBytes = new ByteArrayOutputStream(input.length);
		try (GZIPOutputStream out = new GZIPOutputStream(zipBytes);) {
			out.write(input, 0, input.length);
		}

		return zipBytes.toByteArray();
	}

}
