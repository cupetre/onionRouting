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
        if ( message.getCurrentHopIndex() != message.getFullPath().size() ) {
            Logger.log( "dest node did not get the message due to wrong hop index, the index being : " + message.getCurrentHopIndex(), LogLevel.Error);
            return;
        }

        String intendedRecipientId = message.getFullPath().get(message.getCurrentHopIndex());

        if ( !this.nodeID.equals(intendedRecipientId) ) {
            Logger.log( " missmatch in node that needs to get the message", LogLevel.Error);
            Logger.log (" its expected to go to " + intendedRecipientId + " but it got to : " + this.nodeID, LogLevel.Info);
            return;
        }

        Logger.log("Message is received congrats testing" + sender.getRemoteAddress(), LogLevel.Status);
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}
