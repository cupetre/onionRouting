package Code;

import Logs.LogLevel;
import Logs.Logger;

import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class PeerManager {
    protected final String nodeIdentifier;
    private final Server server;
    private final int listeningPort;
    private final ExecutorService peerHandlerExecutor = Executors.newCachedThreadPool();

    private final Map<String, Peer> activePeers = new ConcurrentHashMap<>();

    public PeerManager(int listeningPort, String nodeIdentifier) {
        this.listeningPort = listeningPort;
        this.nodeIdentifier = nodeIdentifier;
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

        for (Peer peer : new HashSet<>(activePeers.values())) {
                try {
                    peer.sendMessage(new Message("SHUTDOWN", Arrays.asList(peer.getRemoteNodeId()), 0));

                } catch (Exception e) {
                    Logger.log("Error gracefully shutting down peer " + peer.getRemoteAddress() + ": " + e.getMessage(), LogLevel.Error);
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
        Peer peer = new Peer(socket, this, "INCOMING");

        //add to activePeers
        peerHandlerExecutor.submit(peer); //activate and allow msgs sharing
    }

    public Peer connectToPeer(String host, int port, String remoteNodeId) throws IOException {
        Logger.log("Attempt to establish connection to " + host + " : " + port,LogLevel.Info);

        Peer existingPeer = activePeers.get(remoteNodeId);
        if ( existingPeer != null && existingPeer.isConnected() ) {
            Logger.log("already connected to : " + remoteNodeId,LogLevel.Info);
            return existingPeer;
        }

        Socket outGoingSocket = new Socket(host,port);

        Peer peer = new Peer(outGoingSocket, this, remoteNodeId);
        peerHandlerExecutor.submit(peer);
        Logger.log("Successfull outoing connection to " + host + " : " + port,LogLevel.Success);
        return peer;
    }

    public void addPeer(String remoteNodeId, Peer peer) {
        activePeers.put(remoteNodeId, peer);
        Logger.log("Peer successfully added", LogLevel.Info);
    }

    public void removePeer(String remoteNodeId) {
        activePeers.remove(remoteNodeId);
        Logger.log("Peer removed. Total active peers: " + activePeers.size(), LogLevel.Info);
    }

    public abstract void processReceivedMessage(Message message, Peer sender);
}
