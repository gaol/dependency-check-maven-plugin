/**
 * 
 */
package org.jboss.maven.plugins.dependency;

/**
 * @author lgao
 *
 */
public class CollectConfig
{

   /**
    * Which profile will be used during build.
    */
   private String profile;
   
   /**
    * Whether the version is included in the collected string
    */
   private boolean includeVersion;
   

   /**
    * @return the includeVersion
    */
   public boolean isIncludeVersion()
   {
      return includeVersion;
   }


   /**
    * @param includeVersion the includeVersion to set
    */
   public void setIncludeVersion(boolean includeVersion)
   {
      this.includeVersion = includeVersion;
   }


   /**
    * @return the profile
    */
   public String getProfile()
   {
      return profile;
   }


   /**
    * @param profile the profile to set
    */
   public void setProfile(String profile)
   {
      this.profile = profile;
   }

}
