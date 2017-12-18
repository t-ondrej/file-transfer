package sk.upjs.ics.tasks;

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
    BlockingQueue<FileChunk> written;

    public RequestFileTask(FileAccessor fileAccessor, Socket socket, BlockingQueue<FileChunk> chunks,  BlockingQueue<FileChunk> written) {
        this.fileAccessor = fileAccessor;
        this.chunks = chunks;
        this.written = written;

        try {
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            closeDataStreams();
            e.printStackTrace();
        }

        requestListening();
    }

    @Override
    public Boolean call() throws Exception {
        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                return false;
            }

            try {

                FileChunk chunk = chunks.take();

                if (chunk.isPoisonPill()) {
                    //   fileAccessor.close();
                    return true;
                }

                requestFileChunk(chunk);

                byte[] buffer = new byte[chunk.getLength()];
                dis.read(buffer, 0, chunk.getLength());
                System.out.println("RECEIVER: Received " + chunk.getOffset() + " chunk");

                fileAccessor.write(chunk.getOffset(), buffer);
                written.offer(chunk);
                System.out.println("RECEIVER: Wrote " + chunk.getOffset() + " chunk");
            } catch (InterruptedException e) {
            }
        }
    }

    private void requestListening() {
        try {
            dos.writeUTF(Dialect.REQUEST_LISTENING);
            dos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Client requested listening");
    }

    private void requestFileChunk(FileChunk chunk) {
        try {
            dos.writeInt(chunk.getOffset());
            dos.writeInt(chunk.getLength());
            dos.flush();

            System.out.println("RECEIVER: Requested " + chunk.getOffset() + " chunk");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
