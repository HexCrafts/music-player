package com.github.anrimian.simplemusicplayer.di.library;

import android.content.Context;

import com.github.anrimian.simplemusicplayer.data.repositories.music.MusicProviderRepositoryImpl;
import com.github.anrimian.simplemusicplayer.domain.business.library.StorageLibraryInteractor;
import com.github.anrimian.simplemusicplayer.domain.business.player.MusicPlayerInteractor;
import com.github.anrimian.simplemusicplayer.domain.repositories.MusicProviderRepository;
import com.github.anrimian.simplemusicplayer.ui.player.main.PlayerPresenter;

import javax.annotation.Nonnull;
import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import io.reactivex.Scheduler;

import static com.github.anrimian.simplemusicplayer.di.app.SchedulerModule.IO_SCHEDULER;
import static com.github.anrimian.simplemusicplayer.di.app.SchedulerModule.UI_SCHEDULER;

/**
 * Created on 29.10.2017.
 */
@Module
public class LibraryModule {

    @Provides
    @Nonnull
    MusicProviderRepository provideMusicProviderRepository(Context ctx,
                                                           @Named(IO_SCHEDULER) Scheduler scheduler) {
        return new MusicProviderRepositoryImpl(ctx, scheduler);
    }

    @Provides
    @Nonnull
    @LibraryScope
    StorageLibraryInteractor provideStorageLibraryInteractor(MusicProviderRepository musicProviderRepository,
                                                             MusicPlayerInteractor musicPlayerInteractor) {
        return new StorageLibraryInteractor(musicProviderRepository, musicPlayerInteractor);
    }

    @Provides
    @Nonnull
    PlayerPresenter provideLibraryPresenter(MusicPlayerInteractor musicPlayerInteractor,
                                            @Named(UI_SCHEDULER) Scheduler uiScheduler) {
        return new PlayerPresenter(musicPlayerInteractor, uiScheduler);
    }
}
