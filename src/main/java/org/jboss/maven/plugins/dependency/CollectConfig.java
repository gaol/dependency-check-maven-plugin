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
