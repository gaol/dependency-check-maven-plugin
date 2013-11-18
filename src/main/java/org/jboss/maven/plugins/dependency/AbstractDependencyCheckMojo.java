/**
 * 
 */
package org.jboss.maven.plugins.dependency;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.resolvers.AbstractResolveMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * @author lgao
 *
 */
public abstract class AbstractDependencyCheckMojo extends AbstractResolveMojo
{
   
   public static final String MAVEN_CENTRAL_REPO_URL = "http://central.maven.org/maven2/";
   
   // This is used for cache
   protected static List<String> excludedGAs;
   
   /**
    * Excluded poms, where plugins/dependencies are defined in their &lt;pluginManagement&gt; and &lt;dependencyManagement&gt; section.
    * 
    * Specifies using their URLs, and splits using comma: ','.
    * 
    * All defined dependencies in the poms will be skipped during missing artifacts collection.
    */
   @Parameter(property = "excludedPoms")
   protected List<String> excludedPoms;
   
   /**
    * Whether include version string during the artifacts collection.
    * 
    * If not, only groupId:artifactId, if yes, will be groupId:artifactId:version
    */
   @Parameter(property = "includeVersion")
   protected boolean includeVersion;
   
   /**
    * Specify which profile will be active.
    */
   @Parameter(property = "profile")
   protected String profile;

   /**
    * Specify the scope in which the artifacts will be excluded.
    */
   @Parameter(property = "scope")
   protected String scope;
   
   /**
    * 
    * Excluded Artifacts, specify the GroupId[:ArtifactId][:version] to filter the artifacts out from the missing artifacts.
    * If artifactId is specified, then only that artifact of the same groupId will be filtered, otherwise, all artifactIds of that groupId will be filtered.
    * If version is specified, then only that version of artifact will be filtered, otherwise, all version of that artifact will be filtered.
    */
   @Parameter(property = "excludedArtifacts")
   protected List<String> excludedArtifacts;
   
   
   protected String gatv(Artifact artifact)
   {
      StringBuilder sb = new StringBuilder();
      sb.append(artifact.getGroupId());
      sb.append(":");
      sb.append(artifact.getArtifactId());
      sb.append(":");
      sb.append(artifact.getType());
      sb.append(":");
      sb.append(artifact.getVersion());
      return sb.toString();
   }
   
   protected String ga(Dependency dep)
   {
      StringBuilder sb = new StringBuilder();
      sb.append(dep.getGroupId());
      sb.append(":");
      sb.append(dep.getArtifactId());
      return sb.toString();
   }
   
   private List<String> getExcludedGAs() throws IOException, XmlPullParserException
   {
      if (excludedPoms == null || excludedPoms.size() == 0)
      {
         return null;
      }
      getLog().debug("Checking excluded poms: " + excludedPoms);
      List<String> artifactsGAs = new ArrayList<String>();
      MavenDependencyCollector collector = new MavenDependencyCollector();
      collector.setLogger(getLog());
      CollectConfig config = new CollectConfig();
      config.setIncludeVersion(includeVersion);
      config.setProfile(profile);
      for (String pom: excludedPoms)
      {
         URL pomURL = new URL(pom);
         List<String> gas = collector.collectDeclaredArtifacts(pomURL, config);
         for (String ga: gas)
         {
            if (!artifactsGAs.contains(ga))
            {
               artifactsGAs.add(ga);
            }
         }
      }
      return artifactsGAs;
   }
   
   protected boolean isArtifactExcluded(Artifact artifact) throws MojoExecutionException
   {
      return isArtifactExcluded(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getScope());
   }
   
   protected boolean isDependencyExcluded(Dependency dependency) throws MojoExecutionException
   {
      return isArtifactExcluded(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), dependency.getScope());
   }
   
   private boolean isArtifactExcluded(String groupId, String artifactId, String version, String artifactScope) throws MojoExecutionException
   {
      if (excludedGAs == null)
      {
         try
         {
            excludedGAs = getExcludedGAs();
            if (excludedGAs == null)
            {
               excludedGAs = new ArrayList<String>(0);
            }
         }
         catch (IOException e)
         {
            throw new MojoExecutionException("Error when reading from excluded poms", e);
         }
         catch (XmlPullParserException e)
         {
            throw new MojoExecutionException("Error when parsing the excluded poms", e);
         }
      }
      
      if (artifactScope == null || artifactScope.length() == 0)
      {
         artifactScope = "compile";
      }

      String line = groupId + ":" + artifactId + ":" + version;
      
      getLog().debug("Checking if " + line + " should be skipped during dependency check.");
      
      if (this.scope != null)
      {
         getLog().debug("Excluded scope: " + scope);
      }
      if (this.scope != null && this.scope.trim().equals(artifactScope))
      {
         return true;
      }
      
      if (excludedGAs != null && excludedGAs.size() > 0)
      {
         for (String gaV: excludedGAs)
         {
            if (line.startsWith(gaV))
            {
               return true;
            }
         }
      }
      
      if (excludedArtifacts != null && excludedArtifacts.size() > 0)
      {
         
         for (String excludedArti: excludedArtifacts)
         {
            if (line.startsWith(excludedArti))
            {
               return true;
            }
         }
      }
      return false;
   }
   

}
