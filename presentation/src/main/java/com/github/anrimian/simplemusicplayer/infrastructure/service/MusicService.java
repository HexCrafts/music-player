package com.github.anrimian.simplemusicplayer.infrastructure.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;

import com.github.anrimian.simplemusicplayer.di.Components;
import com.github.anrimian.simplemusicplayer.domain.business.player.MusicPlayerInteractor;
import com.github.anrimian.simplemusicplayer.domain.models.Composition;
import com.github.anrimian.simplemusicplayer.domain.models.player.PlayerState;
import com.github.anrimian.simplemusicplayer.infrastructure.service.models.PlayerInfo;
import com.github.anrimian.simplemusicplayer.infrastructure.service.models.TrackInfo;
import com.github.anrimian.simplemusicplayer.infrastructure.service.models.mappers.PlayerStateMapper;
import com.github.anrimian.simplemusicplayer.ui.main.MainActivity;
import com.github.anrimian.simplemusicplayer.ui.notifications.NotificationsDisplayer;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;

import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PAUSE;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY_PAUSE;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_STOP;
import static android.support.v4.media.session.PlaybackStateCompat.Builder;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_STOPPED;
import static com.github.anrimian.simplemusicplayer.ui.notifications.NotificationsDisplayer.FOREGROUND_NOTIFICATION_ID;

/**
 * Created on 03.11.2017.
 */

public class MusicService extends Service/*MediaBrowserServiceCompat*/ {

    public static final String REQUEST_CODE = "request_code";
    public static final int PLAY = 1;
    public static final int PAUSE = 2;
    public static final int SKIP_TO_NEXT = 3;
    public static final int SKIP_TO_PREVIOUS = 4;

    @Inject
    NotificationsDisplayer notificationsDisplayer;

    @Inject
    MusicPlayerInteractor musicPlayerInteractor;

    public MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();
    public Builder stateBuilder = new Builder()
            .setActions(ACTION_PLAY
                    | ACTION_STOP
                    | ACTION_PAUSE
                    | ACTION_PLAY_PAUSE
                    | ACTION_SKIP_TO_NEXT
                    | ACTION_SKIP_TO_PREVIOUS);

    private AudioManager audioManager;
    private MediaSessionCompat mediaSession;
    private AudioFocusChangeListener audioFocusChangeListener = new AudioFocusChangeListener();
    private MediaSessionCallback mediaSessionCallback = new MediaSessionCallback();
    private MusicServiceBinder musicServiceBinder = new MusicServiceBinder(this, mediaSession);
    private CompositeDisposable serviceDisposable = new CompositeDisposable();

    private boolean firstLaunch = true;

    @Override
    public void onCreate() {
        super.onCreate();
        Components.getAppComponent().inject(this);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mediaSession = new MediaSessionCompat(this, getClass().getSimpleName());
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mediaSession.setCallback(mediaSessionCallback);

        Intent activityIntent = new Intent(this, MainActivity.class);
        PendingIntent pActivityIntent = PendingIntent.getActivity(this, 0, activityIntent, 0);
        mediaSession.setSessionActivity(pActivityIntent);

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null, this, MediaButtonReceiver.class);
        PendingIntent pMediaButtonIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
        mediaSession.setMediaButtonReceiver(pMediaButtonIntent);

        registerReceiver(becomingNoisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));

        subscribeOnPlayerChanges();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int requestCode = intent.getIntExtra(REQUEST_CODE, -1);
        switch (requestCode) {
            case PLAY: {
                musicPlayerInteractor.play();
                break;
            }
            case PAUSE: {
                musicPlayerInteractor.pause();
                break;
            }
            case SKIP_TO_NEXT: {
                musicPlayerInteractor.skipToNext();
                break;
            }
            case SKIP_TO_PREVIOUS: {
                musicPlayerInteractor.skipToPrevious();
                break;
            }
        }
        MediaButtonReceiver.handleIntent(mediaSession, intent);
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return musicServiceBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(becomingNoisyReceiver);
        mediaSession.release();
        serviceDisposable.dispose();
    }
/*    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return null;
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {

    }*/

/* .doOnNext(composition -> {
        MediaMetadataCompat metadata = metadataBuilder
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, composition.getTitle())
                .build();
        mediaSession.setMetadata(metadata);
    })*/

    public void startPlaying(List<Composition> compositions) {
        musicPlayerInteractor.startPlaying(compositions);
    }

    public void play() {
        musicPlayerInteractor.play();
    }

    public void pause() {
        musicPlayerInteractor.pause();
    }

    public void stop() {
        musicPlayerInteractor.stop();
    }

    public void skipToPrevious() {
        musicPlayerInteractor.skipToPrevious();
    }

    public void skipToNext() {
        musicPlayerInteractor.skipToNext();
    }

    public Observable<PlayerState> getPlayerStateObservable() {
        return musicPlayerInteractor.getPlayerStateObservable();
    }

    public Observable<Composition> getCurrentCompositionObservable() {
        return musicPlayerInteractor.getCurrentCompositionObservable();
    }

    public Observable<List<Composition>> getCurrentPlayListObservable() {
        return musicPlayerInteractor.getCurrentPlayListObservable();
    }

    public Observable<Long> getTrackPositionObservable() {
        return musicPlayerInteractor.getTrackPositionObservable();
    }

    public boolean isInfinitePlayingEnabled() {
        return musicPlayerInteractor.isInfinitePlayingEnabled();
    }

    public boolean isRandomPlayingEnabled() {
        return musicPlayerInteractor.isRandomPlayingEnabled();
    }

    public void setRandomPlayingEnabled(boolean enabled) {
        musicPlayerInteractor.setRandomPlayingEnabled(enabled);
    }

    public void setInfinitePlayingEnabled(boolean enabled) {
        musicPlayerInteractor.setInfinitePlayingEnabled(enabled);
    }

    private void subscribeOnPlayerChanges() {
        Observable<Integer> playerStateObservable = musicPlayerInteractor.getPlayerStateObservable()
                .map(PlayerStateMapper::toMediaState);
        Observable<Composition> compositionObservable = musicPlayerInteractor.getCurrentCompositionObservable()
                .doOnNext(this::onCurrentCompositionChanged);
        Observable<Long> trackPositionObservable = musicPlayerInteractor.getTrackPositionObservable();

        serviceDisposable.add(Observable.combineLatest(playerStateObservable, trackPositionObservable, TrackInfo::new)
                .subscribe(this::onCurrentTrackInfoChanged));

        serviceDisposable.add(Observable.combineLatest(playerStateObservable, compositionObservable, PlayerInfo::new)
                .subscribe(this::onPlayerStateChanged));
    }

    //TODO can be invalid position in new track(new track emits first), fix later. Maybe don't emit position so often, save on pause/stop?
    private void onCurrentTrackInfoChanged(TrackInfo info) {
        stateBuilder.setState(info.getState(), info.getTrackPosition(), 1);
        mediaSession.setPlaybackState(stateBuilder.build());
    }

    private void onCurrentCompositionChanged(Composition composition) {
        MediaMetadataCompat metadata = metadataBuilder
//                .putBitmap(MediaMetadataCompat.METADATA_KEY_ART,
//                        BitmapFactory.decodeResource(getResources(), track.getBitmapResId()))
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, composition.getTitle())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, composition.getAlbum())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, composition.getArtist())
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, composition.getDuration())
                .build();
        mediaSession.setMetadata(metadata);
    }

    private void onPlayerStateChanged(PlayerInfo info) {
        if (!firstLaunch) {
            notificationsDisplayer.updateForegroundNotification(info, mediaSession);
        }
        firstLaunch = false;

        switch (info.getState()) {
            case STATE_PLAYING: {
                int audioFocusResult = audioManager.requestAudioFocus(//TODO request focus BEFORE playing
                        audioFocusChangeListener,
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN);
                if (audioFocusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    musicPlayerInteractor.pause();
                    return;
                }
                mediaSession.setActive(true);
                startForeground(FOREGROUND_NOTIFICATION_ID, notificationsDisplayer.getForegroundNotification(info, mediaSession));
                break;
            }
            case STATE_STOPPED:
            case STATE_PAUSED: {
                mediaSession.setActive(false);
                audioManager.abandonAudioFocus(audioFocusChangeListener);
                stopForeground(false);
                break;
            }
        }
    }

    private class AudioFocusChangeListener implements AudioManager.OnAudioFocusChangeListener {

        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN: {
                    mediaSessionCallback.onPlay();
                    break;
                }
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK: {
                    mediaSessionCallback.onPause();
                    break;
                }
                default: {
                    mediaSessionCallback.onPause();
                    break;
                }
            }
        }
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {

        @Override
        public void onPlay() {
            musicPlayerInteractor.play();
        }

        @Override
        public void onPause() {
            musicPlayerInteractor.pause();
        }

        @Override
        public void onStop() {
            musicPlayerInteractor.stop();
        }

        @Override
        public void onSkipToNext() {
            musicPlayerInteractor.skipToNext();
        }

        @Override
        public void onSkipToPrevious() {
            musicPlayerInteractor.skipToPrevious();
        }
    }

    private final BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                mediaSessionCallback.onPause();
            }
        }
    };
}
