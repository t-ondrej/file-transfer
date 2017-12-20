package sk.upjs.ics;

import org.apache.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by Tomas on 9.12.2017.
 */
public abstract class CommunicationBase {

    protected DataInputStream dis;
    protected DataOutputStream dos;
    protected Logger logger;

    protected void closeDataStreams() {
        try {
            if (dis != null)
                dis.close();

            if (dos != null)
                dos.close();
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(e);
        }

        logger.info("Closing data streams for " + Thread.currentThread().getName());
    }
}
