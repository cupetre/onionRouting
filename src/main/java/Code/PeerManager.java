package Code;

import Logs.LogLevel;
import Logs.Logger;

import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PeerManager {
    private final int listeningPort;
    private final String nodeType;
    private final Server server;
    private final ExecutorService peerHandlerExecutor = Executors.newCachedThreadPool();

    private final Set<Peer> activePeers = Collections.synchronizedSet(new HashSet<>());

    public PeerManager(int listeningPort, String nodeType) {
        this.listeningPort = listeningPort;
        this.nodeType = nodeType;
        this.server = new Server(listeningPort, this);
    }

    public void start() {
        server.start(); //start the server
        Logger.log("PeerManager started", LogLevel.Info);
        //for mixnet development here
    }

    public void shutdown() {
        Logger.log("PeerManager initiating shutdown...", LogLevel.Info);

        server.shutdown();

        synchronized (activePeers) {
            for (Peer peer : new HashSet<>(activePeers)) {
                try {
                    peer.sendMessage(new Message("SHUTDOWN"));
                    peer.shutdownPeer();
                } catch (Exception e) {
                    Logger.log("Error gracefully shutting down peer " + peer.getRemoteAddress() + ": " + e.getMessage(), LogLevel.Error);
                }
            }
        }

        peerHandlerExecutor.shutdownNow();
        try {
            if (!peerHandlerExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                Logger.log("Peer handler executor did not terminate in time.", LogLevel.Warn);
            }
        } catch (InterruptedException e) {
            Logger.log("Peer handler executor termination interrupted.", LogLevel.Error);
            Thread.currentThread().interrupt(); //vrati status nazad
        }

        Logger.log("PeerManager shut down complete.", LogLevel.Info);
    }

    public void handleNewIncomingConnection(Socket socket) throws IOException {
        Peer peer = new Peer(socket, this);

        //add to activePeers
        peerHandlerExecutor.submit((Runnable) peer); //activate and allow msgs sharing
    }

    public void connectToPeer(String host, int port) throws IOException {
        Logger.log("Attempt to establish connection to " + host + " : " + port,LogLevel.Info);
        Socket outGoingSocket = new Socket(host,port);
        Peer peer = new Peer(outGoingSocket, this);
        peerHandlerExecutor.submit((Runnable) peer);
        Logger.log("Successfull outoing connection to " + host + " : " + port,LogLevel.Success);
    }

    public void addPeer(Peer peer) {
        activePeers.add(peer);
        Logger.log("Peer successfully added", LogLevel.Info);
    }

    // Removes a peer from the active list (called from Peer.run() on disconnect)
    public void removePeer(Peer peer) {
        activePeers.remove(peer);
        Logger.log("Peer removed. Total active peers: " + activePeers.size(), LogLevel.Info);
    }

    // Broadcasts a message to all currently active peers
    public void broadcastMessage(Message message) {
        if (activePeers.isEmpty()) {
            Logger.log("No active peers to broadcast to.", LogLevel.Warn);
            return;
        }
        Logger.log("Broadcasting message: \"" + message.toString() + "\"", LogLevel.Info);
        synchronized (activePeers) { // Synchronize access to the set during iteration
            for (Peer peer : new HashSet<>(activePeers)) { // Iterate over a copy to avoid ConcurrentModificationException
                peer.sendMessage(message);
            }
        }
    }

    // --- Placeholder for actual message processing by the node type ---
    // In a real mixnet, this method would be overridden or branched based on nodeType.
    public void processReceivedMessage(Message message, Peer sender) {
        Logger.log("PeerManager received message from " + sender.getRemoteAddress() + ": \"" + message.toString() + "\"", LogLevel.Debug);

        // --- Mixnet Logic Placeholder ---
        // if (nodeType == "MIXNODE") {
        //     // Decrypt layer, add to shuffle buffer, then forward later
        // } else if (nodeType == "DESTINATION") {
        //     // Final decryption, display message
        // } else if (nodeType == "CLIENT") {
        //     // Handle incoming responses if the client expects them
        // }
    }
}
