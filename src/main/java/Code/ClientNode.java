package Code;

import Logs.LogLevel;
import Logs.Logger;

import java.util.List;
import java.util.Map;

public class ClientNode extends AbstractNode {

    private final List<String> defaultMixPath;
    private final String defaultDestinationID;

    public ClientNode(String nodeID, int listeningPort, Map<String, NodeConfig> knownNodeConfigs, List<String> defaultMixPath, String defaultDestinationID) {
        super(nodeID, listeningPort, knownNodeConfigs);

        this.defaultMixPath = defaultMixPath;
        this.defaultDestinationID = defaultDestinationID;

        Logger.log("ClientNode is done. Path flow is : " + defaultMixPath +  "gets sent to -> " + defaultDestinationID, LogLevel.Info);
    }

    public void sendUserMessage(String content, String targetDestinationId) {

        String firstHopId;
        if (defaultMixPath != null && !defaultMixPath.isEmpty()) {
            firstHopId = defaultMixPath.get(0); // first to the mix
            Logger.log("Preparing to send message via first mix: " + firstHopId, LogLevel.Info);
        } else {
            firstHopId = targetDestinationId; // send em directly to destination if no mixes in path
            Logger.log("Preparing to send message directly to destination: " + firstHopId, LogLevel.Info);
        }

        Message messageToSend = new Message(content, targetDestinationId);

        sendMessageToNode(firstHopId, messageToSend);
    }

    @Override
    public void processReceivedMessage(Message message, Peer sender) {
        Logger.log( "Received message from " + sender.getRemoteAddress() + " with msg : " + message.getContent(), LogLevel.Info);
    }
}
