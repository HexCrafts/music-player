package com.github.anrimian.musicplayer.domain.interactors.player;

import static com.github.anrimian.musicplayer.domain.Constants.NO_POSITION;
import static com.github.anrimian.musicplayer.domain.interactors.player.PlayerType.LIBRARY;
import static com.github.anrimian.musicplayer.domain.models.utils.PlayQueueItemHelper.areSourcesTheSame;
import static com.github.anrimian.musicplayer.domain.models.utils.PlayQueueItemHelper.hasSourceChanges;

import com.github.anrimian.musicplayer.domain.interactors.analytics.Analytics;
import com.github.anrimian.musicplayer.domain.models.composition.Composition;
import com.github.anrimian.musicplayer.domain.models.composition.CorruptionType;
import com.github.anrimian.musicplayer.domain.models.composition.CurrentComposition;
import com.github.anrimian.musicplayer.domain.models.composition.source.CompositionSource;
import com.github.anrimian.musicplayer.domain.models.composition.source.LibraryCompositionSource;
import com.github.anrimian.musicplayer.domain.models.play_queue.PlayQueueEvent;
import com.github.anrimian.musicplayer.domain.models.play_queue.PlayQueueItem;
import com.github.anrimian.musicplayer.domain.models.player.PlayerState;
import com.github.anrimian.musicplayer.domain.models.player.error.ErrorType;
import com.github.anrimian.musicplayer.domain.models.player.events.ErrorEvent;
import com.github.anrimian.musicplayer.domain.models.player.events.FinishedEvent;
import com.github.anrimian.musicplayer.domain.models.player.events.PlayerEvent;
import com.github.anrimian.musicplayer.domain.models.player.modes.RepeatMode;
import com.github.anrimian.musicplayer.domain.repositories.LibraryRepository;
import com.github.anrimian.musicplayer.domain.repositories.PlayQueueRepository;
import com.github.anrimian.musicplayer.domain.repositories.SettingsRepository;
import com.github.anrimian.musicplayer.domain.repositories.UiStateRepository;

import java.util.List;

import javax.annotation.Nullable;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class LibraryPlayerInteractor {

    private final PlayerCoordinatorInteractor playerCoordinatorInteractor;
    private final SettingsRepository settingsRepository;
    private final PlayQueueRepository playQueueRepository;
    private final LibraryRepository musicProviderRepository;
    private final UiStateRepository uiStateRepository;
    private final Analytics analytics;

    private final CompositeDisposable playerDisposable = new CompositeDisposable();

    private final PublishSubject<Long> trackPositionSubject = PublishSubject.create();

    private final Observable<CurrentComposition> currentCompositionObservable;

    @Nullable
    private PlayQueueItem currentItem;

    public LibraryPlayerInteractor(PlayerCoordinatorInteractor playerCoordinatorInteractor,
                                   SettingsRepository settingsRepository,
                                   PlayQueueRepository playQueueRepository,
                                   LibraryRepository musicProviderRepository,
                                   UiStateRepository uiStateRepository,
                                   Analytics analytics) {
        this.playerCoordinatorInteractor = playerCoordinatorInteractor;
        this.settingsRepository = settingsRepository;
        this.playQueueRepository = playQueueRepository;
        this.musicProviderRepository = musicProviderRepository;
        this.uiStateRepository = uiStateRepository;
        this.analytics = analytics;

        currentCompositionObservable = getCurrentQueueItemObservable()
                .distinctUntilChanged()
                .switchMap(event -> getPlayerStateObservable()
                        .map(state -> state == PlayerState.PLAY)
                        .distinctUntilChanged()
                        .map(state -> new CurrentComposition(event, state)))
                .distinctUntilChanged()
                .replay(1)
                .refCount();

        playerDisposable.add(playQueueRepository.getCurrentQueueItemObservable()
                .subscribe(this::onQueueItemChanged));

        playerDisposable.add(playerCoordinatorInteractor.getPlayerEventsObservable(LIBRARY)
                .subscribe(this::onMusicPlayerEventReceived));
    }

    public void startPlaying(List<Composition> compositions) {
        startPlaying(compositions, NO_POSITION);
    }

    public void startPlaying(List<Composition> compositions, int firstPosition) {
        playQueueRepository.setPlayQueue(compositions, firstPosition)
                .doOnComplete(this::play)
                //fixes music gap and state blinking(prepare new queue from stop state)
                .doOnSubscribe(o -> playerCoordinatorInteractor.setInLoadingState(LIBRARY))
                .doOnError(analytics::processNonFatalError)
                .onErrorComplete()
                .subscribe();
    }

    public void play() {
        play(0);
    }

    public void play(int delay) {
        playerCoordinatorInteractor.play(LIBRARY, delay);
    }

    public void playOrPause() {
        playerCoordinatorInteractor.playOrPause(LIBRARY);
    }

    public void stop() {
        playerCoordinatorInteractor.stop(LIBRARY);
    }

    public void reset() {
        playerCoordinatorInteractor.reset(LIBRARY);
    }

    public void pause() {
        playerCoordinatorInteractor.pause(LIBRARY);
    }

    public void fastSeekForward() {
        playerCoordinatorInteractor.fastSeekForward(LIBRARY);
    }

    public void fastSeekBackward() {
        playerCoordinatorInteractor.fastSeekBackward(LIBRARY);
    }

    public void skipToPrevious() {
        //noinspection ResultOfMethodCallIgnored
        getActualTrackPosition().subscribe(position -> {
            if (position > settingsRepository.getSkipConstraintMillis()) {
                onSeekFinished(0);
                return;
            }
            playQueueRepository.skipToPrevious();
        });
    }

    public void skipToNext() {
        playQueueRepository.skipToNext().subscribe();
    }

    public void skipToItem(long itemId) {
        playQueueRepository.skipToItem(itemId);
    }

    public Observable<Integer> getRepeatModeObservable() {
        return settingsRepository.getRepeatModeObservable();
    }

    public int getRepeatMode() {
        return settingsRepository.getRepeatMode();
    }

    public boolean isRandomPlayingEnabled() {
        return settingsRepository.isRandomPlayingEnabled();
    }

    public Observable<Boolean> getRandomPlayingObservable() {
        return settingsRepository.getRandomPlayingObservable();
    }

    public void setRandomPlayingEnabled(boolean enabled) {
        playQueueRepository.setRandomPlayingEnabled(enabled);
    }

    public void onSeekStarted() {
        playerCoordinatorInteractor.onSeekStarted(LIBRARY);
    }

    public void seekTo(long position) {
        trackPositionSubject.onNext(position);
    }

    public void onSeekFinished(long position) {
        boolean processed = playerCoordinatorInteractor.onSeekFinished(position, LIBRARY);
        if (!processed) {
            uiStateRepository.setTrackPosition(position);
            trackPositionSubject.onNext(position);
        }
    }

    public void setRepeatMode(int mode) {
        settingsRepository.setRepeatMode(mode);
    }

    public void changeRepeatMode() {
        switch (settingsRepository.getRepeatMode()) {
            case RepeatMode.NONE: {
                settingsRepository.setRepeatMode(RepeatMode.REPEAT_PLAY_LIST);
                break;
            }
            case RepeatMode.REPEAT_PLAY_LIST: {
                settingsRepository.setRepeatMode(RepeatMode.REPEAT_COMPOSITION);
                break;
            }
            case RepeatMode.REPEAT_COMPOSITION: {
                settingsRepository.setRepeatMode(RepeatMode.NONE);
                break;
            }
        }
    }

    public Observable<Long> getTrackPositionObservable() {
        return playerCoordinatorInteractor.getTrackPositionObservable(LIBRARY)
                .mergeWith(trackPositionSubject)
                .startWith(getActualTrackPosition());
    }

    public Observable<PlayerState> getPlayerStateObservable() {
        return playerCoordinatorInteractor.getPlayerStateObservable(LIBRARY);
    }

    public PlayerState getPlayerState() {
        return playerCoordinatorInteractor.getPlayerState(LIBRARY);
    }

    public Observable<PlayQueueEvent> getCurrentQueueItemObservable() {
        return playQueueRepository.getCurrentQueueItemObservable();
    }

    public Observable<CurrentComposition> getCurrentCompositionObservable() {
        return currentCompositionObservable;
    }

    public Flowable<Integer> getCurrentItemPositionObservable() {
        return playQueueRepository.getCurrentItemPositionObservable();
    }

    public Flowable<List<PlayQueueItem>> getPlayQueueObservable() {
        return playQueueRepository.getPlayQueueObservable();
    }

    public Completable deleteComposition(Composition composition) {
        return musicProviderRepository.deleteComposition(composition);
    }

    public Completable deleteCompositions(List<Composition> compositions) {
        return musicProviderRepository.deleteCompositions(compositions);
    }

    public Completable removeQueueItem(PlayQueueItem item) {
        return playQueueRepository.removeQueueItem(item);
    }

    public Completable restoreDeletedItem() {
        return playQueueRepository.restoreDeletedItem();
    }

    public void swapItems(PlayQueueItem firstItem,
                          PlayQueueItem secondItem) {
        playQueueRepository.swapItems(firstItem, secondItem).subscribe();
    }

    public Single<List<Composition>> addCompositionsToPlayNext(List<Composition> compositions) {
        return playQueueRepository.addCompositionsToPlayNext(compositions)
                .toSingleDefault(compositions);
    }

    public Single<List<Composition>> addCompositionsToEnd(List<Composition> compositions) {
        return playQueueRepository.addCompositionsToEnd(compositions)
                .toSingleDefault(compositions);
    }

    public Completable clearPlayQueue() {
        return playQueueRepository.clearPlayQueue();
    }

    public Observable<Integer> getPlayQueueSizeObservable() {
        return playQueueRepository.getPlayQueueSizeObservable();
    }

    public void setPlaybackSpeed(float speed) {
        playerCoordinatorInteractor.setPlaybackSpeed(speed, LIBRARY);
        uiStateRepository.setCurrentPlaybackSpeed(speed);
    }

    public float getPlaybackSpeed() {
        return uiStateRepository.getCurrentPlaybackSpeed();
    }

    public Observable<Float> getPlaybackSpeedObservable() {
        return uiStateRepository.getPlaybackSpeedObservable();
    }

    public Observable<Boolean> getSpeedChangeAvailableObservable() {
        return playerCoordinatorInteractor.getSpeedChangeAvailableObservable();
    }

    public void changeRandomMode() {
        settingsRepository.setRandomPlayingEnabled(!settingsRepository.isRandomPlayingEnabled());
    }

    private void onQueueItemChanged(PlayQueueEvent compositionEvent) {
        PlayQueueItem previousItem = currentItem;
        this.currentItem = compositionEvent.getPlayQueueItem();
        if (currentItem == null) {
            if (previousItem != null) {
                reset();
            }
            return;
        }

        Composition currentComposition = currentItem.getComposition();

        if (previousItem != null && previousItem.equals(currentItem)) {
            //if file changed - re prepare with actual position
            //if not - check if changes exists - if true - update source with actual position
            boolean isFileChanged = hasSourceChanges(previousItem, currentItem);
            boolean isModelChanged = !areSourcesTheSame(previousItem, currentItem);
            if (isFileChanged || isModelChanged) {
                //noinspection ResultOfMethodCallIgnored
                getActualTrackPosition().subscribe(actualTrackPosition -> {
                    LibraryCompositionSource source = new LibraryCompositionSource(currentComposition, actualTrackPosition);
                    if (isFileChanged) {
                        playerCoordinatorInteractor.prepareToPlay(source, LIBRARY);
                        return;//and cover will be not updated, f.e.?
                    }
                    playerCoordinatorInteractor.updateSource(source, LIBRARY);
                });
            }
            return;
        }

        playerCoordinatorInteractor.prepareToPlay(
                new LibraryCompositionSource(currentComposition, compositionEvent.getTrackPosition()),
                LIBRARY
        );
    }

    private Single<Long> getActualTrackPosition() {
        return playerCoordinatorInteractor.getActualTrackPosition(LIBRARY)
                .map(position -> {
                    if (position == -1) {
                        return uiStateRepository.getTrackPosition();
                    }
                    return position;
                });
    }

    private void onMusicPlayerEventReceived(PlayerEvent playerEvent) {
        if (playerEvent instanceof FinishedEvent) {
            FinishedEvent finishedEvent = (FinishedEvent) playerEvent;
            onCompositionPlayFinished();
            CompositionSource compositionSource = finishedEvent.getComposition();
            if (compositionSource instanceof LibraryCompositionSource) {
                Composition composition = ((LibraryCompositionSource) compositionSource).getComposition();
                if (composition.getCorruptionType() != null) {
                    musicProviderRepository.writeErrorAboutComposition(null, composition)
                            .doOnError(analytics::processNonFatalError)
                            .onErrorComplete()
                            .subscribe();
                }
            }
        } else if (playerEvent instanceof ErrorEvent) {
            ErrorEvent errorEvent = (ErrorEvent) playerEvent;
            handleErrorWithComposition(errorEvent.getErrorType(), errorEvent.getComposition());
        }
    }

    private void handleErrorWithComposition(ErrorType errorType, CompositionSource compositionSource) {
        if (!(compositionSource instanceof LibraryCompositionSource) || errorType == ErrorType.IGNORED) {
            return;
        }
        Composition composition = ((LibraryCompositionSource) compositionSource).getComposition();
        CorruptionType corruptionType = toCorruptionType(errorType);
        musicProviderRepository.writeErrorAboutComposition(corruptionType, composition)
                .doOnError(analytics::processNonFatalError)
                .onErrorComplete()
                .andThen(playQueueRepository.isCurrentCompositionAtEndOfQueue())
                .doOnSuccess(isLast -> {
                    if (isLast) {
                        stop();
                    } else {
                        playQueueRepository.skipToNext().subscribe();
                    }
                })
                .subscribe();
    }

    private CorruptionType toCorruptionType(ErrorType errorType) {
        switch (errorType) {
            case UNSUPPORTED: return CorruptionType.UNSUPPORTED;
            case NOT_FOUND: return CorruptionType.NOT_FOUND;
            case UNKNOWN: return CorruptionType.UNKNOWN;
            default: return null;
        }
    }

    private void onCompositionPlayFinished() {
        if (settingsRepository.getRepeatMode() == RepeatMode.REPEAT_COMPOSITION) {
            onSeekFinished(0);
            return;
        }
        playQueueRepository.skipToNext()
                .doOnSuccess(this::onAutoSkipNextFinished)
                .subscribe();
    }

    private void onAutoSkipNextFinished(int currentPosition) {
        if (currentPosition == 0 && !(settingsRepository.getRepeatMode() == RepeatMode.REPEAT_PLAY_LIST)) {
            stop();
        }
    }
}
