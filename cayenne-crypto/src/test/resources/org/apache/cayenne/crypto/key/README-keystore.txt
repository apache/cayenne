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
