/**
 * 
 */
package org.jboss.maven.plugins.dependency;

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

/**
 * @author lgao
 *
 */
@Mojo( name = "check", requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true )
public class DependencyCheckMojo extends AbstractMojo
{
   // fields -----------------------------------------------------------------

   /**
    * The Maven project.
    */
   @Component
   private MavenProject project;
   
   @Parameter(property = "repoId", defaultValue = "central")
   private String repoId;
   
   public void execute() throws MojoExecutionException, MojoFailureException
   {
      // TODO Auto-generated method stub
      Set<Artifact> artifacts = project.getArtifacts();
      
      for (Artifact artifact: artifacts)
      {
         
         getLog().info(artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion() + ":" + artifact.getScope());
      }
      
      List<ArtifactRepository> artifactRepos = project.getRemoteArtifactRepositories();
      if (artifactRepos != null && artifactRepos.size() > 0)
      {
         for (ArtifactRepository artiRepo: artifactRepos)
         {
            getLog().info("Repo: " + artiRepo.getId() + ":" + artiRepo.getUrl());
         }
      }
      
      
      
   }

}
