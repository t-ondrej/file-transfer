package sk.upjs.ics.state;

import org.apache.log4j.Logger;
import sk.upjs.ics.file.FileChunkRegistry;

/**
 * Created by Tomas on 17.12.2017.
 */
public class TransferState extends FileInfo {

    private int socketCount;
    private int serverTransferPort;
    private int elapsed;

    private FileChunkRegistry chunksRegistry;

    private Logger logger = Logger.getLogger(getClass());

    public static TransferState createDefault() {
        return new TransferState();
    }

    private TransferState() {
        super();
    }

    public TransferState(String directory, String fileName, long fileLength,
                         int chunkSize, int startingOffset, int elapsed,
                         int socketCount, int serverTransferPort) {
        super(directory, fileName, fileLength, chunkSize, startingOffset);
        this.elapsed = elapsed;
        this.socketCount = socketCount;
        this.serverTransferPort = serverTransferPort;

        logger.info("CREATED STATE: "+ this.toString());
    }

    public int getFirstUnreadOffset() {
        return chunksRegistry.firstUnreadOffset();
    }

    public int getSocketCount() {
        return socketCount;
    }

    public int getServerTransferPort() {
        return serverTransferPort;
    }

    public double getProgress() {
        if (chunksRegistry == null)
            return 0;

        return (chunksRegistry.getTotalChunks() - chunksRegistry.getChunksLeft())
                / (double) chunksRegistry.getTotalChunks();
    }

    public void setChunksRegistry(FileChunkRegistry chunksRegistry) {
        this.chunksRegistry = chunksRegistry;
    }

    public int addAndGetElapsed() {
        return ++elapsed;
    }

    public int getElapsed() {
        return elapsed;
    }

    @Override public String toString() {
        return "TransferState{" +
                "socketCount=" + socketCount +
                ", serverTransferPort=" + serverTransferPort +
                ", elapsed=" + elapsed +
                ", chunksRegistry=" + chunksRegistry +
                '}';
    }
}
