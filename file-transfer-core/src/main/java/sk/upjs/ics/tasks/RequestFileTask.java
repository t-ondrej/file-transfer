package sk.upjs.ics.tasks;

import org.apache.log4j.Logger;
import sk.upjs.ics.CommunicationBase;
import sk.upjs.ics.commons.Dialect;
import sk.upjs.ics.file.FileAccessor;
import sk.upjs.ics.file.FileChunk;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

/**
 * Created by Tomas on 9.12.2017.
 */
public class RequestFileTask extends CommunicationBase implements Callable<Boolean> {

    FileAccessor fileAccessor;
    BlockingQueue<FileChunk> chunks;

    public RequestFileTask(FileAccessor fileAccessor, Socket socket, BlockingQueue<FileChunk> chunks) {
        this.fileAccessor = fileAccessor;
        this.chunks = chunks;

        logger = Logger.getLogger(getClass());

        try {
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
            requestListening();
        } catch (IOException e) {
            closeDataStreams();
            logger.error(e);
        }
    }

    @Override
    public Boolean call() throws Exception {
        boolean success = true;
        try {
            while (true) {
                if (Thread.currentThread().isInterrupted()) {
                    success = false;
                    break;
                }

                FileChunk chunk = chunks.take();
                if (chunk.isPoisonPill()) {
                    return true;
                }

                requestFileChunk(chunk);
                byte[] buffer = new byte[chunk.getLength()];
                dis.read(buffer, 0, chunk.getLength());

                fileAccessor.write(chunk.getOffset(), buffer);
            }

        } finally {
            closeDataStreams();
            fileAccessor.close();
        }

        return success;
    }

    private void requestListening() throws IOException {
        dos.writeUTF(Dialect.REQUEST_LISTENING);
        dos.flush();

     //   logger.info("RECEIVER: Requested listening " + Thread.currentThread().getName());
    }

    private void requestFileChunk(FileChunk chunk) throws IOException {
        dos.writeInt(chunk.getOffset());
        dos.writeInt(chunk.getLength());
        dos.flush();

      //  logger.info("RECEIVER: Requested " + chunk.getOffset() + " chunk");
    }
}
