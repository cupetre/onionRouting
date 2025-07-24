package Code;

import Logs.LogLevel;
import Logs.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    public String content;
    private List<String> fullPath;
    private int currentHopIndex;

    private String encryptedPayloadBase64;
    private String ivBase64;

    private String encryptedSymmetricKeyForThisHopBase64;

    public Message(List<String> fullPath, String encryptedPayloadBase64, String ivBase64, String encryptedSymmetricKeyForThisHopBase64) {

        this.content = null;
        this.fullPath = fullPath;
        this.currentHopIndex = 0;

        this.encryptedPayloadBase64 = encryptedPayloadBase64;
        this.ivBase64 = ivBase64;
        this.encryptedSymmetricKeyForThisHopBase64 = encryptedSymmetricKeyForThisHopBase64;
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; } // Used by client to set original, and dest to get final

    public List<String> getFullPath() { return fullPath; }
    public void setFullPath(List<String> fullPath) { this.fullPath = fullPath; }

    public int getCurrentHopIndex() { return currentHopIndex; }
    public void setCurrentHopIndex(int currentHopIndex) { this.currentHopIndex = currentHopIndex; }

    public String getNextHopID() {
        if (currentHopIndex + 1 < fullPath.size() ) {
            return fullPath.get(currentHopIndex + 1);
        }
        return null;
    }

    public void incrementHopIndex() {
        if ( currentHopIndex < fullPath.size() -1 ) {
            currentHopIndex++;
            Logger.log("Increment to hodindex works", LogLevel.Success);
        } else {
            Logger.log("Problem with not recognising last destination check in incrementHopINdex troubelShoot", LogLevel.Error);
        }
    }

    public byte[] getEncryptedPayload() {
        return Base64.getDecoder().decode(encryptedPayloadBase64);
    }
    public void setEncryptedPayload(byte[] encryptedPayload) {
        this.encryptedPayloadBase64 = Base64.getEncoder().encodeToString(encryptedPayload);
    }

    public byte[] getIv() {
        return Base64.getDecoder().decode(ivBase64);
    }
    public void setIv(byte[] iv) {
        this.ivBase64 = Base64.getEncoder().encodeToString(iv);
    }

    public byte[] getEncryptedSymmetricKeyForThisHop() {
        return Base64.getDecoder().decode(encryptedSymmetricKeyForThisHopBase64);
    }
    public void setEncryptedSymmetricKeyForThisHop(byte[] encryptedSymmetricKeyForThisHop) {
        this.encryptedSymmetricKeyForThisHopBase64 = Base64.getEncoder().encodeToString(encryptedSymmetricKeyForThisHop);
    }

    @Override
    public String toString() {
        return "Message{" +
                "content='" + content + '\'' +
                ", fullPath=" + fullPath +
                ", currentHopIndex=" + currentHopIndex +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;

        Message message = (Message) o;

        return currentHopIndex == message.currentHopIndex &&
                Objects.equals(content, message.content) &&
                Objects.equals(fullPath, message.fullPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, fullPath, currentHopIndex);
    }
}
