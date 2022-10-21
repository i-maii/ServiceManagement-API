package com.example.servicemanagement.constant;

import java.util.Arrays;
import java.util.List;

public class Constant {
    public static List<Integer> MOST_PRIORITY = Arrays.asList(1, 2);
    public static List<Integer> ALL_PRIORITY = Arrays.asList(1, 2, 3);

    public static String KEY_TOTAL_REQUEST_HOUR = "KEY_TOTAL_REQUEST_HOUR";
    public static String KEY_TOTAL_TARGET_HOUR = "KEY_TOTAL_TARGET_HOUR";
    public static String KEY_TECHNICIAN_TARGET_HOUR_1 = "KEY_TECHNICIAN_TARGET_HOUR_1";
    public static String KEY_TECHNICIAN_TARGET_HOUR_2 = "KEY_TECHNICIAN_TARGET_HOUR_2";
    public static String KEY_TECHNICIAN_TARGET_HOUR_3 = "KEY_TECHNICIAN_TARGET_HOUR_3";
    public static String KEY_USAGE_TECHNICIAN = "KEY_USAGE_TECHNICIAN";
    public static String KEY_PRIORITY_HOUR_MIN = "KEY_PRIORITY_HOUR_MIN";
    public static String KEY_PRIORITY_HOUR_MAX = "KEY_PRIORITY_HOUR_MAX";
    public static String KEY_LOWEST_PRIORITY_HOUR_MIN = "KEY_LOWEST_PRIORITY_HOUR_MIN";
    public static String KEY_LOWEST_PRIORITY_HOUR_MAX = "KEY_LOWEST_PRIORITY_HOUR_MAX";
    public static String KEY_TOTAL_PRIORITY_HOUR = "KEY_TOTAL_PRIORITY_HOUR";
    public static String KEY_LOWEST_TOTAL_PRIORITY_HOUR = "KEY_LOWEST_TOTAL_PRIORITY_HOUR";
    public static String KEY_LOWEST_TOTAL_REQUEST_HOUR = "KEY_LOWEST_TOTAL_REQUEST_HOUR";

    public static String STATUS_READY_FOR_ESTIMATION = "READY FOR ESTIMATION";
    public static String STATUS_READY_FOR_PLAN = "READY FOR PLAN";
    public static String STATUS_READY_TO_SERVICE = "READY TO SERVICE";
    public static String STATUS_DONE = "DONE";

    public static String ERR_INSERT_DUPLICATE_TENANT = "ERR_INSERT_DUPLICATE_TENANT";
    public static String ERR_UPDATE_INVALID_TENANT = "ERR_UPDATE_INVALID_TENANT";
    public static String ERR_INSERT_DUPLICATE_REQUEST_TYPE = "ERR_INSERT_DUPLICATE_REQUEST_TYPE";
    public static String ERR_UPDATE_INVALID_REQUEST_TYPE = "ERR_UPDATE_INVALID_REQUEST_TYPE";
    public static String ERR_INSERT_DUPLICATE_APARTMENT = "ERR_INSERT_DUPLICATE_APARTMENT";
    public static String ERR_UPDATE_INVALID_APARTMENT = "ERR_UPDATE_INVALID_APARTMENT";
}
