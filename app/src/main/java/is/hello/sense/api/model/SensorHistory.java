package is.hello.sense.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SensorHistory extends ApiResponse {
    @JsonProperty("value")
    private double value;

    @JsonProperty("datetime")
    private long time;

    @JsonProperty("offset_millis")
    private long offset;


    public double getValue() {
        return value;
    }

    public long getTime() {
        return time;
    }

    public long getOffset() {
        return offset;
    }


    @Override
    public String toString() {
        return "SensorHistory{" +
                "value=" + value +
                ", time=" + time +
                ", offset=" + offset +
                '}';
    }
}
