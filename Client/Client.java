/******************************************************************************
 * Compilation:  javac Client.java
 * Execution:    java Chat host port
 * Dependencies: ClientMessage, SendThread
 *
 * Connects a to host server on port 4444 by default, enables an interactive
 * instant chat messenger.
 *
 * @author Rem, 2015
 ******************************************************************************/

package Client;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.kohsuke.args4j.Option;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class Client {

    @Option(required = true, name = "-h", aliases = {"--host"}, usage = "Host Address")
    private static String host;

    @Option(required = true, name = "-p", aliases = {"--port"}, usage = "Port Address")
    private static int port = 4444;

    private static String identity;

    public static void main(String[] args) throws IOException {
        Socket socket = null;
        try {
            socket = new Socket(host, port);
            DataInputStream in = new DataInputStream(socket.getInputStream());

            Thread sendingThread = new Thread(new SendThread(socket));
            sendingThread.start();

            // READS MESSAGES IN FROM THE SERVER
            while (true) {
                String response = in.readUTF();
                Object obj = JSONValue.parse(response);

                JSONObject jsonMsg = (JSONObject) obj;

                String type = (String) jsonMsg.get("type");
                switch (type) {

                    case "message":
                        messageReply(jsonMsg);
                        break;

                    case "newidentity":
                        newIdentityReply(jsonMsg);
                        break;

                    case "roomchange":
                        // This client disconnecting
                        if (jsonMsg.get("roomid").toString().equals("")
                                && jsonMsg.get("identity").toString().equals(identity)) {
                            System.out.println("Disconnected from " + socket.getInetAddress());
                            in.close();
                            socket.close();
                            sendingThread.join();  //close down sending thread
                            System.exit(1);
                        }
                        // Another client changing room/ disconnecting
                        else {
                            String ident = jsonMsg.get("identity").toString();
                            System.out.println(ident + " moves to " + jsonMsg.get("roomid").toString());
                        }
                        break;

                    case "roomcontents":
                        roomContentsReply(jsonMsg);
                        break;

                    case "roomlist":
                        roomListReply(jsonMsg);
                        break;

                    default:
                        System.out.println("Please use a valid message");
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println("Unable to connect to server");
            if (socket != null) {
                socket.close();
                e.printStackTrace();
            }
        }
    }


    private static void messageReply(JSONObject jsonMsg) {
        System.out.print(jsonMsg.get("identity").toString());
        System.out.print(": ");
        System.out.print(jsonMsg.get("content").toString());
        System.out.println();
    }

    private static void newIdentityReply(JSONObject jsonMsg) {
        if (identity == null) {
            identity = jsonMsg.get("identity").toString();
            System.out.println("Connected to localhost as " + identity);
        } else if (jsonMsg.get("former").toString().equals(jsonMsg.get("identity"))) {
            System.out.println("Requested identity invalid or in use");
        } else {
            identity = jsonMsg.get("identity").toString();
            System.out.println(jsonMsg.get("former").toString() + " is now " + identity);
        }
    }

    private static void roomContentsReply(JSONObject jsonMsg) {
        String room = jsonMsg.get("roomid").toString();
        JSONArray jsonRoomMembers = (JSONArray) jsonMsg.get("identities");
        ArrayList<String> roomMembers = new ArrayList<>();
        for (int i = 0; i < jsonRoomMembers.size(); i++) {
            roomMembers.add(jsonRoomMembers.get(i).toString());
        }
        if (room.equals("MainHall")) {
            System.out.print(room + " contains ");

            for (String user : roomMembers) {
                System.out.print(user + " ");
            }
            System.out.println();
        } else {
            System.out.print(room + " contains ");
            for (String user : roomMembers) {
                System.out.print(user + " ");
            }
            System.out.print("Owner: " + jsonMsg.get("owner").toString());
            System.out.println();
        }
    }

    private static void roomListReply(JSONObject jsonMsg) {
        JSONArray rooms = (JSONArray) jsonMsg.get("rooms");
        for (int i = 0; i < rooms.size(); i++) {
            JSONObject singleRoom = (JSONObject) rooms.get(i);
            System.out.print(singleRoom.get("roomid").toString() + ": ");
            System.out.print(singleRoom.get("count").toString() + " guest/s");
            System.out.println();
        }
    }

}
