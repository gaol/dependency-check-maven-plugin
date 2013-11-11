/**
 * 
 */
package org.jboss.maven.plugins.dependency;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.IOUtil;

/**
 * 
 * Goal of "dependency-check:check" is used to check missing artifacts in a specified Maven Repository.
 * 
 * The format of the list printed out is: G:A:T:V, where G is the groupId, A is the ArtifactId, T is the Type, V is the version.
 * 
 * It will print the list to console by default, or you can specify an output file by a parameter: <b>-Doutput=</b>.
 * 
 * This goal checks only one Maven Repository a time, the <b>-DrepoURL</b> has higher priority than <b>-DrepoId</b>.
 *  
 * @author lgao@redhat.com
 *
 */
@Mojo( name = "check", requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true )
public class DependencyCheckMojo extends AbstractDependencyCheckMojo
{
   
   // fields -----------------------------------------------------------------

   
   /**
    * 
    * Which repository do you want to check against. Default is the maven central repository
    * The value is one of predefined remote repositories in the current active profiles.
    * 
    */
   @Parameter(property = "repoId", defaultValue = "central")
   private String repoId;
   
   /**
    * Instead of specifies a predefined repoId, a Maven Repository URL can be specified.
    * This parameter has higher priority.
    */
   @Parameter(property = "repoURL")
   private String repoURL;
   
   /**
    * Specify the file where the missing artifacts list will be written to. 
    * 
    * If not specified, it will print list to console out.
    * 
    * If an absolute file path(starts with '/') is specified, all missing artifacts will be written to this file in case of multiple modules project.
    * 
    */
   @Parameter(property = "output")
   private File output;
   
   
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
      

      URL repoURLLink = null;
      if (this.repoURL != null && this.repoURL.trim().length() > 0)
      {
         try
         {
            repoURLLink = new URL(repoURL);
         }
         catch (MalformedURLException e)
         {
            throw new MojoExecutionException("Wrong repository URL: " + this.repoURL, e);
         }
      }
      if (repoURLLink == null)
      {
         ArtifactRepository repo = getArtifactRepository();
         if (repo == null && this.repoId != null)
         {
            throw new MojoFailureException("Unkown repository: " + this.repoId);
         }
         if (repo == null)
         {
            try
            {
               repoURLLink = new URL(MAVEN_CENTRAL_REPO_URL);
            }
            catch (MalformedURLException e)
            {
            }
         }
         else
         {
            try
            {
               repoURLLink = new URL(repo.getUrl());
            }
            catch (MalformedURLException e)
            {
               throw new MojoFailureException("Unkown repository: " + repo.getUrl(), e);
            }
         }
      }
      if (repoURLLink == null)
      {
         throw new MojoExecutionException("Maven Repository is not specified.");
      }
      getLog().debug("Checking against repository: " + repoURLLink.toString());
      
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
      
      List<String> missingArtifacts = new ArrayList<String>();
      for (Artifact artifact: artifacts)
      {
         String groupId = artifact.getGroupId();
         String arId = artifact.getArtifactId();
         if (projectGroupId.equals(groupId))
         {
            getLog().debug("Artifact: " + gatv(artifact) + " will be skipped.");
            continue;
         }
         
         if (isArtifactExcluded(groupId, arId, artifact.getVersion(),artifact.getScope()))
         {
            continue;
         }
         
         getLog().debug("Checking Artifact: " + gatv(artifact));
         
         String repoURL = repoURLLink.toString();
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
      List<String> recorded = new ArrayList<String>();
      try
      {
         if (output != null)
         {
            recorded = readRecordedFrom(output);
            writer = new PrintWriter(new FileWriter(output, true));
         }
         else
         {
            getLog().info("Missing artifacts in Maven Repository: " + repoURLLink.toString() + " are:");
         }
         getLog().debug("Added are: " + recorded);
         for (String artiStr: missingArtifacts)
         {
            if (recorded.contains(artiStr))
            {
               getLog().info("Added Arleady: " + artiStr);
               continue;
            }
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
         throw new MojoExecutionException("Error to write data into file: " + output.getAbsolutePath(), e);
      }
      finally
      {
         IOUtil.close(writer);
      }
   }
   
   private List<String> readRecordedFrom(File file) throws IOException
   {
      List<String> list = new ArrayList<String>();
      if (!file.exists() || !file.canRead())
      {
         return list;
      }
      BufferedReader reader = null;
      try
      {
         reader = new BufferedReader(new FileReader(file));
         String line = null;
         while((line = reader.readLine()) != null)
         {
            list.add(line);
         }
      }
      finally
      {
         IOUtil.close(reader);
      }
      return list;
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
   
   
}
