package au.com.postnewspapers.postupload.common;

import au.com.postnewspapers.postupload.common.UploadSummary;
import au.com.postnewspapers.postupload.uploadify.UploadifyFileHandler.UploadifyFileObj;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

/**
 * When a set of files is uploaded, notify the recipient that the files have
 * been received, and send a brief notification to the sender too.
 * 
 * @author craig
 */
public class EmailNotifier {

    private static final Logger logger = Logger.getLogger(EmailNotifier.class.getName());
    
    @Resource(name="mail/smtp")
    private Session session;
    
    public void consumeUploadCompletedEvent(@Observes UploadSummary summary) {
        try {
            notifyRecipient(summary);
            notifySender(summary);
        } catch (RuntimeException ex) {
            logger.log(Level.SEVERE, "Exception while sending upload notification email", ex);
        }
    }
    
    private void notifyRecipient(UploadSummary summary) {
        // TODO: special-case no successful files as error report, don't print
        //       ok files section or file path at all.
        // TODO: only use the relative path for the upload dir
        
        StringBuilder subject = new StringBuilder();
        subject.append(summary.okFiles.size());
        subject.append(" files");
        if (summary.customerCode != null && !summary.customerCode.isEmpty()) {
            subject.append(" from ").append(summary.customerCode);
        }
        if (summary.bookingNumber != null && !summary.bookingNumber.isEmpty()) {
            subject.append(" for booking ").append(summary.bookingNumber);
        }
        if (summary.subject != null && !summary.subject.isEmpty()) {
            subject.append(". ").append(summary.subject);
        }
        
        StringBuilder bodyText = new StringBuilder();
        bodyText.append("Sender: ").append(summary.senderAddress.toString()).append('\n');
        if (summary.customerCode != null)
            bodyText.append("Customer code: ").append(summary.customerCode).append('\n');
        if (summary.bookingNumber != null)
            bodyText.append("Booking number: ").append(summary.bookingNumber).append('\n');
        if (summary.comments != null) {
            bodyText.append("\nCustomer comments:\n").append(summary.comments).append('\n');
        }
        appendFileLists(summary, bodyText);
        
        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(summary.senderAddress);
            msg.setRecipient(RecipientType.TO, summary.recipientAddress);
            msg.setSubject(subject.toString());
            msg.setText(bodyText.toString());
            msg.setSentDate(Calendar.getInstance().getTime());
            Transport.send(msg);
        } catch (MessagingException ex) {
            logger.log(Level.SEVERE, "Unable to send email to uploaded file recipient", ex);
        }
        
    }
    
    private void notifySender(UploadSummary summary) {
        StringBuilder subject = new StringBuilder();
        subject.append(summary.okFiles.size()).append(" files sent");
        if (summary.subject != null && !summary.subject.isEmpty()) {
            subject.append(": ").append(summary.subject);
        }
        
        StringBuilder bodyText = new StringBuilder();
        if (summary.comments != null && !summary.comments.isEmpty()) {
            bodyText.append(summary.comments).append("\n\n");
        }
        appendFileLists(summary, bodyText);
        
        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(summary.senderAddress);
            msg.setRecipient(RecipientType.TO, summary.recipientAddress);
            msg.setSubject(subject.toString());
            msg.setText(bodyText.toString());
            msg.setSentDate(Calendar.getInstance().getTime());
            Transport.send(msg);
        } catch (MessagingException ex) {
            logger.log(Level.SEVERE, "Unable to send email to uploaded file sender", ex);
        }
    }

    private void appendFileLists(UploadSummary summary, StringBuilder builder) {
        if (summary.okFiles.size() > 0) {
            builder.append("\nFile location: ").append(summary.outputDirectory).append('\n');
            builder.append('\n').append(summary.okFiles.size()).append(" files sent:\n");
            for (UploadSummary.FileInfo f : summary.okFiles) {
                builder.append("\t").append(f.getName()).append('\n');
            }
        }
        if (summary.badFiles.size() > 0) {
            builder.append("Additionally, ").append(summary.badFiles.size())
                    .append(" files could not be uploaded. You may need to check\n"
                    + "with the client about these files, or they may follow in\n"
                    + "a subsequent upload. The files NOT sent were:\n");
            for (UploadSummary.FileInfo err : summary.badFiles) {
                builder.append('\t').append(err.getName()).append('\n');
                builder.append("\t\tError was: ").append(err.getErrorType())
                        .append(' ').append(err.getErrorInfo())
                        .append('\n');
            }
        }
    }
}
