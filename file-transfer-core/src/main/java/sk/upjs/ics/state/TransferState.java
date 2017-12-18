package sk.upjs.ics.state;

import sk.upjs.ics.file.FileChunkRegistry;

/**
 * Created by Tomas on 17.12.2017.
 */
public class TransferState extends FileInfo {

    private int socketCount;
    private int serverTransferPort;
    private boolean isPaused;
    private int elapsed;

    private FileChunkRegistry chunksRegistry;

    public TransferState(String directory, String fileName, long fileLength, int chunkSize, int startingOffset, int elapsed) {
        super(directory, fileName, fileLength, chunkSize, startingOffset);
        this.elapsed = elapsed;
    }

    public int getFirstUnreadOffset() {
        return chunksRegistry.firstUnreadOffset();
    }

    public int getSocketCount() {
        return socketCount;
    }

    public void setSocketCount(int socketCount) {
        chunksRegistry.addPoisonPills(socketCount - this.socketCount);
        this.socketCount = socketCount;
    }

    public int getServerTransferPort() {
        return serverTransferPort;
    }

    public void setServerTransferPort(int serverTransferPort) {
        this.serverTransferPort = serverTransferPort;
    }

    public double getProgress() {
        return (chunksRegistry.getTotalChunks() - chunksRegistry.getChunksLeft())
                / (double) chunksRegistry.getTotalChunks();
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        isPaused = paused;
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
}
