package Code;

import Logs.LogLevel;
import Logs.Logger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.net.Socket;

public class Peer implements Runnable {

    private static final Gson gson = new GsonBuilder().create();

    private final String remoteAddress;
    private final Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private volatile boolean connected = true;
    private final PeerManager peerManager;
    private String remoteNodeId;

    public Peer(Socket socket, PeerManager peerManager, String remoteNodeId) throws IOException {
        this.socket = socket;
        this.peerManager = peerManager;
        this.remoteNodeId = remoteNodeId;
        this.remoteAddress = socket.getRemoteSocketAddress().toString();

        setupStreams();

        Logger.log("Peer created for OUTGOING connection to " + remoteNodeId + " (" + remoteAddress + ")", LogLevel.Status);
        peerManager.addPeer(remoteNodeId,this);
    }

    public Peer(Socket socket, PeerManager peerManager) throws IOException {
        this.socket = socket;
        this.peerManager = peerManager;
        this.remoteAddress = socket.getRemoteSocketAddress().toString();

        setupStreams();
        Logger.log("Peer created for INCOMING connection to " + remoteAddress, LogLevel.Status);
    }

    private void setupStreams() throws IOException {
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true); // true for auto-flush
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public String getRemoteNodeId() {
        return remoteNodeId;
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    public synchronized void sendMessage(Message message) {
        if (!connected) {
            Logger.log("Attempted to send msg to disc peer" + getRemoteNodeId(), LogLevel.Info);
            return;
        }

        // dont forget to change message za cont + id path
        try {
            String jsonMessage = gson.toJson(message);
            writer.write(jsonMessage);
            writer.newLine(); // new chars in line
            writer.flush(); // clear flush for buffer
            Logger.log("Sent message to " + getRemoteNodeId() + " (" + getRemoteAddress() + ") [HopIndex: " + message.getCurrentHopIndex() + "] Content: \"" + message.getContent() + "\"", LogLevel.Info);

        } catch (IOException e) {
            Logger.log("failed to send message to peer" + getRemoteNodeId() + " : " + e.getMessage(), LogLevel.Error);
            connected = false; // quit connection
        }
    }

    public void shutdownPeer() {
        this.connected = false; // prekinuva
        try {
            if (socket != null && !socket.isClosed()) {
                socket.shutdownInput();
                socket.shutdownOutput();
                socket.close(); // Close the socket
                Logger.log("Peer socket closed for " + getRemoteNodeId(), LogLevel.Info);
            }
        } catch (IOException e) {
            Logger.log("Error closing socket for peer " + getRemoteNodeId() + " during shutdown: " + e.getMessage(), LogLevel.Error);
        }
    }

    @Override
    public void run() {
        if (remoteNodeId == null) {
            try {
                //we first wait for any msg to arrive
                String firstMessageJson = waitForMessage();

                if (firstMessageJson == null) {
                    Logger.log("Incoming peer " + getRemoteAddress() + " disconnected before sendign ID ", LogLevel.Info);
                    return;
                }

                Message handshakeMessage = gson.fromJson(firstMessageJson, Message.class);

                if (handshakeMessage == null || handshakeMessage.getFullPath() == null || handshakeMessage.getFullPath().isEmpty()) {
                    Logger.log(" Incomming peer " + getRemoteAddress() + "sent a non-workign msg " + firstMessageJson, LogLevel.Info);
                    return;
                }

                this.remoteNodeId = handshakeMessage.getFullPath().get(0);

                Logger.log("Peer handler started for the node -> " + remoteNodeId + " as " + remoteAddress, LogLevel.Info);

                peerManager.processReceivedMessage(handshakeMessage, this);
            } catch (JsonSyntaxException e) {
                Logger.log("Incoming peer " + getRemoteAddress() + " sent non-JSON handshake: " + e.getMessage(), LogLevel.Error);
                return; // Exit due to invalid handshake
            } catch (IllegalArgumentException e) {
                // Catch validation errors from Message constructor during handshake deserialization
                Logger.log("Validation error in handshake message from " + getRemoteAddress() + ": " + e.getMessage(), LogLevel.Error);
                return;
            }
        } else {
            Logger.log("Peer handler is started for node: " + remoteNodeId + " as " + remoteAddress, LogLevel.Status);
        }

        try {
            while (connected) {
                String rawMessageJson = waitForMessage(); // Blocks here, waiting for a message

                if (rawMessageJson == null) {
                    // This path is taken if waitForMessage() returns null (connection closed/error)
                    Logger.log( "Peer " + getRemoteNodeId() + " disconnected or error reading. Stopping handler.", LogLevel.Warn);
                    break; // Exit the while loop
                }

                Message message = null;
                try {
                    // --- CRITICAL CHANGE: Deserialize JSON string to Message object ---
                    message = gson.fromJson(rawMessageJson, Message.class);
                    if (message == null) {
                        Logger.log( "Received null message after deserialization from " + getRemoteNodeId() + ".", LogLevel.Error);
                        break; // Malformed/empty JSON
                    }
                    // Validate deserialized message: basic sanity checks
                    if (message.getContent() == null || message.getFullPath() == null || message.getFullPath().isEmpty() || message.getCurrentHopIndex() < 0) {
                        throw new IllegalArgumentException("Deserialized message has missing or invalid fields.");
                    }

                } catch (JsonSyntaxException e) {
                    // This path is taken if the received string is not valid JSON
                    Logger.log( "Protocol error: Malformed JSON message from " + getRemoteNodeId() + ": " + e.getMessage() + ". Raw: \"" + rawMessageJson + "\"", LogLevel.Error);
                    break; // Exit the while loop due to protocol violation
                } catch (IllegalArgumentException e) {
                    // This catches validation errors from the Message constructor
                    // or our custom sanity checks after deserialization
                    Logger.log( "Validation error in message from " + getRemoteNodeId() + ": " + e.getMessage() + ". Raw: \"" + rawMessageJson + "\"", LogLevel.Error);
                    break; // Exit the while loop due to invalid message data
                }

                // Pass the fully constructed Message object to the PeerManager
                // for further processing by the specific node type (Client, Mix, or Destination)
                peerManager.processReceivedMessage(message, this);
            }
        } finally {
            Logger.log( "Cleaning up connection for peer " + getRemoteNodeId() + " (" + getRemoteAddress() + ")", LogLevel.Info);
            peerManager.removePeer(getRemoteNodeId()); // Remove from PeerManager's list
            // Close streams and socket

            try {
                if (reader != null) reader.close();
                if (writer != null) writer.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException ex) {
                Logger.log("Error closing writer/reader/socket from " + getRemoteAddress() + ": " + ex.getMessage(), LogLevel.Error);
            }
            Logger.log("Peer handler finished for " + getRemoteAddress(), LogLevel.Info);
        }
    }

    private String waitForMessage() throws IOException {
        try {
            return reader.readLine();
        } catch (IOException e) {
            // Log if the peer disconnected while waiting for message
            if (e.getMessage() != null && (e.getMessage().contains("Connection reset") || e.getMessage().contains("Socket closed"))) {
                Logger.log("Peer " + getRemoteNodeId() + " disconnected unexpectedly during read.", LogLevel.Warn);
                connected = false; // Mark as disconnected
                return null;
            }
            throw e; // Re-throw other IOExceptions
        }
    }

    public void shutdownPeer() {
        Logger.log("Shutting down peer " + getRemoteNodeId() + " (" + getRemoteAddress() + ")", LogLevel.Info);
        connected = false; // Signal the run loop to terminate
        try {
            if (socket != null && !socket.isClosed()) {
                socket.shutdownInput(); // Interrupt blocking readLine()
                socket.shutdownOutput(); // Cleanly close output
                socket.close();         // Close the socket
            }
        } catch (IOException e) {
            Logger.log("Error during peer shutdown for " + getRemoteNodeId() + ": " + e.getMessage(), LogLevel.Error);
        }
    }
}
