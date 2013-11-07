Dependency Check Maven Plugin
=============================

A Maven Plugin used to check which dependencies or plugins are available in which remote repository


There is a <a href="https://raw.github.com/gaol/dependency-check-maven-plugin/master/maven-settings.xml">maven-settings.xml</a> file in this source code tree, when you need to use this maven plugin, please download this file to your local machine, and then in your project root directory, run:
<pre>
   mvn -s maven-settings.xml dependency-check:check
</pre>

The default action will list all missing artifacts(including dependencies and plugins) in Maven Central Repository.

Of cause, you can specify other Maven repository to check against, just specify the maven repository id using command:

<pre>
   mvn -s maven-settings.xml -DrepoId=jboss-public-repository dependency-check:check
</pre>

   * NOTE: The repoId needs to be predefined in your pom or the maven-settings.xml file, you can specify one by modifying the maven-settings.xml after you downloaded it.

There are 3 predefined Maven Repositores in the <a href="https://raw.github.com/gaol/dependency-check-maven-plugin/master/maven-settings.xml">maven-settings.xml</a> file, they are:
   * jboss-public-repository : http://repository.jboss.org/nexus/content/groups/public/
   * mead-repository  : http://download.lab.bos.redhat.com/brewroot/repos/jb-eap-6-rhel-6-build/latest/maven/
   * dependency-check-plugin-repository : http://gaol.github.io/dependency-check-maven-plugin/

This plugin is hosted at the repository: <b>dependency-check-plugin-repository</b>, so you can use this plugin without adding the plugin into your pom files.

The plugin has only one goal: <b>check</b>, and it has many properties to customerize the check operation.

The following example shows to list all missing artifacts from<b>mead-repository</b>, but not those dependencies defined in the<b>eap6-supported-artifacts-6.2.0.Beta1.pom</b>, nor those plugins defined in the <b>jboss-parent-11-redhat-1.pom</b>, nor the artifacts which has the groupId: <b>org.apache.maven.plugins</b>, and then it will write the missing artifacts list to file: <b>/home/lgao/deps-list.txt</b>.

<pre>
   mvn -s maven-settings.xml -DrepoId=mead-repository -DexcludedPoms=http://maven.repository.redhat.com/earlyaccess/all/org/jboss/bom/eap6-supported-artifacts/6.2.0.Beta1/eap6-supported-artifacts-6.2.0.Beta1.pom,http://download.devel.redhat.com/brewroot/repos/jb-eap-6-rhel-6-build/latest/maven/org/jboss/jboss-parent/11-redhat-1/jboss-parent-11-redhat-1.pom -Doutput=/home/lgao/deps-list.txt -DexcludedArtifacts=org.apache.maven.plugins dependency-check:check
</pre>


You can run:

<pre>
   mvn -s maven-settings.xml dependency-check:help -Ddetail=true -Dgoal=check
</pre>

to see more detail about the plugin properties.

Below is the information got from the command above:

<pre>
[INFO] Apache Maven Dependency Check Plugin 1.0.1
  Provides utility goals to check which dependencies are in which repository

dependency-check:check
  The Maven plugin used to check whether all plugins/dependencies are available
  in a specified maven repository. It will print out all missing artifacts in
  format: G:A:T:V.

  Available parameters:

    excludedArtifacts
      Excluded Artifacts, specify the GroupId[:ArtifactId][:version] to filter
      the artifacts out from the missing artifacts. If artifactId is specified,
      then only that artifact of the same groupId will be filtered, otherwise,
      all artifactIds of that groupId will be filtered. If version is specified,
      then only that version of artifact will be filtered, otherwise, all
      version of that artifact will be filtered.

    excludedPoms
      Excluded poms, where plugins/dependencies are defined in their
      <pluginManagement> and <dependencyManagement> section. Splits using comma:
      ','.

    output
      The output file which contains list of missing dependencies or plugins. If
      not specified, it will print list to console out.

    repoId
      Which repository do you want to check against. Default is the maven
      central repository The value is one of predefined remote repositories in
      the current active profiles.

</pre>


   * NOTE: If the <b>-Doutput=</b> is specified to a absolute file path, all missing artifacts will be written to this file in case of multiple modules project.
