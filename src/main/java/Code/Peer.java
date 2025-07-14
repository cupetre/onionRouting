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

    public Peer(Socket socket, PeerManager peerManager) throws IOException {
        this.socket = socket;
        this.peerManager = peerManager;

        InputStreamReader isr = new InputStreamReader(socket.getInputStream());
        OutputStreamWriter osr = new OutputStreamWriter(socket.getOutputStream());

        reader = new BufferedReader(isr);
        writer = new BufferedWriter(osr);

        //send it to the peermanager first
        peerManager.addPeer(this);
    }

    public String getRemoteAddress() {
        return socket.getInetAddress().getHostAddress() + " : " + socket.getPort();
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
            Logger.log("Attempted to send msg to disc peer" + getRemoteAddress(), LogLevel.Info);
            return;
        }

        try {
            writer.write(message.toString()); // content
            writer.newLine(); // new chars in line
            writer.flush(); // clear flush for buffer
            Logger.log("Message sent to " + getRemoteAddress() + " : " + message.toString() + " \" ", LogLevel.Info);

        } catch (IOException e) {
            Logger.log("failed to send message to peer" + getRemoteAddress() + " : " + e.getMessage(), LogLevel.Error);
            connected = false; // quit connection
        }
    }

    public void shutdownPeer() {
        this.connected = false; // Signal the run() loop to terminate
        try {
            if (socket != null && !socket.isClosed()) {
                // Closing input/output streams individually can unblock read/write calls
                socket.shutdownInput();
                socket.shutdownOutput();
                socket.close(); // Close the socket
                Logger.log("Peer socket closed for " + getRemoteAddress(), LogLevel.Info);
            }
        } catch (IOException e) {
            Logger.log("Error closing socket for peer " + getRemoteAddress() + " during shutdown: " + e.getMessage(), LogLevel.Error);
        }
    }

    @Override
    public void run() {
        Thread.currentThread().setName("PeerHandler-" + getRemoteAddress());
        Logger.log("Peer handler is started for node: " + getRemoteAddress(), LogLevel.Status);

        try { // Outer try block starts here
            while (connected) { // Loop condition relies on 'connected' flag
                String rawMessage = waitForMessage(); // (1) Blocks here, waiting for a message
                if (rawMessage == null) {
                    // (2) This path is taken if waitForMessage() returns null (connection closed/error)
                    Logger.log("Peer " + getRemoteAddress() + " disconnected or error reading. Stopping handler.", LogLevel.Warn);
                    break; // Exit the while loop
                }

                Message message;
                try {
                    message = new Message(rawMessage); // (3) Attempt to parse the message
                } catch (IOException e) {
                    // (4) This path is taken if the Message constructor throws an IOException
                    Logger.log("Protocol error parsing message from " + getRemoteAddress() + ": " + e.getMessage(), LogLevel.Error);
                    break; // Exit the while loop
                }

                // (5) If message successfully parsed and 'rawMessage' was not null
                peerManager.processReceivedMessage(message, this);

                // (6) Loop continues from here, back to 'while(connected)' to wait for the next message
            } // while loop ends here
        } finally { // (7) This finally block executes AFTER the outer try block finishes
            Logger.log("Cleaning up connection for peer " + getRemoteAddress(), LogLevel.Info);
            peerManager.removePeer(this); // Remove from PeerManager's list

            try { // Inner try-catch for resource closing itself
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
