Dependency Check Maven Plugin
=============================

A Maven Plugin used to check which dependencies or plugins are available in which remote repository


There is a <b>maven-settings.xml</b> file in this source code tree, when you want to use this maven plugin, please download this file
to your local machine, and then in your project root directory, run:
<pre>
   mvn -s maven-settings.xml dependency-check:check
</pre>

The default action will list all dependencies which are not in Maven Central Repository

