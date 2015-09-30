/******************************************************************************
 * A factory class that is used to create JSON formatted messages, to be sent
 * to the server over TCP. Uses the default constructor.
 *
 * E.g JSONObject chatMessage = new ClientMessage.chatMessage(message);
 *
 * @author Rem, 2015
 ******************************************************************************/


package Client;

import org.json.simple.JSONObject;

public class ClientMessage {

    JSONObject jsonMessage;

    public String chatMessage(String message) {
        jsonMessage = new JSONObject();
        jsonMessage.put("type", "message");
        jsonMessage.put("content", message);
        String jsonString = jsonMessage.toJSONString() + "\n";
        return jsonString;
    }

    public String newIdentityRequest(String identity) {
        jsonMessage = new JSONObject();
        jsonMessage.put("type", "identitychange");
        jsonMessage.put("identity", identity);
        String jsonString = jsonMessage.toJSONString() + "\n";
        return jsonString;
    }

    public String joinRoomRequest(String roomId) {
        jsonMessage = new JSONObject();
        jsonMessage.put("type", "join");
        jsonMessage.put("roomid", roomId);
        String jsonString = jsonMessage.toJSONString() + "\n";
        return jsonString;
    }

    public String whoRequest(String roomId) {
        jsonMessage = new JSONObject();
        jsonMessage.put("type", "who");
        jsonMessage.put("roomid", roomId);
        String jsonString = jsonMessage.toJSONString() + "\n";
        return jsonString;
    }

    public String listRequest() {
        jsonMessage = new JSONObject();
        jsonMessage.put("type", "list");
        String jsonString = jsonMessage.toJSONString() + "\n";
        return jsonString;
    }

    public String createRomRequest(String newRoomId) {
        jsonMessage = new JSONObject();
        jsonMessage.put("type", "createroom");
        jsonMessage.put("roomid", newRoomId);
        String jsonString = jsonMessage.toJSONString() + "\n";
        return jsonString;
    }

    public String kickRequest(String roomId, String time, String user) {
        jsonMessage = new JSONObject();
        jsonMessage.put("type", "kick");
        jsonMessage.put("roomid", roomId);
        jsonMessage.put("time", time);
        jsonMessage.put("identity", user);
        String jsonString = jsonMessage.toJSONString() + "\n";
        return jsonString;
    }

    public String deleteRequest(String roomId) {
        jsonMessage = new JSONObject();
        jsonMessage.put("type", "delete");
        jsonMessage.put("roomid", roomId);
        String jsonString = jsonMessage.toJSONString() + "\n";
        return jsonString;
    }

    public String quitRequest() {
        jsonMessage = new JSONObject();
        jsonMessage.put("type", "quit");
        String jsonString = jsonMessage.toJSONString() + "\n";
        return jsonString;
    }

}
