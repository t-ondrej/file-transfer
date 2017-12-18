package sk.upjs.ics.state;

/**
 * Created by Tomas on 17.12.2017.
 */
public interface StateManager {
    boolean existsPrevious();
    TransferState getLastState();
    void persist(TransferState transferState);
    void deleteBackup();
}
