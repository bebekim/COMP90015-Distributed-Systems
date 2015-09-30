/******************************************************************************
 * A single thread that is created whenever the Server wants to send a message
 * to the client. The thread is created and then closed after the message has
 * been sent
 *
 * @author Rem, 2015
 ******************************************************************************/

package Server;

import java.io.DataOutputStream;
import java.io.IOException;

public class ServerSendThread implements Runnable {

    DataOutputStream out;
    String message;

    ServerSendThread(DataOutputStream out, String message) {
        this.out = out;
        this.message = message;
    }

    @Override
    public void run() {

        try {
            out.writeUTF(message);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
