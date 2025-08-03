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
        peerHandlerExecutor.submit(peer);
    }

    public Peer connectToPeer(String host, int port, String remoteNodeId) throws IOException {
        Logger.log("Attempt to establish connection to " + remoteNodeId + " : " + port,LogLevel.Info);

        Peer existingPeer = activePeers.get(remoteNodeId);
        if ( existingPeer != null && existingPeer.isConnected() ) {
            Logger.log("already connected to : " + remoteNodeId,LogLevel.Info);
            return existingPeer;
        }

        Socket outGoingSocket = new Socket(host,port);

        Peer peer = new Peer(outGoingSocket, this, remoteNodeId);
        peerHandlerExecutor.submit(peer);

        addPeer(remoteNodeId, peer);

        Logger.log("Successfull outoing connection to " + remoteNodeId + " : " + port,LogLevel.Success);
        return peer;
    }

    public void addPeer(String remoteNodeId, Peer peer) {
        if ( peer.getRemoteNodeId() == null || !peer.getRemoteNodeId().equals(remoteNodeId) ) {
            Logger.log("Adjusting peer ID from " + remoteNodeId + " to actual " + peer.getRemoteNodeId() + " for storage", LogLevel.Debug);
            remoteNodeId = peer.getRemoteNodeId();
        }

        Peer oldPeer = activePeers.put(remoteNodeId, peer);

        if ( oldPeer != null && oldPeer != peer ) {
            Logger.log("Replaced existing connection for peer " + remoteNodeId + " : " + oldPeer.getRemoteNodeId() + " for storage", LogLevel.Info);
            oldPeer.shutdownPeer();
        } else {
            Logger.log("attempt in removing non existant or same peer from the active peers list " + remoteNodeId, LogLevel.Info);
        }

        Logger.log("Peer successfully added", LogLevel.Info);
    }

    public void removePeer(String remoteNodeId) {
        activePeers.remove(remoteNodeId);
        Logger.log("Peer removed. Total active peers: " + activePeers.size(), LogLevel.Info);
    }

    public abstract void processReceivedMessage(Message message, Peer sender);
}
