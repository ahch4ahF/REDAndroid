package ch.redacted.ui.release;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.transition.TransitionInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import com.tbruyelle.rxpermissions2.RxPermissions;

import ch.redacted.data.model.Artist;
import ch.redacted.ui.artist.TorrentGroupAdapter;
import io.reactivex.functions.Consumer;
import ch.redacted.REDApplication;
import org.sufficientlysecure.htmltextview.HtmlTextView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ch.redacted.app.R;
import ch.redacted.data.model.TorrentGroup;
import ch.redacted.ui.artist.ArtistActivity;
import ch.redacted.ui.base.BaseActivity;
import ch.redacted.util.ReleaseTypes;
import ch.redacted.util.Tags;

public class ReleaseActivity extends BaseActivity implements ReleaseMvpView, TorrentsAdapter.Callback {

    private int RELEASE_ID = 0;
    private int ARTIST_ID = 0;

    private boolean showAllDesc = false;
    private boolean isBookmarked;

    @Inject ReleasePresenter mReleasePresenter;
    @Inject TorrentsAdapter mTorrentsAdapter;
    @BindView(R.id.release_image) ImageView releaseImage;
    @BindView(R.id.release_title) HtmlTextView releaseTitle;
    @BindView(R.id.release_artist) Button releaseArtist;
    @BindView(R.id.release_info) TextView releaseInfo;
    @BindView(R.id.release_tags) TextView releaseTags;
    @BindView(R.id.release_description) HtmlTextView releaseDescription;
    @BindView(R.id.recycler_view) RecyclerView mTorrentsRecyclerView;
    @BindView(R.id.toolbar_layout) CollapsingToolbarLayout mToolbarLayout;
    @BindView(R.id.app_bar) AppBarLayout mAppBarLayout;
    @BindView(R.id.read_more_button) Button mReadMore;
    @BindView(R.id.fab) FloatingActionButton bookmarkFab;
    @BindView(R.id.progressBar) ProgressBar loadingProgress;
    private List<Object> mTorrents;

    @OnClick(R.id.release_artist)
    public void OnArtistClick(View v) {
        Intent intent = new Intent(this, ArtistActivity.class);
        intent.putExtra("id", ARTIST_ID);
        startActivity(intent);
    }

    @OnClick(R.id.fab)
    public void OnBookMarkToggle(View v) {
        mReleasePresenter.toggleBookmark(RELEASE_ID, isBookmarked);
    }

    @OnClick(R.id.read_more_button)
    public void OnReadMoreClick(View v) {
        showAllDesc = !showAllDesc;

        if (showAllDesc) {
            mReadMore.setText(getString(R.string.hide_description));
            releaseDescription.setVisibility(View.VISIBLE);
        } else {
            mReadMore.setText(getString(R.string.show_description));
            releaseDescription.setVisibility(View.GONE);
        }
    }

    /**
     * Android activity lifecycle methods
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityComponent().inject(this);
        setContentView(R.layout.activity_release);
        ButterKnife.bind(this);

        mTorrents = new ArrayList<>();

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setSharedElementEnterTransition(TransitionInflater.from(this).inflateTransition(R.transition.shared_element_transation));
            releaseImage.setTransitionName("thumbnailTransition");
        }
        mReleasePresenter.attachView(this);

        if (getIntent().getIntExtra("id", 0) > 0) {
            RELEASE_ID = getIntent().getExtras().getInt("id");
        }
        //this means we are handling a VIEW intent from the browser
        else if (getIntent().getDataString().contains("?id=") || getIntent().getDataString().contains("&id=")) {
            String url = getIntent().getDataString();
            String id = url.substring(url.indexOf("id=") + 3, url.length());
            RELEASE_ID = Integer.parseInt(id);
        }

        mTorrentsRecyclerView.setHasFixedSize(true);
        mTorrentsRecyclerView.setNestedScrollingEnabled(false);
        mTorrentsAdapter.setTorrents(mTorrents);
        mTorrentsAdapter.setCallback(this);
        mTorrentsRecyclerView.setAdapter(mTorrentsAdapter);
        mTorrentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mReleasePresenter.loadRelease(RELEASE_ID);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mReleasePresenter.detachView();
    }

    /*****
     * MVP View methods implementation
     *****/

    @Override
    public void showLoadingProgress(boolean show) {
        if (show) {
            loadingProgress.setVisibility(View.VISIBLE);
        } else {
            loadingProgress.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void showError(String message) {

    }

    @Override
    public void showBookmarked(boolean bookmarked) {
        isBookmarked = bookmarked;
        if (isBookmarked) {
            bookmarkFab.setImageResource(R.drawable.ic_bookmark_24dp);
        } else {
            bookmarkFab.setImageResource(R.drawable.ic_bookmark_border_24dp);
        }
    }

    @Override
    public void showDownloadComplete() {
        Toast.makeText(this, "Download Complete", Toast.LENGTH_LONG).show();
    }

    @Override
    public void showSendToWmComplete() {
        Toast.makeText(this, "Sent to WhatManager", Toast.LENGTH_LONG).show();
    }

    @Override
    public void showRelease(TorrentGroup torrentGroup) {
        if (REDApplication.get(this).getComponent().dataManager().getPreferencesHelper().getLoadImages()) {
            Glide.with(this).load(torrentGroup.response.group.wikiImage).asBitmap().fitCenter().into(releaseImage);
        } else {
            releaseImage.setVisibility(View.GONE);
        }

        final String info = getString(R.string.release_info, torrentGroup.response.group.musicInfo.artists.get(0).name, torrentGroup.response.group.name, torrentGroup.response.group.year, ReleaseTypes.getReleaseType(torrentGroup.response.group.releaseType));
        String releaseInfoSimple = getString(R.string.release_info_simple, torrentGroup.response.group.year, ReleaseTypes.getReleaseType(torrentGroup.response.group.releaseType), torrentGroup.response.torrents.size());
        releaseArtist.setText(torrentGroup.response.group.musicInfo.artists.get(0).name);
        releaseTags.setText(Tags.PrettyTags(3, torrentGroup.response.group.tags));
        releaseInfo.setText(releaseInfoSimple);
        releaseTitle.setHtml(torrentGroup.response.group.name);
        isBookmarked = torrentGroup.response.group.isBookmarked;
        ARTIST_ID = torrentGroup.response.group.musicInfo.artists.get(0).id;

        if (showAllDesc) {
            mReadMore.setText(getString(R.string.hide_description));
            releaseDescription.setVisibility(View.VISIBLE);
        } else {
            mReadMore.setText(getString(R.string.show_description));
            releaseDescription.setVisibility(View.GONE);
        }

        if (isBookmarked) {
            bookmarkFab.setImageResource(R.drawable.ic_bookmark_24dp);
        } else {
            bookmarkFab.setImageResource(R.drawable.ic_bookmark_border_24dp);
        }

        releaseDescription.setHtml(torrentGroup.response.group.wikiBody);

        mToolbarLayout.setCollapsedTitleTextAppearance(R.style.CollapsedAppBar);

        mAppBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean isShow = false;
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }
                if (scrollRange + verticalOffset == 0) {
                    mToolbarLayout.setTitle(Html.fromHtml(info));
                    releaseImage.setFitsSystemWindows(false);
                    isShow = true;
                    bookmarkFab.hide();

                } else if (isShow) {
                    mToolbarLayout.setTitle(" ");
                    releaseImage.setFitsSystemWindows(true);
                    isShow = false;
                    bookmarkFab.show();
                }
            }
        });
    }

    @Override
    public void showTorrents(Object torrent) {
        mTorrents.add(torrent);
        mTorrentsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDownloadClicked(final int id) {
        final Context context = this;
        new RxPermissions(this)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean granted) throws Exception {
                        if (granted) {
                            mReleasePresenter.downloadRelease(id, context);
                        }
                    }
                });
    }
}