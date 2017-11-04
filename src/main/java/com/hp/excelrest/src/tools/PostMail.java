
package com.hp.excelrest.src.tools;

import java.util.Date;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.log4j.Logger;

import com.hp.excelrest.src.config.Email;

public class PostMail {

	static Logger log = Logger.getLogger(PostMail.class);
	
	public PostMail(){}

	public void sendAttachment(Email email, String filename) {

		String[] to = new String[email.getToAddressList().length];

		Properties props = new Properties();
		props.put("mail.smtp.host", email.getSmtpName());
		Session session = Session.getDefaultInstance(props, null);
		session.setDebug(false);

		try {
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(email.getFromAddress()));

			InternetAddress[] addressTo = new InternetAddress[email.getToAddressList().length]; 
			to = email.getToAddressList();

			for(int i=0; i<email.getToAddressList().length; i++){
				addressTo[i] = new InternetAddress(to[i]);
			}
			message.setRecipients(Message.RecipientType.TO, addressTo);

			message.setSubject(email.getSubjectTxt());
			message.setSentDate(new Date());

			//Set the email message text.//
			MimeBodyPart messagePart = new MimeBodyPart();
			messagePart.setText(email.getMsgTxt());

			//Set the email attachment file//
			MimeBodyPart attachmentPart = new MimeBodyPart();
			FileDataSource fileDataSource = new FileDataSource(filename) {
				@Override
				public String getContentType() {
					return "application/octet-stream";
				}
			};
			attachmentPart.setDataHandler(new DataHandler(fileDataSource));
			attachmentPart.setFileName(fileDataSource.getName());

			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(messagePart);
			multipart.addBodyPart(attachmentPart);

			message.setContent(multipart);

			Transport.send(message);
		} catch (MessagingException e) {
			log.error(e.getMessage());
			log.error(e.getStackTrace().toString());
		}
	}
	public void sendMail(Email email) {
		
		String[] to = new String[email.getToAddressList().length];

		Properties props = new Properties();
		props.put("mail.smtp.host", email.getSmtpName());
		Session session = Session.getDefaultInstance(props, null);
		session.setDebug(false);

		try {
			Message msg = new MimeMessage(session);

			InternetAddress addressFrom = new InternetAddress(email.getFromAddress());
			msg.setFrom(addressFrom);

			InternetAddress[] addressTo = new InternetAddress[email.getToAddressList().length]; 
			to = email.getToAddressList();

			for(int i=0; i<email.getToAddressList().length; i++){
				addressTo[i] = new InternetAddress(to[i]);
			}
			msg.setRecipients(Message.RecipientType.TO, addressTo);

			msg.setSubject(email.getSubjectTxt());
			msg.setContent(email.getMsgTxt(), "text/plain");
			Transport.send(msg);
			
		} catch (MessagingException e) {
			e.printStackTrace();
		}

	}

}
