package Code;

import Logs.LogLevel;
import Logs.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server extends Thread {
    private final int port;
    private ServerSocket serverSocket;
    private volatile boolean running = true; // flag to help us with shutdown
    private final PeerManager peerManager;

    public Server(int port, PeerManager peerManager) {
        this.port = port;
        this.peerManager = peerManager;
        Thread.currentThread().setName("Network.Server" + peerManager.nodeIdentifier); // self expl
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            Logger.log("Successfull start and listening on port...", LogLevel.Info);

            while (running) {
                Socket newPeerSocket;

                try {
                    newPeerSocket = serverSocket.accept(); // when a peer connects add it
                } catch (IOException e) {
                    if (!running) {
                        Logger.log("Server timed out/closed. ", LogLevel.Error);
                        break;
                    }
                    Logger.log("Error in connection. " + e.getMessage(), LogLevel.Error);
                    continue;
                }

                Logger.log("---- New incoming connection: ----");
                Logger.log("-> Local IP: " + newPeerSocket.getLocalAddress());
                Logger.log("-> Local Port: " + newPeerSocket.getLocalPort());
                Logger.log("-> Remote IP: " + newPeerSocket.getInetAddress());
                Logger.log("-> Remote Port: " + newPeerSocket.getPort());
                Logger.log("-------------------------");

                try { //add peer to the manager
                    peerManager.handleNewIncomingConnection(newPeerSocket);
                } catch (Exception e) {
                    Logger.log("Peer not added and problem in peer connection" + e.getMessage(), LogLevel.Error);
                    try {
                        newPeerSocket.close(); // inform + close it
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        } catch (IOException e) {
            Logger.log("Port is not listening or it's not working " + port + " <- port and cause -> " + e.getMessage(), LogLevel.Error);
        } finally {
            shutdown();
        }
    }

    public void shutdown() {
        running = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); // we need to close it in order for it to unblock and be able to accept other peers
            }
        } catch (IOException e) {
            Logger.log("Error in closing the server socket" + e.getMessage(), LogLevel.Error);
        }


    }
}



