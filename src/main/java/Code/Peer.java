package Code;

import Logs.LogLevel;
import Logs.Logger;

import java.io.*;
import java.net.Socket;

public class Peer implements Runnable {

    private final Socket socket;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private volatile boolean connected = true;
    private final PeerManager peerManager;
    private final String remoteNodeId;

    public Peer(Socket socket, PeerManager peerManager, String remoteNodeId) throws IOException {
        this.socket = socket;
        this.peerManager = peerManager;
        this.remoteNodeId = remoteNodeId;

        InputStreamReader isr = new InputStreamReader(socket.getInputStream());
        OutputStreamWriter osr = new OutputStreamWriter(socket.getOutputStream());

        reader = new BufferedReader(isr);
        writer = new BufferedWriter(osr);

        //send it to the peermanager first
        peerManager.addPeer(remoteNodeId,this);
    }

    public String getRemoteAddress() {
        return socket.getInetAddress().getHostAddress() + " : " + socket.getPort();
    }

    public String getRemoteNodeId() {
        return remoteNodeId;
    }

    public boolean isConnected() {
        return connected && socket != null && socket.isConnected() && !socket.isClosed();
    }

    public String waitForMessage() {
        try {
            //check if problem with msgs
            return reader.readLine();
        } catch (IOException e) {
            Logger.log("Error reading msg " + e.getMessage(), LogLevel.Error);
            connected = false; //disconect
            return null; //deactivate
        }
    }

    public synchronized void sendMessage(Message message) {
        if (!connected) {
            Logger.log("Attempted to send msg to disc peer" + getRemoteNodeId(), LogLevel.Info);
            return;
        }

        // dont forget to change message za cont + id path
        try {
            writer.write(message.getContent() + "|" + message.getNextHopID()); // fakto deka stv zaboraj
            writer.newLine(); // new chars in line
            writer.flush(); // clear flush for buffer
            Logger.log("Message sent to " + getRemoteNodeId() + " : " + message.toString() + " \" ", LogLevel.Info);

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
        Thread.currentThread().setName("PeerHandler-" + peerManager.nodeIdentifier + " - " +getRemoteNodeId());
        Logger.log("Peer handler is started for node: " + getRemoteNodeId() + " as " + getRemoteAddress(), LogLevel.Status);

        try {
            while (connected) {
                String rawMessageLine = waitForMessage(); // // this is where we activate the blocking queue
                if (rawMessageLine == null) {
                    Logger.log("Peer " + getRemoteNodeId() + " disconnected or error reading. Stopping handler.", LogLevel.Warn);
                    break;
                }

                String[] parts = rawMessageLine.split("\\|", 2);
                if (parts.length != 2) {
                    Logger.log("problem in message content + dest " + getRemoteNodeId() + " needed 2parts, but got only " + rawMessageLine, LogLevel.Error);
                    break;
                }

                String content = parts[0];
                String nextHopID = parts[1];

                Message message = new Message(content, nextHopID);

                peerManager.processReceivedMessage(message, this);
            }
        } catch (IllegalArgumentException e) {
            Logger.log("Protocol error parsing message from " + getRemoteNodeId() + ": " + e.getMessage(), LogLevel.Error);
        } finally {
            Logger.log("Cleaning up connection for peer " + getRemoteAddress() + " as " + getRemoteNodeId(), LogLevel.Info);
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
}
