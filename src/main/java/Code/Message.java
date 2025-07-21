package Code;

import Logs.LogLevel;
import Logs.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    public final String content;
    private List<String> fullPath;
    private int currentHopIndex;

    public Message(String content, List<String> fullPath, int initialHopIndex) {
        if ( content == null || content.trim().isEmpty() ) {
            throw new IllegalArgumentException("Message can't be null");
        }

        if  ( fullPath == null || fullPath.isEmpty() ) {
            throw new IllegalArgumentException("Message can't be null or empty");
        }

        if ( initialHopIndex < 0 || initialHopIndex >= fullPath.size() ) {
            throw new IllegalArgumentException("Initial hop index out of bounds");
        }

        this.content = content;
        this.fullPath = fullPath;
        this.currentHopIndex = initialHopIndex;
    }

    public String getContent() {
        return content;
    }

    public List<String> getFullPath() {
        return fullPath;
    }

    public int getCurrentHopIndex() {
        return currentHopIndex;
    }

    public String getNextHopID() {
        if (currentHopIndex + 1 < fullPath.size() ) {
            return fullPath.get(currentHopIndex + 1);
        }
        return null;
    }

    public boolean isLastHop() {
        return currentHopIndex == fullPath.size() - 1;
    }

    public void incrementHopIndex() {
        if ( currentHopIndex < fullPath.size() -1 ) {
            currentHopIndex++;
            Logger.log("Increment to hodindex works", LogLevel.Success);
        } else {
            Logger.log("Problem with not recognising last destination check in incrementHopINdex troubelShoot", LogLevel.Error);
        }
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
