package Code;

import Logs.LogLevel;
import Logs.Logger;

import java.io.IOException;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        //setup network settings
        int listeningPort = 8000;
        String nodeType = "GENERIC_PEER";

        Logger.log("Starting node as " + nodeType, LogLevel.Info);

        PeerManager peerManager = new PeerManager(listeningPort, nodeType);
        peerManager.start(); // This will start its internal Server thread and any outgoing connection logic

        new UserInput(peerManager).start(); // Pass PeerManager to UserInput for sending messages

        // --- Add a simple way to connect to another peer (for testing P2P connection) ---
        // This is crucial for demonstrating P2P functionality beyond just accepting connections.
        // In a real P2P app, this would be based on a list of known peers.
        // For testing, let's assume if this node isn't the first, it tries to connect to one.
        if (args.length > 0 && "CONNECT_TO".equals(args[0])) {
            try {
                String targetHost = args[1];
                int targetPort = Integer.parseInt(args[2]);
                Logger.log("Attempting to connect to " + targetHost + ":" + targetPort, LogLevel.Info);
                peerManager.connectToPeer(targetHost, targetPort); // PeerManager handles client connection
            } catch (NumberFormatException e) {
                Logger.log("Invalid port argument. Usage: java Main CONNECT_TO <host> <port>", LogLevel.Error);
            } catch (IOException e) {
                Logger.log("Failed to connect to peer: " + e.getMessage(), LogLevel.Error);
            }
        }
    }
}