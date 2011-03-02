package au.com.postnewspapers.postupload.uploadify;

import au.com.postnewspapers.postupload.common.FileHandlerBase;
import au.com.postnewspapers.postupload.common.UploadSummary;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
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
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

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
public class UploadifyFileHandler extends FileHandlerBase implements Serializable {

    private static final long serialVersionUID = 55213L;
    
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
    
    @GET
    @Path("/recipients_str")
    @Produces("application/json")
    public List<String> getReceipientsAsString(@Context HttpServletRequest request) {
        return config.getPossibleRecipientsAsString();
    }
    
    /**
     * Clear any list of uploaded files and empty the session upload
     * directory.
     * 
     * The uploader must call this method before sending any files.
     * It will ensure that an abandoned upload attempt earlier in the session
     * does not contaminate this upload.
     * 
     * @return dummy value
     */
    @POST
    @Path("/clean")
    @Produces("text/plain")
    public String clearUploads(@Context HttpServletRequest request) {
        clearAndInit(request.getSession());
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
    public String uploadFileMultipart(
            @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileInfo) throws IOException {
        uploadFile(file,fileInfo);
        return "1";
    }
    
    //
    // Sanity check: make sure the list of files the server thinks it received
    // matches the list of files the client thinks it sent.
    //
    private void ensureFileListsMatch(List<UploadSummary.FileInfo> clientFileList) {
        List<UploadSummary.FileInfo> sortedServerFileList = new ArrayList<UploadSummary.FileInfo>(getFileList());
        Collections.sort(sortedServerFileList);
        // Yes, this is an in-place sort. We haven't passed the upload summary anywhere yet,
        // so nobody but us is affected.
        Collections.sort(clientFileList);
        
        for (UploadSummary.FileInfo fi : clientFileList) {
            File f = new File(sessionTempFolder, fi.getName());
            if (!f.exists()) {
                throw new WebApplicationException(new IOException("Uploaded file " + f + " not found on server"));
            }
            if (f.length() != fi.getSize()) {
                throw new WebApplicationException(new IOException("Uploaded file " + f + " size mismatch. Client says " + fi.getSize() + " but file is " + f.length() + " on server"));
            }
        }
        if (sortedServerFileList.size() != clientFileList.size()) {
            throw new WebApplicationException(new IllegalStateException("Client and server file lists do not match in length. Client: " + clientFileList +", server: " + sortedServerFileList));
        }
        for (int i = 0; i < sortedServerFileList.size(); i++) {
            if (!sortedServerFileList.get(i).equals(clientFileList.get(i))) {
                // File lists mis-matched. Client thinks it sent more/less
                // files than server received.
                throw new WebApplicationException(new IllegalStateException("Client and server do not agree on list of files sent! Client: " + clientFileList + ", server: " + sortedServerFileList));
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
    protected UploadSummary createUploadSummary(UploadSummaryRaw d) {
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
        for (UploadifyErrorListItem bad : d.badFiles) {
            UploadSummary.FileInfo newBad = new UploadSummary.FileInfo();
            newBad.setName(bad.fileObj.name);
            newBad.setSize(bad.fileObj.size);
            newBad.setErrorType(bad.errorObj.type);
            newBad.setErrorInfo(bad.errorObj.info);
            s.badFiles.add(newBad);
        }
        for (UploadifyFileObj f : d.okFiles) {
            UploadSummary.FileInfo newOk = new UploadSummary.FileInfo();
            newOk.setName(f.name);
            newOk.setSize(f.size);
            newOk.setErrorType(null);
            newOk.setErrorInfo(null);
            s.okFiles.add(newOk);
        }
        s.comments = d.comments;
        s.bookingNumber = d.bookingNumber;
        s.customerCode = d.customerCode;
        return s;
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
    public String submitForm(UploadSummaryRaw d) throws IOException {
        
        // Clean up and verify the JSON sent to us
        UploadSummary s = createUploadSummary(d);
        // Sanity-check the file list we maintain, ensuring it matches
        // what the client thinks it sent us.
        ensureFileListsMatch(s.okFiles);
        
        finishUploadAndSetSummary(s);
        
        return "1";
    }
    
    @GET
    @Path("/summary")
    @Produces("application/json")
    public UploadSummary getSummary() {
        return getUploadSummary();
    }
    
    // At session expiry, clean up the tempdir for the session
    @PreDestroy
    protected void cleanupTempFiles() {
        beforeSessionDestroyed();
    }
}