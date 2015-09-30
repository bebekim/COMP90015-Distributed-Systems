/******************************************************************************
 * A thread created by the Client class at runtime, that reads in input from
 * the clients keyboard, marshalls the data into JSON format and send it to
 * the Server over a TCP connection.
 *
 * @author Rem, 2015
 ******************************************************************************/

package Client;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class SendThread implements Runnable {

    Socket socket;

    SendThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {

        try {
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

            Scanner userInput = new Scanner(System.in);
            String message;

            // Send new identity message for the first time
            String newIdReq = new ClientMessage().newIdentityRequest("");
            outputStream.writeUTF(newIdReq);
            outputStream.flush();

            // Send roomchange request for the first time, to join MainHall
            String roomChange = new ClientMessage().joinRoomRequest("MainHall");
            outputStream.writeUTF(roomChange);
            outputStream.flush();

            // Display the contents of the entire chat
            String listMsg = new ClientMessage().listRequest();
            outputStream.writeUTF(listMsg);
            outputStream.flush();

            // Display contents for MainHall for first-time joiners
            String whoMsg = new ClientMessage().whoRequest("MainHall");
            outputStream.writeUTF(whoMsg);
            outputStream.flush();

            // After identity and room set-up, listen for new messages from clients keyboard.
            while (true) {

                message = userInput.nextLine();

                //  CHECK FOR SPECIAL COMMANDS, OTHERWISE SEND REGULAR MESSAGE
                if (message.charAt(0) == '#') {
                    String[] messageTokens = message.split(" ");
                    String command = messageTokens[0];

                    switch (command) {

                        case "#quit":
                            String quitReq = new ClientMessage().quitRequest();
                            outputStream.writeUTF(quitReq);
                            outputStream.flush();
                            break;

                        case "#identitychange":
                            try {
                                String newIdentity = messageTokens[1];
                                if (newIdentity != null) {
                                    if (isValidName(newIdentity)) {
                                        String newIdMsg = new ClientMessage().newIdentityRequest(newIdentity);
                                        outputStream.writeUTF(newIdMsg);
                                        outputStream.flush();
                                    } else {
                                        System.out.println("Names must be alphanumeric and must not start with a number.");
                                    }
                                }
                            } catch (Exception e) {
                                System.out.println("No name include. Please try again.");
                            }
                            break;

                        case "#join":
                            try {
                                String roomId = messageTokens[1];
                                if (roomId != null && isValidRoomName(roomId)) {
                                    String joinRoomMsg = new ClientMessage().joinRoomRequest(roomId);
                                    outputStream.writeUTF(joinRoomMsg);
                                    outputStream.flush();
                                } else {
                                    System.out.println("Invalid room name.");
                                }
                            } catch (Exception e) {
                                System.out.println("Please provide the name of the room you would like to join.");
                            }
                            break;

                        case "#who":
                            try {
                                String room = messageTokens[1];
                                if (room != null) {
                                    String whoReqMsg = new ClientMessage().whoRequest(room);
                                    outputStream.writeUTF(whoReqMsg);
                                    outputStream.flush();
                                }
                            } catch (Exception e) {
                                System.out.println("Please provide the name of the room you wish to inspect");
                            }
                            break;

                        case "#list":
                            String listReq = new ClientMessage().listRequest();
                            outputStream.writeUTF(listReq);
                            outputStream.flush();
                            break;

                        case "#createroom":
                            try {
                                String newRoom = messageTokens[1];
                                if ((newRoom != null)
                                        && isValidRoomName(newRoom)) {
                                    String createRequest = new ClientMessage().createRomRequest(newRoom);
                                    outputStream.writeUTF(createRequest);
                                } else {
                                    System.out.println("Rooms must be alphanumeric with at least 3 " +
                                            "characters and no more than 16 characters");
                                }
                            } catch (Exception e) {
                                System.out.println("Please provide a room name");
                            }
                            break;

                        case "#kick":
                            // add check for rest of stuff
                            try {
                                String roomFrom = messageTokens[1];
                                String time = messageTokens[2];
                                String userToKick = messageTokens[3];
                                String kickMsg = new ClientMessage().kickRequest(roomFrom, time, userToKick);
                                outputStream.writeUTF(kickMsg);
                                outputStream.flush();
                            } catch (Exception e) {
                                System.out.println("Please provide the room, followed by time followed by the user you wish to kick");
                            }
                            break;

                        case "#delete":
                            try {
                                String deleteReq = new ClientMessage().deleteRequest(messageTokens[1]);
                                outputStream.writeUTF(deleteReq);
                                outputStream.flush();
                            } catch (Exception e) {
                                System.out.println("Please provide the name of the room you wish to delete");
                            }
                            break;

                        default:
                            System.out.println("You may have typed a command incorrectly");
                            break;
                    }
                }
                //  SENDS REGULAR CHAT MESSAGE
                else {
                    String jsonChatMessage = new ClientMessage().chatMessage(message);
                    outputStream.writeUTF(jsonChatMessage);
                    outputStream.flush();
                }
            }
        } catch (IOException e) {
            System.out.println("IO error in client sending thread");
            e.printStackTrace();
        }
    }

    private boolean isValidName(String input) {
        if ((input.length() >= 3 && input.length() <= 16)
                && (input.matches("[A-Za-z0-9]+"))
                && (!Character.isDigit(input.charAt(0)))
                ) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isValidRoomName(String input) {
        if ((input.length() >= 3 && input.length() <= 32)
                && (input.matches("[A-Za-z0-9]+"))
                && (!Character.isDigit(input.charAt(0)))
                ) {
            return true;
        } else {
            return false;
        }
    }

}
