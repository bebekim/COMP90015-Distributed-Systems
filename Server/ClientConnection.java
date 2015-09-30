/********************************************************************************
 * A Thread class that represents a client's connection to the server. This class
 * reads messages from its MessageQueue and proccesses each request according to
 * a specific protocol.
 *
 * Utilizes private methods to implement the request-reply protocol procedures.
 *
 * @author Rem, 2015
 *******************************************************************************/

package Server;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientConnection extends Thread {

    protected String identity;
    protected Socket socket;
    protected ConcurrentLinkedQueue<String> messageQueue = new ConcurrentLinkedQueue<String>();
    protected ChatRoom currentRoom;
    protected ConcurrentHashMap<ClientConnection, Ban> bannedUsers;

    public ClientConnection(Socket socket, String identity, ConcurrentHashMap bannedUsers) {
        this.identity = identity;
        this.socket = socket;
        this.bannedUsers = bannedUsers;
    }

    public String getIdentity() {
        return identity;
    }

    public DataOutputStream getOutput() throws IOException {
        return new DataOutputStream(socket.getOutputStream());
    }

    //  READS INPUT FROM THE CLIENT AND ADDS IT TO THE MESSAGE-QUEUE
    @Override
    public void run() {

        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            Thread readFromQueue = new Thread(new ReadWrite());
            readFromQueue.start();

            try {
                while (true) {
                    synchronized (messageQueue) {
                        String msg = in.readUTF();
                        System.out.println(msg);
                        messageQueue.add(msg);
                    }
                }
            } catch (EOFException e) {
                quitRequest();
                in.close();
                out.close();
                socket.close();
                System.out.println("Client " + identity + " terminated connection");
            }
        } catch (IOException e) {
            System.out.println("Error initialising clients IO");
            e.printStackTrace();
        }

    }

    //  READS AND RESPONDS TO MESSAGES IN MESSAGE-QUEUE
    private class ReadWrite implements Runnable {

        @Override
        public void run() {

            boolean readMessages = true;

            while (readMessages) {

                while (!messageQueue.isEmpty() && readMessages) {

                    String message = messageQueue.poll();
                    Object obj = JSONValue.parse(message);
                    JSONObject jsonMsg = (JSONObject) obj;

                    String type = jsonMsg.get("type").toString();

                    try {

                        switch (type) {

                            case "message":
                                jsonMsg.put("identity", identity);
                                currentRoom.broadcastToRoom(jsonMsg.toString());
                                break;

                            case "join":
                                String roomId = jsonMsg.get("roomid").toString();
                                clientJoinRequest(roomId);
                                break;

                            case "list":
                                ArrayList<JSONObject> roomsWithCount = getRoomListWithCount();
                                String response = new ServerMessage().roomListMsg(roomsWithCount);
                                getOutput().writeUTF(response);
                                getOutput().flush();
                                break;

                            case "createroom":
                                String newRoomId = jsonMsg.get("roomid").toString();
                                createRoomRequest(newRoomId);
                                break;

                            case "delete":
                                String roomToDelete = jsonMsg.get("roomid").toString();
                                deleteRoomRequest(roomToDelete);
                                break;

                            case "who":
                                String roomRequested = jsonMsg.get("roomid").toString();
                                whoRequest(roomRequested);
                                break;

                            case "identitychange":
                                String newIdentityReq = jsonMsg.get("identity").toString();
                                String formerIdentity = identity;
                                identityChangeRequest(newIdentityReq, formerIdentity);
                                break;

                            case "kick":
                                String room = jsonMsg.get("roomid").toString();
                                String userToKick = jsonMsg.get("identity").toString();
                                long banTime = Long.parseLong(jsonMsg.get("time").toString()); // number of seconds
                                kickRequest(room, userToKick, banTime);
                                break;

                            case "quit":
                                quitRequest();
                                readMessages = false;
                                break;

                            default:
                                System.out.println("An error occured when trying to read clients input");
                                break;
                        }
                    } catch (IOException e) {
                        System.out.println("Error receiving client message");
                        e.printStackTrace();
                    }
                } // end while
            }
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Error closing socket on disconnection");
                e.printStackTrace();
            }
        }

    }

    private boolean isUsernameInUse(String newUsername) {
        for (int i = 0; i < Server.userIdentities.size(); i++) {
            if (Server.userIdentities.get(i).contains(newUsername)) {  // No Change
                return true;
            }
        }
        return false;
    }

    private boolean isAlreadyInRoom(String identity, String roomId) {
        ChatRoom room = Server.getRoom(roomId);
        if (room != null) {
            for (String client : room.getUsers()) {
                if (client.equals(identity)) {
                    return true;
                }
            }
        }
        return false;
    }

    private ArrayList<JSONObject> getRoomListWithCount() {
        ArrayList<JSONObject> roomsWithCount = new ArrayList<>();
        for (ChatRoom cR : Server.rooms) {
            JSONObject roomWithCount = new JSONObject();
            roomWithCount.put("roomid", cR.getRoomId());
            roomWithCount.put("count", cR.getUsers().size());
            roomsWithCount.add(roomWithCount);
        }
        return roomsWithCount;
    }

    private void removeAnyRoomOwnerships(String identity) {
        for (ChatRoom room : Server.rooms) {
            if (room.getOwner() != null) {
                if (room.getOwner().equals(identity)) {
                    room.setOwner("");
                    if (room.getUsers().isEmpty()) {
                        deleteRoom(room.getRoomId());
                    }
                }
            }
        }
    }

    private void removeClientFromPreviousRoom(String roomId, String identity) {
        if (roomId != null && !roomId.equals("MainHall")) {
            ChatRoom previousRoom = Server.getRoom(roomId);
            previousRoom.removeUser(identity);
        }
    }

    private void deleteRoom(String roomId) {
        ChatRoom chatRoom = Server.getRoom(roomId);
        Server.rooms.remove(Server.rooms.indexOf(chatRoom));
    }

    private void moveAllToMainHall(String roomId) throws IOException {
        ChatRoom chatRoom = Server.getRoom(roomId);
        ChatRoom mainHall = Server.rooms.get(0);
        mainHall.addClient(ClientConnection.this);
        mainHall.getUsers().add(identity);
        for (String user : chatRoom.getUsers()) {
            String roomChangeMsg = new ServerMessage().roomChangeMsg(user, chatRoom.getRoomId(), "MainHall");
            chatRoom.broadcastToRoom(roomChangeMsg);
            mainHall.broadcastToRoom(roomChangeMsg);
        }
    }

    private void deleteRoomIfOwner(String userLeaving, ChatRoom roomLeaving) {
        if (roomLeaving.getOwner() != null) {
            if (roomLeaving.getOwner().equals(userLeaving)) {
                if (roomLeaving.getUsers().size() == 0) {
                    Server.deleteRoom(roomLeaving.getRoomId());
                }
            }
        }
    }

    private void clientJoinRequest(String roomId) throws IOException {
        if (roomId.equals("MainHall")) {
            if (!isAlreadyInRoom(identity, roomId)) {

                if (currentRoom != null) {
                    removeClientFromPreviousRoom(currentRoom.getRoomId(), identity);
                    // Delete previous room if they are the owner and no one is in it
                    deleteRoomIfOwner(identity, currentRoom);
                }

                // Add them to the room
                ChatRoom mainHall = Server.rooms.get(0);
                currentRoom = mainHall;
                currentRoom.getClientThreads().add(ClientConnection.this);        // Add new thread to chatroom
                currentRoom.getUsers().add(ClientConnection.this.getIdentity()); // Add username to list of users

                // Send room change message to all in the room
                String response = new ServerMessage().roomChangeMsg
                        (mainHall.getUsers().get(mainHall.getUsers().size() - 1), "", "MainHall");
                Server.rooms.get(0).broadcastToRoom(response);

                // List the people in Main Hall to the single user
                String[] mainHallClients = mainHall.getUsersArray();
                String listMsg = new ServerMessage().roomContentsMsg("MainHall", "", mainHallClients);
                getOutput().writeUTF(listMsg);
                getOutput().flush();
            }
        } else {
            ClientConnection thisConnection = ClientConnection.this;
            if (bannedUsers.containsKey(thisConnection)) { // check if they have been banned
                Ban thisBan = bannedUsers.get(thisConnection);
                if (thisBan.isBanned(System.currentTimeMillis() / 1000)) {
                    return;
                } else {
                    Server.removeBan(thisConnection); //the timer has run up
                    joinRoom(roomId);
                }
            } else {
                if (!isAlreadyInRoom(identity, roomId)) {    // not banned so just join them to the room
                    joinRoom(roomId);
                }
            }
        }
    }

    private void joinRoom(String roomId) throws IOException {
        ChatRoom room = Server.getRoom(roomId);
        if (room != null) { // Make sure the room they are joining exists

            // Broadcast changes to rooms
            String roomChangeMsg = new ServerMessage()
                    .roomChangeMsg(identity, currentRoom.getRoomId(), roomId);

            currentRoom.broadcastToRoom(roomChangeMsg);
            room.broadcastToRoom(roomChangeMsg);

            // Remove from current room
            currentRoom.removeUser(identity);

            // Delete previous room if they are the owner and no one is in it
            deleteRoomIfOwner(identity, currentRoom);

            // Put user in the new room
            currentRoom = room;

            // Record them as now being in the new room
            currentRoom.getUsers().add(identity);
            currentRoom.getClientThreads().add(ClientConnection.this);
        }
    }

    private void createRoomRequest(String newRoomId) throws IOException {
        boolean roomNameInUse = false;
        for (ChatRoom r : Server.rooms) {
            if (r.getRoomId().equals(newRoomId)) {
                roomNameInUse = true;
            }
        }
        if (roomNameInUse) {  // NO CHANGE
            ArrayList<JSONObject> roomsResponse = getRoomListWithCount();
            String roomListResponse = new ServerMessage().roomListMsg(roomsResponse);
            getOutput().writeUTF(roomListResponse);
            getOutput().flush();
        } else {  //  CREATES THE NEW ROOM
            Server.createRoom(newRoomId, identity);
            ArrayList<JSONObject> roomsResponse = getRoomListWithCount();
            String roomListResponse = new ServerMessage().roomListMsg(roomsResponse);
            getOutput().writeUTF(roomListResponse);
            getOutput().flush();
        }
    }

    private void deleteRoomRequest(String roomToDelete) throws IOException {
        ChatRoom deletedRoom = Server.getRoom(roomToDelete);
        if (deletedRoom != null) {
            if (deletedRoom.getOwner().equals(identity)) {          // Make sure its not MainHall & the owner is deleting
                // Do a room change for all users to main hall
                moveAllToMainHall(roomToDelete);
                // Delete room
                deleteRoom(roomToDelete);
                // Reply only to the user that deleted the room with a roomlist
                ArrayList<JSONObject> serverRooms = getRoomListWithCount();
                String deleteResponse = new ServerMessage().roomListMsg(serverRooms);
                getOutput().writeUTF(deleteResponse);
                getOutput().flush();
            } else {
                System.out.println("Client tried to delete an invalid rooom");
            }
        }
    }

    private void whoRequest(String roomId) throws IOException {
        if (roomId.equals("MainHall")) {
            String[] users = Server.rooms.get(0).getUsers().toArray(
                    new String[Server.rooms.get(0).getUsers().size()]);
            String whoMainHallResponse = new ServerMessage().roomContentsMsg("MainHall", "", users);
            getOutput().writeUTF(whoMainHallResponse);
            getOutput().flush();
        } else {
            ChatRoom roomToLookIn = Server.getRoom(roomId);
            if (roomToLookIn != null) {
                String roomOwner = roomToLookIn.getOwner();
                String[] usersInside = roomToLookIn.getUsers().toArray(
                        new String[Server.getRoom(roomId).getUsers().size()]);
                String whoResponse = new ServerMessage().roomContentsMsg(roomId, roomOwner, usersInside);

                getOutput().writeUTF(whoResponse);
                getOutput().flush();
            }
        }
    }

    private void identityChangeRequest(String newIdentityReq, String formerIdentity) throws IOException {
        // FIRST TIME SETTING UP IDENTITY
        if (newIdentityReq.equals("")) {
            Server.userIdentities.add(identity);
            String firstIdResponse = new ServerMessage().newIdentityMsg("", identity);

            getOutput().writeUTF(firstIdResponse);
            getOutput().flush();
        } else {
            // CHECK IF ALREADY IN USE
            if (isUsernameInUse(newIdentityReq)) {
                String noChangeResponse = new ServerMessage().newIdentityMsg(formerIdentity, formerIdentity);
                getOutput().writeUTF(noChangeResponse);
                getOutput().flush();
            }
            // NOT IN USE SO UPDATE THE NAME
            else {
                // First Make integer available for other new guests
                if (formerIdentity.matches("guest\\d{1,3}")) {
                    String getNumber = formerIdentity.replaceAll("[^0-9]", "");
                    Integer nowAvailableID = Integer.parseInt(getNumber);
                    Server.makeIdAvailable(nowAvailableID);
                }
                // USER NAME NOT IN-USE, SO UPDATE THE SERVER LIST
                for (int i = 0; i < Server.userIdentities.size(); i++) {
                    if (Server.userIdentities.get(i).equals(formerIdentity)) {
                        Server.userIdentities.set(i, newIdentityReq);
                    }
                }
                // ALSO UPDATE CHAT-ROOM LIST
                for (int i = 0; i < currentRoom.getUsers().size(); i++) {
                    if (currentRoom.getUsers().get(i).equals(formerIdentity)) {
                        currentRoom.getUsers().set(i, newIdentityReq);
                    }
                }
                // CHECK IF THE OWNER CURRENT ROOM
                if (currentRoom.getOwner() != null) {
                    if (currentRoom.getOwner().equals(identity)) {
                        currentRoom.setOwner(newIdentityReq);
                    }
                }
                // Check if owner of any other rooms
                for (int i = 1; i < Server.rooms.size(); i++) {
                    if (Server.rooms.get(i).getOwner().equals(formerIdentity)) {
                        Server.rooms.get(i).setOwner(newIdentityReq);
                    }
                }

                // SEND TO THE REST OF THE CHAT ROOM
                identity = newIdentityReq;
                String updatedId = new ServerMessage().newIdentityMsg(formerIdentity, identity);
                Server.announce(updatedId);
            }
        }
    }

    private void quitRequest() throws IOException {
        if (identity.matches("guest\\d{1,3}")) {
            String digit = identity.replaceAll("[A-Za-z]", "");
            Integer availableId = Integer.parseInt(digit);
            Server.makeIdAvailable(availableId);
        }
        // Remove from current room
        currentRoom.removeUser(identity);

        // Send room change message, new room is an empty string
        String roomChangeMsg = new ServerMessage().roomChangeMsg(identity, currentRoom.getRoomId(), "");
        currentRoom.broadcastToRoom(roomChangeMsg);

        // If they are the owner of any rooms, set the owner to be an empty string
        removeAnyRoomOwnerships(identity);

        Server.decreaseGuestCount();
        // Send room change message back to client so it can quit
        String singleRoomChangeMessage = new ServerMessage().roomChangeMsg(identity, "", "");
        getOutput().writeUTF(singleRoomChangeMessage);
        getOutput().flush();
    }

    private void kickRequest(String roomId, String userToKick, long banTime) throws IOException {
        ChatRoom chatRoom = Server.getRoom(roomId);
        if (chatRoom != null) {
            if ((!chatRoom.getRoomId().equals("MainHall")) && chatRoom.getOwner().equals(identity)) {

                long currentTime = System.currentTimeMillis() / 1000; //seconds

                Ban ban = new Ban(currentTime, banTime, roomId, userToKick);
                ClientConnection usersThread = Server.getUserThread(userToKick);
                Server.addBannedUser(usersThread, ban);

                if (usersThread != null) {
                    usersThread.joinRoom("MainHall");
                }

            }
        }

    }


}

