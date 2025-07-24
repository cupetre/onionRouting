package NodesConstruct;

import Code.Message;
import Code.Peer;
import CryptoUtils.AesEncryptionUtil;
import CryptoUtils.NodeKeyRegistry;
import CryptoUtils.RsaEncryptionUtil;
import Logs.LogLevel;
import Logs.Logger;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.PrivateKey;
import java.util.Map;

public class DestinationNode extends AbstractNode {

    public DestinationNode(String nodeID, int listeningPort, Map<String, NodeConfig> knownNodeConfigs) {
        super(nodeID, listeningPort, knownNodeConfigs);
        Logger.log("Destination peer is working ", LogLevel.Status);
    }

    @Override
    public void processReceivedMessage(Message message, Peer sender) {

        Logger.log("DestinationNode " + this.nodeID + ": Message received from " + sender.getRemoteAddress(), LogLevel.Info);
        Logger.log("DestinationNode " + this.nodeID + ": Current hop index: " + message.getCurrentHopIndex() + ", Full path: " + message.getFullPath(), LogLevel.Info);


        if ( ( message.getCurrentHopIndex() + 1 )!= message.getFullPath().size() ) {
            Logger.log( "dest node did not get the message due to wrong hop index, the index being : " + message.getCurrentHopIndex(), LogLevel.Error);
            return;
        }

        String intendedRecipientId = message.getFullPath().get(message.getCurrentHopIndex());
        if ( !this.nodeID.equals(intendedRecipientId) ) {
            Logger.log( " missmatch in node that needs to get the message", LogLevel.Error);
            Logger.log (" its expected to go to " + intendedRecipientId + " but it got to : " + this.nodeID, LogLevel.Info);
            return;
        }

        //decrypting again , starts from here
        PrivateKey myPrivateKey = NodeKeyRegistry.getPrivateKey(this.nodeID);
        if (myPrivateKey == null) {
            Logger.log("DestinationNode " + this.nodeID + ": Private key not found. Cannot decrypt final message.", LogLevel.Error);
            return;
        }

        byte[] encryptedAesKeyBytes = message.getEncryptedSymmetricKeyForThisHop();
        if (encryptedAesKeyBytes == null) {
            Logger.log("DestinationNode " + this.nodeID + ": Encrypted symmetric key is null. Malformed final message.", LogLevel.Error);
            return;
        }

        SecretKey aesKeyForThisLayer;
        try {
            // Decrypt the AES key for this layer using the DestinationNode's RSA private key
            byte[] decryptedAesKeyBytes = RsaEncryptionUtil.decrypt(encryptedAesKeyBytes, myPrivateKey);
            aesKeyForThisLayer = new SecretKeySpec(decryptedAesKeyBytes, 0, decryptedAesKeyBytes.length, "AES");
            Logger.log("DestinationNode " + this.nodeID + ": AES key decrypted successfully.", LogLevel.Debug);
        } catch (Exception e) {
            Logger.log("DestinationNode " + this.nodeID + ": Failed to decrypt AES key with RSA for final layer. Error: " + e.getMessage(), LogLevel.Error);
            return;
        }

        byte[] encryptedPayloadBytes = message.getEncryptedPayload();
        byte[] ivBytes = message.getIv();
        if (encryptedPayloadBytes == null || ivBytes == null) {
            Logger.log("DestinationNode " + this.nodeID + ": Encrypted payload or IV is null. Malformed final message.", LogLevel.Error);
            return;
        }

        byte[] decryptedOriginalMessageBytes;
        try {
            // Decrypt the payload with the AES key - this should be the original message bytes
            AesEncryptionUtil.EncryptedData encryptedData = new AesEncryptionUtil.EncryptedData(encryptedPayloadBytes, ivBytes);
            decryptedOriginalMessageBytes = AesEncryptionUtil.decrypt(encryptedData, aesKeyForThisLayer);
            Logger.log("DestinationNode " + this.nodeID + ": Final AES payload decrypted successfully. Bytes length: " + decryptedOriginalMessageBytes.length, LogLevel.Debug);
        } catch (Exception e) {
            Logger.log("DestinationNode " + this.nodeID + ": Failed to decrypt final AES payload. Error: " + e.getMessage(), LogLevel.Error);
            return;
        }

        // --- 3. Extract and Print the Original Message ---
        try {
            String originalMessage = new String(decryptedOriginalMessageBytes, "UTF-8"); // Convert bytes to String

            // Set the 'content' field in the Message object (NOW it's meaningful)
            message.setContent(originalMessage);

            Logger.log("-------------------------------------------------------", LogLevel.Success);
            Logger.log("DESTINATION NODE (" + nodeID + ") RECEIVED AND DECRYPTED ORIGINAL MESSAGE:", LogLevel.Success);
            Logger.log("-------------------------------------------------------", LogLevel.Success);
            Logger.log("'" + originalMessage + "'", LogLevel.Success);
            Logger.log("-------------------------------------------------------", LogLevel.Info);

            // You can now process the original message, e.g., display it to a user interface, etc.

        } catch (UnsupportedEncodingException e) {
            Logger.log("DestinationNode " + nodeID + ": Failed to convert decrypted bytes to UTF-8 string. Error: " + e.getMessage(), LogLevel.Error);
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}
