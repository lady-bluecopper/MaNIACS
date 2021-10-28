package it.isi.approxmni.utils;

/**
 *
 * @author giulia
 */
public class StopWatch {
    
    private long startTime = 0;
    private long stopTime = 0;
    private boolean running = false;

    public void start() {
        this.startTime = System.currentTimeMillis();
        this.running = true;
    }

    public void stop() {
        this.stopTime = System.currentTimeMillis();
        this.running = false;
    }
    
    //elaspsed time in milliseconds
    public long getElapsedTime() {
        if (running) {
            return System.currentTimeMillis() - startTime;
        }
        return stopTime - startTime;
    }

    //elaspsed time in seconds
    public double getElapsedTimeSecs() {
        if (running) {
            return (System.currentTimeMillis() - startTime) / 1000.0;
        }
        return (stopTime - startTime) / 1000.0;
    }
}