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
import sk.upjs.ics.FileClient;
import sk.upjs.ics.utils.Time;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Tomas on 9.12.2017.
 */
public class ClientSceneController implements Initializable {


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

    @Inject
    public ClientSceneController(FileClient fileClient) {
        this.fileClient = fileClient;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        prepareComponents();
        progressBar.setProgress(fileClient.getProgress());
        addTimerSchedule();
    }

    public void setCurrentStage(Stage currentStage) {
        this.currentStage = currentStage;
    }

    @FXML
    private void onStartButtonClicked() {
        resumeButton.setDisable(true);
        pauseButton.setDisable(false);
        cancelButton.setDisable(false);

        int socketCount = (int)socketCountSlider.getValue();

        socketCountSlider.setDisable(true);

        System.out.println("GUI: added timer");

        Service<Boolean> downloadService = new Service<Boolean>() {
            protected Task<Boolean> createTask() {
                return new Task<Boolean>() {
                    protected Boolean call() throws Exception {
                        return fileClient.startDownloading(socketCount, directoryLabel.getText());
                    }
                };
            }
        };

        System.out.println("GUI: created service");

        downloadService.setOnSucceeded((event) -> {
            timer.cancel();

            //if ((boolean) event.getSource().getValue())
        });

      //  downloadService.set

        System.out.println("GUI: onSucceeded set");

        downloadService.start();
    }

    @FXML
    private void onPauseButtonClicked() {
        timer.cancel();
        pauseButton.setDisable(true);
        resumeButton.setDisable(false);

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
        pauseButton.setDisable(false);
        cancelButton.setDisable(false);
        resumeButton.setDisable(true);

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
        timer.cancel();

        new Service<Void>() {
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    protected Void call() throws Exception {
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
                Platform.runLater(() -> {
                    progressBar.setProgress(fileClient.getProgress());
                    timeLabel.setText(Time.fromSeconds(fileClient.addAndGetElapsed()));
                });
            }
        }, 0, 1000);
    }

    private void prepareComponents() {
        if (fileClient.resumeAvailable()) {
            resumeButton.setDisable(false);
            directoryLabel.setText(fileClient.getFileInfo().getDirectory());
            chooseDirectoryButton.setDisable(true);
            timeLabel.setText(Time.fromSeconds(fileClient.addAndGetElapsed() - 1));
        }
    }
}
