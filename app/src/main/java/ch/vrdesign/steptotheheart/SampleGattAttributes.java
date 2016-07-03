package ch.vrdesign.steptotheheart;

import java.util.HashMap;

/**
 * Created by vr on 27/06/16.
 */
public class SampleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    // 0x2A37
    public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    // 0x2A53
    public static String RUNNINGSPEEDANDCADENCE_MEASUREMENT = "00002a53-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    static {
        // Sample Services.
        attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service");
        attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Cadence Service");
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        // Sample Characteristics.
        attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
        attributes.put(RUNNINGSPEEDANDCADENCE_MEASUREMENT, "Running Speed and Cadence Measurement");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
