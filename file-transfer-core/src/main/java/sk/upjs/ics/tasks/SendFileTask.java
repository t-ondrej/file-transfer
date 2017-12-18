package sk.upjs.ics.tasks;

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

        try {
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            closeDataStreams();
            e.printStackTrace();
        }
    }

    public Void call() throws Exception {
        System.out.println("Started sender");
        while (true) {

            if (Thread.currentThread().isInterrupted()) {
                System.out.println("Stopped sender");
                break;
            }

            int offset = dis.readInt();
            int length = dis.readInt();

            System.out.println("SENDER: Received request for " + offset + " chunk");

            byte[] buffer = fileAccesor.read(offset, length);
            System.out.println("SENDER: Read " + offset + " chunk");

            sendData(buffer);
            System.out.println("SENDER: Sent " + offset + " chunk");
        }

        return null;
    }

    private void sendData(byte[] buffer) {
        try {
            dos.write(buffer);
            dos.flush();
        } catch (IOException e) {
            closeDataStreams();
            logger.info("SERVER: Closing data streams");
        }
    }
}
