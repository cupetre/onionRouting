package NodesConstruct;

import Code.ClientMessageBuilder;
import Code.Message;
import Code.Peer;
import CryptoUtils.AesEncryptionUtil;
import CryptoUtils.NodeKeyRegistry;
import CryptoUtils.RsaEncryptionUtil;
import Logs.LogLevel;
import Logs.Logger;
import com.google.gson.GsonBuilder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.PrivateKey;
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

        Logger.log("is the recipient okey ? ->> " + intendedRecipientID, LogLevel.Info);

        if ( !this.nodeID.equals(intendedRecipientID) ) {
            Logger.log( " the wrong peer is getting this message and not : " + intendedRecipientID + ", instead its on this : " + this.nodeID, LogLevel.Info);
            return;
        }

        // start decrypting here

        PrivateKey privateKey = NodeKeyRegistry.getPrivateKey(intendedRecipientID);

        if ( privateKey == null ) {
            Logger.log("Private key is null and has a prob ", LogLevel.Error);
            return;
        }

        byte[] encryptedAesKeyBytes = message.getEncryptedSymmetricKeyForThisHop();
        if (encryptedAesKeyBytes == null ) {
            Logger.log("the symmetric key for encrypted is malfunctioning, the node is : " + this.nodeID, LogLevel.Info);
            return;
        }

        Logger.log("aj da vidime encryptedAesKeyBytes i privKey dali se okej -> " + privateKey + encryptedAesKeyBytes , LogLevel.Info);


        SecretKey aesKeyForThisLayer;

        try {
            byte[] decryptedAesKeyBytes = RsaEncryptionUtil.decrypt(encryptedAesKeyBytes, privateKey);

            Logger.log("or it didnt get the decrypted bytes message" + decryptedAesKeyBytes , LogLevel.Info);

            aesKeyForThisLayer = new SecretKeySpec(decryptedAesKeyBytes, 0, decryptedAesKeyBytes.length, "AES");

            Logger.log("or its the secretkeyspec something " + aesKeyForThisLayer  , LogLevel.Info);

            Logger.log("the mixnode : " + this.nodeID + "has decrypted the rsa good", LogLevel.Info);
        } catch (Exception e) {
            Logger.log("Problem with decryption of mixnode: " + this.nodeID, LogLevel.Error);
        }

        byte[] encryptedPayloadBytes = message.getEncryptedPayload();
        byte[] encryptedIvBytes = message.getIv();

        if ( encryptedPayloadBytes == null || encryptedIvBytes == null ) {
            Logger.log("the payload or iv is null and has a prob ", LogLevel.Info);
            return;
        }

        byte[] decryptedPayloadBytes;


        try {
            AesEncryptionUtil.EncryptedData encryptedData = new AesEncryptionUtil.EncryptedData(encryptedPayloadBytes, encryptedIvBytes);
            decryptedPayloadBytes = AesEncryptionUtil.decrypt(encryptedData, aesKeyForThisLayer);
            Logger.log("The mixnode : " + this.nodeID + "has decrypted the aes good", LogLevel.Info);
        } catch (Exception e) {
            Logger.log("the mixnode : " + this.nodeID + "did not decrypt the aes good " , LogLevel.Info);
            return;
        }

        // the gson stuff , ne gi ni razb pola
        ClientMessageBuilder.NextHopPayload nextHopPayload;

        try {
            String decryptedJsonString = new String(decryptedPayloadBytes, "UTF-8");
            nextHopPayload = new GsonBuilder().create().fromJson(decryptedJsonString, ClientMessageBuilder.NextHopPayload.class);

            if (nextHopPayload == null) {
                Logger.log("MixNode " + this.nodeID + ": Deserialized NextHopPayload is null. Malformed JSON?", LogLevel.Error);
                return;
            }
            // Basic validation of nextHopPayload's content before using
            if (nextHopPayload.getNextEncryptedPayload() == null ||
                    nextHopPayload.getNextIv() == null ||
                    nextHopPayload.getNextEncryptedSymmetricKey() == null ||
                    nextHopPayload.getNextHopId() == null) {
                Logger.log("MixNode " + this.nodeID + ": NextHopPayload contains null fields. Malformed message structure.", LogLevel.Error);
                return;
            }
            Logger.log("MixNode " + this.nodeID + ": NextHopPayload parsed. Next hop ID: " + nextHopPayload.getNextHopId(), LogLevel.Debug);

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        Logger.log("msg is recieved where is should be recieved, from : " + sender.getRemoteNodeId(), LogLevel.Info);
        Logger.log("the message is : " + message.getContent(), LogLevel.Info);
        Logger.log( "Current hop rn is : " + message.getCurrentHopIndex() + "with path of " + message.getFullPath(), LogLevel.Info);

        message.setEncryptedPayload(nextHopPayload.getNextEncryptedPayload()); // These setters expect Base64 String
        message.setIv(nextHopPayload.getNextIv());                               // from NextHopPayload getters
        message.setEncryptedSymmetricKeyForThisHop(nextHopPayload.getNextEncryptedSymmetricKey());

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