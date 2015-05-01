package ch.fhnw.kvan.chat.servlet;

import java.io.IOException;
import java.net.Socket;

import ch.fhnw.kvan.chat.general.Chats;
import ch.fhnw.kvan.chat.general.Participants;
import ch.fhnw.kvan.chat.gui.ClientGUI;
import ch.fhnw.kvan.chat.interfaces.IChatDriver;
import ch.fhnw.kvan.chat.interfaces.IChatRoom;
import ch.fhnw.kvan.chat.utils.*;

public class Client implements IChatDriver, IChatRoom {

	private static Client client;

	private Socket s;
	protected In in;
	protected Out out;
	private boolean running;
	private ClientGUI gui;

	// Same as ChatRoom-Class
	private final Participants participantInfo = new Participants();
	private final Chats chatInfo = new Chats();

	public Client() {
		running = true;
		// Start GUI
	}

	public static void main(String args[]) {
		System.out.println("Client-Class has been called with arguments: ");
		System.out.println("Name: " + args[0]);
		System.out.println("Host: " + args[1]);
		System.out.println("Port: " + args[2]);

		int port = Integer.parseInt(args[2]);
		String clientName = args[0];

		client = new Client();
		try {
			client.connect(args[1], port);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Send name
		client.out.println("name=" + clientName);

		client.gui = new ClientGUI(client, clientName);
		client.startListening();
	}

	public void startListening() {
		new Thread() {
			public void run() {

				String input = in.readLine();

				while (running && input != null) {
					System.out.println("input from server: " + input);
					processServerMessage(input);
					input = in.readLine();
				}
			}
		}.start();
	}

	public void processServerMessage(String input) {
		System.out.println("serverMessage:" + input);

		// Handle empty response
		if (input.equals("topics=") || input.equals("participants=")) {
			return;
		}

		String key = input.split("=")[0];
		String value = input.split("=")[1];

		switch (key) {
		case "message":
			// FORMAT: "message=Hello World;topic=myTopic"
			String topic = input.split("=")[2];
			System.out.println("topic:" + topic);
			String message = input.split("=")[1].split(";")[0];
			System.out.println("message:" + message);
			if (!chatInfo.addMessage(topic, message)) {
				System.out.println("ERROR: Couldnt add message!");
				break;
			}
			// Create String[] for the gui
			// ("messages=String1;String2;String3;")
			System.out.println(chatInfo.getMessages(topic));
			String messages = chatInfo.getMessages(topic).split("=")[1];
			String[] stringArray = messages.split(";;");

			// Check if should update the gui
			if (gui.getCurrentTopic().equals(topic)) {
				gui.updateMessages(stringArray);
			}

			break;
		case "add_topic":
			// FORMAT: "add_topic=myTopic"
			String addedTopic = input.split("=")[1];
			chatInfo.addTopic(addedTopic);
			gui.addTopic(addedTopic);
			break;
		case "remove_topic":
			// FORMAT: "remove_topic=myTopic"
			String removedTopic = input.split("=")[1];
			System.out.println("removedTopic:" + removedTopic);
			chatInfo.removeTopic(removedTopic);
			gui.removeTopic(removedTopic);
			break;
		case "topics":
			// FORMAT: "topics=Topic1;Topic2;"
			String[] topics = value.split(";");
			for (int i = 0; i < topics.length; i++) {
				chatInfo.addTopic(topics[i]);
			}
			gui.updateTopics(topics);
			break;
		case "add_participant":
			// FORMAT: "add_participant=client1"
			String addedPart = input.split("=")[1];
			participantInfo.addParticipant(addedPart);
			gui.addParticipant(addedPart);
			break;
		case "remove_participant":
			// FORMAT: "remove_participant=client2"
			String removedPart = input.split("=")[1];
			participantInfo.removeParticipant(removedPart);
			gui.removeParticipant(removedPart);
			break;
		case "participants":
			String[] parts = value.split(";");
			for (int i = 0; i < parts.length; i++) {
				participantInfo.addParticipant(parts[i]);
			}
			gui.updateParticipants(parts);
			break;
		case "messages":
			// FORMAT: "messages=Meldung1;Meldung2;topic=myTopic"
			String topic3 = input.split("=")[2];
			System.out.println("topic:" + topic3);
			String[] messages2 = input.split("=")[1].split(";;");
			for (int i = messages2.length - 2; i>=0; i--) {
				if (!chatInfo.addMessage(topic3, messages2[i])) {
					System.out.println("ERROR: Couldnt add message!");
					break;
				}				
			}
			// Create String[] for the gui
			// ("messages=String1;String2;String3;")
			System.out.println(chatInfo.getMessages(topic3));
			String messages3 = chatInfo.getMessages(topic3).split("=")[1];
			String[] stringArray2 = messages3.split(";;");

			// Check if should update the gui
			if (gui.getCurrentTopic().equals(topic3)) {
				gui.updateMessages(stringArray2);
			}

			break;
		default:
			throw new IllegalArgumentException("Invalid key: " + key);
		}
	}

	// --------------------------------
	// IChatDriver-Interface-Functions
	// --------------------------------

	@Override
	public void connect(String host, int port) throws IOException {
		// Setup socket-connection
		s = null;
		try {
			s = new Socket(host, port, null, 0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
		in = new In(s);
		out = new Out(s);
	}

	@Override
	public void disconnect() throws IOException {
		s.close();
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
		System.out.println("addParticipant SHOULD NEVER BE CALLED");
		return true;
	}

	@Override
	public boolean removeParticipant(String name) throws IOException {
		if (!name.trim().equalsIgnoreCase("")) {
			System.out.println("Participant removed");
			out.println("remove_name=" + name);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean addTopic(String topic) throws IOException {
		if (!topic.trim().equalsIgnoreCase("")) {
			System.out.println("Topic added: " + topic);
			out.println("add_topic=" + topic);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean removeTopic(String topic) throws IOException {
		if (!topic.trim().equalsIgnoreCase("")) {
			System.out.println("Topic removed");
			out.println("remove_topic=" + topic);
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
			out.println("message=" + message + ";topic=" + topic);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public String getMessages(String topic) throws IOException {
		System.out.println("getMessages CALLED--------------");
		if (!topic.trim().equalsIgnoreCase("")
				&& !chatInfo.getMessages(topic).equals("messages=")) {
			String messages = chatInfo.getMessages(topic).split("=")[1];
			String[] stringArray = messages.split(";;");
			gui.updateMessages(stringArray);	// This is ugly but it works

			return chatInfo.getMessages(topic);
		} else {
			gui.updateMessages(new String[0]);
			return ("messages=");
		}
	}

	@Override
	public String refresh(String topic) throws IOException {
		if (!topic.trim().equalsIgnoreCase("")) {
			return chatInfo.getMessages(topic);
		} else {
			return ("messages=");
		}
	}

}
