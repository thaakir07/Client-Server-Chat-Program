import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap; 

/*
 * This class represents a server that accepts incoming clients and
 * assigns them a client handler to handle their communication.
 */
public class Server {
    
    private ServerSocket serverSocket;
    private ConcurrentHashMap<String, ClientHandler> clientList = new ConcurrentHashMap<>();

    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    /**
     * Starts the server. This method is the main entry point of the
     * program. It creates a ServerSocket and a Server, and starts the
     * server's main loop.
     * @param args The command line arguments.
     */
    public static void main(String[] args) {
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(1234);
            Server server = new Server(serverSocket);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Start the server. This method is the main loop of the server.
     * It will accept incoming client sockets, assign a new ClientHandler
     * to each socket, and start a new thread for each client.
     * It will also notify all clients that a new client has joined.
     */
    public void start()
    {
        System.out.println("Server started.");
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                
                //Deal with the username of the client
                String username;
                while (true) {
                    username = reader.readLine();

                    if (username == null) {
                        continue;
                    } else if (username.isEmpty()) {
                        writer.write("Username cannot be empty.");
                        writer.newLine();
                        writer.flush();
                        continue;
                    } else if (clientList.containsKey(username)) {
                        writer.write("Username already taken.");
                        writer.newLine();
                        writer.flush();
                        continue;
                    } else {
                        writer.write("Username accepted.");
                        writer.newLine();
                        writer.flush();
                        break;
                    }
                }
                ClientHandler clientHandler = new ClientHandler(socket, clientList, username);
                clientList.put(username, clientHandler);
                System.out.println("Client has joined the chat.");
                
                //Synchronize the client list to avoid any race conditions and data corruption
                synchronized (clientList) {
                    clientHandler.groupChat(username + " has joined the chat.", true);
                    broadcastOnlineUsers();
                }

                //create a thread for this client to provide concurrency
                Thread thread = new Thread(clientHandler);
                thread.start();
            
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Broadcasts the list of online users to all connected clients.
     * It sends a the client list to all clients.
     */
    public void broadcastOnlineUsers() {
        //I tried sending the entire user hashmap but ran into trouble so Im sending it as a string
        String online = "ONLINE:" + String.join(",", clientList.keySet());
        for (ClientHandler handler : clientList.values()) {
            handler.communicate(online);
        }
    }

    /**
     * Stops the server by closing the server socket.
     * This will prevent any new clients from connecting
     * and terminate the server's main loop.
     */
    public void stop() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
