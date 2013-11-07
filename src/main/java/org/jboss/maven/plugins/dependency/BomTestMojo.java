/**
 * 
 */
package org.jboss.maven.plugins.dependency;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;

/**
 * @author lgao
 *
 */
@Mojo(name = "bom-test", defaultPhase = LifecyclePhase.VERIFY, requiresDependencyResolution = ResolutionScope.COMPILE, requiresProject = true, threadSafe = true, aggregator = false)
public class BomTestMojo extends AbstractMojo
{

   @Component
   private MavenProject project;
   
   private static final String BOM_SUFFIX = "-test-bom";
   
   private static final String DEFAULT_VERSION = "1.0.0";
   
   public void execute() throws MojoExecutionException, MojoFailureException
   {
      DependencyManagement depmgmt = project.getDependencyManagement();
      if (depmgmt != null) {
          List<Dependency> dependencies = depmgmt.getDependencies();
          if (dependencies != null && dependencies.size() > 0)
          {
             File rootTargetDir = getProjectRootTargetDirectory();
             getLog().info("Generates poms to: " + rootTargetDir.getAbsolutePath());
             
             String groupId = project.getGroupId();
             String artifactId = project.getArtifactId();
             String version = project.getVersion();
             String type = project.getPackaging();
             if (!type.equals("pom"))
             {
                getLog().warn("packaging of a BOM should be pom");
                return;
             }
             
             Model model = new Model();
             model.setModelVersion("4.0.0");
             model.setName("JBoss BOM Test Pom");
             model.setGroupId(groupId + BOM_SUFFIX);
             model.setArtifactId(artifactId + BOM_SUFFIX);
             model.setVersion(DEFAULT_VERSION);
             
             DependencyManagement depMan = new DependencyManagement();
             model.setDependencyManagement(depMan);
             Dependency depInMan = new Dependency();
             
             depInMan.setGroupId(groupId);
             depInMan.setArtifactId(artifactId);
             depInMan.setVersion(version);
             depInMan.setType(type);
             depInMan.setScope("import");
             depMan.addDependency(depInMan);
             
             for (Dependency dep : dependencies) {
                if (dep.getScope() != null
                    && (dep.getScope().equals("runtime") || dep.getScope().equals("system"))) {
                   
                    getLog().debug("Ignoring runtime/system dependency " + dep);
                    continue;
                }
                // we only need groupId and artifactId
                Dependency depClone = new Dependency();
                depClone.setGroupId(dep.getGroupId());
                depClone.setArtifactId(dep.getArtifactId());
                
                model.addDependency(depClone);
            }
             
             File generatedPom = new File(rootTargetDir, groupId + "-" + artifactId + "-" + version + BOM_SUFFIX + ".pom");
             getLog().info("Generates test bom pom at: " + generatedPom.getAbsolutePath());
             
             OutputStream out = null;
             try
             {
                out = new FileOutputStream(generatedPom);
                new MavenXpp3Writer().write(out, model);
             }
             catch(IOException e)
             {
                throw new MojoExecutionException("Can't generate test bom POM file: " + generatedPom.getAbsolutePath(), e);
             }
             finally
             {
                IOUtil.close(out);
             }
             
          }
      }
   }
   
   /**
    * Gets the project root target directory, where the poms are generated to.
    */
   private File getProjectRootTargetDirectory()
   {
      MavenProject current = project;
      while (current.getParent() != null)
      {
         current = current.getParent();
      }
      File rootDir = current.getBasedir();
      File rootTargetDir = new File(rootDir, "target/generated-bom-poms/");
      if (!rootTargetDir.exists())
      {
         rootTargetDir.mkdirs();
      }
      return rootTargetDir;
   }
   

}
