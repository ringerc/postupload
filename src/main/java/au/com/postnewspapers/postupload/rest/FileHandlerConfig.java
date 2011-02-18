package au.com.postnewspapers.postupload.rest;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * This container-managed singleton class maintains configuration
 * information about the file handler.
 * 
 * @author craig
 */
@ApplicationScoped
public class FileHandlerConfig implements Serializable {
    public static final long serialVersionUid = 54555L;
    
    // Temp XXX FIXME
    private final static Logger COM_SUN_JERSEY_LOGGER = Logger.getLogger("com.sun.jersey");
    // TEMP XXX FIXME
    static {
        COM_SUN_JERSEY_LOGGER.setLevel(Level.ALL);
    }
        
    // TODO: Make file path configurable 
    // XXX FIXME
    private static final File tempOutputPath, finalOutputPath;
    static {
        tempOutputPath = new File(System.getProperty("java.io.tmpdir"),"postupload"+File.separator+"temp");
        finalOutputPath = new File(System.getProperty("java.io.tmpdir"),"postupload"+File.separator+"done");
        if (!tempOutputPath.exists()) {
            tempOutputPath.mkdirs();
        }
        if (!finalOutputPath.exists()) {
            finalOutputPath.mkdirs();
        }
    }
    
    File getTempOutputPath() {
        return tempOutputPath;
    }
    
    File getFinalOutputPath() {
        return finalOutputPath;
    }
    
}
