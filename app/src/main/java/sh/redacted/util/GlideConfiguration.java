package ch.redacted.util;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.Headers;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.module.GlideModule;

import java.io.InputStream;

import ch.redacted.REDApplication;
import ch.redacted.data.local.PreferencesHelper;

/**
 * Created by hfatih on 1/20/2017.
 */

public class GlideConfiguration implements GlideModule {
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        builder.setDecodeFormat(DecodeFormat.PREFER_ARGB_8888);
    }

    @Override
    public void registerComponents(Context context, Glide glide) {
        glide.register(String.class, InputStream.class, new AuthenticatedStringLoader.Factory());
    }

    /**
     * redacted's image host requires imgauth cookies
     */
    private static class AuthenticatedStringLoader implements ModelLoader<String, InputStream> {
        private final Context context;
        private final ModelLoader<GlideUrl, InputStream> glideUrlLoader;

        public AuthenticatedStringLoader(Context context, ModelLoader<GlideUrl, InputStream> glideUrlLoader) {
            this.context = context;
            this.glideUrlLoader = glideUrlLoader;
        }

        @Override
        public DataFetcher<InputStream> getResourceFetcher(String model, int width, int height) {
            if (model == null) {
                return null;
            }

            if (model.startsWith("/")) {
                model = REDApplication.DEFAULT_SITE + model;
            } else if (!model.contains(REDApplication.DEFAULT_SITE)) {
                return glideUrlLoader.getResourceFetcher(new GlideUrl(model), width, height);
            }

            PreferencesHelper prefs = REDApplication.get(context).getComponent().preferencesHelper();
            String h = prefs.getImgAuthH();
            long e = prefs.getImgAuthE();
            int u = prefs.getUserId();

            Headers headers = Headers.DEFAULT;
            if (h != null && e != 0 && u != 0) {
                String cookieValue = "imgauth_h=" + h + "; imgauth_e=" + e + "; imgauth_u=" + u;
                headers = new LazyHeaders.Builder()
                        .addHeader("Cookie", cookieValue)
                        .build();
            }

            GlideUrl glideUrl = new GlideUrl(model, headers);
            return glideUrlLoader.getResourceFetcher(glideUrl, width, height);
        }

        public static class Factory implements ModelLoaderFactory<String, InputStream> {
            @Override
            public ModelLoader<String, InputStream> build(Context context, com.bumptech.glide.load.model.GenericLoaderFactory factories) {
                ModelLoader<GlideUrl, InputStream> glideUrlLoader = factories.buildModelLoader(GlideUrl.class, InputStream.class);
                return new AuthenticatedStringLoader(context, glideUrlLoader);
            }

            @Override
            public void teardown() {
            }
        }
    }
}
