package com.sos.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.telephony.SmsManager;
import android.location.LocationManager;
import android.location.Location;
import android.Manifest;
import android.content.pm.PackageManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri; 

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // پرمیشن لسٹ
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                                   Manifest.permission.SEND_SMS, 
                                   Manifest.permission.ACCESS_FINE_LOCATION,
                                   Manifest.permission.ACCESS_COARSE_LOCATION,
                                   Manifest.permission.READ_PHONE_STATE,
                                   Manifest.permission.CALL_PHONE 
                               }, 1);
        }

        // ریسیورز کو رجسٹر کرنا
        registerReceiver(smsSentReceiver, new IntentFilter("SMS_SENT_ACTION"));
        registerReceiver(networkReceiver, new IntentFilter("android.intent.action.SERVICE_STATE"));
        registerReceiver(networkReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));

        Button btnSOS = (Button) findViewById(R.id.btnSOS);
        Button btnFamily = (Button) findViewById(R.id.btnFamily);
        Button btnSettings = (Button) findViewById(R.id.btnSettings);

        // SOS بٹن کا ایکشن (اب یہ سیٹنگز والے نمبر پر SMS بھیجے گا اور 1122 ڈائل کرے گا)
        btnSOS.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // سیٹنگز سے نمبر اٹھانا
                    String savedNum = getSharedPreferences("SOS_PREFS", MODE_PRIVATE).getString("SOS_NUM", "");

                    if(!savedNum.isEmpty()){
                        String currentLocLink = getDeviceLocation();
                        String fullMessage = "SOS ALERT! Meri madad karein! Location: " + currentLocLink;

                        // سیٹنگز والے نمبر پر ہی ایس ایم ایس جائے گا
                        sendSecureSms(savedNum, fullMessage);
                        Toast.makeText(MainActivity.this, "SOS Action Triggered!", Toast.LENGTH_SHORT).show();

                        // ساتھ ہی 1122 کا ایکشن چلے گا
                        makeEmergencyCall("1122");
                    } else {
                        Toast.makeText(MainActivity.this, "Settings mein number save karein", Toast.LENGTH_SHORT).show();
                    }
                }
            });

        // فیملی بٹن کا ایکشن (صرف سیٹنگز والے نمبر پر SMS)
        btnFamily.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String savedNum = getSharedPreferences("SOS_PREFS", MODE_PRIVATE).getString("SOS_NUM", "");
                    if(!savedNum.isEmpty()){
                        String currentLocLink = getDeviceLocation();
                        String fullMessage = "FAMILY ALERT! Meri madad karein! Location: " + currentLocLink;
                        sendSecureSms(savedNum, fullMessage);
                        Toast.makeText(MainActivity.this, "Family Alert Sent!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Settings mein number save karein", Toast.LENGTH_SHORT).show();
                    }
                }
            });

        // سیٹنگز بٹن کا ایکشن
        btnSettings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                }
            });
    }

    // خودکار ڈائریکٹ کال یا ڈائلر کھولنے کا فنکشن
    private void makeEmergencyCall(String targetNumber) {
        try {
            if (checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:" + targetNumber));
                startActivity(callIntent);
            } else {
                Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                dialIntent.setData(Uri.parse("tel:" + targetNumber));
                startActivity(dialIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // لوکیشن نکالنے کا فنکشن
    private String getDeviceLocation() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location l = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (l == null) {
                l = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            if (l != null) {
                return "https://maps.google.com/?q=" + l.getLatitude() + "," + l.getLongitude();
            }
        }
        return "(Location hidden or GPS is OFF)";
    }

    // میسج بھیجنے اور فیل ہونے پر ٹریک کرنے کا سمارٹ فنکشن
    private void sendSecureSms(String phoneNum, String messageContent) {
        try {
            SmsManager sms = SmsManager.getDefault();

            Intent sentIntent = new Intent("SMS_SENT_ACTION");
            sentIntent.putExtra("pending_phone", phoneNum);
            sentIntent.putExtra("pending_msg", messageContent);

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                flags |= 0x04000000; 
            }

            PendingIntent sentPI = PendingIntent.getBroadcast(this, phoneNum.hashCode(), sentIntent, flags);
            sms.sendTextMessage(phoneNum, null, messageContent, sentPI, null);
        } catch (Exception e) {
            saveFailedSms(phoneNum, messageContent);
            Toast.makeText(MainActivity.this, "Network Issue! Message saved for auto-retry.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    // میسج کا سٹیٹس چیک کرنے والا ریسیور
    private final BroadcastReceiver smsSentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String phone = intent.getStringExtra("pending_phone");
            String msg = intent.getStringExtra("pending_msg");

            if (getResultCode() == Activity.RESULT_OK) {
                clearPendingSms();
                Toast.makeText(context, "SOS Delivered Successfully!", Toast.LENGTH_SHORT).show();
            } else {
                saveFailedSms(phone, msg);
                Toast.makeText(context, "Signal nahi hain! Signal aate hi app khud message bhej degi.", Toast.LENGTH_LONG).show();
            }
        }
    };

    // سگنل یا انٹرنیٹ تبدیل ہونے پر نظر رکھنے والا ریسیور
    private final BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkAndResendPendingSms();
        }
    };

    // فیل میسج کو سیو کرنے کا طریقہ
    private void saveFailedSms(String phone, String msg) {
        SharedPreferences.Editor editor = getSharedPreferences("SOS_PREFS", MODE_PRIVATE).edit();
        editor.putString("FAILED_NUM", phone);
        editor.putString("FAILED_MSG", msg);
        editor.putBoolean("IS_PENDING", true);
        editor.apply();
    }

    // میسج سینڈ ہونے پر پینڈنگ سٹیٹس ختم کرنے کا طریقہ
    private void clearPendingSms() {
        SharedPreferences.Editor editor = getSharedPreferences("SOS_PREFS", MODE_PRIVATE).edit();
        editor.putBoolean("IS_PENDING", false);
        editor.apply();
    }

    // رکے ہوئے میسج کو خودکار دوبارہ فائر کرنے کا لاجک
    private void checkAndResendPendingSms() {
        SharedPreferences prefs = getSharedPreferences("SOS_PREFS", MODE_PRIVATE);
        boolean isPending = prefs.getBoolean("IS_PENDING", false);

        if (isPending) {
            String phone = prefs.getString("FAILED_NUM", "");
            String msg = prefs.getString("FAILED_MSG", "");

            if (!phone.isEmpty() && !msg.isEmpty()) {
                sendSecureSms(phone, msg);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(smsSentReceiver);
            unregisterReceiver(networkReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

