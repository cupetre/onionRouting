package Code;

import Logs.LogLevel;
import Logs.Logger;

import java.io.IOException;
import java.util.Scanner;

public class UserInput extends Thread {
    // private String peerID; // Removed, as its purpose is unclear here. Node identity belongs elsewhere.
    private final PeerManager peerManager; // Reference to PeerManager for sending messages
    private volatile boolean running = true; // Flag for graceful shutdown

    public UserInput(PeerManager peerManager) {
        this.peerManager = peerManager;
        Thread.currentThread().setName("UserInputThread");
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        Logger.log("UserInput ready. Type messages to broadcast:", LogLevel.Info);

        while(running) { // Loop controlled by 'running' flag
            String input = scanner.nextLine();
            if ("exit".equalsIgnoreCase(input.trim())) { // Added simple exit command
                Logger.log("UserInput exiting...", LogLevel.Info);
                running = false;
                break;
            }

            try {
                // Now UserInput asks PeerManager to broadcast, PeerManager manages the PeerList
                peerManager.broadcastMessage(new Message(input));
            } catch (IOException e) { // Catch from Message constructor, though less likely now
                Logger.log("Error creating message: " + e.getMessage(), LogLevel.Error);
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
