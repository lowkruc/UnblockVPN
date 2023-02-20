package com.unblock.vpn.websites.fragments;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.unblock.vpn.websites.MainActivity;
import com.unblock.vpn.websites.R;
import com.unblock.vpn.websites.ads.SmallAdsTemplate;
import com.unblock.vpn.websites.services.DnsVpnService;
import com.unblock.vpn.websites.util.BitsUtil;
import com.unblock.vpn.websites.util.ITrafficSpeedListener;
import com.unblock.vpn.websites.util.RippleBackground;
import com.unblock.vpn.websites.util.TrafficSpeedMeasurer;

import java.util.Locale;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.NOTIFICATION_SERVICE;

public class MainFragment extends Fragment{

    private static TrafficSpeedMeasurer trafficSpeedMeasurer;
    private TextView downloadSpeed, uploadSpeed, downloadSize, uploadSize, textButton, statusConnected, timerConnection;
    private LinearLayout speedTrafic, infoStart;
    private RippleBackground buttonVpnStart, buttonVpnStop;
    private ImageView imageBtnIcon;
    private FrameLayout adFrame;
    private static final boolean SHOW_SPEED_IN_BITS = false;
    private static final int VPN_REQUEST_CODE = 0x0F;
    private Handler handler;
    private int iTime = 0;
    private final boolean isRunning = false;

    private InterstitialAd mInterstitialAd;

    private SmallAdsTemplate smallAdsTemplate;
    private AdRequest adRequest;

    public static MainFragment newInstance() {
        MainFragment fragment = new MainFragment();
        Bundle args = new Bundle();
        args.putBoolean("isRunning", false);
        fragment.setArguments(args);
        return fragment;
    }

    public void adLoad(){
        AdLoader adLoader = new AdLoader.Builder(getContext(), getResources().getString(R.string.nativAds))
                .forUnifiedNativeAd(new UnifiedNativeAd.OnUnifiedNativeAdLoadedListener() {
                    @Override
                    public void onUnifiedNativeAdLoaded(UnifiedNativeAd unifiedNativeAd) {
                        adFrame.setVisibility(View.VISIBLE);
                        smallAdsTemplate.setupNativeAd(unifiedNativeAd);
                    }
                })
                .withAdListener(new AdListener(){
                    @Override
                    public void onAdFailedToLoad(int i) {
                        super.onAdFailedToLoad(i);
                        Log.d("Load Failed", String.valueOf(i));
                    }

                    @Override
                    public void onAdOpened() {
                        super.onAdOpened();
                        Log.d("Load Finish","Finish");
                    }
                })
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        .build())
                .build();
        adRequest = new AdRequest.Builder().build();
        adLoader.loadAd(adRequest);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            getArguments().putBoolean("isRunning", false);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        registerBroadcast();
        Log.d("SaveInstace onAttach", ""+getArguments().getBoolean("isRunning"));
        if(trafficSpeedMeasurer == null){
            setupMeter();
        }

    }

    @Override
    public void onDetach() {
        super.onDetach();
        unregisterBroadcast();
    }

    private void setupMeter(){
        trafficSpeedMeasurer = new TrafficSpeedMeasurer(TrafficSpeedMeasurer.TrafficType.ALL);
        trafficSpeedMeasurer.startMeasuring();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        setupMeter();
        setupView(view);
        handler = new Handler();
        if(isMyServiceRunning(DnsVpnService.class)){
            registerBroadcast();
            buttonVpnStop.setVisibility(View.VISIBLE);
            speedTrafic.setVisibility(View.VISIBLE);
            statusConnected.setText(R.string.connected_status_text);
            infoStart.setVisibility(View.GONE);
            setRippleStopButton();
        }else{
            buttonVpnStart.setVisibility(View.VISIBLE);
            speedTrafic.setVisibility(View.INVISIBLE);
            statusConnected.setText(R.string.not_connected_text);
            infoStart.setVisibility(View.VISIBLE);
            setRippleStartButton();
        }
        if(isRunning){
            handler.removeCallbacksAndMessages(null);
        }
        adLoad();
        setupInteristial();
        return view;
    }


    public void registerBroadcast(){
        getContext().registerReceiver(DnsVpnReciver, new IntentFilter(DnsVpnService.DNS_STATE));
    }

    private void unregisterBroadcast(){
        getContext().unregisterReceiver(DnsVpnReciver);
    }

    private final BroadcastReceiver DnsVpnReciver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getExtras().getBoolean("running")){
                if(buttonVpnStart.getVisibility() == View.VISIBLE){
                    buttonVpnStart.setVisibility(View.GONE);
                }
                buttonVpnStop.setVisibility(View.VISIBLE);
                speedTrafic.setVisibility(View.VISIBLE);
                infoStart.setVisibility(View.GONE);
                setRippleStopButton();
            }else{
                iTime = 0;
                if(buttonVpnStop.getVisibility() == View.VISIBLE){
                    buttonVpnStop.setVisibility(View.GONE);
                }
                handler.removeCallbacksAndMessages(null);
                buttonVpnStart.setVisibility(View.VISIBLE);
                speedTrafic.setVisibility(View.INVISIBLE);
                infoStart.setVisibility(View.VISIBLE);
                statusConnected.setText(R.string.not_connected_text);
                setRippleStartButton();
            }
        }
    };

    private void setRippleStartButton(){
        if(buttonVpnStart.isRippleAnimationRunning()){
            buttonVpnStart.stopRippleAnimation();
        }
        buttonVpnStart.startRippleAnimation();
        textButton.setTextColor(getResources().getColor(R.color.white));
        imageBtnIcon.setColorFilter(ContextCompat.getColor(getContext(), R.color.white), android.graphics.PorterDuff.Mode.MULTIPLY);
        textButton.setText(getResources().getString(R.string.start_text));
        buttonVpnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startVPN();
                getArguments().putBoolean("isRunning", true);
                trafficSpeedMeasurer.registerListener(mStreamSpeedListener);
                speedTrafic.setVisibility(View.VISIBLE);
                infoStart.setVisibility(View.GONE);
                statusConnected.setText(R.string.connected_status_text);
                buttonVpnStart.setVisibility(View.GONE);
            }
        });
    }


    private void setRippleStopButton(){
        if(buttonVpnStop.isRippleAnimationRunning()){
            buttonVpnStop.stopRippleAnimation();
        }
        buttonVpnStop.startRippleAnimation();
        textButton.setText(getResources().getString(R.string.stop_text));
        textButton.setTextColor(getResources().getColor(R.color.primaryColor));
        imageBtnIcon.setColorFilter(ContextCompat.getColor(getContext(), R.color.primaryColor), android.graphics.PorterDuff.Mode.MULTIPLY);
        buttonVpnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopVPN();
                getArguments().putBoolean("isRunning", false);
                trafficSpeedMeasurer.removeListener();
                speedTrafic.setVisibility(View.INVISIBLE);
                statusConnected.setText(R.string.not_connected_text);
                infoStart.setVisibility(View.VISIBLE);
                buttonVpnStop.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        trafficSpeedMeasurer.stopMeasuring();
    }

    @Override
    public void onPause() {
        super.onPause();
        trafficSpeedMeasurer.removeListener();
    }

    @Override
    public void onResume() {
        trafficSpeedMeasurer.registerListener(mStreamSpeedListener);

        if(isRunning){
            handler.removeCallbacksAndMessages(null);
            timerConnection.setVisibility(View.VISIBLE);
        }
        super.onResume();
    }

    private void setupView(View view){
        buttonVpnStart =  view.findViewById(R.id.buttonVpnStart);
        buttonVpnStop =  view.findViewById(R.id.buttonVpnStop);
        downloadSpeed = view.findViewById(R.id.downloadSpeed);
        uploadSpeed = view.findViewById(R.id.uploadSpeed);
        downloadSize = view.findViewById(R.id.downloadSize);
        uploadSize = view.findViewById(R.id.uploadSize);
        speedTrafic = view.findViewById(R.id.speedTrafic);
        textButton = view.findViewById(R.id.textButton);
        imageBtnIcon = view.findViewById(R.id.imageIconButton);
        statusConnected = view.findViewById(R.id.statusConnection);
        timerConnection = view.findViewById(R.id.timerConnection);
        infoStart = view.findViewById(R.id.infoStart);
        adFrame = view.findViewById(R.id.frameAds);
        smallAdsTemplate = view.findViewById(R.id.adView);
    }

    private final ITrafficSpeedListener mStreamSpeedListener = new ITrafficSpeedListener() {

        @Override
        public void onTrafficSpeedMeasured(final double upStream, final double downStream) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String upStreamSpeed = BitsUtil.parseSpeed(upStream, SHOW_SPEED_IN_BITS);
                    String downStreamSpeed = BitsUtil.parseSpeed(downStream, SHOW_SPEED_IN_BITS);
                    String downSize = BitsUtil.sizeByte(downStream, SHOW_SPEED_IN_BITS);
                    String upSize = BitsUtil.sizeByte(upStream, SHOW_SPEED_IN_BITS);
                    downloadSpeed.setText(downStreamSpeed);
                    uploadSpeed.setText(upStreamSpeed);
                    downloadSize.setText(downSize);
                    uploadSize.setText(upSize);
                }
            });
        }
    };


    public void startVPN(){
        Intent vpnIntent = VpnService.prepare(getContext());
        if (vpnIntent != null) {
            startActivityForResult(vpnIntent, VPN_REQUEST_CODE);
        } else {
            onActivityResult(VPN_REQUEST_CODE, RESULT_OK, null);
        }
        showInteristial();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            getActivity().startService(new Intent(getContext(), DnsVpnService.class));
        }
    }

    public void stopVPN() {
        getActivity().sendBroadcast(new Intent(DnsVpnService.DNS_STOPED));
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    private void setupInteristial(){
        mInterstitialAd = new InterstitialAd(getContext());
        mInterstitialAd.setAdUnitId(getString(R.string.interisitalAds));
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener(){
            @Override
            public void onAdLoaded() {
                Log.d("Interisial", "Is Loaded");
            }

            @Override
            public void onAdFailedToLoad(LoadAdError adError) {
                Log.d("Interisial", "Is Failed to Load | Error :" + adError.getMessage());
            }

            @Override
            public void onAdOpened() {
                Log.d("Interisial", "Is Opened");
            }

            @Override
            public void onAdClicked() {
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
                Log.d("Interisial", "Clicked");
            }

            @Override
            public void onAdLeftApplication() {
                // Code to be executed when the user has left the app.
            }

            @Override
            public void onAdClosed() {
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
                Log.d("Interisial", "is Closed");
            }
        });
    }

    public void showInteristial(){
        if(mInterstitialAd.isLoaded()){
            mInterstitialAd.show();
        }else{
            Log.d("Interistial", "The interstitial wasn't loaded yet.");
        }
    }


}