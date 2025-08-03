package Code;

import CryptoUtils.AesEncryptionUtil;
import CryptoUtils.RsaEncryptionUtil;
import Logs.LogLevel;
import Logs.Logger;
import com.google.gson.GsonBuilder;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.*;

public class ClientMessageBuilder {

    public static Message buildOnionMessage(byte[] originalMessageBytes, List<String> fullPath,
                                            Map<String, PublicKey> publicKeyMap) throws NoSuchAlgorithmException,
            NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, UnsupportedEncodingException {

        //import the list or make the same one just reversed
        List<String> encryptionPath = new ArrayList<>(fullPath);

        Collections.reverse(encryptionPath);

        //cryptography 101
        byte[] currentEncryptedPayload = originalMessageBytes;
        byte[] currentIv = null;
        byte[] currentEncryptedSymmetricKey = null;

        //iterate through it
        for (int i = 0; i < encryptionPath.size(); i++) {
            String currentNodeId = encryptionPath.get(i);
            PublicKey currentNodePublicKey = publicKeyMap.get(currentNodeId);

            if (currentNodePublicKey == null) {
                Logger.log("Public key not found for node: " + currentNodeId + ". Cannot build onion layer.", LogLevel.Error);
                throw new InvalidKeyException("Public key not found for node: " + currentNodeId);
            }

            SecretKey aesKeyForThisLayer = AesEncryptionUtil.generateAesKey();
            byte[] plainTextForAes;

            if (i == 0) {
                plainTextForAes = currentEncryptedPayload;
                Logger.log("Building innermost layer for Destination: " + currentNodeId, LogLevel.Debug);
            } else {
                String nextHopId = encryptionPath.get(i - 1);

                if (currentEncryptedPayload == null || currentIv == null || currentEncryptedSymmetricKey == null) {
                    Logger.log("Critical: Missing data from previous encryption layer (payload, IV, or symmetric key). Cannot build NextHopPayload for " + currentNodeId, LogLevel.Warn);
                    throw new IllegalStateException("Missing encrypted data from previous hop for node: " + currentNodeId);
                }

                NextHopPayload nextHopPayload = new NextHopPayload(
                        Base64.getEncoder().encodeToString(currentEncryptedPayload),
                        Base64.getEncoder().encodeToString(currentIv),
                        Base64.getEncoder().encodeToString(currentEncryptedSymmetricKey),
                        nextHopId
                );

                String jsonPayload = new GsonBuilder().disableHtmlEscaping().create().toJson(nextHopPayload);
                Logger.log("JSON plaintext for AES for " + currentNodeId + ": " + jsonPayload, LogLevel.Debug);
                plainTextForAes = jsonPayload.getBytes("UTF-8");
            }

            AesEncryptionUtil.EncryptedData encryptedDataForThisLayer =
                    AesEncryptionUtil.encrypt(plainTextForAes, aesKeyForThisLayer);

            currentEncryptedPayload = encryptedDataForThisLayer.getCiphertext();
            currentIv = encryptedDataForThisLayer.getIv();

            currentEncryptedSymmetricKey =
                    RsaEncryptionUtil.encrypt(aesKeyForThisLayer.getEncoded(), currentNodePublicKey);

            Logger.log("Successfully encrypted layer for: " + currentNodeId, LogLevel.Success);
        }

        String encodedPayload = Base64.getEncoder().encodeToString(currentEncryptedPayload);
        String encodedIv = Base64.getEncoder().encodeToString(currentIv);
        String encodedSymmetricKey = Base64.getEncoder().encodeToString(currentEncryptedSymmetricKey);

        Message finalMessage = new Message(fullPath, encodedPayload, encodedIv, encodedSymmetricKey);

        Logger.log("Client: Built onion message" + finalMessage, LogLevel.Info);
        return finalMessage;
    }

    public static class NextHopPayload {
        private String nextEncryptedPayloadBase64;
        private String nextIvBase64;
        private String nextEncryptedSymmetricKeyBase64;
        private String nextHopId;

        public NextHopPayload(String nextEncryptedPayloadBase64, String nextIvBase64,
                              String nextEncryptedSymmetricKeyBase64, String nextHopId) {
            this.nextEncryptedPayloadBase64 = nextEncryptedPayloadBase64;
            this.nextIvBase64 = nextIvBase64;
            this.nextEncryptedSymmetricKeyBase64 = nextEncryptedSymmetricKeyBase64;
            this.nextHopId = nextHopId;
        }

        public NextHopPayload() {}

        // Getters
        public byte[] getNextEncryptedPayload() { return Base64.getDecoder().decode(nextEncryptedPayloadBase64); }
        public byte[] getNextIv() { return Base64.getDecoder().decode(nextIvBase64); }
        public byte[] getNextEncryptedSymmetricKey() { return Base64.getDecoder().decode(nextEncryptedSymmetricKeyBase64); }
        public String getNextHopId() { return nextHopId; }
    }
}
