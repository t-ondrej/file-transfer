package sk.upjs.ics.tasks;

import org.apache.log4j.Logger;
import sk.upjs.ics.CommunicationBase;
import sk.upjs.ics.file.FileAccessor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Callable;

/**
 * Created by Tomas on 6.12.2017.
 */
public class SendFileTask extends CommunicationBase implements Callable<Void> {

    private FileAccessor fileAccesor;

    public SendFileTask(FileAccessor fileAccessor, Socket socket) {
        this.fileAccesor = fileAccessor;
        logger = Logger.getLogger(getClass());

        try {
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            closeDataStreams();
            logger.error(e);
        }
    }

    public Void call() throws Exception {
        try {
            while (true) {

                if (Thread.currentThread().isInterrupted()) {
                    logger.info("Sender cancellled");
                    break;
                }

                int offset = dis.readInt();
                int length = dis.readInt();

                byte[] buffer = fileAccesor.read(offset, length);

                sendData(buffer);
            }
        } finally {
            closeDataStreams();
        }

        return null;
    }

    private void sendData(byte[] buffer) throws IOException {
        dos.write(buffer);
        dos.flush();
    }
}
