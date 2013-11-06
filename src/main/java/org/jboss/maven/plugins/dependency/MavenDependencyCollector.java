/**
 * 
 */
package org.jboss.maven.plugins.dependency;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * @author lgao
 *
 */
public class MavenDependencyCollector
{
   
   private Log logger;
   
   public MavenDependencyCollector()
   {
      super();
   }
   

   /**
    * 
    * Collects each GroupId:ArtifactId key-value pairs defined in &lt;pluginManagement&gt; and &lt;dependencyManagement&gt; section.
    * It will NOT check any sub modules.
    * For example: org.jboss.logging:jboss-logging
    * 
    * 
    * @return
    * @throws XmlPullParserException 
    * @throws IOException 
    */
   public List<String> collectDeclaredArtifacts(URL pomURL, CollectConfig config) throws IOException, XmlPullParserException
   {
      if (config == null)
      {
         config = new CollectConfig();
      }
      List<String> dependencies = new ArrayList<String>();
      
      if (logger != null)
      {
         logger.info("Check dependencies of: " + pomURL.toString());
      }
      
      Model model = readMavenModel(pomURL);
      if (model == null)
      {
         throw new RuntimeException("Maven Model of pom: " + pomURL.toString() + " can not be parsed.");
      }
      
      List<Dependency> allDependencies = new ArrayList<Dependency>();
      List<Plugin> allPlugins = new ArrayList<Plugin>();
      
      DependencyManagement dependencyMan = model.getDependencyManagement();
      if (dependencyMan != null)
      {
         List<Dependency> deps = dependencyMan.getDependencies();
         mergeDependencies(deps,allDependencies);
         Profile profile = getBuildProfile(model, config);
         if (profile != null)
         {
            DependencyManagement dependencyManInProfile = profile.getDependencyManagement();
            if (dependencyManInProfile != null)
            {
               List<Dependency> dpesInProfile = dependencyManInProfile.getDependencies();
               mergeDependencies(dpesInProfile, allDependencies);
            }
         }
      }
      
      Build build = model.getBuild();
      if (build != null)
      {
         PluginManagement pluginMan = build.getPluginManagement();
         if (pluginMan != null)
         {
            List<Plugin> plugins = pluginMan.getPlugins();
            mergePlugins(plugins, allPlugins);
         }
         Profile profile = getBuildProfile(model, config);
         if (profile != null)
         {
            BuildBase buildInProfile = profile.getBuild();
            if (buildInProfile != null)
            {
               PluginManagement pluginManInProfile = build.getPluginManagement();
               if (pluginManInProfile != null)
               {
                  List<Plugin> pluginsInProfile = pluginManInProfile.getPlugins();
                  mergePlugins(pluginsInProfile, allPlugins);
               }
            }
         }
      }
      
      for (Dependency dep: allDependencies)
      {
         String groupId = dep.getGroupId();
         String artiId = dep.getArtifactId();
         String line = groupId + ":" + artiId;
         if (!dependencies.contains(line))
         {
            dependencies.add(line);
         }
      }
      
      for (Plugin plugin: allPlugins)
      {
         String groupId = plugin.getGroupId();
         String artiId = plugin.getArtifactId();
         String line = groupId + ":" + artiId;
         if (!dependencies.contains(line))
         {
            dependencies.add(line);
         }
      }
      
      return dependencies;
   }
   
   /**
    * Dependency in from list overrides what is in to list
    */
   private void mergeDependencies(List<Dependency> from, List<Dependency> to)
   {
      for (Dependency dep: from)
      {
         Dependency depInToList = getDependencyFromList(to, dep);
         if (depInToList != null)
         {
            depInToList.setVersion(dep.getVersion());
         }
         else
         {
            to.add(dep);
         }
      }
   }
   
   private void mergePlugins(List<Plugin> from, List<Plugin> to)
   {
      for (Plugin plugin: from)
      {
         Plugin pluginInToList = getPluginFromList(to, plugin);
         if (pluginInToList != null)
         {
            pluginInToList.setVersion(plugin.getVersion());
         }
         else
         {
            to.add(plugin);
         }
      }
   }

   private Plugin getPluginFromList(List<Plugin> plugins, Plugin pluginToCheck)
   {
      for (Plugin plugin: plugins)
      {
         if (plugin.getGroupId().equals(pluginToCheck.getGroupId()) && plugin.getArtifactId().equals(pluginToCheck.getArtifactId()))
         {
            return pluginToCheck;
         }
      }
      return null;
   }

   private Dependency getDependencyFromList(List<Dependency> dependencies, Dependency depToCheck)
   {
      for (Dependency dependency: dependencies)
      {
         if (dependency.getGroupId().equals(depToCheck.getGroupId()) && dependency.getArtifactId().equals(depToCheck.getArtifactId()))
         {
            return depToCheck;
         }
      }
      return null;
   }

   /**
    * @param pomURL
    * @return
    * @throws IOException
    * @throws XmlPullParserException
    */
   private Model readMavenModel(URL pomURL) throws IOException, XmlPullParserException
   {
      Model model = null;
      InputStream input = null;
      try
      {
         input = pomURL.openStream();
         model = new MavenXpp3Reader().read(input);
      }
      finally
      {
         IOUtil.close(input);
      }
      return model;
   }

   /** Single Maven Model **/
   private Profile getBuildProfile(Model model, CollectConfig config)
   {
      if (config.getProfile() != null && model.getProfiles() != null && model.getProfiles().size() > 0)
      {
         for (Profile p: model.getProfiles())
         {
            if (config.getProfile().equals(p.getId()))
            {
               return p;
            }
         }
      }
      return null;
   }
   
   /**
    * @return the logger
    */
   public Log getLogger()
   {
      return logger;
   }

   /**
    * @param logger the logger to set
    */
   public void setLogger(Log logger)
   {
      this.logger = logger;
   }
}
