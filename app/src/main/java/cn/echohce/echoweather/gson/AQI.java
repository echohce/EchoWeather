package cn.echohce.echoweather.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by lin on 2016/12/17.
 */

public class AQI {
    public AQICity city;

    public class AQICity {

        public String aqi;

        @SerializedName("pm25")
        public String pm25;
    }
}
