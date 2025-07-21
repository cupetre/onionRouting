package Code;

import Logs.LogLevel;
import Logs.Logger;

import java.io.IOException;
import java.util.Scanner;

public class UserInput extends Thread {

    private final ClientNode clientNode; // Reference to PeerManager for sending messages
    private volatile boolean running = true; // Flag for graceful shutdown

    public UserInput(ClientNode clientNode) {
        this.clientNode = clientNode;
        Thread.currentThread().setName("UserInputThread - " + clientNode.getNodeID());
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        Logger.log("UserInput ready. Type messages to broadcast:", LogLevel.Info);
        Logger.log("Format: <message_content>|<destination_id>", LogLevel.Info);
        Logger.log("Type 'exit' to quit.", LogLevel.Info);

        while(running) { // as long as its true the 'running' flag

            System.out.println("->");
            String input = scanner.nextLine();

            if ("exit".equalsIgnoreCase(input.trim())) { // Added simple exit command
                Logger.log("UserInput exiting...", LogLevel.Info);
                running = false;
                break;
            }

            //parse the input here
            String[] parts = input.split("\\|", 2);

            if ( parts.length != 2 ) {
                Logger.log("Something is missing", LogLevel.Status);
                continue;
            }

            String content = parts[0].trim();
            String destinationId = parts[1].trim();

            if (content.isEmpty() || destinationId.isEmpty()) {
                Logger.log("content or destinaion not filled" , LogLevel.Error);
                continue;
            }

            try {
                clientNode.sendUserMessage(content, destinationId);
            } catch (Exception e) { // Catch general exceptions during broadcast (e.g., if PeerManager isn't ready)
                Logger.log("Problem sending message: " + e.getMessage(), LogLevel.Error);
            }
        }
        scanner.close(); // Close scanner when done
    }

    public void shutdown() {
        running = false;
    }
}
