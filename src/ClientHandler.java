import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/*
 * This class represents a client that connects to a server and sends and
 * receives messages. It handles private messages, group messages, and
 * broadcast messages between clients.
 */
public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private String username;
    private ConcurrentHashMap<String, ClientHandler> clientList;

    public ClientHandler(Socket socket, ConcurrentHashMap<String, ClientHandler> clientList, String username) {
        try {
            this.socket = socket;
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.username = username;
            this.clientList = clientList;
        } catch (IOException e) {
            communicate("terminate");
        }
    }

    /**
     * The main loop of the client handler. Listens for incoming messages from
     * the client, and broadcasts them to all other connected clients.
     */
    public void run() {
        String messageFromClient;
        while (socket.isConnected()) {
            try {
                messageFromClient = reader.readLine();
                broadcastMessage(messageFromClient);
            } catch (IOException e) {
                communicate("terminate");
                break;
            }
        }
    }

    /**
     * Broadcasts a message from the client to all other connected clients.
     * This method handles private messages, group messages, and the /exit
     * command.
     * @param message The message from the client.
     */
    public void broadcastMessage(String message) {
        if (message == null) {
            return;
        }
        String actualMessage = message.trim();
        if (actualMessage.equals("/exit")) {
            communicate("Exiting chat...");
            System.out.println("Client has left the chat.");
            removeClient();
            return;
        } else if (actualMessage.startsWith("@") && actualMessage.indexOf(" ") != -1) {
            whisper(actualMessage);
        } else {
            groupChat(message, false);
        }
        return;
    }

    /**
     * Sends a message to all connected clients except the sender, simulating
     * a group chat.
     * @param message The message to be sent to other clients.
     * @param bool is just used to solve an edge case with the @prefix
     */
    public void groupChat(String message, boolean bool) {
        //Deals with an edge case with the '@' prefix
        if (message.startsWith("@")) {
            communicate("No message attached");
            return;
        }
        //Accessing the individual clients' handlers and using them to broadcast the message was the cleanest way
        // to implement the feature in our humble opinion
        for (ClientHandler client : clientList.values()) {
            if (!client.username.equals(username)) {
                synchronized (client) {
                    try {
                        if (bool) {
                            //Deals with an edge case with regards to clients leaving and joining
                            client.writer.write(message);
                        } else {
                            //Normal group chat message
                            client.writer.write(username + ": " + message);
                        }
                        client.writer.newLine();
                        client.writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

/**
 * Sends a private message to a specified client. The message must be prefixed
 * with the '@' symbol followed by the recipient's username and a space.
 * @param message The message format should be "@username message" where
 *                "username" is the recipient's username and "message" is the
 *                content to be delivered.
 */
    public void whisper (String message) {
        if (message.indexOf(" ") == -1) {
            communicate("No message attached");
            return;
        }

        String receiver = message.substring(1, message.indexOf(" "));
        message = message.substring(message.indexOf(" ") + 1);
        ClientHandler client = clientList.get(receiver);
        //Deal with any edge cases
        if (client == null) {
            communicate("Client not found");
        } else if (message.equals("")) {
            communicate("No message attached");
        } else {
            //Send the message to the receiver, again synchrozing to avoid race conditions etc.
            synchronized (client) {
                try {
                    client.writer.write("Whisper from " + this.username + ": " + message);
                    client.writer.newLine();
                    client.writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }   

    /**
     * Removes the client from the ConcurrentHashMap and broadcasts a message to
     * all other clients that the client has left the group chat. This method is
     * called when the client sends the /exit command or when the client's
     * connection is terminated.
     */
    public void removeClient() {
        String exitMessage = username + " has left the group chat.";
        String leavingmsg = "LEAVING: " + username;
        synchronized (clientList) {
            clientList.remove(username);
        }
        //We send 2 messages because one lets the clients know to remove the former client locally
        groupChat(leavingmsg, true);
        groupChat(exitMessage, true);
        communicate("terminate");
    }

    /**
     * Sends a communication message to the client through the output stream.
     * This method writes the message, adds a newline, and flushes the stream
     * to ensure the message is sent.
     * @param comms The message to be sent to the client.
     */
    public void communicate(String comms) {
        try {
            writer.write(comms);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
