This directory contains some Eclipse extras (http://www.eclipse.org).

NOTE: .classpath and .project are already in the root of the Cayenne project tree,
so Cayenne can be checked out from CVS via Eclipse.

To make sure your code formatting is setup the same way as formatting used by
Cayenne committers, import coding standard templates located in this directory:

Eclipse 3.0 - workspace-wide import:
 
* Windows -> Preferences -> Java -> Code Style -> Code Templates -> Import -> file "codetemplates.xml"
* Windows -> Preferences -> Java -> Code Style -> Formatter -> Import -> file "formatting.xml"
* Windows -> Preferences -> Java -> Code Style -> Organize Imports -> Import -> file "imports.txt"

Eclipse 3.1 - project-specific import:

* Project -> Properties -> Java Code Style -> Formatter -> enable project specific settings -> true
* Project -> Properties -> Java Code Style -> Formatter -> Import -> file "formatting.xml"
* Project -> Properties -> Java Code Style -> Organize Imports -> enable project specific settings -> true
* Project -> Properties -> Java Code Style -> Organize Imports -> Import -> file "imports.txt"

Unfortunately, there's not yet a way to define Code Templates per-project.

For further help send mail to cayenne-devel@objectstyle.org!

