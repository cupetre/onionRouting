package Code;

import java.io.IOException;
import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    public final String content;

    public Message(String rawMessage) throws IOException {
        //we check in peer waitformessage if the content is okey
        //we mainly have this for transparency and strcuture of msg as an object
        if ( rawMessage == null ) {
            throw new IOException("Message cannot be null.");
        }
        this.content = rawMessage;
    }

    @Override
    public String toString() {
        return content;
    }
}
