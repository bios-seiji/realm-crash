package io.binarysolutions.realmtest.model;

import java.util.Date;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

/**
 * Class used for containing the information regarding a message for a contact.
 * Created by Craig on 12/20/2016.
 */

public class Message extends RealmObject {
	public static String LORUM_IPSUM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed ut pharetra ex. Phasellus varius sollicitudin quam. Pellentesque et purus non magna molestie suscipit id eu sapien. Aliquam lobortis velit vitae nunc scelerisque molestie. Integer cursus sed arcu eget bibendum. Vestibulum congue ante id dui luctus porta id sed massa.";
	public static String MESSAGE_ID_START = "Message";

	@PrimaryKey
	private String id;

	@Index
	private String contactId;

	@Required
	private String type;

	private String body;

	@Required
	private Date dateCreated;

	public Message() {}

	public void setContactId(String contactId) { this.contactId = contactId; }

	public void setType(String msgType) { type = msgType; }

	public void setBody(String body) {
		this.body = body; }

	public void setCreationDate(Date createTime) {
		dateCreated = createTime; }

	public String getId() { return id; }

	public String getContactId() { return contactId; }

	public String getType() { return type; }

	public String getBody() { return body; }

	public Date getCreationDate() { return dateCreated; }

	public String getMessageInfo() {
		return "Id: " + id + ", Contact Id: " + contactId + ", Message Type: " + type;
	}

	/**
	 * Create a random message for
	 * @param realm realm class for creating the message classes.
	 * @param index number used for creating a unique message id along with the contactId number
	 * @param contactId number used for reference the owner of the message and used for creating the unique message id.
	 */
	public static Message createMessage(Realm realm, int index, String contactId) {
		Date currentTime = new Date(System.currentTimeMillis());
		String messageType = "Sent";// mRandom.nextBoolean() ? "Sent" : "Received";
		String messageBody = index + " " + LORUM_IPSUM;
		String id = contactId + MESSAGE_ID_START + index;

		for(int i = 0; i < 2; i++) {
			messageBody += LORUM_IPSUM;
		}

		Message message = realm.createObject(Message.class, id);
		message.setContactId(contactId);
		message.setType(messageType);
		message.setBody(messageBody);
		message.setCreationDate(currentTime);

		return message;
	}
}
