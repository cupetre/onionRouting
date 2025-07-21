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

        String nodeId = args[0];
        String nodeType = args[1];

        NodeConfig thisNodeConfig = allNetworkNodes.get(nodeId);
        if (thisNodeConfig == null) {
            Logger.log("Error: Node ID '" + nodeId + "' not found in known network configurations.", LogLevel.Error);
            return;
        }

        AbstractNode currentNode = null;

        try {
            switch (nodeType.toLowerCase()) {
                case "client":
                    List<String> clientPath = Arrays.asList("MixNode_Alpha");
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
                currentNode.start();

                if (nodeType.equalsIgnoreCase("client")) {
                    UserInput userInput = new UserInput((ClientNode) currentNode);
                    userInput.start();
                    Runtime.getRuntime().addShutdownHook(new Thread(userInput::shutdown));
                }

                AbstractNode finalCurrentNode = currentNode;
                Runtime.getRuntime().addShutdownHook(new Thread(finalCurrentNode::shutdown));

                Logger.log("Node '" + nodeId + "' of type '" + nodeType + "' is running.", LogLevel.Info);
            }

        } catch (Exception e) {
            Logger.log("Unhandled exception during node startup: " + e.getMessage(), LogLevel.Error);
            e.printStackTrace();
        }
    }
}