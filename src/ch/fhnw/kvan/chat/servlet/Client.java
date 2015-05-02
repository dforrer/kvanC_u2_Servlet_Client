package ch.fhnw.kvan.chat.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import ch.fhnw.kvan.chat.general.Chats;
import ch.fhnw.kvan.chat.general.Participants;
import ch.fhnw.kvan.chat.gui.ClientGUI;
import ch.fhnw.kvan.chat.interfaces.IChatDriver;
import ch.fhnw.kvan.chat.interfaces.IChatRoom;
import ch.fhnw.kvan.chat.utils.*;

public class Client implements IChatDriver, IChatRoom {

	private static Client client;
	private static String clientName;
	private static String url;
	private boolean running;
	private ClientGUI gui;

	// Same as ChatRoom-Class
	private final Participants participantInfo = new Participants();
	private final Chats chatInfo = new Chats();

	public Client() {
		running = true;
		// Start GUI
	}

	public static void main(String args[]) throws IOException {
		System.out.println("Client-Class has been called with arguments: ");
		System.out.println("Name: " + args[0]);
		System.out.println("Host: " + args[1]);

		clientName = args[0];
		url = "http://" + args[1];

		client = new Client();

		client.gui = new ClientGUI(client, clientName);

		// Send name

		client.addParticipant(clientName);

		// Get topics
		client.getTopics();
		client.getParticipants();
		client.getMessages(client.gui.getCurrentTopic());
	}

	/*
	 * "?action=addParticipant&name=<Name>"
	 * "?action=removeParticipant&name=<Name>" "?action=addTopic&topic=<Thema>"
	 * "?action=removeTopic&topic=<Thema>"
	 * "?action=postMessage&message=<Nachricht>&topic=<Thema>"
	 * ￼"?action=getMessages&topic=<Thema>" ￼"?action=getTopics"
	 * "?action=refresh&topic=<Thema>"
	 */

	/**
	 * Expects a URLEncoder.encode(topic, "UTF-8")-encoded string as "request"
	 * 
	 * @param request
	 * @return
	 */
	public String makeRequest(String request) {
		System.out.println("request:" + request);
		URL url_temp = null;
		try {
			url_temp = new URL(url + request);
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			HttpURLConnection c = (HttpURLConnection) url_temp.openConnection();
			c.setRequestMethod("GET");
			c.setRequestProperty("Content-length", "0");
			c.setUseCaches(false);
			c.setAllowUserInteraction(false);
			c.setConnectTimeout(30);
			c.setReadTimeout(30);
			c.connect();
			int status = c.getResponseCode();
			System.out.println("Status-Code:" + status);

			switch (status) {
			case 200: // notice the missing break!
			case 201:
				BufferedReader br = new BufferedReader(new InputStreamReader(
						c.getInputStream()));
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = br.readLine()) != null) {
					sb.append(line);
				}
				br.close();
				return sb.toString();
			default:
				System.out.println(request + "-request failed!!!");
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/*
	 * public void handleResponse(String input) {
	 * System.out.println("serverResponse:" + input);
	 * 
	 * // Handle empty response if (input.equals("topics=") ||
	 * input.equals("participants=")) { return; }
	 * 
	 * String key = input.split("=")[0]; String value = input.split("=")[1];
	 * 
	 * switch (key) { case "topics": // FORMAT: "topics=Topic1;Topic2;" String[]
	 * topics = value.split(";"); for (int i = 0; i < topics.length; i++) {
	 * chatInfo.addTopic(topics[i]); } gui.updateTopics(topics); break; case
	 * "participants": // FORMAT: "participants=Peter;Paul;Mary;" String[] parts
	 * = value.split(";"); for (int i = 0; i < parts.length; i++) {
	 * participantInfo.addParticipant(parts[i]); }
	 * gui.updateParticipants(parts); break; case "messages": // FORMAT:
	 * "messages=Meldung1;Meldung2;" String[] messages =
	 * input.split("=")[1].split(";;"); for (int i = messages.length - 2; i >=
	 * 0; i--) { if (!chatInfo.addMessage(topic, messages[i])) {
	 * System.out.println("ERROR: Couldnt add message!"); break; } } // Create
	 * String[] for the gui // ("messages=String1;String2;String3;")
	 * System.out.println(chatInfo.getMessages(topic)); String messages3 =
	 * chatInfo.getMessages(topic).split("=")[1]; String[] stringArray2 =
	 * messages3.split(";;");
	 * 
	 * // Check if should update the gui if
	 * (gui.getCurrentTopic().equals(topic)) { gui.updateMessages(stringArray2);
	 * }
	 * 
	 * break; default: throw new IllegalArgumentException("Invalid key: " +
	 * key); } }
	 */
	// --------------------------------
	// IChatDriver-Interface-Functions
	// --------------------------------

	@Override
	public void connect(String host, int port) throws IOException {
	}

	@Override
	public void disconnect() throws IOException {
	}

	@Override
	public IChatRoom getChatRoom() {
		return this;
	}

	// ----------------------------------------------------------------
	// IChatRoom-Interface-Functions
	// ----------------------------------------------------------------
	// I have implemented the IChatRoom-Interface here, so that we get
	// a notification when the ClientGUI changes the chatroom-model.
	// All changes are then forwarded to the server, which in turn
	// distributes the change to all other clients.
	// ----------------------------------------------------------------

	@Override
	public boolean addParticipant(String name) throws IOException {
		if (!name.trim().equalsIgnoreCase("")) {
			makeRequest("?action=addParticipant&name="
					+ URLEncoder.encode(clientName, "UTF-8"));
			getParticipants();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean removeParticipant(String name) throws IOException {
		if (!name.trim().equalsIgnoreCase("")) {
			System.out.println("Participant removed");
			makeRequest("?action=removeParticipant&name="
					+ URLEncoder.encode(clientName, "UTF-8"));
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean addTopic(String topic) throws IOException {
		if (!topic.trim().equalsIgnoreCase("")) {
			System.out.println("Topic added: " + topic);
			makeRequest("?action=addTopic&topic="
					+ URLEncoder.encode(topic, "UTF-8"));
			getTopics();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean removeTopic(String topic) throws IOException {
		if (!topic.trim().equalsIgnoreCase("")) {
			System.out.println("Topic removed");
			makeRequest("?action=removeTopic&topic="
					+ URLEncoder.encode(topic, "UTF-8"));
			getTopics();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean addMessage(String topic, String message) throws IOException {
		if (!topic.trim().equalsIgnoreCase("")
				&& !message.trim().equalsIgnoreCase("")) {
			System.out.println("Message added");
			makeRequest("?action=postMessage&message="
					+ URLEncoder.encode(clientName + " : " + message, "UTF-8")
					+ "&topic=" + URLEncoder.encode(topic, "UTF-8"));
			getMessages(topic);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public String getMessages(String topic) throws IOException {
		System.out.println("getMessages CALLED: " + topic);
		if (topic == null) {
			return ("messages=");
		}
		if (!topic.trim().equalsIgnoreCase("")) {
			String response = makeRequest("?action=getMessages&topic="
					+ URLEncoder.encode(topic, "UTF-8"));
			// FORMAT: "messages=Meldung1;Meldung2;"
			System.out.println("response:" + response);
			chatInfo.removeTopic(topic);
			chatInfo.addTopic(topic);
			if (!response.equals("messages=")) {
				String[] messages = response.split("=")[1].split(";;");
				for (int i = messages.length - 1; i >= 0; i--) {
					if (!chatInfo.addMessage(topic, messages[i])) {
						System.out.println("ERROR: Couldnt add message!");
						break;
					}
				}
			}
			return chatInfo.getMessages(topic);
		} else {
			return ("messages=");
		}
	}

	// FORMAT: "topics=Topic1;Topic2;"
	public void getTopics() {
		String response = client.makeRequest("?action=getTopics");
		System.out.println("response:" + response);

		if (!response.equals("topics=")) {
			String value = response.split("=")[1];
			String[] topics = value.split(";");
			for (int i = 0; i < topics.length; i++) {
				System.out.println("topics[i]:" + topics[i]);
				client.chatInfo.addTopic(topics[i]);
			}
			client.gui.updateTopics(topics);
		}
	}

	// FORMAT: "participants=Mary;Pete;"
	public void getParticipants() {
		String response = client.makeRequest("?action=getParticipants");
		System.out.println("response:" + response);

		if (!response.equals("participants=")) {
			String value = response.split("=")[1];
			String[] participants = value.split(";");
			for (int i = 0; i < participants.length; i++) {
				System.out.println("participant[i]:" + participants[i]);
				client.participantInfo.addParticipant(participants[i]);
			}
			client.gui.updateParticipants(participants);
		}
	}

	@Override
	public String refresh(String topic) throws IOException {
		System.out.println("refresh CALLED--------------");
		if (!topic.trim().equalsIgnoreCase("")) {
			client.getParticipants();
			client.getTopics();
			client.getMessages(topic);
			return chatInfo.getMessages(topic);
		} else {
			return ("messages=");
		}
	}

}
