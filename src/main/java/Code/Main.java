package Code;

import Logs.LogLevel;
import Logs.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    private static Map<String, NodeConfig> allNetworkNodes = new HashMap<>();

    static {

        allNetworkNodes.put("AliceClient", new NodeConfig("AliceClient", "client-a", 8000));

        allNetworkNodes.put("MixNode_Alpha", new NodeConfig("MixNode_Alpha", "mixnode-alpha", 8001));

        allNetworkNodes.put("BobDestination", new NodeConfig("BobDestination", "destination-bob", 8002));

    }


    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java -jar app.jar <node_id> <node_type>");
            System.out.println("Example: java -jar app.jar AliceClient Client");
            System.out.println("Example: java -jar app.jar MixNode_Alpha Mix");
            System.out.println("Example: java -jar app.jar BobDestination Destination");
            return;
        }

        String nodeId = args[0];
        String nodeType = args[1]; // "Client", "Mix", or "Destination"

        NodeConfig thisNodeConfig = allNetworkNodes.get(nodeId);
        if (thisNodeConfig == null) {
            Logger.log("Error: Node ID '" + nodeId + "' not found in known network configurations.", LogLevel.Error);
            return;
        }

        AbstractNode currentNode = null; // Use AbstractNode as the common type

        try {
            switch (nodeType.toLowerCase()) {
                case "client":
                    // Client needs to know its path (e.g., via MixNode_Alpha to BobDestination)
                    // For this simple example, Client -> MixNode_Alpha -> BobDestination
                    List<String> clientPath = Arrays.asList("MixNode_Alpha"); // Only one mix for now
                    String clientDestination = "BobDestination";
                    currentNode = new ClientNode(thisNodeConfig.getId(), thisNodeConfig.getPort(), allNetworkNodes, clientPath, clientDestination);
                    break;
                case "mix":
                    currentNode = new MixNode(thisNodeConfig.getId(), thisNodeConfig.getPort(), allNetworkNodes);
                    break;
                case "destination":
                    currentNode = new DestinationNode(thisNodeConfig.getId(), thisNodeConfig.getPort(), allNetworkNodes);
                    break;
                default:
                    Logger.log("Invalid node type: " + nodeType, LogLevel.Error);
                    return;
            }

            if (currentNode != null) {
                currentNode.start(); // Start the node's server and any internal logic

                // If it's a client node, start the UserInput thread
                if (nodeType.equalsIgnoreCase("client")) {
                    UserInput userInput = new UserInput((ClientNode) currentNode);
                    userInput.start(); // Start the UserInput thread
                    // Register shutdown hook for UserInput as well
                    Runtime.getRuntime().addShutdownHook(new Thread(userInput::shutdown));
                }

                // Register shutdown hook for the current node (PeerManager/AbstractNode)
                AbstractNode finalCurrentNode = currentNode; // Need final variable for lambda
                Runtime.getRuntime().addShutdownHook(new Thread(finalCurrentNode::shutdown));

                Logger.log("Node '" + nodeId + "' of type '" + nodeType + "' is running.", LogLevel.Info);
            }

        } catch (Exception e) {
            Logger.log("Unhandled exception during node startup: " + e.getMessage(), LogLevel.Error);
            e.printStackTrace();
        }
    }
}