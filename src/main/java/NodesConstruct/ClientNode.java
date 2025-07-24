package NodesConstruct;

import Code.ClientMessageBuilder;
import Code.Message;
import Code.Peer;
import CryptoUtils.NodeKeyRegistry;
import Logs.LogLevel;
import Logs.Logger;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientNode extends AbstractNode {

    public ClientNode(String nodeID, int listeningPort, Map<String, NodeConfig> knownNodeConfigs) {
        super(nodeID, listeningPort, knownNodeConfigs);

        Logger.log("ClientNode is done.", LogLevel.Info);
    }

    public void sendUserMessage(String content, String targetDestinationId, List<String> mixNodeIds) {
        try {
            List<String> fullPath = new ArrayList<>();

            if ( mixNodeIds != null && !mixNodeIds.isEmpty() ) {
                fullPath.addAll(mixNodeIds);
            }

            fullPath.add(targetDestinationId);

            Map<String, PublicKey> publicKeysFromPathNodes = new HashMap<>();

            for ( String keyForNode : fullPath ) {
                PublicKey pkForNode = NodeKeyRegistry.getPublicKey(keyForNode);
                if ( pkForNode == null ) {
                    Logger.log("Client: public key was not found for this node -> " + keyForNode + " in path i think", LogLevel.Info);
                    return;
                }
                publicKeysFromPathNodes.put(keyForNode, pkForNode);
            }

            Message onionMessage = ClientMessageBuilder.buildOnionMessage(
                    content.getBytes("UTF-8"),
                    fullPath,
                    publicKeysFromPathNodes
            );

            Logger.log( " this is the message after buildonionmessage : " + onionMessage, LogLevel.Info );

            String firstHopId = fullPath.get(0);

            Logger.log("First hop is going to be at : " + firstHopId + ", with a full path of -> " + fullPath, LogLevel.Info);

            sendMessageToNode(firstHopId, onionMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void processReceivedMessage(Message message, Peer sender) {
        Logger.log( "Received message from " + sender.getRemoteAddress() + " with msg : " + message.getContent() + " this should not be happening btw ", LogLevel.Info);
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}
