package com.iot.doorunlocker.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.iot.doorunlocker.R;

public class ViewHolder implements ViewHolderImpl {

    // UI elements;
    private ImageView mPhotoView;
    private ProgressBar mProgressView;
    private TextView mResponseView;
    private TextView mNumberView;
    private TextView mQualityView;


    public ViewHolder(final Activity activity) {
        mPhotoView = (ImageView) activity.findViewById(R.id.photo);
        mProgressView = (ProgressBar) activity.findViewById(R.id.progressBar);
        mResponseView = (TextView) activity.findViewById(R.id.title);
        mNumberView = (TextView) activity.findViewById(R.id.number);
        mQualityView = (TextView) activity.findViewById(R.id.quality);
    }

    @Override
    public void setTitle(String title) {
        mResponseView.setText(title);
    }

    @Override
    public void setImage(@Nullable Bitmap bitmap) {
        mPhotoView.setImageBitmap(bitmap);
    }

    @Override
    public void showProgress(boolean visibility) {
        mProgressView.setVisibility(visibility ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setQuality(final String quality) {
        mQualityView.setText(quality);
    }

    @Override
    public void setNumber(int number) {
        mNumberView.setText(number > 0 ? "" + number : "");
    }
}
