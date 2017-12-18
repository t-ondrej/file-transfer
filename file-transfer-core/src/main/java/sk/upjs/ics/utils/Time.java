package sk.upjs.ics.utils;

/**
 * Created by Tomas on 18.12.2017.
 */
public final class Time {

    private Time() {
        // Placeholder
    }

    public static String fromSeconds(int totalSecs) {
        int minutes = (totalSecs % 3600) / 60;
        int seconds = totalSecs % 60;

        return String.format("%02d:%02d", minutes, seconds);
    }
}
