package NodesConstruct;

import Code.Message;
import Code.Peer;
import Code.PeerManager;
import CryptoUtils.NodeKeyRegistry;
import Logs.LogLevel;
import Logs.Logger;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;

public abstract class AbstractNode extends PeerManager {

    protected final String nodeID;

    protected final Map<String, NodeConfig> knownNodeConfigs;

    protected PrivateKey privateKey;
    protected PublicKey publicKey;

    public AbstractNode(String nodeID, int listeningPort, Map<String, NodeConfig> knownNodeConfigs) {
        super(listeningPort, nodeID);
        this.nodeID = nodeID;
        this.knownNodeConfigs = knownNodeConfigs;
        try {
            this.privateKey = NodeKeyRegistry.getPrivateKey(nodeID);
            this.publicKey = NodeKeyRegistry.getPublicKey(nodeID);

            if ( this.privateKey == null || this.publicKey == null ) {
                Logger.log("key pair is not found or smth is not working in taking the keys from nodekeyreg for the node : " + nodeID, LogLevel.Status);
            }

            Logger.log("Key pair is successfully generated and the node acquired them" , LogLevel.Info);
        } catch (Exception e) {
            Logger.log( " Error in loading or using the keys for node : " + nodeID, LogLevel.Error);
        }

        Logger.log("Node is created as : " + this.getClass().getSimpleName(), LogLevel.Info);
    }

    protected PrivateKey getMyPrivateKey() {
        return privateKey;
    }

    public PublicKey getMyPublicKey() {
        return publicKey;
    }

    @Override
    public abstract void processReceivedMessage(Message message, Peer sender);

    public String getNodeID() {
        return nodeID;
    }

    protected void sendMessageToNode(String targetNodeId, Message message) {
        NodeConfig targetConfig = knownNodeConfigs.get(targetNodeId);

        if ( targetConfig == null ) {
            Logger.log("The node : " + targetNodeId + " can't find the id of the targetted node -> " + targetNodeId, LogLevel.Error);
            return;
        }

        try {
            Peer targetPeer = super.connectToPeer(targetConfig.getHost(), targetConfig.getPort(), targetNodeId);

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
