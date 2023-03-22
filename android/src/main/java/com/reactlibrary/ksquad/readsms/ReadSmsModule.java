package com.reactlibrary.ksquad.readsms;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.database.Cursor;
import android.net.Uri;

import androidx.core.content.ContextCompat;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ReadSmsModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    private BroadcastReceiver msgReceiver;

    public ReadSmsModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "ReadSms";
    }

    @ReactMethod
    public void stopReadSMS() {
        try {
            if (reactContext != null && msgReceiver != null) {
                reactContext.unregisterReceiver(msgReceiver);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @ReactMethod
    public void startReadSMS(final Callback success, final Callback error) {
        try {
            if (ContextCompat.checkSelfPermission(reactContext, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(reactContext, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
                msgReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                                .emit("received_sms", getMessageFromMessageIntent(intent));
                    }
                };
                String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";
                reactContext.registerReceiver(msgReceiver, new IntentFilter(SMS_RECEIVED_ACTION));
                success.invoke("Start Read SMS successfully");
            } else {
                // Permission has not been granted
                error.invoke("Required RECEIVE_SMS and READ_SMS permission");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getMessageFromMessageIntent(Intent intent) {
        final Bundle bundle = intent.getExtras();
        String message = "";
        try {
            if (bundle != null) {
                final Object[] pdusObj = (Object[]) bundle.get("pdus");
                if (pdusObj != null) {
                    for (Object aPdusObj : pdusObj) {
                        SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) aPdusObj);
                        message = currentMessage.getDisplayMessageBody();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return message;
    }

    @ReactMethod
    public void list(String filter, final Callback errorCallback, final Callback successCallback) {
        try {
            JSONObject filterJ = new JSONObject(filter);
            String uri_filter = filterJ.has("box") ? filterJ.optString("box") : "inbox";
            int fread = filterJ.has("read") ? filterJ.optInt("read") : -1;
            int fid = filterJ.has("_id") ? filterJ.optInt("_id") : -1;
            String faddress = filterJ.optString("address");
            String fcontent = filterJ.optString("body");
            int indexFrom = filterJ.has("indexFrom") ? filterJ.optInt("indexFrom") : 0;
            int maxCount = filterJ.has("maxCount") ? filterJ.optInt("maxCount") : -1;
            Cursor cursor = getCurrentActivity().getContentResolver().query(Uri.parse("content://sms/" + uri_filter),
                    null, "", null, null);
            int c = 0;
            JSONArray jsons = new JSONArray();
            while (cursor.moveToNext()) {
                boolean matchFilter = false;
                if (fid > -1)
                    matchFilter = fid == cursor.getInt(cursor.getColumnIndex("_id"));
                else if (fread > -1)
                    matchFilter = fread == cursor.getInt(cursor.getColumnIndex("read"));
                else if (faddress.length() > 0)
                    matchFilter = faddress.equals(cursor.getString(cursor.getColumnIndex("address")).trim());
                else if (fcontent.length() > 0)
                    matchFilter = fcontent.equals(cursor.getString(cursor.getColumnIndex("body")).trim());
                else {
                    matchFilter = true;
                }
                if (matchFilter) {
                    if (c >= indexFrom) {
                        if (maxCount > 0 && c >= indexFrom + maxCount)
                            break;
                        c++;
                        // Long dateTime =
                        // Long.parseLong(cursor.getString(cursor.getColumnIndex("date")));
                        // String message = cursor.getString(cursor.getColumnIndex("body"));
                        JSONObject json;
                        json = getJsonFromCursor(cursor);
                        jsons.put(json);

                    }
                }

            }
            cursor.close();
            try {
                successCallback.invoke(c, jsons.toString());
            } catch (Exception e) {
                errorCallback.invoke(e.getMessage());
            }
        } catch (JSONException e) {
            errorCallback.invoke(e.getMessage());
            return;
        }
    }

    private JSONObject getJsonFromCursor(Cursor cur) {
        JSONObject json = new JSONObject();

        int nCol = cur.getColumnCount();
        String[] keys = cur.getColumnNames();
        try {
            for (int j = 0; j < nCol; j++)
                switch (cur.getType(j)) {
                    case 0:
                        json.put(keys[j], null);
                        break;
                    case 1:
                        json.put(keys[j], cur.getLong(j));
                        break;
                    case 2:
                        json.put(keys[j], cur.getFloat(j));
                        break;
                    case 3:
                        json.put(keys[j], cur.getString(j));
                        break;
                    case 4:
                        json.put(keys[j], cur.getBlob(j));
                }
        } catch (Exception e) {
            return null;
        }

        return json;
    }
}
