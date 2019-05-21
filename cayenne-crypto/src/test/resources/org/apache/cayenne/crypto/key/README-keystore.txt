#	Licensed to the Apache Software Foundation (ASF) under one
#	or more contributor license agreements.  See the NOTICE file
#	distributed with this work for additional information
#	regarding copyright ownership.  The ASF licenses this file
#	to you under the Apache License, Version 2.0 (the
#	"License"); you may not use this file except in compliance
#	with the License.  You may obtain a copy of the License at
#	
#	https://www.apache.org/licenses/LICENSE-2.0
#	
#	Unless required by applicable law or agreed to in writing,
#	software distributed under the License is distributed on an
#	"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#	KIND, either express or implied.  See the License for the
#	specific language governing permissions and limitations
#	under the License.   

Contains unit test keystore passwords. Storing in plaintext here, as none of these 
keystores store any real keys.

ks1.jceks
---------

Created with: 
keytool -genseckey -keystore ./ks1.jceks -storetype JCEKS -alias k1
keytool -genseckey -keystore ./ks1.jceks -storetype JCEKS -alias k2
keytool -genseckey -keystore ./ks1.jceks -storetype JCEKS -keyalg AES -keysize 128 -alias k3

	Keystore - testkspass
		k1 - testkeypass
		k2 - testkeypass
		k3 - testkeypass
