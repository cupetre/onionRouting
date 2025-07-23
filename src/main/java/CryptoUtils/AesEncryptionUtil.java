package CryptoUtils;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class AesEncryptionUtil {
    private static final String ALGORITHM = "AES";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";

    private static final int KEY_SIZE_BITS = 256;
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BYTES = 128;


    public static SecretKey generateAesKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(KEY_SIZE_BITS, new SecureRandom());
        return keyGenerator.generateKey();
    }

    public static EncryptedData encrypt(byte[] plainText, SecretKey secretKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        GCMParameterSpec gcmParSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BYTES, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParSpec);

        byte[] cipherTextWithTag = cipher.doFinal(plainText);
        return new EncryptedData(cipherTextWithTag, iv);
    }

    public static byte[] decrypt(EncryptedData encryptedData, SecretKey secretKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        GCMParameterSpec gcmParaSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BYTES, encryptedData.getIv());
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParaSpec);
        return cipher.doFinal(encryptedData.getCiphertext());
    }

    public static class EncryptedData {
        private final byte[] ciphertext;
        private final byte[] iv;

        public EncryptedData(byte[] ciphertext, byte[] iv) {
            this.ciphertext = ciphertext;
            this.iv = iv;
        }

        public byte[] getCiphertext() {
            return ciphertext;
        }

        public byte[] getIv() {
            return iv;
        }
    }
}
