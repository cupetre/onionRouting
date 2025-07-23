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
    }

    public Peer(Socket socket, PeerManager peerManager) throws IOException {
        this.socket = socket;
        this.peerManager = peerManager;
        this.remoteAddress = socket.getRemoteSocketAddress().toString();

        setupStreams();
        Logger.log("Peer created for INCOMING connection from " + remoteAddress, LogLevel.Status);
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
            writer.println(jsonMessage);
            writer.flush(); // clear flush for buffer
            Logger.log("Sent message to " + getRemoteNodeId() + " (" + getRemoteAddress() + ") [HopIndex: " + message.getCurrentHopIndex() + "] Content: \"" + message.getContent() + "\"", LogLevel.Info);

        } catch (Exception e) {
            Logger.log("failed to send message to peer" + getRemoteNodeId() + " : " + e.getMessage(), LogLevel.Error);
            connected = false; // quit connection
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

                peerManager.addPeer(this.remoteNodeId, this);

                Logger.log("Peer handler started for the node -> " + remoteNodeId + " as " + remoteAddress, LogLevel.Info);

                peerManager.processReceivedMessage(handshakeMessage, this);
            } catch (JsonSyntaxException e) {
                Logger.log("Incoming peer " + getRemoteAddress() + " sent non-JSON handshake: " + e.getMessage(), LogLevel.Error);
                return;
            } catch (IllegalArgumentException e) {
                Logger.log("Validation error in handshake message from " + getRemoteAddress() + ": " + e.getMessage(), LogLevel.Error);
                return;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            Logger.log("Peer handler is started for node: " + remoteNodeId + " as " + remoteAddress, LogLevel.Status);
        }

        try {
            while (connected) {
                String rawMessageJson = waitForMessage();

                if (rawMessageJson == null) {
                    Logger.log( "Peer " + getRemoteNodeId() + " disconnected or error reading. Stopping handler.", LogLevel.Warn);
                    break;
                }

                Message message = null;
                try {

                    message = gson.fromJson(rawMessageJson, Message.class);
                    if (message == null) {
                        Logger.log( "Received null message after deserialization from " + getRemoteNodeId() + ".", LogLevel.Error);
                        break;
                    }

                    if (message.getContent() == null || message.getFullPath() == null || message.getFullPath().isEmpty() || message.getCurrentHopIndex() < 0) {
                        throw new IllegalArgumentException("Deserialized message has missing or invalid fields.");
                    }

                } catch (JsonSyntaxException e) {
                    Logger.log( "Protocol error: Malformed JSON message from " + getRemoteNodeId() + ": " + e.getMessage() + ". Raw: \"" + rawMessageJson + "\"", LogLevel.Error);
                    break;
                } catch (IllegalArgumentException e) {
                    Logger.log( "Validation error in message from " + getRemoteNodeId() + ": " + e.getMessage() + ". Raw: \"" + rawMessageJson + "\"", LogLevel.Error);
                    break;
                }

                peerManager.processReceivedMessage(message, this);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            Logger.log( "Cleaning up connection for peer " + getRemoteNodeId() + " (" + getRemoteAddress() + ")", LogLevel.Info);
            peerManager.removePeer(getRemoteNodeId());

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
            if (e.getMessage() != null && (e.getMessage().contains("Connection reset") || e.getMessage().contains("Socket closed"))) {
                Logger.log("Peer " + getRemoteNodeId() + " disconnected unexpectedly during read.", LogLevel.Warn);
                connected = false;
                return null;
            }
            throw e;
        }
    }

    public void shutdownPeer() {
        Logger.log("Shutting down peer " + getRemoteNodeId() + " (" + getRemoteAddress() + ")", LogLevel.Info);
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.shutdownInput();
                socket.shutdownOutput();
                socket.close();
            }
        } catch (IOException e) {
            Logger.log("Error during peer shutdown for " + getRemoteNodeId() + ": " + e.getMessage(), LogLevel.Error);
        }
    }
}
