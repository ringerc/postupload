package au.com.postnewspapers.postupload.common;

import au.com.postnewspapers.postupload.config.FileHandlerConfig;
import com.sun.jersey.core.header.FormDataContentDisposition;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.servlet.http.HttpSession;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

/**
 * Shared code between ajax/rest based and form-based upload handlers
 * @author Craig
 */
public abstract class FileHandlerBase {
    
    private static final Logger logger = Logger.getLogger(FileHandlerBase.class.getName());
    
    @Inject
    protected FileHandlerConfig config;
    
    @Inject
    protected Event<UploadSummary> uploadEvent;
    
    protected File sessionTempFolder;
    private UploadSummary uploadSummary;
    
    private final List<UploadSummary.FileInfo> fileList = new ArrayList<UploadSummary.FileInfo>();
    
    
    protected UploadSummary getUploadSummary() {
        return uploadSummary;
    }
    
    /**
     * Gain access to the file list. While this list is read/write, it should
     * not be modified.
     * 
     * @return 
     */
    protected List<UploadSummary.FileInfo> getFileList() {
        return fileList;
    }
    
    /**
     * Clear state and make ready for another upload. Must be called by subclass
     * before starting work.
     * 
     * This method is safe to call repeatedly, without intervening file uploads
     * or finishUploadAndSetSummary() calls.
     * 
     * @throws IOException if tempdir cleaning/creation failed
     */
    protected void clearAndInit(HttpSession session) throws IOException {
        logger.log(Level.FINE, "Session {0} clearAndInit()", session.getId());
        createOrClearSessionTempFolder(session);
        fileList.clear();
        uploadSummary = null;
    }
    
    // TODO: handle duplicate files
    
    /**
     * Accept a file upload and record it in the file list.
     * May only be called after clearAndInit() and before
     * finishUploadAndSetSummary().
     * 
     * @param file File data stream to write to session temp folder
     * @param fileInfo Info about uploaded file
     * @throws IOException 
     */
    protected void uploadFile(
            InputStream file,
            FormDataContentDisposition fileInfo) throws IOException {
        try {
            logger.log(Level.FINE, "Uploading file {0} to {1} (streaming)", new Object[]{fileInfo.getFileName(), sessionTempFolder});
            sessionTempFolder.mkdirs();
            final File outFile = new File(sessionTempFolder, fileInfo.getFileName());
            final FileOutputStream os = new FileOutputStream(outFile);
            try {
                IOUtils.copy(file, os);
            } finally {
                os.close();
            }
            recordFileInfo(fileInfo.getFileName(), outFile.length());
        } finally {
            file.close();
        }
    }
    
    /**
     * As uploadFile(InputStream file, FormDataContentDisposition fileInfo)
     * 
     * @param file Local temp file to read and copy to working location
     * @param fileInfo Details about uploaded file
     * @throws IOException 
     */
    protected void uploadFile(
            File file,
            FormDataContentDisposition fileInfo) throws IOException {            
        logger.log(Level.FINE, "Uploading file {0} to {1} (copying)", new Object[]{fileInfo.getFileName(), sessionTempFolder});
        sessionTempFolder.mkdirs();
        final File outFile = new File(sessionTempFolder, fileInfo.getFileName());
        final FileOutputStream os = new FileOutputStream(outFile);
        final FileInputStream is = new FileInputStream(file);
        try {
            try {
                IOUtils.copy(is, os);
            } finally {
                os.close();
            }
        } finally {
            is.close();
        }
        recordFileInfo(fileInfo.getFileName(), file.length());
    }
    
    /**
     * Remove an already-uploaded file from the temporary files folder
     * and the uploaded files list.
     * 
     * @param fileName Name of file to delete
     */
    protected void deleteFile(String fileName) {
        logger.log(Level.FINE, "Deleting file {0} from {1}", new Object[]{fileName, sessionTempFolder});
        for (UploadSummary.FileInfo f : fileList) {
            if (f.getName().equals(fileName)) {
                (new File(sessionTempFolder, f.getName())).delete();
                fileList.remove(f);
                break;
            }
        }
        
    }
    
    /**
     * Call finishUploadAndSetSummary() after your addUploadedFile() calls are
     * all done and you're ready to show a status report to the user, send notifications
     * etc.
     * 
     * If summary.okFiles is empty, it'll be populated with the contents of the
     * file list created by addSuccessfullyUploadedFile() calls.
     * 
     * @param summary Data about this upload
     * @throws IOException 
     */
    protected void finishUploadAndSetSummary(UploadSummary summary) throws IOException {
        logger.log(Level.FINE, "Accepting upload completion for {0}", sessionTempFolder);
        if (this.uploadSummary != null) 
            throw new IllegalStateException("Upload summary is set; previous upload not cleared yet!");
        if (summary == null) 
            throw new IllegalArgumentException("Cannot set a null upload summary.");
        
        uploadSummary = summary;
        if (uploadSummary.okFiles.isEmpty()) {
            uploadSummary.okFiles.addAll(fileList);
        }
        
        moveTempsToFinal(uploadSummary, sessionTempFolder);
        
        // Notify listeners who are interested in uploads
        // Exceptions fired by listeners will propagate up and be
        // thrown from this app, so make sure to write your listeners to
        // capture and log non-critical exceptions. Listeners may throw
        // WebApplicationException to indicate failure to the client.
        logger.log(Level.FINER, "Notifying upload listeners for {0}", sessionTempFolder);
        uploadEvent.fire(uploadSummary);
        logger.log(Level.FINER, "Upload listeners notified for {0}", sessionTempFolder);
    }
    
    /**
     * Work to be done before session is destroyed. Must be
     * called by subclass.
     */
    protected void beforeSessionDestroyed() {
        logger.log(Level.FINER, "Destroying session for {0}", sessionTempFolder);
        clearAndDeleteSessionTempFolder();
    }
    
    
    /**
     * use JAXB annotations to transform an object to a JSON serialized
     * representation. This is just a handy helper method.
     */ 
    protected static String toJson(Object o) {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospector introspector = new JaxbAnnotationIntrospector();
        mapper.getSerializationConfig().setAnnotationIntrospector(introspector);
        try {
            return mapper.writeValueAsString(o);
        } catch (IOException ex) {
            // Dodgy, but in debug code we don't care
            throw new RuntimeException(ex);
        }
    }
    
    // Implementation detail //
    
    /**
     * Record information about an uploaded file
     * 
     * @param fileInfo 
     * @param outFileLength 
     */
    private void recordFileInfo(String fileName, long outFileLength) {
        UploadSummary.FileInfo savedFileInfo = new UploadSummary.FileInfo();
        savedFileInfo.setName(fileName);
        savedFileInfo.setSize((int)outFileLength);
        fileList.add(savedFileInfo);
    }
    
    /**
     * Ensure that at the end of this call, the session temp folder is empty
     * and exists.
     * @throws IOError if the temp folder couldn't be created or cleared
     */
    private void createOrClearSessionTempFolder(HttpSession session) throws IOException {
        logger.log(Level.FINER, "Ensuring session temp folder exists for {0}", session.getId());
        if (sessionTempFolder == null) {
            sessionTempFolder = new File(config.getTempOutputDir(), session.getId());
        }
        logger.log(Level.FINEST, "Session temp folder name is {0}", sessionTempFolder);
        if (sessionTempFolder != null) {
            // There's no race here because we're @SessionScoped and dealing with
            // a file named after the session id.
            if (sessionTempFolder.exists()) {
                FileUtils.cleanDirectory(sessionTempFolder);
            } else {
                FileUtils.forceMkdir(sessionTempFolder);
            }
        }
    }
    
    /**
     * Ensure that at the end of this call the session temp folder does
     * NOT exist. No error is thrown if it didn't exist before the call was made.
     * If deletion fails, an error report is logged but no other action is taken.
     */
    private void clearAndDeleteSessionTempFolder() {
        if (sessionTempFolder != null && sessionTempFolder.exists()) {
            // This session involved an upload. If the session tempdir exists
            // we know the files haven't been moved to their final destination.
            // As the session is being destroyed, these files are abandoned and
            // should be removed.
            logger.log(Level.FINEST, "Deleting temp folder {0}", sessionTempFolder);
            try {
                FileUtils.deleteDirectory(sessionTempFolder);
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Unable to delete temporary folder " + sessionTempFolder, ex);
            }
        }
    }
    
    private void moveTempsToFinal(UploadSummary uploadSummary, File sessionTempFolder) throws IOException {
        // Move uploaded files to final destination and write a report
        // to the directory.
        if (sessionTempFolder != null && sessionTempFolder.exists()) {
            File finalOutDir = makeFinalOutputDirectoryPath(uploadSummary, sessionTempFolder);
            logger.log(Level.FINE, "Moving temporary session folder {0} to final location {1}", new Object[]{sessionTempFolder, finalOutDir});
            FileUtils.moveDirectory(sessionTempFolder,finalOutDir);
            File uploadInfoFile = new File(finalOutDir, ".info.json");
            FileWriter w = new FileWriter(uploadInfoFile);
            try {
                w.write(toJson(uploadSummary));
            } finally {
                w.close();
            }
        }
    }
    
    /**
     * Contruct the path for a directory to put the final files in, creating
     * any required parent directories but not the final step in the path.
     * The caller is expected to move a directory to the returned path or
     * mkdir() it before copying files to it.
     * @return Path to a non-existent directory for which all parent directories already exist
     */
    private File makeFinalOutputDirectoryPath(UploadSummary uploadSummary, File sessionTempFolder) throws IOException {
        final File outDirParent = config.getFinalOutputDir();
        // Create a directory for today if one doesn't already exist
        final Date now = Calendar.getInstance().getTime();
        final String dateDirName = (new SimpleDateFormat("yyyy-MM-dd")).format(now);
        final File dateDir = new File(outDirParent, dateDirName);
        FileUtils.forceMkdir(dateDir);
        // then generate a path within today's directory for the uploaded
        // files to be moved to.
        StringBuilder b = new StringBuilder();
        b.append(dateDirName)
                .append(File.separatorChar)
                .append(uploadSummary.senderAddress.getPersonal())
                .append(" (").append(uploadSummary.senderAddress.getAddress()).append(") ");
        if (uploadSummary.customerCode != null && !uploadSummary.customerCode.isEmpty()) {
            b.append("(C: ").append(uploadSummary.customerCode).append(')');
        }
        if (uploadSummary.bookingNumber != null && !uploadSummary.bookingNumber.isEmpty()) {
            b.append("(B: ").append(uploadSummary.bookingNumber).append(')');
        }
        // Add a timestamp suffix to handle multiple uploads from one person in a day.
        // Second resolution should be more than good enough.
        b.append(' ').append((new SimpleDateFormat("hh-mm-ss").format(now)));
        uploadSummary.outputDirectory = new File(outDirParent, b.toString());
        return uploadSummary.outputDirectory;
    }
}
