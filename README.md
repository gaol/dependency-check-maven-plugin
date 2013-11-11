Dependency Check Maven Plugin
=============================

This is a Maven Plugin which is mainly used to check the missing artifacts(including dependencies and plugins) in a specified Maven Repository.

It can also be used to generate testing pom file to test against a [BOM](http://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html) project.

Each goal is kept as simple as possible to make it handy to use.


Maven Settings file to use this plugin:
---------------------------
A [maven-settings.xml](https://raw.github.com/gaol/dependency-check-maven-plugin/master/maven-settings.xml) is needed to use this plugin, because it is not released to any official Maven Repository yet.

After download this maven-settings.xml, you can run:

> mvn -s maven-settings.xml dependency-check:help

to list all goals.

Prerequisite:
------------
It requires that the <b>dependency:tree</b> run successfully:

> mvn -s maven-settings.xml dependency:tree

* NOTE: The maven-settings.xml defines some well-known maven repositories to increase the possibilities to make the dependency:tree succeed. 

Prefix of this plugin:
---------------------
All goals of this plugin has the prefix:
<pre>
dependency-check
</pre>


Plugin Document:
--------------
For more detail information, please see: [Plugin Document](http://gaol.github.io/dependency-check-maven-plugin/site/plugin-info.html)


If you want to check missing artifacts in a maven repository:
--------------------------------
1. First you download the [maven-settings.xml](https://raw.github.com/gaol/dependency-check-maven-plugin/master/maven-settings.xml)
2. Change your working directory to the maven project directory you want to check
3. Run following command for different cases:

* If you want to check missing artifacts of this project in Maven Central Repository:

> mvn -s maven-settings.xml dependency-check:check

* If you want to check other maven repository:

> mvn -s maven-settings.xml -DrepoId=jboss-public-repository dependency-check:check

where the <b>jboss-public-repository</b> is a predefined maven repository id.

   or you can run command:
   
> mvn -s maven-settings.xml -DrepoURL=http://repository.jboss.org/nexus/content/groups/public/ dependency-check:check

to specify the Maven repository url if there is no repo id predefined in current build context.

* If you dont' want to list some artifacts:

> mvn -s maven-settings.xml -DrepoId=jboss-public-repository -DexcludedPoms=http://maven.repository.redhat.com/earlyaccess/all/org/jboss/bom/eap6-supported-artifacts/6.2.0.Beta1/eap6-supported-artifacts-6.2.0.Beta1.pom,http://download.devel.redhat.com/brewroot/repos/jb-eap-6-rhel-6-build/latest/maven/org/jboss/jboss-parent/11-redhat-1/jboss-parent-11-redhat-1.pom dependency-check:check

or:

> mvn -s maven-settings.xml -DrepoId=jboss-public-repository -DexcludedArtifacts=org.apache.maven.plugins dependency-check:check

or specify both <b>-DexcludedPoms</b> and <b>-DexcludedArtifacts</b>


* If you want to print out the missing artifacts list to a file:

> mvn -s maven-settings.xml -DrepoId=jboss-public-repository -Doutput=/home/lgao/dep-list.txt dependency-check:check

If the output is an absolute file path, all missing artifacts will be written to this file in case of multiple modules project.



If you want to check your BOM project:
----------------------------------------
1. First you download the [maven-settings.xml](https://raw.github.com/gaol/dependency-check-maven-plugin/master/maven-settings.xml)
2. Change your working directory to the maven project directory you want to check
3. Run command:

> mvn -s maven-settings.xml dependency-check:generate-poms

If will generate each test pom file in *target/generated-bom-poms/* directory of your project root directory for each defined BOM module.

* If you want to specify the version of the BOM in the *import* scope, run:

> mvn -s maven-settings.xml -DbomVersion=XXXX dependency-check:generate-poms

* Also you can skip some dependencies like what the goal: <b>dependency-check:check</b> does.

After the pom files are generated, you can test the pom files using command:

> mvn -f target/generated-bom-poms/XXX--test-bom.pom dependency:tree

against the predefined maven repositories to see the BOM is in fine state in the maven repositories.

