
package com.iot.doorunlocker.ui;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;

public interface ViewHolderImpl {

    public static final ViewHolderImpl DEFAULT = new ViewHolderImpl() {
        @Override
        public void setTitle(final String title) {
            // nothing
        }

        @Override
        public void setImage(@Nullable final Bitmap bitmap) {
            // nothing
        }

        @Override
        public void showProgress(final boolean visibility) {
            // nothing
        }

        @Override
        public void setQuality(final String quality) {
            // nothing
        }

        @Override
        public void setNumber(final int number) {
            // nothing
        }
    };

    public void setTitle(String title);

    public void setImage(@Nullable Bitmap bitmap);

    public void showProgress(boolean visibility);

    public void setQuality(final String quality);

    public void setNumber(int number);
}