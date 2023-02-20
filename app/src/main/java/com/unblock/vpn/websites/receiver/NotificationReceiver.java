package com.unblock.vpn.websites.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.unblock.vpn.websites.services.DnsVpnService;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = NotificationReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        context.sendBroadcast(new Intent(DnsVpnService.DNS_STOPED));
        context.sendBroadcast(new Intent(DnsVpnService.DNS_STATE).putExtra("isRunning", false));
    }
}
