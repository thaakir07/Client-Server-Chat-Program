# Client-Server Chat program

A sophisticated multi-client Java chat application built with JavaFX that provides real-time communication between multiple users through a central server. The application features an intuitive graphical interface with support for both group chat and private messaging.

## Authors

- **Gideon Daniel Botha**
- **Priyal Bhana**
- **Sulaiman Bandarkar**
- **Thaakir Fernandez** (Group leader) - thaakir07@gmail.com

## Project Timeline

- **Start Date:** February 2025
- **Completed:** March 2025

## Features

- **Multi-client chat server** - Support for multiple simultaneous connections using multithreading
- **Username authentication** - Unique username validation with error handling
- **Group chat functionality** - Real-time broadcast messaging to all connected users
- **Private messaging ("Whispers")** - Send direct messages using `@username` format with dedicated windows
- **Exit command** - Clean disconnect using `/exit` command
- **JavaFX GUI** - Modern, responsive graphical interface with dark theme
- **Active user list** - Real-time display of online users with click-to-whisper functionality
- **Concurrent operations** - Thread-safe operations using ConcurrentHashMap
- **Multi-window support** - Multiple private chat windows can be open simultaneously

## Project Structure

```
.
├── src/
│   ├── Client.java          # JavaFX client application with GUI
│   ├── ClientHandler.java   # Server-side client handler (multithreaded)
│   ├── Server.java          # Main server class
├── Makefile                 # Build and execution automation
└── README.md
```

## Prerequisites

- Java Development Kit (JDK) 8 or higher with JavaFX support
- Make utility (for using the Makefile)
- Network connectivity (application uses IP 10.242.69.49 via ZeroTier VPN)
- JavaFX library

## Compilation and Execution

This project includes a Makefile for easy compilation and execution. All source files are located in the `src/` directory.

### Starting the Server

To compile all Java files and run the server:

```
make server
```

### Connecting Clients

To compile all Java files and run a client (in a separate terminal):

```
make client
```

**Note**: You can run multiple clients by opening additional terminals and executing `make client` in each.

### Cleaning Build Files

To remove the `bin/` directory containing compiled class files:

```
make clean
```

## Usage

1. **Start the Server**: Run `make server` first - you'll see "Server started." message
2. **Connect Clients**: Run `make client` in separate terminals for each user
3. **Login**: Enter a unique username when prompted
4. **Group Chat**: 
   - Type messages and press Enter to broadcast to all users
   - Your message appears immediately in your chat window
5. **Private Messaging ("Whispers")**:
   - Click on a username in the Active Users list (right panel)
   - Click "Whisper?" when it appears
   - A private chat window opens in the center panel
   - Multiple whisper windows can be open simultaneously
6. **Exit**: Type `/exit` to disconnect cleanly

### Interface Layout
- **Left Panel**: Global chat area with message history and input field
- **Center Panel**: Private whisper windows (opens when initiated)
- **Right Panel**: Active users list (click to start whispers)

## Network Configuration

- **Default IP Address**: 10.242.69.49 (Priyal Bhana ZeroTier VPN)
- **Default Port**: 1234

**Important**: The server must be running before any clients can connect.

## Additional Notes

- The server handles multiple clients concurrently using multithreading
- Client-server communication is thread-safe using synchronized blocks
- The GUI automatically updates user lists when clients join/leave
- Private whisper windows persist until manually closed
- Username validation prevents empty or duplicate usernames
- Robust error handling for network disconnections

## Contributing

This is an academic project. For questions or issues, please contact any of the authors listed above.

## License

This project is created for educational purposes.
