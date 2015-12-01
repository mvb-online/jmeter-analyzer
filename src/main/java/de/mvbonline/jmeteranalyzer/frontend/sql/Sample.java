package de.mvbonline.jmeteranalyzer.frontend.sql;

/**
 * Created by mholz on 24.11.2015.
 */
public class Sample {

    private long timestamp;

    private String name;

    private int success;

    private String responseCode;

    private String responseMessage;

    private int duration;

    private int responseSize;

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSuccess() {
        return success;
    }

    public void setSuccess(int success) {
        this.success = success;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        if(duration == 0) {
            duration = Integer.MAX_VALUE;
        }
        this.duration = duration;
    }

    public int getResponseSize() {
        return responseSize;
    }

    public void setResponseSize(int responseSize) {
        this.responseSize = responseSize;
    }
}
