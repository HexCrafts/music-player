package com.github.anrimian.musicplayer.di.app.play_list;

import com.github.anrimian.musicplayer.domain.business.player.MusicPlayerInteractor;
import com.github.anrimian.musicplayer.domain.business.playlists.PlayListsInteractor;
import com.github.anrimian.musicplayer.ui.common.error.parser.ErrorParser;
import com.github.anrimian.musicplayer.ui.playlist_screens.playlist.PlayListPresenter;

import javax.annotation.Nonnull;
import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import io.reactivex.Scheduler;

import static com.github.anrimian.musicplayer.di.app.SchedulerModule.UI_SCHEDULER;

@Module
public class PlayListModule {

    private final long playListId;

    public PlayListModule(long playListId) {
        this.playListId = playListId;
    }

    @Provides
    @Nonnull
    PlayListPresenter playListsPresenter(MusicPlayerInteractor musicPlayerInteractor,
                                         PlayListsInteractor playListsInteractor,
                                         @Named(UI_SCHEDULER) Scheduler uiSchedule,
                                         ErrorParser errorParser) {
        return new PlayListPresenter(playListId,
                musicPlayerInteractor,
                playListsInteractor,
                errorParser,
                uiSchedule);
    }
}
