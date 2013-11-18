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
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.dependency.utils.DependencyStatusSets;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;
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
    * Remote repositories which will be searched for plugins.
    */
   @Parameter( defaultValue = "${project.pluginArtifactRepositories}", readonly = true, required = true )
   private List<ArtifactRepository> remotePluginRepositories;
   
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
    * 
    * Include parent poms in the dependency resolution list.
    * 
    */
   @Parameter( property = "includeParents", defaultValue = "false" )
   private boolean includeParents;
   
   
   protected void doExecute() throws MojoExecutionException ,MojoFailureException {
      try
      {
         URL repoURLLink = null;
         if (this.repoURL != null && this.repoURL.trim().length() > 0)
         {
            repoURLLink = new URL(repoURL);
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
               
               repoURLLink = new URL(AbstractDependencyCheckMojo.MAVEN_CENTRAL_REPO_URL);
            }
            else
            {
               repoURLLink = new URL(repo.getUrl());
            }
         }
         String repoURL = repoURLLink.toString();
         if (!repoURL.endsWith("/"))
         {
            repoURL = repoURL + "/";
         }
         getLog().debug("Checking against repository: " + repoURL);
         
         if (this.outputFile == null)
         {
            getLog().debug("Will record missing artifacts into console out.");
         }
         else
         {
            getLog().info("Will record missing artifacts into: " + this.outputFile.getAbsolutePath());
            if (!this.outputFile.getParentFile().exists())
            {
               this.outputFile.getParentFile().mkdirs();
            }
         }
         
         // all artifacts
         Set<Artifact> artifacts = getAllArtifacts();
         for (Artifact artifact: artifacts)
         {
            // besides the default filter, there is another filter here.
            if (isArtifactExcluded(artifact))
            {
               getLog().debug("Artifact: " + gatv(artifact) + " is skipped during dependency check.");
               continue;
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
                  getLog().debug("Artifact: " + gatv(artifact) + " does not exist in repository: " + repoURL);
                  writeMissingArtifact(artifact);
               }
               else
               {
                  getLog().debug("Artifact: " + gatv(artifact) + " is resolved.");
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
      }
      catch (Exception e)
      {
         throw new MojoFailureException("Error: ", e);
      }
   };
   
   private Set<Artifact> getAllArtifacts() throws Exception
   {
      Set<Artifact> artifacts = new HashSet<Artifact>();
      
      // all dependencies
      DependencyStatusSets result = this.getDependencySets(false, includeParents);
      if (result.getResolvedDependencies() != null && !result.getResolvedDependencies().isEmpty())
      {
         artifacts.addAll(result.getResolvedDependencies());
      }
      if (result.getSkippedDependencies() != null && !result.getSkippedDependencies().isEmpty())
      {
         artifacts.addAll(result.getSkippedDependencies());
      }
      if (result.getUnResolvedDependencies() != null && !result.getUnResolvedDependencies().isEmpty())
      {
         artifacts.addAll(result.getUnResolvedDependencies());
      }
      
      // all plugins
      final Set<Artifact> plugins = resolvePluginArtifacts();
      for ( final Artifact plugin : plugins )
      {
         artifacts.add(plugin);
         // adds all plugin dependencies if not exclude transitive
         if (!this.excludeTransitive)
         {
            for ( final Artifact artifact : resolveArtifactDependencies( plugin ) )
            {
               artifacts.add(artifact);
            }
         }
      }
      return artifacts;
   }
   
   /**
    * This method resolves the plugin artifacts from the project.
    *
    * @return set of resolved plugin artifacts.
    * @throws ArtifactResolutionException
    * @throws ArtifactNotFoundException
    * @throws ArtifactFilterException 
    */
   @SuppressWarnings( "unchecked" )
   protected Set<Artifact> resolvePluginArtifacts()
       throws ArtifactResolutionException, ArtifactNotFoundException, ArtifactFilterException
   {
       final Set<Artifact> plugins = project.getPluginArtifacts();
       final Set<Artifact> reports = project.getReportArtifacts();

       Set<Artifact> artifacts = new HashSet<Artifact>();
       artifacts.addAll( reports );
       artifacts.addAll( plugins );

       final FilterArtifacts filter = getPluginArtifactsFilter();
       artifacts = filter.filter( artifacts );

       //        final ArtifactFilter filter = getPluginFilter();
       for ( final Artifact artifact : new HashSet<Artifact>( artifacts ) )
       {
           // resolve the new artifact
           this.resolver.resolve( artifact, this.remotePluginRepositories, this.getLocal() );
       }
       return artifacts;
   }
   
   @Override
   protected ArtifactsFilter getMarkedArtifactFilter()
   {
      return null;
   }
   
   private void writeMissingArtifact(Artifact artifact) throws IOException
   {
      PrintWriter writer = null;
      List<String> recorded = new ArrayList<String>();
      try
      {
         if (this.outputFile != null)
         {
            recorded = readRecordedFrom(outputFile);
            writer = new PrintWriter(new FileWriter(outputFile, true));
         }
         String logStr = gatv(artifact);
         if (recorded.contains(logStr))
         {
            getLog().info("Added Arleady: " + logStr);
            return;
         }
         getLog().debug("Log artifact: " + logStr);
         if (writer != null)
         {
            writer.println(logStr);
         }
         else
         {
            getLog().info(logStr);
         }
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
