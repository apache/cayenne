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


package org.apache.cayenne.conf;

/**
 * The ROT-13 password encoder passes the text of the database password
 * through a simple Caesar cipher to obscure the password text.  The ROT-13
 * cipher only processes letters -- numbers and symbols are left untouched.
 * ROT-13 is also a symmetrical cipher and therefore provides no real
 * encryption since applying the cipher to the encrypted text produces the
 * original source text.  See the Wikipedia entry on
 * <a href="http://en.wikipedia.org/wiki/Rot-13">ROT13</a>
 * for more information on this topic.
 * 
 * @since 3.0
 * @author Michael Gentry
 */
public class Rot13PasswordEncoder implements PasswordEncoding
{
  /* (non-Javadoc)
   * @see org.apache.cayenne.conf.PasswordEncoding#decodePassword(java.lang.String, java.lang.String)
   */
  public String decodePassword(String encodedPassword, String salt)
  {
    return rotate(encodedPassword);
  }

  /* (non-Javadoc)
   * @see org.apache.cayenne.conf.PasswordEncoding#encodePassword(java.lang.String, java.lang.String)
   */
  public String encodePassword(String normalPassword, String salt)
  {
    return rotate(normalPassword);
  }

  /**
   * Applies a ROT-13 Caesar cipher to the supplied value.  Each letter in
   * the supplied value is substituted with a new value rotated by 13 places
   * in the alphabet.  See <a href="http://en.wikipedia.org/wiki/ROT13">ROT13</a>
   * for more information.
   * <p>
   * A Unix command to perform a ROT-13 cipher is:
   *   <pre>tr "[a-m][n-z][A-M][N-Z]" "[n-z][a-m][N-Z][A-M]"</pre>
   *   
   * @param value The text to be rotated.
   * @return The rotated text.
   */
  public String rotate(String value)
  {
    int          length = value.length();
    StringBuffer result = new StringBuffer();

    for (int i = 0; i < length; i++)
    {
      char c = value.charAt(i);

      // If c is a letter, rotate it by 13.  Numbers/symbols are untouched.
      if ((c >= 'a' && c <= 'm') || (c >= 'A' && c <= 'M'))
        c += 13;
      else if ((c >= 'n' && c <= 'z') || (c >= 'A' && c <= 'Z'))
        c -= 13;

      result.append(c);
    }

    return result.toString();
  }

  public static void main(String[] args) 
  {
    Rot13PasswordEncoder encoder = new Rot13PasswordEncoder();
    
    for (int i = 0; i < args.length; i++)
    {
      String string = args[i];

      System.out.println(encoder.rotate(string));
    }
  }
}
