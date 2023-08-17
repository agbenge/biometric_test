/*
 * Copyright (C) 2016 SecuGen Corporation
 *
 */

package com.cihp.biometrictest;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import java.nio.ByteBuffer;
import java.util.Arrays;

import SecuGen.FDxSDKPro.JSGFPLib;
import SecuGen.FDxSDKPro.SGFDxDeviceName;
import SecuGen.FDxSDKPro.SGFDxErrorCode;
import SecuGen.FDxSDKPro.SGFDxTemplateFormat;
import SecuGen.FDxSDKPro.SGFingerInfo;
import SecuGen.FDxSDKPro.SGImpressionType;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener, Runnable {

    private static final String TAG = "SecuGen USB";
    private static final int IMAGE_CAPTURE_TIMEOUT_MS = 1000000;
    private static final int IMAGE_CAPTURE_QUALITY = 50;
    private Button mButtonRegister;

    private PendingIntent mPermissionIntent;
    private ImageView mImageViewFingerprint;

    private byte[] mRegisterImage;
    private byte[] mRegisterTemplate;
    private int[] mMaxTemplateSize;
    private int mImageWidth;
    private int mImageHeight;
    private int mImageDPI;
    private String mDeviceSN;

    private Bitmap grayBitmap;
    private IntentFilter filter;
    private boolean bSecuGenDeviceOpened;
    private JSGFPLib sgfplib;
    private boolean usbPermissionRequested;

    private String[] permissions = {
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            android. Manifest.permission.CAMERA,
    };


    public void capturePrint() {
        mRegisterImage = null;
            mRegisterImage = new byte[mImageWidth * mImageHeight];
            long result = sgfplib.GetImageEx(mRegisterImage, IMAGE_CAPTURE_TIMEOUT_MS,IMAGE_CAPTURE_QUALITY);
            if (result != 0L) {
                String errorMsg = FingerPrintUtility.getDeviceErrors((int) result);
                CustomDebug(errorMsg, false);
                return;
            }

            mImageViewFingerprint.setImageBitmap(this.toGrayscale(mRegisterImage));
            result = sgfplib.SetTemplateFormat(SGFDxTemplateFormat.TEMPLATE_FORMAT_ISO19794);
        Log.d(TAG , "SetTemplateFormat "+result);
            int[] quality1 = new int[1];
            result = sgfplib.GetImageQuality(mImageWidth, mImageHeight, mRegisterImage, quality1);
        Log.d(TAG , "GetImageQuality "+result);
            SGFingerInfo fpInfo = new SGFingerInfo();
            fpInfo.FingerNumber = 1;
            fpInfo.ImageQuality = quality1[0];
            fpInfo.ImpressionType = SGImpressionType.SG_IMPTYPE_LP;
            fpInfo.ViewNumber = 1;


     Arrays.fill(mRegisterTemplate, (byte) 0);


        result = sgfplib.CreateTemplate(fpInfo, mRegisterImage, mRegisterTemplate);
        int imageQuality=fpInfo.ImageQuality;
     Log.d(TAG , "CreateTemplate "+result);
            int[] size = new int[1];
            result = sgfplib.GetTemplateSize(mRegisterTemplate, size);
        Log.d(TAG , "GetTemplateSize "+result);
            String template = Base64.encodeToString(mRegisterTemplate, Base64.DEFAULT);
            if (template != null ) {
String res="Capture  ImageQuality: "+imageQuality +"\nmImageDPI: "+mImageDPI
        +"\nmImageHeight: "+mImageHeight
        +"\nmImageWidth: "+mImageWidth+
        "\nmDeviceSN: "+mDeviceSN;
                CustomDebug(res, false);
                Log.d(TAG , "R "+res);
            } else {
             //   CustomDebug("Failed to capture ", false);
            }

        }




    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createViewObject();
        checkAndRequestPermissions();
        mMaxTemplateSize = new int[1];
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
        filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(ACTION_USB_PERMISSION); //USB Permissions
       //lastest imp of sdk sgfplib = new JSGFPLib(this,(UsbManager) getSystemService(Context.USB_SERVICE));

        sgfplib = new JSGFPLib((UsbManager) getSystemService(Context.USB_SERVICE));

        bSecuGenDeviceOpened = false;
        usbPermissionRequested = false;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onPause() {
        Log.d(TAG, "Enter onPause()");
        if (bSecuGenDeviceOpened) {
            //autoOn.stop();
            EnableControls();
            sgfplib.CloseDevice();
            bSecuGenDeviceOpened = false;
        }
        unregisterReceiver(mUsbReceiver);
        mRegisterImage = null;
        mRegisterTemplate = null;
        mImageViewFingerprint.setImageBitmap(grayBitmap);
        super.onPause();
        Log.d(TAG, "Exit onPause()");
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onResume() {
        Log.d(TAG, "Enter onResume()");
        super.onResume();
        DisableControls();
        registerReceiver(mUsbReceiver, filter);
        openUSBDevice(true);
        Log.d(TAG, "Exit onResume()");
    }
    boolean loadingBiometric;
    private void openUSBDevice(boolean isResume) {
        Log.d(TAG, "Enter isResume  "+isResume);
        loadingBiometric=true;
        long error = sgfplib.Init(SGFDxDeviceName.SG_DEV_AUTO);
        if (error != SGFDxErrorCode.SGFDX_ERROR_NONE) {
            Snackbar.make(mButtonRegister, "Plug in the SecuGen device properly", Snackbar.LENGTH_LONG).show();
            loadingBiometric=false;
            // Loading stop here
        } else {
            UsbDevice usbDevice = sgfplib.GetUsbDevice();
            if (usbDevice == null) {
                AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
                dlgAlert.setMessage("SecuGen fingerprint sensor not found!");
                dlgAlert.setTitle("SecuGen Fingerprint SDK");
                dlgAlert.setPositiveButton("OK",
                        (dialog, whichButton) -> finish()
                );
                dlgAlert.setCancelable(false);
                dlgAlert.create().show();
                loadingBiometric=false;
            } else {
                boolean hasPermission = sgfplib.GetUsbManager().hasPermission(usbDevice);
                if (hasPermission) {
                    Log.d(TAG,"hasPermission "+hasPermission);
                    openSecuGen(usbDevice);// open the device
                } else
                {
                    // request permission if permission is not already requested
                    if (usbPermissionRequested) { ;
                        Toast.makeText(this, "Waiting for USB Permission", Toast.LENGTH_SHORT).show();
                    } else {
                        usbPermissionRequested = true;
                        Log.d(TAG," Requesting permission  ");
                        sgfplib.GetUsbManager().requestPermission(usbDevice, mPermissionIntent);
                    }
                    loadingBiometric=false;
                }

            }
        }
    }

    private void checkAndRequestPermissions() {
            ActivityCompat.requestPermissions(this,
                   permissions,
                    0);

    }
    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onDestroy() {
        Log.d(TAG, "Enter onDestroy()");
        sgfplib.CloseDevice();
        mRegisterImage = null;
        mRegisterTemplate = null;
        sgfplib.Close();
        super.onDestroy();
        Log.d(TAG, "Exit onDestroy()");
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////
    //Converts image to grayscale (NEW)
    public Bitmap toGrayscale(byte[] mImageBuffer) {
        byte[] Bits = new byte[mImageBuffer.length * 4];
        for (int i = 0; i < mImageBuffer.length; i++) {
            Bits[i * 4] = Bits[i * 4 + 1] = Bits[i * 4 + 2] = mImageBuffer[i]; // Invert the source bits
            Bits[i * 4 + 3] = -1;// 0xff, that's the alpha.
        }

        Bitmap bmpGrayscale = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.ARGB_8888);
        bmpGrayscale.copyPixelsFromBuffer(ByteBuffer.wrap(Bits));
        return bmpGrayscale;
    }



    @Override
    public void run() {
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////
    public void EnableControls() {

        this.mButtonRegister.setClickable(true);
        this.mButtonRegister.setTextColor(getResources().getColor(android.R.color.holo_green_dark));

    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////
    public void DisableControls() {

        this.mButtonRegister.setClickable(false);
        this.mButtonRegister.setTextColor(getResources().getColor(android.R.color.black));

    }

    private void CustomDebug(String s, boolean finishOnOk) {
        AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
        dlgAlert.setMessage(s);
        dlgAlert.setTitle("Patient's Biometric Capture");
        dlgAlert.setPositiveButton("OK",
                (dialog, whichButton) -> {
                    if(finishOnOk){
                        finish();
                    }
                }
        );
        dlgAlert.setCancelable(false);
        dlgAlert.create().show();
    }





    public void createViewObject(){

        //Changing selected Image View
        mImageViewFingerprint = (ImageView) findViewById(R.id.fingerPrintImage);

        mButtonRegister = (Button) findViewById(R.id.buttonRegister);
        mButtonRegister.setOnClickListener( this);

        int[] grayBuffer = new int[JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES * JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES];
        Arrays.fill(grayBuffer, Color.GRAY);

        grayBitmap = Bitmap.createBitmap(JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES, JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES, Bitmap.Config.ARGB_8888);
        grayBitmap.setPixels(grayBuffer, 0, JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES, 0, 0, JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES, JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES);
        mImageViewFingerprint.setImageBitmap(grayBitmap);

        int[] sintbuffer = new int[(JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES / 2) * (JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES / 2)];
        Arrays.fill(sintbuffer, Color.GRAY);

        Bitmap sb = Bitmap.createBitmap(JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES / 2, JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES / 2, Bitmap.Config.ARGB_8888);
        sb.setPixels(sintbuffer, 0, JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES / 2, 0, 0, JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES / 2, JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES / 2);

    }




    private  void  openSecuGen(UsbDevice device){
        boolean hasPermission = sgfplib.GetUsbManager().hasPermission(device);
        if (hasPermission) {

            long        error = sgfplib.OpenDevice(0);
            if (error == SGFDxErrorCode.SGFDX_ERROR_NONE) {
                bSecuGenDeviceOpened = true;
                SecuGen.FDxSDKPro.SGDeviceInfoParam deviceInfo = new SecuGen.FDxSDKPro.SGDeviceInfoParam();
                error = sgfplib.GetDeviceInfo(deviceInfo);


                mImageWidth = deviceInfo.imageWidth;
                mImageHeight = deviceInfo.imageHeight;
                mImageDPI = deviceInfo.imageDPI;
                mDeviceSN = new String(deviceInfo.deviceSN());




                sgfplib.SetTemplateFormat(SGFDxTemplateFormat.TEMPLATE_FORMAT_ISO19794);
                sgfplib.GetMaxTemplateSize(mMaxTemplateSize);
                mRegisterTemplate = new byte[(int) mMaxTemplateSize[0]];
                loadingBiometric=false;
                EnableControls();
            } else {

            }



        }

        loadingBiometric=false;

    }

    private static final String ACTION_USB_PERMISSION = "org.openmrs.mobile.activities.USB_PERMISSION";//broadcast const listen to if permission is sent
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction(); //retrieve action constant passed in the filter.
            if (ACTION_USB_PERMISSION.equals(action)) {
                /* This is not return here and the activity paused  during permission request and resume at onResume()
                          Hence   openSecuGen(device); will not called here...
                          code not comment for device indifference
                             */
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {

                            openSecuGen(device);
                            Log.d(TAG, "Opening SecuGen device");

                        } else {
                            Log.e(TAG, "mUsbReceiver.onReceive() Device is null");
                        }
                    } else
                        Log.e(TAG, "mUsbReceiver.onReceive() permission denied for device " + device);
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                Log.e(TAG,"USB device connected");
                if(!loadingBiometric ) {
                    Toast.makeText(context, "USB device connected: Opening", Toast.LENGTH_LONG).show();
                    // if biometric is not in process of opening open it or if it is not already opened
                    openUSBDevice(false);
                } else Toast.makeText(context, "USB device connected: Please wait and reconnect", Toast.LENGTH_LONG).show();
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                bSecuGenDeviceOpened = false;// turn off device connection flag
                usbPermissionRequested = false;// turn off permission requested
                DisableControls();// disable capture  button
                Toast.makeText(context, "USB device disconnected", Toast.LENGTH_SHORT).show();// notify the user
                Log.e(TAG,"USB device disconnected");
            }
        }
    };


    @Override
    public void onClick(View view) {
        capturePrint();
    }
}