/*********************************************************************************
 * Compilation:  javac Server.java
 * Execution:    java Server port
 * Dependencies: Ban, ChatRoom, ClientConnection, ServerMessage, ServerSendThread
 *
 * Establishes a Server that listens for TCP connections, using a thread-per-request
 * connection model, messages are then sent using a request-reply protocol over
 * TCP sockets
 *
 *
 * @author Rem, 2015
 *********************************************************************************/

package Server;

import org.kohsuke.args4j.Option;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {

    @Option(required = false, name = "-p", aliases = {"--port"}, usage = "Port Address")
    private static int port = 4444;

    // Keep track of number of connected users
    protected static Integer guestCount = 0;
    protected static PriorityQueue<Integer> nextLowestQueue = new PriorityQueue<>();

    // Connected user info    (  Uses Thread-Safe Variant of ArrayList   )
    protected static CopyOnWriteArrayList<String> userIdentities = new CopyOnWriteArrayList();
    protected static CopyOnWriteArrayList<ClientConnection> userThreads = new CopyOnWriteArrayList<>();

    // Users within chat rooms
    protected static CopyOnWriteArrayList<ChatRoom> rooms = new CopyOnWriteArrayList<>();
    // Currently banned users
    protected static ConcurrentHashMap<ClientConnection, Ban> bannedUsers = new ConcurrentHashMap<>();


    public static void main(String[] args) throws IOException, InterruptedException {

        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server is listening...");

            ChatRoom mainHall = new ChatRoom("MainHall");
            rooms.add(mainHall);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client Connected...");

                int guestId = getNextAvailableId();

                if (guestId == guestCount) {
                    increaseGuestCount();
                    guestId = guestCount;
                }

                String newGuest = "guest" + guestId;
                userIdentities.add(newGuest);

                ClientConnection client = new ClientConnection(socket, newGuest, bannedUsers);
                userThreads.add(client);
                client.start();
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                serverSocket.close();
                closeAllThreads();
            }
        }
    }

    private static void closeAllThreads() throws InterruptedException {
        for (ClientConnection connection : userThreads) {
            connection.join();
        }
    }

    public static void announce(String message) throws IOException {
        for (ClientConnection cc : userThreads) {
            DataOutputStream out = cc.getOutput();
            Thread messageSender = new Thread(new ServerSendThread(out, message));
            messageSender.start();
        }
    }

    public static ChatRoom getRoom(String roomId) {
        for (ChatRoom room : rooms) {
            if (room.getRoomId().equals(roomId)) {
                return room;
            }
        }
        return null;
    }

    public static void createRoom(String roomId, String owner) {
        synchronized (rooms) {
            ChatRoom newRoom = new ChatRoom(roomId);
            newRoom.setOwner(owner);
            rooms.add(newRoom);
        }
    }

    public static void deleteRoom(String roomId) {
        ChatRoom roomToBeDeleted = getRoom(roomId);
        rooms.remove(rooms.indexOf(roomToBeDeleted));
    }

    public static void increaseGuestCount() {
        synchronized (guestCount) {
            guestCount++;
        }
    }

    public static void decreaseGuestCount() {
        synchronized (guestCount) {
            guestCount--;
        }
    }

    public static void makeIdAvailable(Integer id) {
        synchronized (nextLowestQueue) {
            nextLowestQueue.add(id);
        }
    }

    public static int getNextAvailableId() {
        synchronized (nextLowestQueue) {
            if (!nextLowestQueue.isEmpty()) {
                int nextLowest = nextLowestQueue.poll();
                increaseGuestCount();
                return nextLowest;
            }
            return guestCount;
        }
    }

    public static void addBannedUser(ClientConnection clientConnection, Ban ban) {
        synchronized (bannedUsers) {
            bannedUsers.put(clientConnection, ban);
            // update client threads with new list
            for (ClientConnection cc : userThreads) {
                cc.bannedUsers = bannedUsers;
            }
        }
    }

    public static void removeBan(ClientConnection clientConnection) {
        synchronized (bannedUsers) {
            bannedUsers.remove(clientConnection);
            // update client threads with new list
            for (ClientConnection cc : userThreads) {
                cc.bannedUsers = bannedUsers;
            }
        }
    }

    public static ClientConnection getUserThread(String userId) {
        synchronized (userThreads) {
            for (ClientConnection clientConnection : userThreads) {
                if (clientConnection.getIdentity().equals(userId)) {
                    return clientConnection;
                }
            }
        }
        return null;
    }

}
