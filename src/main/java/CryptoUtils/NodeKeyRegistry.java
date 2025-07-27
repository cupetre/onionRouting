package CryptoUtils;

import NodesConstruct.NodeConfig;
import Logs.LogLevel;
import Logs.Logger;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NodeKeyRegistry {

    private static final Map<String, PublicKey> publicKeys = new ConcurrentHashMap<>();

    private static final Map<String, PrivateKey> privateKeys = new ConcurrentHashMap<>();

    private static final Map<String, KeyPair> allNodeKeyPairs = new ConcurrentHashMap<>();

    public static void generateAndRegisterKeys(Map<String, NodeConfig> nodeIds) throws NoSuchAlgorithmException {

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
