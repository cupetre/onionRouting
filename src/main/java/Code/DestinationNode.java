package Code;

import Logs.LogLevel;
import Logs.Logger;

import java.util.Map;

public class DestinationNode extends AbstractNode {

    public DestinationNode(String nodeID, int listeningPort, Map<String, NodeConfig> knownNodeConfigs) {
        super(nodeID, listeningPort, knownNodeConfigs);
        Logger.log("Destination peer is working ", LogLevel.Status);
    }

    @Override
    public void processReceivedMessage(Message message, Peer sender) {
        Logger.log("Message is received congrats testing" + sender.getRemoteAddress(), LogLevel.Status);
    }
}
