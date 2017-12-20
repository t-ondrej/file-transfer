package sk.upjs.ics.ui.controllers;

import com.google.inject.Inject;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXSlider;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import sk.upjs.ics.FileClient;
import sk.upjs.ics.utils.Time;
import sun.rmi.runtime.Log;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Tomas on 9.12.2017.
 */
public class ClientSceneController implements Initializable {

    @FXML private JFXButton startButton;
    @FXML private JFXButton chooseDirectoryButton;
    @FXML private JFXSlider socketCountSlider;
    @FXML private ProgressBar progressBar;
    @FXML private Label directoryLabel;
    @FXML private JFXButton pauseButton;
    @FXML private JFXButton resumeButton;
    @FXML private JFXButton cancelButton;
    @FXML private Label timeLabel;

    private Stage currentStage;
    private FileClient fileClient;
    private Timer timer;

    private Logger logger = Logger.getLogger(getClass());

    @Inject
    public ClientSceneController(FileClient fileClient) {
        this.fileClient = fileClient;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initComponents();
        progressBar.setProgress(fileClient.getProgress());
    }

    public void setCurrentStage(Stage currentStage) {
        this.currentStage = currentStage;
    }

    @FXML
    private void onStartButtonClicked() {
        updateComponentsStarted();
        int socketCount = (int)socketCountSlider.getValue();

        logger.info("GUI: timer added");

        Service<Boolean> downloadService = new Service<Boolean>() {
            protected Task<Boolean> createTask() {
                return new Task<Boolean>() {
                    protected Boolean call() throws Exception {
                        return fileClient.startDownloading(socketCount, directoryLabel.getText());
                    }
                };
            }
        };

        addTimerSchedule();

        logger.info("GUI: download service created");

        downloadService.setOnSucceeded((event) -> {
            if (downloadService.getValue())
                updateComponentsDefault();
            progressBar.setProgress(fileClient.getProgress());
            timer.cancel();
        });

        logger.info("GUI: download service onSucceded set");

        downloadService.start();
    }

    @FXML
    private void onPauseButtonClicked() {
        updateComponentsPaused();

        new Service<Void>() {
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    protected Void call() throws Exception {
                        fileClient.pauseDownloading();
                        return null;
                    }
                };
            }
        }.start();
    }

    @FXML
    private void onResumeButtonClicked() {
        addTimerSchedule();
        updateComponentsResumed();

        new Service<Void>() {
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    protected Void call() throws Exception {
                        fileClient.resumeDownloading();

                        return null;
                    }
                };
            }
        }.start();
    }

    @FXML
    private void onCancelButtonClicked() {
        if (timer != null)
            timer.cancel();

        updateComponentsCancelled();

        new Service<Void>() {
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    protected Void call() throws Exception {
                        logger.info("GUI: cancelled");
                        fileClient.cancelDownloading();
                        return null;
                    }
                };
            }
        }.start();
    }

    @FXML
    private void onChooseDirectoryClicked() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(currentStage);

        if (selectedDirectory == null) {
            directoryLabel.setText("No directory selected ");
        } else {
            directoryLabel.setText(selectedDirectory.getAbsolutePath() + " ");
        }
    }

    private void addTimerSchedule() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (fileClient.getProgress() >= 1)
                    return;

                if (fileClient.getElapsed() % 10 == 0)
                    logger.info((fileClient.getProgress() * 100) + " %");

                Platform.runLater(() -> {
                    progressBar.setProgress(fileClient.getProgress());
                    timeLabel.setText(Time.fromSeconds(fileClient.addAndGetElapsed()));
                });
            }
        }, 0, 1000);
    }

    private void initComponents() {
        if (fileClient.resumeAvailable()) {
            updateComponentsPaused();

            directoryLabel.setText(fileClient.getFileInfo().getDirectory());
            socketCountSlider.setDisable(true);
            chooseDirectoryButton.setDisable(true);
            timeLabel.setText(Time.fromSeconds(fileClient.getElapsed()));
        }
    }

    private void updateComponentsStarted() {
        socketCountSlider.setDisable(true);

        startButton.setDisable(true);
        pauseButton.setDisable(false);
        resumeButton.setDisable(true);
        cancelButton.setDisable(false);

        chooseDirectoryButton.setDisable(true);
        socketCountSlider.setDisable(true);
    }

    private void updateComponentsPaused() {
        if (timer != null)
            timer.cancel();

        startButton.setDisable(false);
        pauseButton.setDisable(true);
        resumeButton.setDisable(false);
        cancelButton.setDisable(false);
    }

    private void updateComponentsResumed() {
        startButton.setDisable(true);
        pauseButton.setDisable(false);
        resumeButton.setDisable(true);
        cancelButton.setDisable(false);
    }

    private void updateComponentsCancelled() {
        directoryLabel.setText("No directory selected");
        timeLabel.setText(Time.fromSeconds(0));
        progressBar.setProgress(0);

        startButton.setDisable(false);
        pauseButton.setDisable(true);
        resumeButton.setDisable(true);
        cancelButton.setDisable(true);

        chooseDirectoryButton.setDisable(false);
        socketCountSlider.setDisable(false);
    }

    private void updateComponentsDefault() {
        updateComponentsCancelled();
    }
}
