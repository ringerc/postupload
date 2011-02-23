package au.com.postnewspapers.postupload.simple;

import au.com.postnewspapers.postupload.common.FileHandlerBase;
import au.com.postnewspapers.postupload.common.UploadSummary;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PreDestroy;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Produces;
import javax.faces.application.Application;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
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
    
    private boolean isDone;
    private InternetAddress recipientAddress;
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

    public boolean isIsDone() {
        return isDone;
    }

    public void setIsDone(boolean isDone) {
        this.isDone = isDone;
    }

    // TODO FIXME XXX: Send full file info in a useful way
    /*
    public List<String> getOkFiles() {
        List<UploadSummary.FileInfo> fi = getFileList();
        List<String> fn = new ArrayList<String>(fi.size());
        for (UploadSummary.FileInfo f: fi) {
            fn.add(f.name);
        }
        return fn;
    }*/
    
    public List<UploadSummary.FileInfo> getOkFiles() {
        return getFileList();
    }

    public InternetAddress getRecipientAddress() {
        return recipientAddress;
    }

    public void setRecipientAddress(InternetAddress recipientAddress) {
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
    
    public List<InternetAddress> getPossibleRecipients() {
        return config.getPossibleRecipients();
    }
    
    /**
     * Begin the process by clearing the class's state from any previous
     * upload run and displaying the upload form. This, not navigation to the
     * simplemode_start form, is the entry point.
     * 
     * @return JSF outcome
     */
    public String showFileUploadForm() {
        HttpServletRequest httpRequest = (HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest();
        clearAndInit(httpRequest.getSession());
        isDone = false;
        return "simplemode_addfile";
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
        return Response.seeOther(new URI("../faces/simplemode_addfile.xhtml")).build();
    }
    
    /**
     * As uploadFileMultipart, but accepts data in the much less efficient
     * application/x-www-form-urlencoded form used by some browsers. This is
     * NOT preferred, and is only supported for elderly browsers that might
     * have to use the simple mode uploader.
     * 
     * @param file
     * @param fileInfo
     * @return
     * @throws IOException 
     */
    /*
    @POST
    @Path("/upload")
    @Consumes("application/x-www-form-urlencoded")
    public Response uploadFileUrlencoded(
            @Context HttpServletRequest httpRequest,
            @FormParam("file") File file,
            @FormParam("file") FormDataContentDisposition fileInfo) throws IOException, URISyntaxException {
        System.err.println("Urlencoded");
        uploadFile(file, fileInfo);
        String redirectDest = FacesContext.getCurrentInstance().getExternalContext().encodeActionURL("simplemode_addfile");
        return Response.seeOther(new URI(redirectDest)).build();
    }
     * 
     */
    
    public String deleteUploadedFile(String fileName) {
        deleteFile(fileName);
        return "simplemode_addfile";
    }
    
    
    /**
     * When the user has finished all uploads, create a summary and
     * display the finished page.
     * 
     * @return JSF outcome
     */
    public String finishedUploadingAction() throws IOException {
        finishUploadAndSetSummary(createUploadSummary());
        return "simplemode_finished";
    }
    
    @PreDestroy
    protected void cleanupTempFiles() {
        beforeSessionDestroyed();
    }
    
    // Prepare an UploadSummary record with the results of the user's session.
    // This mostly involves direct copies from attributes of the upload handler
    // jsf2 class, but a few fields need special handling.
    private UploadSummary createUploadSummary() {
        UploadSummary newSummary = new UploadSummary();
        try {
            InternetAddress addr = new InternetAddress(senderEmail, senderName);
            addr.validate();
            newSummary.senderAddress = addr;
        } catch (UnsupportedEncodingException ex) {
            // TODO: Report a suitable JSF eror to the user here
            throw new IllegalArgumentException("Bad sender address", ex);
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
