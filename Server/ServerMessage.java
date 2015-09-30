/******************************************************************************
 * A factory class that is used to create JSON formatted messages, to be sent
 * as replies back to the clients over the TCP connection.
 *
 * E.g JSONObject ServerMessage
 * = new ServerMessage.newIdentityMsg(String former, String newIdentity)
 *
 * @author Rem, 2015
 ******************************************************************************/

package Server;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;

public class ServerMessage {

    JSONObject jsonMessage;

    public String newIdentityMsg(String former, String newIdentity) {
        jsonMessage = new JSONObject();
        jsonMessage.put("type", "newidentity");
        jsonMessage.put("former", former);
        jsonMessage.put("identity", newIdentity);
        return jsonMessage.toString();
    }

    public String roomChangeMsg(String identity, String former, String newRoomId) {
        jsonMessage = new JSONObject();
        jsonMessage.put("type", "roomchange");
        jsonMessage.put("identity", identity);
        jsonMessage.put("former", former);
        jsonMessage.put("roomid", newRoomId);
        return jsonMessage.toString();
    }

    public String roomContentsMsg(String roomId, String owner, String[] identities) {
        jsonMessage = new JSONObject();
        JSONArray identyList = new JSONArray();
        jsonMessage.put("type", "roomcontents");
        jsonMessage.put("roomid", roomId);

        JSONArray jsonIdentities = new JSONArray();
        for (String identity : identities) {
            jsonIdentities.add(identity);
        }

        jsonMessage.put("identities", jsonIdentities);
        jsonMessage.put("owner", owner);
        return jsonMessage.toString();
    }

    public String roomListMsg(ArrayList<JSONObject> rooms) {
        jsonMessage = new JSONObject();
        jsonMessage.put("type", "roomlist");

        JSONArray roomList = new JSONArray();
        for (JSONObject jo : rooms) {
            roomList.add(jo);
        }

        jsonMessage.put("rooms", roomList);

        return jsonMessage.toString();
    }

}
