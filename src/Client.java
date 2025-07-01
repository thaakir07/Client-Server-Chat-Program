import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/*
 * Client class for the Chat Application. 
 * This class represents a client that connects to a server and sends and receives messages.
 * It also implements the main entry point for the JavaFX application and handles the user interface.
 */
public class Client extends Application {
    private Socket socket;
    private BufferedWriter output;
    private BufferedReader input;
    private String username;
    private String[] users;
    private ConcurrentHashMap<String, Integer> activeClientMap = new ConcurrentHashMap<String, Integer>();
    private ConcurrentHashMap<String, VBox> openWispWindows;
    private VBox wispBox;
    private ListView<String> activeClientList;
    private TextArea globalOutput;
    private BorderPane backGroundPane;

    /*
     * Constructor for the Client class.
     * @param socket the socket to connect to the server
     * @param username the username of the client
     */
    public Client() {
        try {
            this.socket = new Socket("10.242.69.49", 1234);
            this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            System.out.println("Unable to connect to the server.");
            Platform.exit();
            close();
        }
    }

     /**
     * Starts a new thread that listens for incoming messages from the server.
     * The thread blocks on the readLine() call until a message is received.
     * The different responses from the server are handled differently.
     * If the message is "ONLINE:", the client list is updated.
     * If the message is "LEAVING", the client is removed from the list.
     * If the message is a whisper, the whisper is displayed in the whisper window.
     * If the message is "terminate", the connection is closed and the program exits.
     * If the message is anything else, it is appended to the global chat text area in the GUI.
     */
    public void listenForMessages() {
        // Running the message listener in a separate thread avoids any potential blockages
        new Thread(() -> { 
            String msg;
            while (isConnectionActive()) {
                try {
                    msg = input.readLine();
                    //Different reactions to the different responses from the Server/ClientHandler
                    if (msg == null) {
                        System.out.println("Oops! Something went wrong, try again later :(");
                        System.exit(0);
                    //Get client list from server
                    } else if (msg.startsWith("ONLINE:")) {
                        populateClientList(msg);
                    } else if (msg.startsWith("LEAVING")) {
                        removeClientFromList(msg);
                    } else if (msg.startsWith("Whisper from ")) {
                        receiveIncomingWisp(msg);
                    } else if (msg.equals("terminate")) {
                        close();
                        System.exit(0);
                    } else {
                        final String tmpMsg = msg;
                        Platform.runLater(() -> {
                            globalOutput.appendText(tmpMsg + "\n");
                        });
                    }
                } catch (IOException e) {
                    close();
                    break;
                }
            }
        }).start();
    }

    /**
     * Returns true if the socket is not null, is not closed, and is connected.
     * This is used to check if the connection to the server is active.
     * @return true if the socket is active, false otherwise
     */
    private boolean isConnectionActive() {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }

    /**
     * Closes all open connections and streams associated with this client.
     */
    private void close() {
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*##############################################################################
      ######################## Deon's GUI Zone, DON'T TOUCH >:( ####################
      ##############################################################################
      */

    /**
     * This method is the main entry point for the JavaFX application.
     * It prompts the user for a username, logs in the user, and starts the message listener.
     * It also builds the global chat area, the client list and the backdrop.
     * It sets up the scene and shows the main stage.
     * @param mainStage the main stage of the application
     */
    public void start(Stage mainStage) {
        logInClient();

        Platform.runLater(() -> {
            // Build global chat area (Output,Input) using a VBox to cobine the 3 controls into one object
            VBox globalChatArea = buildGlobalChat();

            listenForMessages();

            backGroundPane = new BorderPane();
            openWispWindows = new ConcurrentHashMap<String, VBox>();
            wispBox = new VBox();
            wispBox.setSpacing(10);

            // Build active client list on the right hand side of the GUI 
            // using a ListView object to make interactions with the list easier
            activeClientList = buildClientList(activeClientMap, backGroundPane);
            
            buildBackDrop(globalChatArea, activeClientList, wispBox);

            Scene scene = new Scene(backGroundPane, 1200, 700);
            mainStage.setScene(scene);

            mainStage.show();
        });
    }

    /**
     * Prompt the user for a username. The method returns the username entered or
     * null otherwise. The method also handles the cases where the username
     * is empty or already taken by displaying an appropriate error message.
     * @param errMessage the error message to display if applicable
     * @return the username entered or null otherwise
     */
    private String promptUsername(String errMessage) {
        // Prompt a dialog box asking for a username before displaying the main GUI
        TextInputDialog usernameDialog = new TextInputDialog();
        usernameDialog.setTitle("Log In");

        // Display null error message
        if (errMessage.equals("Username cannot be empty.")) {
            usernameDialog.setHeaderText("Username my not be empty.");
        // Display unique name error message
        } else if (errMessage.equals("Username already taken.")) {
            usernameDialog.setHeaderText("Username already taken.");
        // Display normal log in case
        } else {
            usernameDialog.setHeaderText("Welcome to the Chillax chat server");
        }

        usernameDialog.setContentText("Please enter your username:");
        String username = usernameDialog.showAndWait().orElse(null);

        // Implements the cancel button via checking for the orElse case
        if (username == null) {
            Platform.exit();
            close();
            System.exit(0);
        }

        return username;
    }
    
/**
 * Handles the client login process by prompting the user for a username,
 * validating it and doing another iteration with the correct error message 
 * if needed and then sending it to the server. 
 */
    private void logInClient() {
        // While loop to be able to ask for a username till a valid name is given
        while (true) {
            username = promptUsername("");

            // Handles Null usernames
            while (username == null || username.trim().isEmpty()) {
                username = promptUsername("Username cannot be empty.");
            }

            // Username not null so send to server and see if valid (unique)
            try {
                while (true) {
                    sendNameToServer();
                    String errResponse = input.readLine();
                    if (errResponse == null) {
                        close();
                        Platform.exit();
                    }
                    if (errResponse.equals("Username accepted.")) {
                        return;
                    }

                    // Username invalid
                    username = promptUsername(errResponse);
                }
            } catch (IOException e) {
                System.out.println("Failed to log in");
                close();
                Platform.exit();
            }
        }
    }

    /**
     * Builds the global chat area with a text area for output and a HBox 
     * containing a TextField for input and a Button for sending the message.
     * The global chat area is returned as a VBox to be added to the main pane.
     * @return the global chat area as a VBox containing the Output area, input area and the send button
     */
    private VBox buildGlobalChat() {
        // Use TextArea for appending messages that get sent
        globalOutput = new TextArea();
        globalOutput.setEditable(false);
        globalOutput.setPrefHeight(650);
        globalOutput.setPrefWidth(600);
        globalOutput.setWrapText(true);
        globalOutput.setStyle("-fx-control-inner-background: rgb(69, 69, 69); -fx-font-size: 14");

        // Use TextField since it allows the setOnAction event manager to be able to send messages
        TextField globalInput = new TextField();
        globalInput.setPromptText("Type message here...");
        globalInput.setOnAction(e -> sendMessage(username, globalInput, globalOutput));
        globalInput.setStyle("-fx-background-color:rgb(69, 69, 69); -fx-text-fill: white; -fx-font-size: 14");

        // Button to send messages if your not feeling in a rush
        Button sendMessageButton = new Button("Send");
        sendMessageButton.setOnAction(e -> sendMessage(username, globalInput, globalOutput));
        sendMessageButton.setStyle("-fx-background-color:rgb(200, 20, 250); -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold");

        // Combine the bottom two components in a HBox to put them next to each other
        HBox globalInputBox = new HBox(globalInput, sendMessageButton);
        HBox.setHgrow(globalInput, Priority.ALWAYS);

        // Combine these the output and the new input combination box to get the full global chat Object (VBox)
        VBox globalChatArea = new VBox(10, globalOutput, globalInputBox);
        globalChatArea.setPadding(new Insets(10));

        return globalChatArea;
    }
    
    /**
     * Sends a message from the client to the server. This method sends the
     * message and appends it to the global chat text area.
     * @param username the username of the client
     * @param globalInput the TextField containing the message to be sent
     * @param globalOutput the TextArea where the message will be appended
     */
    private void sendMessage(String username, TextField globalInput, TextArea globalOutput) {
        String message = globalInput.getText();
        if (!message.isEmpty()) {
            // Creates new thread to not get mixed up with listenformessages() thread
            // or the GUI thread so both can contiue running smoothly at the same time
            new Thread(() -> {  
                try {
                    output.write(message);
                    output.newLine();
                    output.flush();

                    // Visuals on Client sending message GUI
                    Platform.runLater(() -> {
                        globalOutput.appendText(username + ": " + message + "\n");
                        globalInput.clear();
                    });
                    
                } catch (IOException e) {
                    System.out.println("Error sending message: "  + e.getMessage());
                    close();
                }
            }).start();
            
        }
    }

    /**
     * Sends a whisper message from the client to the server. This method sends
     * the message and appends it to the whisper text area.
     * @param username the username of the client
     * @param receiver the username of the client the whisper is intended for
     * @param wispMessageIn the TextField containing the message to be sent
     * @param wispMessageOut the TextArea where the message will be appended
     */
    private void sendWisp(String username, String receiver, TextField wispMessageIn, TextArea wispMessageOut) {
        String message = wispMessageIn.getText();
        if (!message.isEmpty()) {

            // Same as send messages, assign its own thread to avoid stoppages and bad responsiveness
            new Thread(() -> {
                try {
                    output.write("@" + receiver + " " + message);
                    output.newLine();
                    output.flush();

                    Platform.runLater(() -> {
                        wispMessageOut.appendText(username + ": " + message + "\n");
                        wispMessageIn.clear();
                    });

                } catch (IOException e) {
                    System.out.println("Error sending whisper: "  + e.getMessage());
                    close();
                }
            }).start();
        }
    }

    /**
     * Builds the backdrop of the main stage, with the global chat, whisper windows,
     * and active clients list.
     * @param globalChatArea the VBox containing the global chat
     * @param activeClientList the ListView containing the active client list
     * @param wispBox the VBox containing the whisper windows
     */
    private void buildBackDrop(VBox globalChatArea, ListView<String> activeClientList, VBox wispBox) {
        backGroundPane.setStyle("-fx-background-color: rgb(40, 45, 50)");
        backGroundPane.setLeft(globalChatArea);
        backGroundPane.setCenter(wispBox);
        backGroundPane.setRight(activeClientList);
        BorderPane.setMargin(activeClientList, new Insets(10, 10, 10, 10));
    }

    /**
     * Builds the ListView for the active clients in the client list.
     * The user can click on a client name to toggle the visibility of
     * a "Whisper?" label, which when clicked will open a whisper window
     * for the chosen client.
     * @param activeClientMap the ConcurrentHashMap containing the active clients
     * @param myPane the BorderPane where the ListView will be placed
     * @return the ListView containing the active clients
     */
    private ListView<String> buildClientList(ConcurrentHashMap<String, Integer> activeClientMap, BorderPane myPane) {
        ListView<String> activeClientList = new ListView<>();
        activeClientList.setPrefWidth(150);
        activeClientList.setPrefHeight(200);
        activeClientList.setEditable(false);

        // ListCells used to dynamically handle the client list when new clients join or online clients leave
        activeClientList.setCellFactory(param -> new ListCell<String>() {
            @Override
            protected void updateItem(String chosenClient, boolean empty) {
                super.updateItem(chosenClient, empty);

                if (empty || chosenClient == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: rgb(69, 69, 69); -fx-text-fill: transparent;");
                    return;
                }

                Label clientName = new Label(chosenClient);
                clientName.setStyle("-fx-text-fill: white; -fx-font-size: 15; -fx-font-weight: bold");

                // Whisper prompt starts invisible till asked for by double clicking the user's name in the client list
                Label wispLabel = new Label("Whisper?");
                wispLabel.setStyle("-fx-cursor: hand;");
                wispLabel.setVisible(false);

                VBox clientVBox = new VBox(clientName, wispLabel);
                
                clientName.setOnMouseClicked(whisper -> {
                    if (!(chosenClient.equals(username))) {
                        wispLabel.setVisible(!wispLabel.isVisible());
                    }
                });

                wispLabel.setOnMouseClicked(creaWisp -> {
                     if (wispLabel.isVisible() && !(chosenClient.equals(username))) {
                        String defaultMessage = "";

                        // Initialise wispBox if needed
                        if (wispBox == null) {
                            wispBox = new VBox();
                            wispBox.setSpacing(10);
                            backGroundPane.setCenter(wispBox);
                        }
                         // Checks if whisper window already open for chosen client
                         if(!openWispWindows.containsKey(chosenClient)) {
                            VBox wispWindow = buildWispWindow(username, chosenClient, defaultMessage);
                            wispBox.getChildren().add(wispWindow);
                            openWispWindows.put(chosenClient, wispWindow);
                        }
                    }
                });

                if (isSelected()) {
                    setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white;");
                } else {
                    setStyle("-fx-background-color: rgb(69, 69, 69); -fx-text-fill: white;");
                }

                setGraphic(clientVBox);
            }
        });

        return activeClientList;
    }

    /**
     * Builds the whisper window for the client.
     * @param username the username of the client
     * @param receiver the username of the client the whisper is intended for
     * @param message the message to be displayed in the window
     * @return the whisper window
     */
    private VBox buildWispWindow(String username, String receiver, String message) {
        // Build instances of a text output area, input area and a send and close window button 
        // so that if a client needs more than one window open it is no issue, also store each VBox 
        // in a hashmap to be able to easily remove (and add) windows when a client leave or someone 
        // requests a new whisper window
        TextArea wispMessageOut = new TextArea();
        wispMessageOut.setEditable(false);
        wispMessageOut.setPrefWidth(200);
        wispMessageOut.setPrefHeight(700);
        wispMessageOut.setWrapText(true);
        wispMessageOut.setStyle("-fx-control-inner-background: rgb(69, 69, 69); -fx-font-size: 14; -fx-font-color: white;");

        TextField wispMessageIn = new TextField();
        wispMessageIn.setPromptText("Whisper " + receiver + "...");
        wispMessageIn.setOnAction(e -> sendWisp(username, receiver, wispMessageIn, wispMessageOut));
        wispMessageIn.setStyle("-fx-background-color:rgb(69, 69, 69); -fx-text-fill: white; -fx-font-size: 14");

        Button wispButton = new Button("Send");
        wispButton.setStyle("-fx-background-color:rgb(200, 20, 250); -fx-text-fill: white; -fx-font-weight: bold");
        wispButton.setOnAction(e -> sendWisp(username, receiver, wispMessageIn, wispMessageOut));

        Button wispCloseButton = new Button("Close");
        wispCloseButton.setStyle("-fx-background-color:rgb(200, 20, 250); -fx-text-fill: white; -fx-font-weight: bold");

        HBox wispInBox = new HBox(wispMessageIn, wispButton, wispCloseButton);
        wispInBox.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(wispMessageIn, Priority.ALWAYS);

        VBox wispWindow = new VBox(10, wispMessageOut, wispInBox);
        wispWindow.setPadding(new Insets(10));
        wispWindow.setStyle("-fx-background-color: rgb(40, 45, 50)");

        wispCloseButton.setOnAction(e ->  {
            wispBox.getChildren().remove(wispWindow);
            openWispWindows.remove(receiver);
        });

        if (!(message.equals(""))) {
            wispMessageOut.appendText(receiver + ":" + message + "\n");
        }

        return wispWindow;
    }

    /*
     * Sends the username of the client to the server.
     */
    private void sendNameToServer() {
        try {
            output.write(username);
            output.newLine();
            output.flush();
            
        } catch (IOException e) {
            System.out.println("Unsuccesful sending username to Server");
        }
    }
    
    /*
     * Updates the ListView of active clients by clearing the current list and
     * readding all the usernames in the activeClientMap.
     */
    private void updateClientList() {
        activeClientList.getItems().clear();
        activeClientList.getItems().addAll(activeClientMap.keySet());
    }
    
/**
 * Populates the active client list by parsing the incoming message from the server.
 * @param msg a string message from the server in the format "ONLINE:user1,user2,...,userN"
 */
    private void populateClientList(String msg) {
        users = msg.substring(7).split(",");
        // Loop thorugh the ConcurrentHashmap of currently online users to populate the client list on the GUI
        Platform.runLater(() -> {
            activeClientMap.clear();
            for (int i = 0; i < users.length; i++) {
                activeClientMap.put(users[i], i);
            }
            updateClientList();
        });
    }
    
    /**
     * Handles the message from the server when a client leaves the group chat.
     * This method removes the leaving user from the activeClientMap and updates
     * the client list in the GUI. It also removes any associated whisper windows
     * from the GUI.
     * @param msg a string message from the server
     */
    private void removeClientFromList(String msg) {
        String leavingUser = msg.substring(msg.indexOf(" ") + 1);
        activeClientMap.remove(leavingUser);

        Platform.runLater(() -> {
            // Remove associated Whisper windows by looping through the openWispWinodows hashmap
            // containing the username as key and their assoicated whipser window as value
            for (String client: openWispWindows.keySet()) {
                if (client.equals(leavingUser)) {
                    VBox wispWindow = openWispWindows.get(client);
                    wispBox.getChildren().remove(wispWindow);
                    openWispWindows.remove(client);
                }
            }

            // Update client list to reflect user leaving
            updateClientList();
        });
    }
    
    /**
     * Handles the message from the server when a whisper is received from another client.
     * This method creates a new whisper window if one does not exist for the sender, and
     * appends the message to the existing whisper window if one does exist.
     * @param msg a string message from the server
     */
    private void receiveIncomingWisp(String msg) {
        String sender = msg.substring(13, msg.indexOf(":"));
        String message = msg.substring(msg.indexOf(":") + 1);

        //Gui implementation of wisp
        Platform.runLater(() -> {
            // Create wisp window if it does not exist
            if (!openWispWindows.containsKey(sender)) {
                VBox wispWindow = buildWispWindow(username, sender, message);
                wispBox.getChildren().add(wispWindow);
                openWispWindows.put(sender, wispWindow);
            // Append text to the open whisperWinodow if it does exist
            } else {
                VBox wispWindow = openWispWindows.get(sender);
                TextArea wispMessageOut = (TextArea) wispWindow.getChildren().get(0);
                wispMessageOut.appendText(sender + ":" + message + "\n");
            }
        });
    }

    /**
     * This is the main entry point of the JavaFX application.
     * It calls launch to start the JavaFX application thread.
     * @param args The command line arguments.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
