package au.com.postnewspapers.postupload.rest;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;
import javax.annotation.PreDestroy;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

/**
 * Based on http://tiainen.sertik.net/2009/10/easy-file-upload-in-java-using-jersey.html by Joeri Sykora
 * 
 * Adapted to Jersey 1.5 and more efficient stream-based I/O.
 * Extended to handle summary data form submission, enumerate possible
 * recipients. Support for JSON-based data interchange added.
 * 
 * @author craig
 */
@Path("/file")
@SessionScoped
public class FileHandler implements Serializable {

    private static final long serialVersionUID = 55213L;
    
    @Inject
    private FileHandlerConfig config;
    
    private UploadSummary uploadSummary;
    private final List<String> fileList = new ArrayList<String>();
    private File sessionTempFolder;
    @Inject private Event<UploadSummary> uploadEvent;
    
    // XML/JSON types for file info sent by server
    @XmlType
    @XmlAccessorType(XmlAccessType.FIELD)
    @JsonIgnoreProperties(ignoreUnknown=true)
    public static class UploadifyFileObj {
        public UploadifyFileObj() {}
        @XmlElement
        public String name;
        @XmlElement
        public int size;
    };

    @XmlType
    @XmlAccessorType(XmlAccessType.FIELD)
    @JsonIgnoreProperties(ignoreUnknown=true)
    public static class UploadifyErrorObj {
        public UploadifyErrorObj() {}
        @XmlElement
        public String type;
        @XmlElement
        public String info;
    };

    @XmlType
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class UploadifyErrorListItem {
        public UploadifyErrorListItem() {}
        @XmlElement
        public UploadifyFileObj fileObj;
        @XmlElement
        public UploadifyErrorObj errorObj;
    };
    
    /**
     * The raw upload summary info as POSTed by the client
     * to us.
     */
    @XmlType
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class UploadSummaryRaw {
        public UploadSummaryRaw() {}
        @XmlElement String recipient;
        @XmlElement String senderEmail;
        @XmlElement String senderName;
        @XmlElement String customerCode;
        @XmlElement String bookingNumber;
        @XmlElement String subject;
        @XmlElement String comments;
        List<UploadifyFileObj> okFiles;
        List<UploadifyErrorListItem> badFiles;
    }
    
    /**
     * Upload results summary, cleaned up for sending to
     * other parts of the app.
     */
    @XmlType
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class UploadSummary {
        public UploadSummary() {}
        @XmlElement InternetAddress recipientAddress;
        @XmlElement InternetAddress senderAddress;
        @XmlElement String customerCode;
        @XmlElement String bookingNumber;
        @XmlElement String subject;
        @XmlElement String comments;
        List<UploadifyFileObj> okFiles;
        List<UploadifyErrorListItem> badFiles;
        @XmlElement File outputDirectory;
    }
    
    /**
     * Return a JSON array of address objects representing possible
     * recipients. Each address object is of the form:
     * 
     * {"address":"blah@example.com","type":"rfc822", "group":false, "personal":"My Name"}
     * 
     * @param request Servlet request
     * @return JSON
     */
    @GET
    @Path("/recipients")
    @Produces("application/json")
    public List<InternetAddress> getRecipients(@Context HttpServletRequest request) {
        
        // TODO fetch this dynamically
        ArrayList<InternetAddress> possibleRecipients = new ArrayList<InternetAddress>();
        try {
            possibleRecipients.add(new InternetAddress("Craig Ringer <craig@postnewspapers.com.au>"));
        } catch (AddressException ex) {
            throw new RuntimeException(ex);
        }
        return possibleRecipients;
    }
    
    @GET
    @Path("/recipients_str")
    @Produces("application/json")
    public List<String> getReceipientsAsString(@Context HttpServletRequest request) {
        List<InternetAddress> recips = getRecipients(request);
        ArrayList<String> stringRecips = new ArrayList<String>(recips.size());
        for (InternetAddress a: recips) {
            stringRecips.add(a.toString());
        }
        return stringRecips;
    }
    
    /**
     * Clear any list of uploaded files and empty the session upload
     * directory.
     * 
     * The uploader should call this method before sending any files.
     * It will ensure that an abandoned upload attempt earlier in the session
     * does not contaminate this upload.
     * 
     * @return dummy value
     */
    @POST
    @Path("/clean")
    @Produces("text/plain")
    public String clearUploads() {
        if (fileList != null) 
            fileList.clear();
        if (sessionTempFolder != null) {
            if (sessionTempFolder.exists()) {
                for (File f : sessionTempFolder.listFiles()) {
                    f.delete();
                }
            } else {
                sessionTempFolder.mkdir();
            }
        }
        uploadSummary = null;
        return "1";
    }

    /**
     * Accept a file upload and associate it with the user's session. Uploadify
     * will send files to this resource one by one, before submitting the form
     * to send the final fields.
     * 
     * @param request Servlet request
     * @param file Input stream containing file data from jersey-file
     * @param fileInfo Information about file from jersey-file
     * @return "1" for success
     */
    @POST
    @Path("/upload")
    @Consumes("multipart/form-data")
    @Produces("text/plain")
    public String uploadFile(
            @Context HttpServletRequest request,
            @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileInfo) throws IOException {
        try {
            final File outFile = new File(getSessionFolder(request.getSession()), fileInfo.getFileName());
            final FileOutputStream os = new FileOutputStream(outFile);
            try {
                IOUtils.copy(file, os);
            } finally {
                os.close();
            }
            fileList.add(fileInfo.getFileName());
        } finally {
            file.close();
        }
        return "1";
    }
    
    /** Get the output directory for this session from the session environment, 
     * creating the session key and file system directory if they don't exist */
    private File getSessionFolder(HttpSession session) {
        if (sessionTempFolder == null) {
            sessionTempFolder = new File(config.getTempOutputPath(), session.getId());
            if (!sessionTempFolder.exists()) {
                sessionTempFolder.mkdir();
            }
        }
        return sessionTempFolder;
    }
    
    // use JAXB annotations to transform an object to a JSON
    // serialized representation
    private String toJson(Object o) {
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
    
    private void ensureFileListsMatch(List<String> serverFileList, List<UploadifyFileObj> clientFileList) {
        ArrayList<String> jsonFiles = new ArrayList<String>(clientFileList.size());
        for (UploadifyFileObj fi : clientFileList) {
            File f = new File(sessionTempFolder, fi.name);
            if (!f.exists()) {
                throw new WebApplicationException(new IOException("Uploaded file " + f + " not found on server"));
            }
            if (f.length() != fi.size) {
                throw new WebApplicationException(new IOException("Uploaded file " + f + " size mismatch. Client says " + fi.size + " but file is " + f.length() + " on server"));
            }
            jsonFiles.add(fi.name);
        }
        Collections.sort(jsonFiles);
        Collections.sort(serverFileList);
        for (int i = 0; i < serverFileList.size(); i++) {
            if (!serverFileList.get(i).equals(jsonFiles.get(i))) {
                // File lists mis-matched. Client thinks it sent more/less
                // files than server received.
                throw new WebApplicationException(new IllegalStateException("Client and server do not agree on list of files sent! Client: " + jsonFiles + ", server: " + serverFileList));
            }
        }
    }
    
    /**
     * Given a POJO representation of the JSON sent to us by the client,
     * validate it, tidy it up and produce cleaned up upload summary that
     * we can associate with the session as metadata.
     * 
     * @param d Direct conversion of original JSON POST from client
     * @return Cleaned up upload summary
     * @throws WebApplicationException if addresses invalid, etc
     */
    private UploadSummary cleanUploadSummary(UploadSummaryRaw d) {
        UploadSummary s = new UploadSummary();
        try {
            InternetAddress[] recipients = InternetAddress.parse(d.recipient == null ? "" : d.recipient);
            if (recipients.length != 1) {
                throw new WebApplicationException(new IllegalArgumentException("Recipient address invalid"), Response.Status.BAD_REQUEST);
            }
            s.recipientAddress = recipients[0];
        } catch (AddressException ex) {
            throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
        }

        try {
            s.senderAddress = new InternetAddress(d.senderEmail, d.senderName);
        } catch (UnsupportedEncodingException ex) {
            throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
        }
        s.badFiles = d.badFiles;
        s.okFiles = d.okFiles;
        s.comments = d.comments;
        s.bookingNumber = d.bookingNumber;
        s.customerCode = d.customerCode;
        return s;
    }
    
    /**
     * Contruct the path for a directory to put the final files in,
     * creating any parent directories but not the directory its self.
     * @return 
     */
    private File makeFinalOutputDirectoryPath() {
        final File outDirParent = config.getFinalOutputPath();
        // Create a directory for today if one doesn't already exist
        final Date now = Calendar.getInstance().getTime();
        final String dateDirName = (new SimpleDateFormat("yyyy-MM-dd")).format(now);
        final File dateDir = new File(outDirParent, dateDirName);
        // Try to create then check for existence, to avoid race of test-create-fail
        dateDir.mkdirs();
        if (!dateDir.exists()) {
            throw new WebApplicationException(new IOException("Unable to create final output folder " + dateDir));
        }
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
    
    /**
     * Once all files have been uploaded, this resource is submitted with 
     * summary information about the upload like the intended recipient
     * and sender, number of files that should have been received, any files
     * that failed, etc.
     * 
     * @param request HTTP servlet request
     * @param recipient Intended recipient name and email
     * @param senderEmail Sender's email address
     * @param senderName Sender's name
     * @param customerCode If not blank/null, customer code
     * @param bookingNumber If not blank/null, booking number
     * @param comments User-supplied comment text
     * @param okFiles JSON of UploadifyFileObj[] for files that we should have received
     * @param badFiles JSON of UploadifyErrorListItem[] for files that uploadify couldn't send
     * @return 
     * @throws AddressException
     * @throws UnsupportedEncodingException 
     */
    @POST
    @Path("/finished")
    @Consumes("application/json")
    @Produces("text/plain")
    public String submitForm(UploadSummaryRaw d) {
        
        // Clean up and verify the JSON sent to us
        uploadSummary = cleanUploadSummary(d);
        d = null;
        // Sanity-check the file list we maintain, ensuring it matches
        // what the client thinks it sent us.
        ensureFileListsMatch(fileList, uploadSummary.okFiles);
        
        // Move uploaded files to final destination and write a report
        // to the directory.
        if (sessionTempFolder != null && sessionTempFolder.exists()) {
            File finalOutDir = makeFinalOutputDirectoryPath();
            File uploadInfoFile = new File(finalOutDir, ".info.json");
            try {
                sessionTempFolder.renameTo(finalOutDir);
                FileWriter w = new FileWriter(uploadInfoFile);
                try {
                    w.write(toJson(uploadSummary));
                } finally {
                    w.close();
                }
            } catch (IOException ex) {
                throw new WebApplicationException(ex);
            }
        }
        
        // Notify listeners who are interested in uploads
        // Exceptions fired by listeners will propagate up and be
        // thrown from this app, so make sure to write your listeners to
        // capture and log non-critical exceptions. Listeners may throw
        // WebApplicationException to indicate failure to the client.
        uploadEvent.fire(uploadSummary);
        
        return "1";
    }
    
    @GET
    @Path("/summary")
    @Produces("application/json")
    public UploadSummary getSummary() {
        if (uploadSummary != null) {
            return uploadSummary;
        } else {
            // Should we be crafting and returning a Response instead?
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }
    
    // At session expiry, clean up the tempdir for the session
    @PreDestroy
    void cleanupTempFiles() {
        if (sessionTempFolder != null && sessionTempFolder.exists()) {
            // This session involved an upload. If the session tempdir exists
            // we know the files haven't been moved to their final destination.
            // As the session is being destroyed, these files are abandoned and
            // should be removed.
            for (File f : sessionTempFolder.listFiles()) {
                f.delete();
            }
            sessionTempFolder.delete();
        }
    }
    
}