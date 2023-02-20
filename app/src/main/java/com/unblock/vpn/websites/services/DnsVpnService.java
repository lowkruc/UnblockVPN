package com.unblock.vpn.websites.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import com.unblock.vpn.websites.MainActivity;
import com.unblock.vpn.websites.R;
import com.unblock.vpn.websites.receiver.NotificationReceiver;
import com.unblock.vpn.websites.thread.VPNRunnable;
import com.unblock.vpn.websites.util.BitsUtil;
import com.unblock.vpn.websites.util.ITrafficSpeedListener;
import com.unblock.vpn.websites.util.TrafficSpeedMeasurer;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DnsVpnService extends VpnService{
    public static final String DNS_STATE = "UNBLOCK_DNS_STATE";
    public static final String DNS_STOPED = "UNBLOCK_DNS_STOPED";

    private static final String ACTION_STOP = "com.thecodeprogram.themusic.ACTION.STOP";
    private static final String ACTION_OPEN_BROWSER = "com.thecodeprogram.themusic.ACTION.OPEN_BROWSER";
    private static final String NOTIFICATION_CHANNEL_ID = "com.unblock.vpn.websites";
    private static final String channelName = "UnblockVPN Services";
    private static final int NOTIFICATION_ID = 1333;

    private static final String TAG = DnsVpnService.class.getSimpleName();

    private ParcelFileDescriptor vpnInterface = null;
    private ExecutorService executorService;
    private VPNRunnable vpnRunnable;
    private final Handler handler = new Handler();
    private Notification notification;
    private RemoteViews mViewNotif;

    private NotificationManager notificationManager;
    private NotificationChannel notificationChannel;
    private Notification.Builder notificationBuilder;


    @Override
    public void onCreate() {
        super.onCreate();
        registerReceiver(stopBroadcast, new IntentFilter(DNS_STOPED));
        startVpn();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sendNotification();
        return START_STICKY;

    }

    private void sendNotification() {
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_FROM_BACKGROUND);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mViewNotif = new RemoteViews(getPackageName(), R.layout.notification_layout);

        Intent disIntent = new Intent(this, NotificationReceiver.class);
        PendingIntent disPendingIntent = PendingIntent.getBroadcast(this, 0, disIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(getResources().getColor(R.color.primaryColorVariant));
            notificationChannel.enableVibration(false);
            notificationManager.createNotificationChannel(notificationChannel);
            notificationBuilder = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle(getString(R.string.connected_notif))
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setContentText(getString(R.string.info_notif))
                    .setPriority(Notification.PRIORITY_MAX)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .addAction(new Notification.Action(R.drawable.ic_on_off_button, getString(R.string.disconnect_action), disPendingIntent))
                    .setContentIntent(pendingIntent);
            notificationManager.notify(NOTIFICATION_ID , notificationBuilder.build());
        }else {
            notification = new NotificationCompat.Builder(this)
                    .setContentTitle(getString(R.string.connected_notif))
                    .setContentText(getString(R.string.info_notif))
                    .setAutoCancel(false)
                    .setSmallIcon(R.drawable.ic_notification)
                    .addAction(new NotificationCompat.Action(R.drawable.ic_notification, getString(R.string.disconnect_action), disPendingIntent))
                    .setContentIntent(pendingIntent).build();

            notificationManager.notify(NOTIFICATION_ID , notification);
        }
    }


    public void startVpn(){
        if(setupVPN()){
            sendBroadcast(new Intent(DNS_STATE).putExtra("running", true));
            vpnRunnable = new VPNRunnable(vpnInterface);
            executorService = Executors.newFixedThreadPool(1);
            executorService.submit(vpnRunnable);
            sendNotification();
        }
    }

    private boolean setupVPN() {
        try {
            if(vpnInterface == null){
                Builder builder = new Builder();
                builder.addAddress("172.31.255.250", 30);
                builder.addDnsServer("8.8.8.8");
                builder.addDnsServer("8.8.4.4");
                builder.addDnsServer("2001:4860:4860::8888");
                builder.addDnsServer("2001:4860:4860::8844");

                Intent configure = new Intent(this, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, configure, PendingIntent.FLAG_UPDATE_CURRENT);
                builder.setConfigureIntent(pendingIntent);
                vpnInterface = builder.setSession(getString(R.string.app_name_short)).establish();
            }
            return true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    private final BroadcastReceiver stopBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent == null || intent.getAction() == null){
                return;
            }

            if(DNS_STOPED.equals(intent.getAction())){
                onRevoke();
                stopVpn();
            }
        }
    };

    private void stopVpn() {
        if(vpnRunnable !=null) {
            vpnRunnable.stop();
        }
        if(vpnInterface !=null) {
            try {
                vpnInterface.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        vpnInterface = null;
        vpnRunnable = null;
        executorService = null;
        sendBroadcast(new Intent(DNS_STATE).putExtra("running", false));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopVpn();
        notificationManager.cancel(NOTIFICATION_ID);
        notificationManager = null;
        notificationBuilder = null;
        unregisterReceiver(stopBroadcast);
    }
}
