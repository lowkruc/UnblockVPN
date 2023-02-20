package com.unblock.vpn.websites.ads;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAdView;
import com.unblock.vpn.websites.R;

public class SmallAdsTemplate extends FrameLayout {
    private UnifiedNativeAd nativeAd;
    private UnifiedNativeAdView nativeAdView;

    ImageView image;
    CardView frameImage;
    TextView headline, secondary, store, action;

    public SmallAdsTemplate(@NonNull Context context) {
        super(context);
    }

    public SmallAdsTemplate(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    public SmallAdsTemplate(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attributeSet) {

        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.small_ads_layout, this);
    }

    private boolean adHasOnlyStore(UnifiedNativeAd nativeAd) {
        String store = nativeAd.getStore();
        String advertiser = nativeAd.getAdvertiser();
        return !TextUtils.isEmpty(store) && TextUtils.isEmpty(advertiser);
    }

    public UnifiedNativeAdView getNativeAdView() {
        return nativeAdView;
    }

    public void destroyNativeAd() {
        nativeAd.destroy();
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        nativeAdView = (UnifiedNativeAdView) findViewById(R.id.nativeAds);
        image = (ImageView) findViewById(R.id.image);
        headline = (TextView) findViewById(R.id.headline);
        secondary = (TextView) findViewById(R.id.secondary);
        action = (TextView) findViewById(R.id.btnAction);
        frameImage = (CardView) findViewById(R.id.wraperIconAdsHome);
    }

    public void setupNativeAd(UnifiedNativeAd nativeAd){
        this.nativeAd = nativeAd;

        String headlineVal = nativeAd.getHeadline();
        String second = nativeAd.getBody();
        String cta = nativeAd.getCallToAction();
        NativeAd.Image icon = nativeAd.getIcon();

        nativeAdView.setCallToActionView(action);
        headline.setText(headlineVal);
        secondary.setText(second);
        Log.d("SmallAds", headlineVal);
        action.setText(cta);
        headline.setText(headlineVal);
        if(icon != null){
            frameImage.setVisibility(VISIBLE);
            image.setImageDrawable(icon.getDrawable());
        }else{
            frameImage.setVisibility(GONE);
        }

        nativeAdView.setNativeAd(nativeAd);
    }
}
