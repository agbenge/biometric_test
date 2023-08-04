package com.cihp.biometrictest;

public class FingerPrintUtility {

        public static String getDeviceErrors(int errorCode) {

            switch (errorCode) {

                case 1:
                    return "CREATION FAILED";
                case 2:
                    return "FUNCTION FAILED";
                case 3:
                    return "INVALID PARAM";
                case 4:
                    return "NOT USED";
                case 5:
                    return "DLL LOAD FAILED";
                case 6:
                    return "DLL LOAD FAILED DRV";
                case 7:
                    return "DLL LOAD FAILED ALGO";
                case 8:
                    return "No LONGER SUPPORTED";
                case 51:
                    return "SYS LOAD FAILED";
                case 52:
                    return "INITIALIZE FAILED";
                case 53:
                    return "LINE DROPPED";
                case 54:
                    return "TIME OUT";
                case 55:
                    return "DEVICE NOT FOUND";
                case 56:
                    return "Driver LOAD FAILED";
                case 57:
                    return "WRONG IMAGE";
                case 58:
                    return "LACK OF BANDWIDTH";
                case 59:
                    return "DEV ALREADY OPEN";
                case 60:
                    return "GET Serial Number FAILED";
                case 61:
                    return "UNSUPPORTED DEV";
                case 101:
                    return "FEAT NUMBER";
                case 102:
                    return "INVALID TEMPLATE TYPE";
                case 103:
                    return "INVALID TEMPLATE1";
                case 104:
                    return "INVALID TEMPLATE2";
                case 105:
                    return "EXTRACT FAIL";
                case 106:
                    return "MATCH FAIL";
                default:
                    return "";
            }
        }

}
