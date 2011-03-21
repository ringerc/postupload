package au.com.postnewspapers.postupload.simple;

import au.com.postnewspapers.postupload.common.EmailAddress;
import au.com.postnewspapers.postupload.common.FileHandlerBase;
import au.com.postnewspapers.postupload.common.UploadSummary;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.mail.internet.AddressException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * SimpleModeFileHandler is the backend bean that manages all the simple mode
 * uploader interactions. It is responsible for accepting the initial form state,
 * maintaining the transferred file list, and submitting the final results
 * for processing. Most of its work is done via JSF2 calls, but actual file uploads
 * are accepted via JAX-RS because stock JSF2 doesn't offer a file upload handler.
 * 
 * @author Craig
 */
@Named
@SessionScoped
@Path("/simple")
public class SimpleModeFileHandler extends FileHandlerBase implements Serializable {
    
    protected static final long serialVersionUid = 994991L;
    private static final Logger logger = Logger.getLogger(SimpleModeFileHandler.class.getName());
    
    private EmailAddress recipientAddress;
    private String senderName, senderEmail, customerCode, bookingNumber, subject, comments;

    public String getBookingNumber() {
        return bookingNumber;
    }

    public void setBookingNumber(String bookingNumber) {
        this.bookingNumber = bookingNumber;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getCustomerCode() {
        return customerCode;
    }

    public void setCustomerCode(String customerCode) {
        this.customerCode = customerCode;
    }

    public List<UploadSummary.FileInfo> getOkFiles() {
        return getFileList();
    }

    public EmailAddress getRecipientAddress() {
        return recipientAddress;
    }

    public void setRecipientAddress(EmailAddress recipientAddress) {
        this.recipientAddress = recipientAddress;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public List<EmailAddress> getPossibleRecipients() {
        return config.getPossibleRecipients();
    }
    
    /**
     * Begin the process by clearing the class's state from any previous
     * upload run and displaying the upload form. This, not navigation to the
     * simplemode_start form, is the entry point.
     * 
     * @return JSF outcome
     * @throws IOException if tempdir cleaning/creation failed
     */
    public String showFileUploadForm() throws IOException {
        HttpServletRequest httpRequest = (HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest();
        clearAndInit(httpRequest.getSession());
        return "/simple/simplemode_addfile";
    }
    
    /**
     * Accept a file upload, store the file, and respond with a redirect
     * sending the user back to the upload JSF2 page to send another file. This
     * method is the action target of the file upload form.
     * 
     * @param request Servlet request
     * @param file Input stream containing file data from jersey-file
     * @param fileInfo Information about file from jersey-file
     * @return "1" for success
     */
    @POST
    @Path("/upload")
    @Consumes("multipart/form-data")
    public Response uploadFileMultipart(
            @Context HttpServletRequest httpRequest,
            @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileInfo) throws IOException, URISyntaxException {
        uploadFile(file,fileInfo);
        // FIXME TODO: there must be a way to do this without hard-coding the file extension
        // JSF pages are mapped to, and the prefix JSF is using. Surely... ?!?
        // Maybe we have to map it earlier, when we have a faces context, and store it.
        return Response.seeOther(new URI("../faces/simple/simplemode_addfile.xhtml")).build();
    }
    
    public String deleteUploadedFile(String fileName) {
        deleteFile(fileName);
        return "/simple/simplemode_addfile";
    }
    
    
    /**
     * When the user has finished all uploads, create a summary and
     * display the finished page.
     * 
     * @return JSF outcome
     */
    public String finishedUploadingAction() throws IOException {
        finishUploadAndSetSummary(createUploadSummary());
        return "/simple/simplemode_finished";
    }
    
    @PreDestroy
    protected void cleanupTempFiles() {
        // Before doing cleanup, we want to send any files currently queued
        // up. Abandoned sessions are very likely with the slow dumb mode
        // uploader and we don't want to lose files users have uploaded
        // thinking they're "sent" without ever confirming.
        if (!getFileList().isEmpty() && getUploadSummary() == null) {
            // Files are queued, but the upload hasn't been sent off yet. If it
            // fails, swallow the error, since there's nobody to tell.
            try {
                subject = "(Abandoned upload) " + subject;
                comments = "[System note: These files were part of an upload that wasn't confirmed\n"
                        + "by the sender. They could be out of date or wrong. They've been sent to you\n"
                        + "just in case the sender meant to confirm them but forgot or had a technical problem.\n\n";
                finishUploadAndSetSummary(createUploadSummary());
            } catch (IOException ex) {
                logger.log(Level.INFO, "Failed to send abandoned session", ex);
            }
        }
        beforeSessionDestroyed();
    }
    
    // Prepare an UploadSummary record with the results of the user's session.
    // This mostly involves direct copies from attributes of the upload handler
    // jsf2 class, but a few fields need special handling.
    private UploadSummary createUploadSummary() {
        UploadSummary newSummary = new UploadSummary();
        try {
            EmailAddress addr = new EmailAddress(senderEmail, senderName);
            addr.validate();
            newSummary.senderAddress = addr;
        } catch (AddressException ex) {
            // TODO: Report a suitable JSF eror to the user here
            throw new IllegalArgumentException("Bad sender address", ex);
        }
        newSummary.recipientAddress = recipientAddress;
        newSummary.bookingNumber = bookingNumber;
        newSummary.customerCode = customerCode;
        newSummary.comments = comments;
        newSummary.subject = subject;
        // okFiles and badFiles are populated by the parent class
        return newSummary;
    }


}
