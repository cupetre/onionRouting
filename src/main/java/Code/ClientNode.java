package Code;

import Logs.LogLevel;
import Logs.Logger;

import java.util.ArrayList;
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

            if ( fullPath.isEmpty() ) {
                Logger.log ( "the whole msg is empty, can't continue with message sending " , LogLevel.Warn);
                return;
            }

            Message message = new Message(content, fullPath, 0);

            String firstHopId = fullPath.get(0);

            Logger.log("First hop is going to be at : " + firstHopId + ", with a full path of -> " + fullPath, LogLevel.Info);

            sendMessageToNode(firstHopId, message);
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
