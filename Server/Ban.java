/**********************************************************************************
 * A class used to represent a clients ban from a particular ChatRoom.
 * (excluding the default "MainHall" ChatRoom which no client can be banned from.
 *
 * Comes with a method 'isBanned' to check whether or not the Clients ban timer
 * is still running.
 *
 * @author Rem, 2015
 *********************************************************************************/

package Server;

public class Ban {

    private long timeOfBan;
    private long duration;

    public Ban(long banTime, long duration, String roomId, String userId) {
        this.timeOfBan = banTime;
        this.duration = duration;
    }

    public boolean isBanned(long currentTime) {
        boolean isBanned;
        if (timeOfBan + duration < currentTime) {
            isBanned = false;
        } else {
            isBanned = true;
        }
        return isBanned;
    }

}
