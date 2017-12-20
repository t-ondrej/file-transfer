package sk.upjs.ics.state;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;


/**
 * Created by Tomas on 12.12.2017.
 */
public class FileStateManager implements StateManager {

    private static final String BACKUP_FILE_PATH = "backup.txt";
    private static final String splitter = ";";
    private final Logger logger = Logger.getLogger(getClass());

    public boolean existsPrevious() {
        File backupFile = new File(BACKUP_FILE_PATH);

        return backupFile.exists() && !backupFile.isDirectory();
    }

    public TransferState getLastState() {
        String[] result = new String[0];
        File backupFile = new File(BACKUP_FILE_PATH);

        try (Scanner sc = new Scanner(backupFile)) {
            result = sc.nextLine().split(splitter);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        TransferState state =  new TransferState(result[0],
                                                 result[1],
                                                 Long.parseLong(result[2]),
                                                 Integer.parseInt(result[3]),
                                                 Integer.parseInt(result[4]),
                                                 Integer.parseInt(result[5]),
                                                 Integer.parseInt(result[6]),
                                                 Integer.parseInt(result[7]));

        logger.info("Got last state");

        return state;
    }

    public void deleteBackup() {
        File backupFile = new File(BACKUP_FILE_PATH);
        backupFile.delete();
        logger.info("Backup deleted");
    }

    public void persist(TransferState transferState) {
        String backupMessage =
                transferState.getDirectory() + splitter +
                transferState.getFileName() + splitter +
                Long.toString(transferState.getFileLength()) + splitter +
                Integer.toString(transferState.getChunkSize()) + splitter +
                Integer.toString(transferState.getFirstUnreadOffset()) + splitter +
                Integer.toString(transferState.getElapsed()) + splitter +
                Integer.toString(transferState.getSocketCount()) + splitter +
                Integer.toString(transferState.getServerTransferPort());

        try (PrintWriter pw = new PrintWriter(BACKUP_FILE_PATH)) {
            pw.println(backupMessage);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        logger.info("Backup persisted");
    }
}
