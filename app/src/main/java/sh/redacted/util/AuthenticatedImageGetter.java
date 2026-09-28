package ch.redacted.util;

import android.content.Context;
import android.util.LruCache;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.lang.ref.WeakReference;

public class AuthenticatedImageGetter implements Html.ImageGetter {

    private final WeakReference<TextView> containerRef;
    private final Context context;
    private static final int CACHE_SIZE = 50;
    private static final LruCache<String, Drawable> drawableCache = new LruCache<String, Drawable>(CACHE_SIZE) {
        @Override
        protected void entryRemoved(boolean evicted, String key, Drawable oldValue, Drawable newValue) {
            if (oldValue instanceof GifDrawable) {
                ((GifDrawable) oldValue).stop();
            }
        }
    };

    public AuthenticatedImageGetter(TextView container) {
        this.containerRef = new WeakReference<>(container);
        this.context = container.getContext().getApplicationContext();
    }

    @Override
    public Drawable getDrawable(String source) {
        if (source == null) {
            return null;
        }

        Drawable cached = drawableCache.get(source);
        if (cached != null) {
            URLDrawable cachedDrawable = new URLDrawable(containerRef.get());
            if (cached instanceof GifDrawable) {
                ((GifDrawable) cached).setCallback(cachedDrawable);
                ((GifDrawable) cached).start();
            }
            cachedDrawable.setDrawable(cached);
            return cachedDrawable;
        }

        URLDrawable urlDrawable = new URLDrawable(containerRef.get());

        TextView container = containerRef.get();
        if (container == null) {
            return urlDrawable;
        }

        if (source.endsWith(".gif")) {
            Glide.with(context)
                .load(source)
                .asGif()
                .into(new SimpleTarget<GifDrawable>() {
                    @Override
                    public void onResourceReady(GifDrawable resource, GlideAnimation<? super GifDrawable> glideAnimation) {
                        resource.setLoopCount(GifDrawable.LOOP_FOREVER);
                        drawableCache.put(source, resource);
                        urlDrawable.setDrawable(resource);
                        resource.setCallback(urlDrawable);
                        resource.start();
                        container.setText(container.getText());
                    }
                });
        } else {
            Glide.with(context)
                .load(source)
                .asBitmap()
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                        BitmapDrawable drawable = new BitmapDrawable(context.getResources(), resource);
                        urlDrawable.setDrawable(drawable);
                        drawableCache.put(source, drawable);
                        container.setText(container.getText());
                    }
                });
        }

        return urlDrawable;
    }

    private static class URLDrawable extends Drawable implements Drawable.Callback {
        private Drawable drawable;
        private final WeakReference<TextView> containerRef;

        URLDrawable(TextView container) {
            this.containerRef = new WeakReference<>(container);
        }

        @Override
        public void draw(Canvas canvas) {
            if (drawable != null) {
                drawable.draw(canvas);
            }
        }

        @Override
        public void setBounds(int left, int top, int right, int bottom) {
            super.setBounds(left, top, right, bottom);
            if (drawable != null) {
                drawable.setBounds(left, top, right, bottom);
            }
        }

        @Override
        public int getIntrinsicWidth() {
            return drawable != null ? drawable.getIntrinsicWidth() : 0;
        }

        @Override
        public int getIntrinsicHeight() {
            return drawable != null ? drawable.getIntrinsicHeight() : 0;
        }

        @Override
        public int getOpacity() {
            return drawable != null ? drawable.getOpacity() : PixelFormat.TRANSLUCENT;
        }

        @Override
        public void setAlpha(int alpha) {
            if (drawable != null) {
                drawable.setAlpha(alpha);
            }
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            if (drawable != null) {
                drawable.setColorFilter(colorFilter);
            }
        }

        void setDrawable(Drawable drawable) {
            this.drawable = drawable;
            setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        }

        @Override
        public void invalidateDrawable(Drawable who) {
            TextView container = containerRef.get();
            if (container != null) {
                container.invalidate();
            }
        }

        @Override
        public void scheduleDrawable(Drawable who, Runnable what, long when) {
            TextView container = containerRef.get();
            if (container != null) {
                container.postDelayed(what, when - System.currentTimeMillis());
            }
        }

        @Override
        public void unscheduleDrawable(Drawable who, Runnable what) {
            TextView container = containerRef.get();
            if (container != null) {
                container.removeCallbacks(what);
            }
        }
    }
}
