package au.com.postnewspapers.postupload.config;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

/**
 * This container-managed singleton class maintains configuration
 * information about the file handler.
 * 
 * Settings in this class are accessible across the app, and may be set
 * via JAX-RS calls or from JSF2 pages. Make sure access to these is limited
 * to the ADMIN role in web.xml, keeping in mind that you need to lock down
 * both direct REST (JAX-RS) and indirect access via jsf2 pages.
 * 
 * @author craig
 */
@ApplicationScoped
// Make this class accessible via JAX-RS and JSF2/EL
@Named
@Path("/config")
public class FileHandlerConfig implements Serializable {
    public static final long serialVersionUid = 54555L;
    
    private static final Logger logger = Logger.getLogger(FileHandlerConfig.class.getName());
    private static final Preferences prefsRoot = Preferences.userNodeForPackage(FileHandlerConfig.class);
    private static final String 
            PREFS_KEY_TEMP_PATH = "tempPath",
            PREFS_KEY_OUTPUT_PATH = "outputPath";
    
    private File tempOutputDir, finalOutputDir;
    
    {
        prepareTempOutputDir();
        prepareFinalOutputDir();
    }
    
    
    
    /**
     * Get any user-configured output path. This will be
     * null or the empty string if the default system temporary
     * folder should be used, otherwise it'll be a path relative
     * to the server.
     * 
     * @return String representing temp path
     */
    @GET
    @Path("/temppath")
    public String getConfigTempPath() {
        synchronized(prefsRoot) {
            return prefsRoot.get(PREFS_KEY_TEMP_PATH, null);
        }
    }
    
    /**
     * Assign a new temporary output path
     * @param tempPath Empty string for system default, or custom path
     */
    @PUT
    @Path("/temppath")
    public void setConfigTempPath(String tempPath) {
        synchronized(prefsRoot) {
            prefsRoot.put(PREFS_KEY_TEMP_PATH, tempPath);
            prepareTempOutputDir();
        }
    }
    
    /**
     * Get the application's output path. 
     * 
     * @return Server-relative path where output is saved
     */
    @GET
    @Path("/outputpath")
    public String getConfigOutputPath() {
        synchronized(prefsRoot) {
            return prefsRoot.get(PREFS_KEY_OUTPUT_PATH, null);
        }
    }
    
    /**
     * Set the server-relative path where output is to be saved.
     * 
     * @param outputPath Server-relative path where output is saved
     */
    @PUT
    @Path("/outputpath")
    public void setConfigOutputPath(String outputPath) {
        if (outputPath == null || outputPath.isEmpty()) {
            throw new IllegalArgumentException("Output path may not be empty or null");
        }
        synchronized(prefsRoot) {
            prefsRoot.put(PREFS_KEY_OUTPUT_PATH, outputPath);
            prepareFinalOutputDir();
        }
    }
    
    
    /**
     * @return The File that'll be used to write temp files. This is
     *         the path from the configTempPath property if one is set,
     *         and the system temporary file path otherwise.
     */
    public File getTempOutputDir() {
        return tempOutputDir;
    }
    
    /**
     * @return The File for the dir that'll be used to write the final output
     *         after an upload completes.
     */
    public File getFinalOutputDir() {
        return finalOutputDir;
    }
    
    /**
     * Get a list of recipients that might receive files.
     * 
     * @return List of possible recipients
     */
    public List<InternetAddress> getPossibleRecipients() {
        // TODO fetch this dynamically
        ArrayList<InternetAddress> possibleRecipients = new ArrayList<InternetAddress>();
        try {
            possibleRecipients.add(new InternetAddress("Craig Ringer <craig@postnewspapers.com.au>"));
        } catch (AddressException ex) {
            throw new RuntimeException(ex);
        }
        return possibleRecipients;
    }
    
    /**
     * Get a list of recipients that might receive files.
     * 
     * @return List of possible recipients
     */
    public List<String> getPossibleRecipientsAsString() {
        List<InternetAddress> recips = getPossibleRecipients();
        ArrayList<String> stringRecips = new ArrayList<String>(recips.size());
        for (InternetAddress a: recips) {
            stringRecips.add(a.toString());
        }
        return stringRecips;
    }
    
    // check preferences to determine the temporary output dir and 
    // update the File property for it appropriately.
    private void prepareTempOutputDir() {
        // If no temporary item path is defined, use the system
        // temp folder.
        String prefsTempPath = prefsRoot.get(PREFS_KEY_TEMP_PATH, null);
        if (prefsTempPath == null || prefsTempPath.isEmpty()) {
            tempOutputDir = new File(System.getProperty("java.io.tmpdir"),"postupload_temp");
        } else {
            tempOutputDir = new File(prefsTempPath);
        }
        if (!tempOutputDir.exists()) {
            tempOutputDir.mkdirs();
        }
    }
    
    // check preferences to determine the final output dir and 
    // update the File property for it appropriately.
    private void prepareFinalOutputDir() {
        String prefsOutputPath = prefsRoot.get(PREFS_KEY_OUTPUT_PATH, "");
        if (prefsOutputPath == null || prefsOutputPath.isEmpty()) {
            String outDir = System.getProperty("java.io.tmpdir") + File.separator + "postupload_done";
            logger.log(Level.SEVERE, "No output path set. Files will be output to temporary path: {0}", outDir);
            finalOutputDir = new File(outDir);
        } else {
            finalOutputDir = new File(prefsOutputPath);
            if (!finalOutputDir.exists()) {
                finalOutputDir.mkdirs();
            }
        }
    }
}
