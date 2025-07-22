package Code;

import Logs.LogLevel;
import Logs.Logger;

import java.util.Map;

public abstract class AbstractNode extends PeerManager {

    protected final String nodeID;

    protected final Map<String, NodeConfig> knownNodeConfigs;

    public AbstractNode(String nodeID, int listeningPort, Map<String, NodeConfig> knownNodeConfigs) {
        super(listeningPort, nodeID);
        this.nodeID = nodeID;
        this.knownNodeConfigs = knownNodeConfigs;
        Logger.log("Node is created as : " + this.getClass().getSimpleName(), LogLevel.Info);
    }

    @Override
    public abstract void processReceivedMessage(Message message, Peer sender);

    public String getNodeID() {
        return nodeID;
    }

    protected void sendMessageToNode(String targetNodeId, Message message) {
        NodeConfig targetConfig = knownNodeConfigs.get(targetNodeId);

        Logger.log("this is the target config " + targetConfig, LogLevel.Info);

        if ( targetConfig == null ) {
            Logger.log("The node : " + targetNodeId + " can't find the id of the targetted node -> " + targetNodeId, LogLevel.Error);
            return;
        }

        try {
            Peer targetPeer = super.connectToPeer(targetConfig.getHost(), targetConfig.getPort(), targetNodeId);

            Logger.log("this is the target peer : " + targetPeer, LogLevel.Info);

            if ( targetPeer != null ) {
                targetPeer.sendMessage(message);
                Logger.log("Send message to : " + targetNodeId + ", following " + message.getContent(), LogLevel.Info);
            } else {
                Logger.log("failed connection to " + targetNodeId, LogLevel.Error);
            }
        } catch (Exception e) {
            Logger.log("Error sending the message to the wanted node", LogLevel.Error);
        }
    }
}
