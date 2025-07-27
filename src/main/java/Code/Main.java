package Code;

import CryptoUtils.NodeKeyRegistry;
import Logs.LogLevel;
import Logs.Logger;
import NodesConstruct.*;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

    private static Map<String, NodeConfig> allNetworkNodes = new HashMap<>();

    static {
        allNetworkNodes.put("AliceClient", new NodeConfig("AliceClient", "client-a", 8000));
        allNetworkNodes.put("MixNode_Alpha", new NodeConfig("MixNode_Alpha", "mixnode-alpha", 8001));
        allNetworkNodes.put("BobDestination", new NodeConfig("BobDestination", "destination-bob", 8002));
    }

    public static void main(String[] args) {
        Logger.log("Starting Mixnet Nodes...", LogLevel.Info);

        try {
            NodeKeyRegistry.generateAndRegisterKeys(allNetworkNodes);

            Logger.log("The app has started with key pairs assigned successfully AMEN", LogLevel.Status);
        } catch (NoSuchAlgorithmException e) {
            Logger.log("Error in generating and giving keys to the nodes when starting app" + e.getMessage(), LogLevel.Error);
            System.exit(1);
        }
        // Command line arguments: <node_type> <node_id>
        if (args.length < 2) {
            System.err.println("Usage: java -jar target/socket-1.0-SNAPSHOT-shaded.jar <node_type> <node_id>");
            System.exit(1);
        }

        String nodeType = args[0];
        String nodeID = args[1];

        NodeConfig thisNodeConfig = allNetworkNodes.get(nodeID);
        if (thisNodeConfig == null) {
            Logger.log("Error: Node ID '" + nodeID + "' not found in known network configurations.", LogLevel.Error);
            System.exit(1);
        }

        AbstractNode currentNode = null;
        UserInput userInput = null;

        ExecutorService executor = Executors.newCachedThreadPool();

        try {
            switch (nodeType.toLowerCase()) {
                case "client":
                    currentNode = new ClientNode(thisNodeConfig.getId(), thisNodeConfig.getPort(), allNetworkNodes);

                    userInput = new UserInput((ClientNode) currentNode);
                    executor.submit(userInput);
                    break;
                case "mix":
                    currentNode = new MixNode(thisNodeConfig.getId(), thisNodeConfig.getPort(), allNetworkNodes);
                    break;
                case "destination":
                    currentNode = new DestinationNode(thisNodeConfig.getId(), thisNodeConfig.getPort(), allNetworkNodes);
                    break;
                default:
                    Logger.log("Invalid node type: " + nodeType, LogLevel.Error);
                    System.exit(1);
            }

            final AbstractNode finalCurrentNode = currentNode;

            if (finalCurrentNode != null) {
                Logger.log("Initializing " + nodeType + " node " + nodeID + " on port " + thisNodeConfig.getPort() + "...", LogLevel.Info);
                executor.submit(() -> finalCurrentNode.start());
            }

            final UserInput finalUserInput = userInput;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                Logger.log( "Shutdown hook activated. Initiating graceful shutdown...", LogLevel.Info);
                if (finalUserInput != null) {
                    finalUserInput.interrupt();
                }
                if (finalCurrentNode != null) {
                    finalCurrentNode.shutdown();
                }
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                        Logger.log("Executor did not terminate cleanly within timeout. Forcing shutdown.", LogLevel.Warn);
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    Logger.log("Shutdown interrupted during executor termination.", LogLevel.Error);
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
                Logger.log("Node '" + nodeID + "' shutdown complete.", LogLevel.Info);
            }));

            Logger.log("Node '" + nodeID + "' of type '" + nodeType + "' is running. Press Ctrl+C to stop.", LogLevel.Info);

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Logger.log("Main thread interrupted. Exiting loop.", LogLevel.Info);
                    Thread.currentThread().interrupt();
                }
            }

        } catch (Exception e) {
            Logger.log("An unhandled exception occurred during node startup: " + e.getMessage(), LogLevel.Error);
            e.printStackTrace();
            System.exit(1);
        }
    }
}