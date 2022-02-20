package com.oci.fn.acme.email;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import com.fnproject.fn.api.FnConfiguration;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.RuntimeContext;
import com.fnproject.fn.api.httpgateway.HTTPGatewayContext;

public class EmailSender {
	// This address must be added to Approved Senders in the console.
	private static String FROM_EMAIL;
	private static String FROM_NAME;


	// Replace smtp_username with your Oracle Cloud Infrastructure SMTP username
	// generated in console.
	private static String SMTP_USER_NAME;

	// Replace smtp_password with your Oracle Cloud Infrastructure SMTP password
	// generated in console.
	private static String SMTP_PASSWORD;

	// Oracle Cloud Infrastructure Email Delivery hostname.
	private static String SMTP_ENDPOINT;

	// The port you will connect to on the SMTP endpoint. Port 25 or 587 is allowed.
	private static int SMTP_PORT;


	@FnConfiguration
	public void setUp(RuntimeContext ctx) throws Exception {
		try {
			SMTP_USER_NAME = ctx.getConfigurationByKey("SMTP_USER_NAME").get();
			SMTP_PASSWORD = ctx.getConfigurationByKey("SMTP_PASSWORD").get();
			SMTP_ENDPOINT = ctx.getConfigurationByKey("SMTP_ENDPOINT").get();
			SMTP_PORT = Integer.valueOf(ctx.getConfigurationByKey("SMTP_PORT").get());
			FROM_EMAIL = ctx.getConfigurationByKey("FROM_EMAIL").get();
			FROM_NAME = ctx.getConfigurationByKey("FROM_NAME").get();
		} catch (Exception e) {
			System.out.println("Error occurred while loading the function configuration parameters");
			e.printStackTrace();
		}
	}

	public String handleRequest(HTTPGatewayContext hctx, final InputEvent input) throws Exception {
		String requestUrl = hctx.getRequestURL();
		System.out.println("Request URL: " + requestUrl);
		String inputData = "";

		inputData = input.consumeBody(is -> {
			try {
				return new StringBuilder(IOUtils.toString(is, StandardCharsets.UTF_8.toString())).toString();
			} catch (IOException ie) {
				System.out.println("Error while reading the body of the request" + ie.getMessage());
			}
			return "";
		});
		System.out.println("The content of the body: " + inputData);
		JSONObject requestBody = new JSONObject(inputData);
		String toEmailAddress = requestBody.getString("toEmailAddress");
		String emailSubject = requestBody.getString("emailSubject");
		String emailBody = requestBody.getString("emailBody");
		String emailContentType = StringUtils.isBlank(requestBody.getString("emailContentType")) ? "text/html"
				: requestBody.getString("emailContentType");
		// Create a Properties object to contain connection configuration information.

		Properties props = System.getProperties();
		props.put("mail.transport.protocol", "smtp");
		props.put("mail.smtp.port", SMTP_PORT);

		// props.put("mail.smtp.ssl.enable", "true"); //the default value is false if
		// not set
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.auth.login.disable", "true"); // the default authorization order is "LOGIN PLAIN DIGEST-MD5
															// NTLM". 'LOGIN' must be disabled since Email Delivery
															// authorizes as 'PLAIN'
		props.put("mail.smtp.starttls.enable", "true"); // TLSv1.2 is required
		props.put("mail.smtp.starttls.required", "true"); // Oracle Cloud Infrastructure required

		// Create a Session object to represent a mail session with the specified
		// properties.
		Session session = Session.getDefaultInstance(props);

		// Create a message with the specified information.
		MimeMessage msg = new MimeMessage(session);
		msg.setFrom(new InternetAddress(FROM_EMAIL, FROM_NAME));
		msg.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmailAddress));
		msg.setSubject(emailSubject);
		msg.setContent(emailBody, emailContentType);

		// Create a transport.
		Transport transport = session.getTransport();
		// Send the message.
		try {
			System.out.println("Sending Email to " + toEmailAddress);
			// Connect to OCI Email Delivery using the SMTP credentials specified.
			transport.connect(SMTP_ENDPOINT, SMTP_USER_NAME, SMTP_PASSWORD);
			// Send email.
			transport.sendMessage(msg, msg.getAllRecipients());
			System.out.println("Email sent!");
		} catch (Exception ex) {
			System.out.println("Error while sending email" + ex.getMessage());
			ex.printStackTrace();

		} finally

		{
			// Close & terminate the connection.
			transport.close();
		}
		return "Email sent successfully!";
	}
}
