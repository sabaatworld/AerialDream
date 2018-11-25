package com.codingbuffalo.aerialdream;

import android.content.Context;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import com.codingbuffalo.aerialdream.data.Apple2015Video;
import com.codingbuffalo.aerialdream.data.Video;
import com.codingbuffalo.aerialdream.data.VideoInteractor;
import com.codingbuffalo.aerialdream.data.VideoPlaylist;
import com.codingbuffalo.aerialdream.databinding.AerialDreamBinding;
import com.codingbuffalo.aerialdream.databinding.VideoViewBinding;

public class VideoController implements VideoInteractor.Listener, ExoPlayerView.OnPlayerEventListener {
    private AerialDreamBinding binding;

    private VideoPlaylist playlist;

    private String source_apple_2015;
    private String source_apple_2017;

    public VideoController(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        binding = DataBindingUtil.inflate(inflater, R.layout.aerial_dream, null, false);

        // Apply preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // Backwards compatibility
        String cache = prefs.getBoolean("cache", false) ? "-1" : "0";
        prefs.edit().remove("cache").apply();

        // If user has chosen to hide the loading message, set it to an empty text
        if(!prefs.getBoolean("show_loading_msg", false)) {
            binding.loadingView.setText("");
        }

        boolean showClock = prefs.getBoolean("show_clock", true);
        boolean showLocation = prefs.getBoolean("show_location", true);
        boolean showProgress = prefs.getBoolean("show_progress", false);
        int cacheSize = Integer.valueOf(prefs.getString("cache_size", cache));

        source_apple_2015 = prefs.getString("source_apple_2015", "all");
        source_apple_2017 = prefs.getString("source_apple_2017", "1080_sdr");

        binding.setShowLocation(showLocation);
        binding.setShowClock(showClock);
        binding.setShowProgress(showProgress);
        binding.setCacheSize(cacheSize);

        binding.videoView0.setController(binding.videoView0.videoView);
        binding.videoView1.setController(binding.videoView1.videoView);

        binding.videoView0.videoView.setOnPlayerListener(this);
        binding.videoView1.videoView.setOnPlayerListener(this);

        new VideoInteractor(
                !source_apple_2015.equals("disabled"),
                !source_apple_2017.equals("disabled"),
                this
        ).fetchVideos();
    }

    public View getView() {
        return binding.getRoot();
    }

    public void start() {
        binding.videoView0.getRoot().setAlpha(0);

        loadVideo(binding.videoView0, getVideo());
        loadVideo(binding.videoView1, getVideo());

        binding.videoView1.videoView.start();
    }

    public void stop() {
        binding.videoView0.videoView.release();
        binding.videoView1.videoView.release();
    }

    private void playVideo(final VideoViewBinding deactivate, final VideoViewBinding activate) {
        activate.videoView.start();

        Animation animation = new AlphaAnimation(1, 0);
        animation.setDuration(ExoPlayerView.DURATION);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                binding.container.bringChildToFront(activate.getRoot());
                deactivate.videoView.pause();
                deactivate.getRoot().setAlpha(1);
                loadVideo(deactivate, getVideo());
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        deactivate.getRoot().startAnimation(animation);
    }

    @Override
    public void onFetch(VideoPlaylist videos) {
        this.playlist = videos;
        binding.getRoot().post(this::start);
    }

    private void loadVideo(VideoViewBinding videoBinding, Video video) {
        String option = video instanceof Apple2015Video ? source_apple_2015 : source_apple_2017;
        videoBinding.videoView.setUri(video.getUri(option));
        videoBinding.location.setText(video.getLocation());
    }

    private Video getVideo() {
        Video video = playlist.getVideo();
        // Verify that the video is able to satisfy the options, otherwise skip it
        return (video instanceof Apple2015Video && video.getUri(source_apple_2015) == null) ? getVideo() : video;
    }

    @Override
    public void onPrepared(ExoPlayerView view) {
        if (binding.loadingView.getVisibility() == View.VISIBLE && view == binding.videoView1.videoView) {
            binding.videoView0.getRoot().setAlpha(1);

            Animation animation = new AlphaAnimation(1, 0);
            animation.setDuration(ExoPlayerView.DURATION / 2);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    binding.loadingView.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });

            binding.loadingView.startAnimation(animation);
        }
    }

    @Override
    public void onAlmostFinished(ExoPlayerView view) {
        if (view == binding.videoView0.videoView) {
            playVideo(binding.videoView0, binding.videoView1);
        } else {
            playVideo(binding.videoView1, binding.videoView0);
        }
    }
}
