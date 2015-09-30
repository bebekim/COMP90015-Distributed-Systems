/**********************************************************************************
 * A class used to represent a chat room that holds an undefined amount of clients
 *
 * Comes with methods to broadcast chat messages to the clients in the room, in
 * addition to methods that return the list of users for each instance etc.
 *
 * @author Rem, 2015
 *********************************************************************************/

package Server;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatRoom {

    private String roomId;
    private String owner;
    private CopyOnWriteArrayList identities = new CopyOnWriteArrayList();
    private CopyOnWriteArrayList<ClientConnection> clientThreads = new CopyOnWriteArrayList<>();

    ChatRoom(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public CopyOnWriteArrayList<String> getUsers() {
        return identities;
    }

    public void addClient(ClientConnection cC) {
        clientThreads.add(cC);
    }

    public CopyOnWriteArrayList<ClientConnection> getClientThreads() {
        return clientThreads;
    }

    public void removeUser(String userIdentity) {
        identities.remove(identities.indexOf(userIdentity));
        for (int i = 0; i < clientThreads.size(); i++) {
            if (clientThreads.get(i).getIdentity().equals(userIdentity)) {
                clientThreads.remove(i);
            }
        }
    }

    public void broadcastToRoom(String message) throws IOException {
        for (ClientConnection c : clientThreads) {
            Thread send = new Thread(new ServerSendThread(c.getOutput(), message)); // here used to be json object
            send.start();
        }
    }

    public String[] getUsersArray() {
        return (String[]) identities.toArray(new String[identities.size()]);
    }

}
