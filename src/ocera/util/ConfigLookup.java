package ocera.util;
/*
 * ConfigLookup.java
 */

import java.io.*;
import java.net.*;

/**
 *
 * @author  fanda
 */
public class ConfigLookup 
{
    protected String configDir = ".config";
    protected String resourceDir = "resources";
    protected String configFileName = "config.conf.xml";
    protected Class resourceClass = null;
    
    /** Creates a new instance of ConfigLookup */
    public ConfigLookup(String confdir, String conffilename) 
    {
        configDir = confdir;
        configFileName = conffilename;
    }
    public ConfigLookup(String confdir, String conffilename, Class resclass) 
    {
        configDir = confdir;
        configFileName = conffilename;
        resourceClass = resclass;
    }
    
    /** makes 'configDir' in HOME directory if it doesn't exist
     * @return true if config dir exists
     */
    public boolean makeConfigDir() 
    {
        String home = System.getProperty("user.home", ".");
        // look if home exists
        File ff;
        String conf_dir = home + "/" + configDir;
        ff = new File(conf_dir);
        if(!ff.exists()) {
            // try to make directory at home
            if(ff.mkdir()) return true;
        }
        else return true;
        return false;
    }
    
    /** looks for the configFileName in following locations:<br>
     * 1. <CODE>HOME/configDir/configFileName</CODE><br>
     * 2. <CODE>RESOURCE_CLASS_DIR/resourceDir/configFileName</CODE> (only if resourceClass != null)<br>
     * @return name of found filename or null
     */
    public String findConfigFile() 
    {
        // try to find configuration
        String home = System.getProperty("user.home", ".");
        String conf_file;
        String conf_dir;

        conf_dir = home + "/" + configDir;
        conf_file = conf_dir + "/" + configFileName;
        File ff = new File(conf_file);
        if(ff.canRead()) return conf_file;
        
        if(resourceClass != null) {
            // find default config in resources
            URL url = resourceClass.getResource("resources/" + configFileName);
            if(url != null) return url.toExternalForm();
            else System.err.println("resource: 'resources/" + configFileName + "' NOT FOUND!");
        }
        
        return null;
    }

    /** Creates empty config file in the HOME/configDir directory. If created file
     * already exists function does nothing and returns <CODE>name of existing file</CODE>
     * @return name of created file or null if the file doesn't exist and its creation fails
     */    
    public String createConfigFile() 
    {
        String conf_file = null;
        if(makeConfigDir()) {
            String home = System.getProperty("user.home", ".");
            String conf_dir = home + "/" + configDir;
            conf_file = conf_dir + "/" + configFileName;
            File ff = new File(conf_file);
            try {
                ff.createNewFile();
            }
            catch(Exception e) {
                conf_file = null;
            }
        }
        return conf_file;
    }
}
