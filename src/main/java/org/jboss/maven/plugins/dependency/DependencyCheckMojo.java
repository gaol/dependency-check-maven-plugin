/**
 * 
 */
package org.jboss.maven.plugins.dependency;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * 
 * The Maven plugin used to check whether all plugins/dependencies are available in a specified maven repository.
 * It will print out all missing artifacts in format: G:A:T:V. 
 * 
 * @author lgao
 *
 */
@Mojo( name = "check", requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true )
public class DependencyCheckMojo extends AbstractMojo
{
   private static final List<String> excludedGroupIds = new ArrayList<String>();
   static
   {
      excludedGroupIds.add("org.apache.maven.plugins");
   }
   
   // fields -----------------------------------------------------------------

   /**
    * The Maven project.
    */
   @Component
   private MavenProject project;
   
   /**
    * Which repository do you want to check against. Default is the maven central repository
    * The value is one of predefined remote repositories in the current active profiles.
    * 
    */
   @Parameter(property = "repoId", defaultValue = "central")
   private String repoId;
   
   
   /**
    * The output file which contains list of missing dependencies or plugins. 
    * If not specified, it will print list to console out.
    * 
    */
   @Parameter(property = "output")
   private File output;
   
   /**
    * Excluded poms, where plugins/dependencies are defined in their &lt;pluginManagement&gt; and &lt;dependencyManagement&gt; section.
    * Splits using comma: ','.
    */
   @Parameter(property = "excludedPoms")
   private List<String> excludedPoms; 
   
   public void execute() throws MojoExecutionException, MojoFailureException
   {
      
      String projectGroupId = project.getGroupId();
      
      // dependencies
      Set<Artifact> dependencies = project.getArtifacts();
      
      // plugins
      Set<Artifact> plugins = project.getPluginArtifacts();
      
      // collect all artifacts needed
      Set<Artifact> artifacts = new HashSet<Artifact>();
      artifacts.addAll(dependencies);
      artifacts.addAll(plugins);
      
      if (output == null)
      {
         getLog().debug("Will record missing artifacts into console out.");
      }
      else
      {
         getLog().info("Will record missing artifacts into: " + output.getAbsolutePath());
         if (!output.getParentFile().exists())
         {
            output.getParentFile().mkdirs();
         }
      }

      ArtifactRepository repo = getArtifactRepository();
      if (repo == null)
      {
         throw new MojoFailureException("Unkown repository: " + this.repoId);
      }
      
      getLog().debug("Checking against repository: " + repo.getId());
      
      List<String> excludedGAs;
      try
      {
         excludedGAs = getExcludedGAs();
      }
      catch (IOException e)
      {
         throw new MojoExecutionException("Error when reading from excluded poms", e);
      }
      catch (XmlPullParserException e)
      {
         throw new MojoExecutionException("Error when parsing the excluded poms", e);
      }
      
      List<String> missingArtifacts = new ArrayList<String>();
      for (Artifact artifact: artifacts)
      {
         String groupId = artifact.getGroupId();
         String arId = artifact.getArtifactId();
         if (projectGroupId.equals(groupId) || excludedGroupIds.contains(groupId))
         {
            getLog().debug("Artifact: " + gatv(artifact) + " will be skipped.");
            continue;
         }
         
         if (excludedGAs != null && excludedGAs.size() > 0)
         {
            if (excludedGAs.contains(groupId + ":" + arId))
            {
               getLog().debug("Artifact: " + gatv(artifact) + " will be skipped.");
               continue;
            }
         }
         
         getLog().debug("Checking Artifact: " + gatv(artifact));
         
         
         String repoURL = repo.getUrl();
         if (!repoURL.endsWith("/"))
         {
            repoURL = repoURL + "/";
         }
         String artifactLink = repoURL + artifact.getGroupId().replaceAll("\\.", "/") + "/" + artifact.getArtifactId() + "/" + artifact.getVersion() + "/" + artifact.getArtifactId() + "-" + artifact.getVersion() + ".pom";
         getLog().debug("Checking artifact: " + gatv(artifact) + " at: " + artifactLink);
         HttpURLConnection urlConn = null;
         try
         {
            URL linkURL = new URL(artifactLink);
            urlConn = (HttpURLConnection)linkURL.openConnection();
            urlConn.connect();
            int code = urlConn.getResponseCode();
            if (code >= 400)
            {
               getLog().debug("\tNot found.");
               missingArtifacts.add(gatv(artifact));
            }
         }
         catch (IOException e)
         {
            getLog().warn("Wrong link: " + artifactLink, e);
         }
         finally
         {
            urlConn.disconnect();
         }
      }
      
      PrintWriter writer = null;
      try
      {
         if (output != null)
         {
            writer = new PrintWriter(new FileWriter(output, true));
         }
         for (String artiStr: missingArtifacts)
         {
            getLog().debug("Added missing artifact: " + artiStr);
            if (writer != null)
            {
               writer.println(artiStr);
            }
            else
            {
               getLog().info(artiStr);
            }
         }
      }
      catch (IOException e)
      {
         getLog().warn("Error to write data into file: " + output.getAbsolutePath(), e);
      }
      finally
      {
         IOUtil.close(writer);
      }
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
      for (String pom: excludedPoms)
      {
         URL pomURL = new URL(pom);
         List<String> gas = collector.collectDeclaredArtifacts(pomURL, null);
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

   /**
    * Gets current ArtifactRepository which will be used to check against
    */
   private ArtifactRepository getArtifactRepository()
   {
      List<ArtifactRepository> artifactRepos = project.getRemoteArtifactRepositories();
      if (artifactRepos != null && artifactRepos.size() > 0)
      {
         for (ArtifactRepository artiRepo: artifactRepos)
         {
            if (artiRepo.getId().equals(this.repoId))
            {
               return artiRepo;
            }
         }
      }
      return null;
   }
   
   
   private String gatv(Artifact artifact)
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

}
