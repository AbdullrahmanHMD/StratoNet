package user;

import utils.TCPPayload;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Pattern;

import static utils.Utilities.*;

public class ClientMain {

    private static String accessToken;

    public static void main(String[] args) {
        if (!InitializeConnection())
            System.err.println("Failed to connect to server.");
        else {
            InitializeQuerying();
        }
    }

    private static boolean InitializeConnection() {

        TCPPayload serverResponse;
        byte[] clientResponse;
        String clientMessage;

        AuthenticatedConnection connectionToServer =
                new AuthenticatedConnection(DEFAULT_SERVER_ADDRESS, AUTH_PORT);

        connectionToServer.EstablishConnection();
        Scanner reader = new Scanner(System.in);

        System.out.println("Establishing network...");
        System.out.println("Enter your username:");

        clientMessage = reader.nextLine();

        clientResponse = getRequestByteArray(Auth_Phase, Auth_Request, clientMessage.length(), clientMessage);
        serverResponse = connectionToServer.sendRequest(clientResponse);

        if (serverResponse.getType() == Auth_Fail) {
            System.err.println(serverResponse.getMessage());
            connectionToServer.TerminateConnection();
            return false;
        }

        while (serverResponse.getType() == Auth_Challenge) {
            System.out.println(serverResponse.getMessage());
            clientMessage = reader.nextLine();
            clientResponse = getRequestByteArray(Auth_Phase, Auth_Request, clientMessage.length(), clientMessage);
            serverResponse = connectionToServer.sendRequest(clientResponse);
        }
        if (serverResponse.getType() == Auth_Fail) {
            System.err.println(serverResponse.getMessage());
            connectionToServer.TerminateConnection();
            return false;

        } else if (serverResponse.getType() == Auth_Success) {
            System.out.println("Authentication complete!");
            accessToken = serverResponse.getMessage();
            System.out.println("Access Token Generated | Your access token is: " + accessToken);
            connectionToServer.TerminateConnection();
            return true;
        }
        return false;
    }

    private static void InitializeQuerying() {
        TCPPayload serverResponse;
        byte[] clientResponse;
        String clientMessage;
        byte query = 0;
        AuthenticatedConnection connectionToServer =
                new AuthenticatedConnection(DEFAULT_SERVER_ADDRESS, QUERY_PORT);

        connectionToServer.EstablishConnection();
        System.out.println(connectionToServer.readFromServer().getMessage());

        Scanner reader = new Scanner(System.in);
        clientMessage = reader.nextLine();

        while (query != Query_Exit) {
            query = getQuery(clientMessage);

            while (query == 0) {
                System.err.println("Invalid query, try again");
                clientMessage = reader.nextLine();
                query = getQuery(clientMessage);
            }
            if(query == Query_Image){
                clientResponse = getRequestByteArray(Query_Phase, query, clientMessage.length(), clientMessage);
                serverResponse = connectionToServer.sendRequest(clientResponse);

                createImage(serverResponse.getMessage().getBytes());
                System.out.println("Image received");
            }
            else if(query == Query_Weather){
                clientResponse = getRequestByteArray(Query_Phase, query, clientMessage.length(), clientMessage);
                serverResponse = connectionToServer.sendRequest(clientResponse);

                System.out.println(serverResponse.getMessage() + "\n");
            }

            System.out.println("Enter a request:");
            clientMessage = reader.nextLine();

        }
        clientMessage = "Disconnect";
        clientResponse = getRequestByteArray(Query_Phase, Query_Exit, clientMessage.length(), clientMessage);
        serverResponse = connectionToServer.sendRequest(clientResponse);
        System.out.println(serverResponse.getMessage());
        connectionToServer.TerminateConnection();
    }


    private static byte getQuery(String message) {
        if (Pattern.compile(dateRegex).matcher(message).matches())
            return Query_Image;
        else if (message.toLowerCase().equals("weather"))
            return Query_Weather;
        else if (message.toLowerCase().equals("disconnect"))
            return Query_Exit;
        return 0;
    }

    private static void createImage(byte[] byteArray) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArray);
            BufferedImage byteImage = ImageIO.read(inputStream);

            ImageIO.write(byteImage, "jpg", new File("image_of_the_day.jpg"));

        } catch (NullPointerException | IOException e) {
            e.printStackTrace();
        }
    }

}
