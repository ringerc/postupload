package au.com.postnewspapers.postupload.common;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

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
    
    public File getTempOutputPath() {
        return tempOutputPath;
    }
    
    public File getFinalOutputPath() {
        return finalOutputPath;
    }
    
    
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
    
    public List<String> getPossibleRecipientsAsString() {
        List<InternetAddress> recips = getPossibleRecipients();
        ArrayList<String> stringRecips = new ArrayList<String>(recips.size());
        for (InternetAddress a: recips) {
            stringRecips.add(a.toString());
        }
        return stringRecips;
    }
    
}
