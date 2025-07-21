package Code;

import java.io.IOException;
import java.io.Serializable;

public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    public final String content;
    public final String nextHopID ; // the dest for the msg (reciever)

    public Message(String content, String nextHopID) {
        if ( content == null || content.trim().isEmpty() ) {
            throw new IllegalArgumentException("Message can't be null");
        }
        if (nextHopID == null || nextHopID.trim().isEmpty() ) {
            throw new IllegalArgumentException("ID can't be null");
        }

        this.content = content;
        this.nextHopID = nextHopID;
    }

    @Override
    public String toString() {
        return "Content : -> " + content + " , Hop : -> " + nextHopID;
    }

    public String getContent() {
        return content;
    }

    public String getNextHopID() {
        return nextHopID;
    }
}
