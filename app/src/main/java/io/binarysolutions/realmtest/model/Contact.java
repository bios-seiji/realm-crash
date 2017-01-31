package io.binarysolutions.realmtest.model;

import android.support.annotation.Nullable;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

/**
 * Class used for containing the contact information.
 * Created by Craig on 12/20/2016.
 */

public class Contact extends RealmObject {
	public static String CONTACT_ID_START = "Contact";

	@PrimaryKey
	private String id;

	@Index
	@Required
	private String username;

	private String number;

	@Nullable
	private RealmList<Message> mMessageList;

	public Contact() {}

	public void setUsername(String username) { this.username = username; }

	public void setNumber(String number) { this.number = number; }

	public void addMessage(Message message) {
		if(mMessageList == null) {
			mMessageList = new RealmList<>();
		}

		mMessageList.add(message);
	}

	public String getId() { return id; }

	public String getUsername() { return username; }

	/**
	 * Get contacts telephone number.
	 * @return	Return the telephone number as a 10 digit string.
	 */
	public String getNumber() { return number; }

	/**
	 * Gets the number of messages associated with a contact.
	 * @return Returns the number of messages or -1 if there are no messages.
	 */
	public int getNumMessages() {
		if(mMessageList == null) {
			return -1;
		}

		return mMessageList.size(); }

	/**
	 * Retrieves the message at the specified index value.
	 * @param index	The index value for the message to retrieve.
	 * @return	Returns the Message if the index is valid otherwise a null value is returned.
	 */
	public Message getMessage(int index) {
		if(mMessageList == null) {
			return null;
		}

		if(index < 0 || index >= mMessageList.size()) {
			return null;
		}

		return mMessageList.get(index);
	}

	/**
	 * Removes a message at the specified index.
	 * @param index	Index of the message to remove;
	 * @return		Returns true if the message is erased or false if the index is for an invalid location.
	 */
	public boolean removeMessage(int index) {
		if(mMessageList == null) {
			return false;
		}

		if(index < 0 || index >= mMessageList.size()) {
			return false;
		}

		mMessageList.remove(index);
		return true;
	}

	/**
	 * Remove a message based on the id string for the message.
	 * @param messageId	The message id value to remove from the list of messages.
	 * @return			Returns true if the messageId is found and the message is removed from the list.
	 */
	public boolean removeMessageById(String messageId) {
		if(mMessageList == null) {
			return false;
		}

		for(Message message : mMessageList) {
			if(message.getId().equals(messageId)) {
				mMessageList.remove(message);
				return true;
			}
		}

		return false;
	}

	/**
	 * Retrieve the realm list of messages assocated with this contact.
	 * @return a RealmList of Message objects.
	 */
	public RealmList<Message> getRealmList() { return mMessageList; }

	/**
	 * Create a generic random contact with basic random information contained within.
	 * @param realm realm class for creating the contact and message classes.
	 * @param messages number of messages to generate for this contact
	 * @param index	number for creating a unique contact id
	 */
	public static void createContact(Realm realm, int messages, int index) {
		int lastFourNum = (int)(Math.random() * 10000);
		while(lastFourNum < 1000) {
			lastFourNum = (int)(Math.random() * 10000);
		}

		final String number = "000-555-" + String.valueOf(lastFourNum);
		final String username = "John Smith" + index;
		final String id = CONTACT_ID_START + index;

		Contact contact = realm.createObject(Contact.class, id);
		contact.setUsername(username);
		contact.setNumber(number);

		for(int i = 0; i < messages; i++) {
			contact.addMessage(Message.createMessage(realm, i, id));
		}
	}
}
