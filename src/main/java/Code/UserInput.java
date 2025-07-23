package Code;

import Logs.LogLevel;
import Logs.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class UserInput extends Thread {

    private final ClientNode clientNode;
    private volatile boolean running = true;
    private final Scanner scanner;

    public UserInput(ClientNode clientNode) {
        this.clientNode = clientNode;
        this.scanner = new Scanner(System.in);
    }

    @Override
    public void run() {
        Logger.log("Type your message, followed by '|' and the destination ID, " +
                "followed by '|' and a comma-separated list of mix node IDs (e.g., 'Hello|BobDestination|MixNode_Alpha,MixNode_Beta')", LogLevel.Info);
        Logger.log("Type 'exit' to quit.", LogLevel.Info);

        while(running) { // as long as its true the 'running' flag

            System.out.println("->");
            String inputLine = scanner.nextLine();

            if ("exit".equalsIgnoreCase(inputLine.trim())) {
                Logger.log("UserInput exiting...", LogLevel.Info);
                running = false;
                clientNode.shutdown();
                break;
            }

            //parse the input here
            String[] parts = inputLine.split("\\|", 3);

            if ( parts.length != 3 ) {
                Logger.log("Something is missing out of  the 3 parts the message needs to contain", LogLevel.Status);
                continue;
            }

            String content = parts[0].trim();
            String destinationId = parts[1].trim();
            List<String> mixNodeIds = null;

            if ( parts.length == 3 && !parts[2].trim().isEmpty()) {
                mixNodeIds = Arrays.stream(parts[2].trim().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
            }

            if (content.isEmpty() || destinationId.isEmpty()) {
                Logger.log("content or destinaion not filled" , LogLevel.Error);
                continue;
            }

            try {
                clientNode.sendUserMessage(content, destinationId, mixNodeIds);
            } catch (Exception e) {
                Logger.log("Problem sending message: " + e.getMessage(), LogLevel.Error);
            }
        }
        scanner.close();
    }

    public void shutdown() {
        running = false;
    }
}
