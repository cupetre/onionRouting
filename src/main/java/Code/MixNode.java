package Code;

import Logs.LogLevel;
import Logs.Logger;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class MixNode extends AbstractNode {

    private BlockingQueue<Message> messageBuffer;

    private ScheduledExecutorService scheduler;
    private final long DISPATCH_INTERVAL_SECONDS = 5;

    public MixNode(String nodeID, int listeningPort, Map<String, NodeConfig> knownNodeConfigs) {
        super(nodeID, listeningPort, knownNodeConfigs);

        this.messageBuffer = new LinkedBlockingQueue<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        startDispatchScheduler();

        Logger.log("Mixnode started. Dispatch counter: " + DISPATCH_INTERVAL_SECONDS + " seconds ", LogLevel.Info);
    }

    @Override
    public void processReceivedMessage(Message message, Peer sender) {
        //collects the messages and stacks in buffer
        Logger.log("Message received from " + sender.getRemoteAddress() + " : " + message.getContent(), LogLevel.Info);

        if (message.getCurrentHopIndex() >= message.getFullPath().size() ) {
            Logger.log("invalid hop index, its " + message.getCurrentHopIndex(), LogLevel.Info);
            return;
        }

        String intendedRecipientID = message.getFullPath().get(message.getCurrentHopIndex());

        if ( !this.nodeID.equals(intendedRecipientID) ) {
            Logger.log( " the wrong peer is getting this message and not : " + intendedRecipientID + ", instead its on this : " + this.nodeID, LogLevel.Info);
            return;
        }

        Logger.log("msg is recieved where is should be recieved, from : " + sender.getRemoteNodeId(), LogLevel.Info);
        Logger.log("the message is : " + message.getContent(), LogLevel.Info);
        Logger.log( "Current hop rn is : " + message.getCurrentHopIndex() + "with path of " + message.getFullPath(), LogLevel.Info);

        message.incrementHopIndex();

        try {
            messageBuffer.put(message);
        } catch (InterruptedException e) {
            Logger.log("problem in adding message to buffer", LogLevel.Info);
            Thread.currentThread().interrupt();
        }
    }

    private void startDispatchScheduler() {
        scheduler.scheduleAtFixedRate(this::dispatchMessages, 0, DISPATCH_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void dispatchMessages() {
        if (messageBuffer.isEmpty()) {
            return;
        }

        List<Message> batch = new ArrayList<>();

        //start process to collect and buffer the messages
        messageBuffer.drainTo(batch);
        //shuffle them
        Collections.shuffle(batch);

        for (Message message : batch) {
            String nextHopId = message.getFullPath().get(message.getCurrentHopIndex());

            Logger.log("we are sending the message to : " + nextHopId, LogLevel.Info);

            sendMessageToNode(nextHopId, message);
        }

    }

    @Override
    public void shutdown() {
        super.shutdown(); //proveri zs ne raboti vo peermanage shutdown

        if (scheduler != null) {
            scheduler.shutdownNow();

            try {
                if (!scheduler.awaitTermination(DISPATCH_INTERVAL_SECONDS + 1, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                Logger.log("MixNode terminator interrupted ", LogLevel.Error);
                Thread.currentThread().interrupt();
            }
        }
    }

}