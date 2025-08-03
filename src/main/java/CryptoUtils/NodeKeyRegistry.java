package CryptoUtils;

import NodesConstruct.NodeConfig;
import Logs.LogLevel;
import Logs.Logger;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NodeKeyRegistry {

    private static final Map<String, PublicKey> publicKeys = new ConcurrentHashMap<>();

    private static final Map<String, PrivateKey> privateKeys = new ConcurrentHashMap<>();

    private static final Map<String, KeyPair> allNodeKeyPairs = new ConcurrentHashMap<>();

    public static void generateAndRegisterKeys(Map<String, NodeConfig> nodeIds) throws NoSuchAlgorithmException {

        //clear these becuase of the triple generation of keys

        publicKeys.clear();
        privateKeys.clear();
        allNodeKeyPairs.clear();

        Logger.log("Generating RSA key pairs", LogLevel.Status);

        for ( NodeConfig nodeConf : nodeIds.values() ) {
            //we extract the node id from the element in the map of the nodeconfig first value
            String nodeId = nodeConf.getId();

            KeyPair keyPair = RsaEncryptionUtil.generateKeyPair();
            publicKeys.put(nodeId, keyPair.getPublic());
            privateKeys.put(nodeId, keyPair.getPrivate());

            allNodeKeyPairs.put(nodeId, keyPair);
            Logger.log("Successfully generated key pair for : " + nodeId, LogLevel.Success);

            
        }
        Logger.log("Finished generating RSA key pairs for the nodes and assigned", LogLevel.Status);
    }

    // lets try with file creation
    public static void saveKeysToFile(String nodeIdToSavePrivKey, String keysDirPath) throws IOException {
        Path dirPath = Paths.get(keysDirPath);

        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        //now we load the public keys to a file
        Map<String, String> pubKeysBase64 = new HashMap<>();

        for (Map.Entry<String, PublicKey> entry : publicKeys.entrySet()) {
            pubKeysBase64.put(entry.getKey(), Base64.getEncoder().encodeToString(entry.getValue().getEncoded()));
        }

        String publicKeysJson = new GsonBuilder().setPrettyPrinting().create().toJson(pubKeysBase64);
        Files.writeString(dirPath.resolve("public_keys.json"), publicKeysJson);
        Logger.log("The public keys are saved to the jar ", LogLevel.Status);

        // now we create special ones for the nodes themselves
        PrivateKey privateKey = privateKeys.get(nodeIdToSavePrivKey);
        if(privateKey != null) {
            Files.write(dirPath.resolve(nodeIdToSavePrivKey + "_private.key"), privateKey.getEncoded());
            Logger.log("Saved private key for " + nodeIdToSavePrivKey + " to " + nodeIdToSavePrivKey + "_private.key", LogLevel.Info);
        } else {
            Logger.log("Warning: Private key for " + nodeIdToSavePrivKey + " not found for saving.", LogLevel.Warn);
        }
    }

    //keys hopefully generated , file created, now load
    public static void loadKeysFromFile(String nodeId, String keysDirPath) throws IOException, NoSuchAlgorithmException, java.security.spec.InvalidKeySpecException {
        // Clear current keys first
        publicKeys.clear();
        privateKeys.clear();

        Path dirPath = Paths.get(keysDirPath);

        // Load all public keys
        Path pubKeysPath = dirPath.resolve("public_keys.json");
        if (Files.exists(pubKeysPath)) {
            String publicKeysJson = Files.readString(pubKeysPath);
            Map<String, String> pubKeysBase64 = new GsonBuilder().create().fromJson(publicKeysJson, Map.class);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            for (Map.Entry<String, String> entry : pubKeysBase64.entrySet()) {
                X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(entry.getValue()));
                publicKeys.put(entry.getKey(), keyFactory.generatePublic(pubKeySpec));
            }
            Logger.log("Loaded all public keys from public_keys.json", LogLevel.Info);
        } else {
            Logger.log( "Error: public_keys.json not found at " + pubKeysPath + ". Cannot load public keys.", LogLevel.Error);
            throw new IOException("Public keys file not found.");
        }

        // now we load THIS(THE ONE thats usin it rn for enncryptin) node's private key
        Path privateKeyPath = dirPath.resolve(nodeId + "_private.key");
        if (Files.exists(privateKeyPath)) {
            byte[] privateKeyBytes = Files.readAllBytes(privateKeyPath);
            PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            privateKeys.put(nodeId, keyFactory.generatePrivate(privKeySpec));
            Logger.log("Loaded private key for " + nodeId + " from " + nodeId + "_private.key", LogLevel.Info);
        } else {
            Logger.log("Error: Private key file for " + nodeId + " not found at " + privateKeyPath + ". Cannot load private key.", LogLevel.Error);
            throw new IOException("Private key file for " + nodeId + " not found.");
        }
        Logger.log("Keys loaded successfully for node: " + nodeId, LogLevel.Status);
    }

    public static PublicKey getPublicKey(String nodeId) {
        return publicKeys.get(nodeId);
    }

    public static PrivateKey getPrivateKey(String nodeId) {
        return privateKeys.get(nodeId);
    }

    public static KeyPair getKeyPair(String nodeId) {
        return allNodeKeyPairs.get(nodeId);
    }
}
