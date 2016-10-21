/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.crypto.transformer.bytes;

import org.apache.cayenne.crypto.unit.CryptoUnitUtils;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

public class GzipDecryptorTest {

	@Test
	public void testGunzip() throws IOException {

		byte[] input1 = CryptoUnitUtils.hexToBytes("1f8b0800000000000000f348cdc9c957f0409000a91a078c11000000");
		byte[] output1 = GzipDecryptor.gunzip(input1);
		byte[] expectedOutput1 = "Hello Hello Hello".getBytes("UTF8");

		assertArrayEquals(expectedOutput1, output1);
	}

	@Test
	public void testGunzip_Large() throws IOException {

		byte[] input1 = readResource("plain.gz");
		byte[] output1 = GzipDecryptor.gunzip(input1);
		byte[] expectedOutput1 = readResource("plain");

		assertArrayEquals(expectedOutput1, output1);
	}

	private byte[] readResource(String name) throws IOException {

		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try (InputStream in = getClass().getResourceAsStream(name);) {

			assertNotNull(in);
			int read;
			byte[] buffer = new byte[1024];
			while ((read = in.read(buffer)) > 0) {
				out.write(buffer, 0, read);
			}
		}

		return out.toByteArray();
	}
}
