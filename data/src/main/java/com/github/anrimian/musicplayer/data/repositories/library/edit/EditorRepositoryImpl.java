package com.github.anrimian.musicplayer.data.repositories.library.edit;

import static com.github.anrimian.musicplayer.domain.Constants.TRIGGER;

import androidx.core.util.Pair;

import com.github.anrimian.musicplayer.data.database.dao.albums.AlbumsDaoWrapper;
import com.github.anrimian.musicplayer.data.database.dao.artist.ArtistsDaoWrapper;
import com.github.anrimian.musicplayer.data.database.dao.compositions.CompositionsDaoWrapper;
import com.github.anrimian.musicplayer.data.database.dao.folders.FoldersDaoWrapper;
import com.github.anrimian.musicplayer.data.database.dao.genre.GenresDaoWrapper;
import com.github.anrimian.musicplayer.data.models.composition.CompositionId;
import com.github.anrimian.musicplayer.data.repositories.library.edit.exceptions.AlbumAlreadyExistsException;
import com.github.anrimian.musicplayer.data.repositories.library.edit.exceptions.ArtistAlreadyExistsException;
import com.github.anrimian.musicplayer.data.repositories.library.edit.exceptions.DuplicateFolderNamesException;
import com.github.anrimian.musicplayer.data.repositories.library.edit.exceptions.EditorTimeoutException;
import com.github.anrimian.musicplayer.data.repositories.library.edit.exceptions.FileExistsException;
import com.github.anrimian.musicplayer.data.repositories.library.edit.exceptions.GenreAlreadyExistsException;
import com.github.anrimian.musicplayer.data.repositories.library.edit.exceptions.MoveFolderToItselfException;
import com.github.anrimian.musicplayer.data.repositories.library.edit.exceptions.MoveInTheSameFolderException;
import com.github.anrimian.musicplayer.data.storage.files.StorageFilesDataSource;
import com.github.anrimian.musicplayer.data.storage.providers.genres.StorageGenresProvider;
import com.github.anrimian.musicplayer.data.storage.providers.music.FilePathComposition;
import com.github.anrimian.musicplayer.data.storage.providers.music.StorageMusicProvider;
import com.github.anrimian.musicplayer.data.storage.source.CompositionSourceEditor;
import com.github.anrimian.musicplayer.domain.models.composition.Composition;
import com.github.anrimian.musicplayer.domain.models.composition.FullComposition;
import com.github.anrimian.musicplayer.domain.models.composition.source.CompositionSourceTags;
import com.github.anrimian.musicplayer.domain.models.folders.FileSource;
import com.github.anrimian.musicplayer.domain.models.folders.FolderFileSource;
import com.github.anrimian.musicplayer.domain.models.genres.ShortGenre;
import com.github.anrimian.musicplayer.domain.models.image.ImageSource;
import com.github.anrimian.musicplayer.domain.repositories.EditorRepository;
import com.github.anrimian.musicplayer.domain.repositories.SettingsRepository;
import com.github.anrimian.musicplayer.domain.repositories.StateRepository;
import com.github.anrimian.musicplayer.domain.utils.Objects;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;

public class EditorRepositoryImpl implements EditorRepository {

    private static final long CHANGE_COVER_TIMEOUT_MILLIS = 25000;

    private final CompositionSourceEditor sourceEditor;
    private final StorageFilesDataSource filesDataSource;
    private final CompositionsDaoWrapper compositionsDao;
    private final AlbumsDaoWrapper albumsDao;
    private final ArtistsDaoWrapper artistsDao;
    private final GenresDaoWrapper genresDao;
    private final FoldersDaoWrapper foldersDao;
    private final StorageMusicProvider storageMusicProvider;
    private final StorageGenresProvider storageGenresProvider;
    private final StateRepository stateRepository;
    private final SettingsRepository settingsRepository;
    private final Scheduler scheduler;

    public EditorRepositoryImpl(CompositionSourceEditor sourceEditor,
                                StorageFilesDataSource filesDataSource,
                                CompositionsDaoWrapper compositionsDao,
                                AlbumsDaoWrapper albumsDao,
                                ArtistsDaoWrapper artistsDao,
                                GenresDaoWrapper genresDao,
                                FoldersDaoWrapper foldersDao,
                                StorageMusicProvider storageMusicProvider,
                                StorageGenresProvider storageGenresProvider,
                                StateRepository stateRepository,
                                SettingsRepository settingsRepository,
                                Scheduler scheduler) {
        this.sourceEditor = sourceEditor;
        this.filesDataSource = filesDataSource;
        this.compositionsDao = compositionsDao;
        this.albumsDao = albumsDao;
        this.artistsDao = artistsDao;
        this.genresDao = genresDao;
        this.foldersDao = foldersDao;
        this.storageMusicProvider = storageMusicProvider;
        this.storageGenresProvider = storageGenresProvider;
        this.stateRepository = stateRepository;
        this.settingsRepository = settingsRepository;
        this.scheduler = scheduler;
    }

    /*
    rename genre - working
    add genre - working( no:( )
    change genre
    remove genre
    update\change album artist
    update genre name - not sure

    ******
    Seems, jaudiotagger names 'album-artist' and 'genre' differently than android media scanner

    We can add genre, but can't delete?

    Scan genres once?
     */

    @Override
    public Completable changeCompositionGenre(FullComposition composition,
                                              ShortGenre oldGenre,
                                              String newGenre) {
        return sourceEditor.changeCompositionGenre(composition, oldGenre.getName(), newGenre)
                .doOnComplete(() -> {
                    genresDao.changeCompositionGenre(composition.getId(), oldGenre.getId(), newGenre);
                    runSystemRescan(composition);
                })
                .subscribeOn(scheduler);
    }

    @Override
    public Completable addCompositionGenre(FullComposition composition,
                                           String newGenre) {
        return sourceEditor.addCompositionGenre(composition, newGenre)
                .doOnComplete(() -> {
                    genresDao.addCompositionToGenre(composition.getId(), newGenre);
                    runSystemRescan(composition);
                })
                .subscribeOn(scheduler);
    }

    @Override
    public Completable removeCompositionGenre(FullComposition composition, ShortGenre genre) {
        return sourceEditor.removeCompositionGenre(composition, genre.getName())
                .doOnComplete(() -> {
                    genresDao.removeCompositionFromGenre(composition.getId(), genre.getId());
                    runSystemRescan(composition);
                })
                .subscribeOn(scheduler);
    }

    @Override
    public Completable changeCompositionAuthor(FullComposition composition, String newAuthor) {
        return sourceEditor.setCompositionAuthor(composition, newAuthor)
                .doOnComplete(() -> {
                    compositionsDao.updateArtist(composition.getId(), newAuthor);
                    runSystemRescan(composition);
                })
                .subscribeOn(scheduler);
    }

    @Override
    public Completable changeCompositionAlbumArtist(FullComposition composition, String newAuthor) {
        return sourceEditor.setCompositionAlbumArtist(composition, newAuthor)
                .doOnComplete(() -> {
                    compositionsDao.updateAlbumArtist(composition.getId(), newAuthor);
                    runSystemRescan(composition);
                })
                .subscribeOn(scheduler);
    }

    @Override
    public Completable changeCompositionAlbum(FullComposition composition, String newAlbum) {
        return sourceEditor.setCompositionAlbum(composition, newAlbum)
                .doOnComplete(() -> {
                    compositionsDao.updateAlbum(composition.getId(), newAlbum);
                    runSystemRescan(composition);
                })
                .subscribeOn(scheduler);
    }

    @Override
    public Completable changeCompositionTitle(FullComposition composition, String title) {
        return sourceEditor.setCompositionTitle(composition, title)
                .doOnComplete(() -> {
                    compositionsDao.updateTitle(composition.getId(), title);
                    runSystemRescan(composition);
                })
                .subscribeOn(scheduler);
    }

    @Override
    public Completable changeCompositionLyrics(FullComposition composition, String text) {
        return sourceEditor.setCompositionLyrics(composition, text)
                .doOnComplete(() -> {
                    compositionsDao.updateLyrics(composition.getId(), text);
                    runSystemRescan(composition);
                })
                .subscribeOn(scheduler);
    }

    @Override
    public Completable changeCompositionFileName(FullComposition composition, String fileName) {
        return Completable.fromAction(() -> {
            Pair<String, String> newPathAndName = filesDataSource.renameCompositionFile(composition, fileName);
            if (newPathAndName.first != null) {
                compositionsDao.updateFilePath(composition.getId(), newPathAndName.first);
            }
            compositionsDao.updateCompositionFileName(composition.getId(), newPathAndName.second);
        }).subscribeOn(scheduler);
    }

    @Override
    public Completable changeFolderName(long folderId, String newName) {
        return getFullFolderPath(folderId)
                .map(fullPath -> {
                    List<Composition> compositions = compositionsDao.getAllCompositionsInFolder(folderId, settingsRepository.isDisplayFileNameEnabled());

                    List<FilePathComposition> updatedCompositions = new LinkedList<>();
                    String name = filesDataSource.renameCompositionsFolder(compositions,
                            fullPath,
                            newName,
                            updatedCompositions);

                    compositionsDao.updateFilesPath(updatedCompositions);
                    return name;
                })
                .doOnSuccess(name -> foldersDao.changeFolderName(folderId, name))
                .ignoreElement()
                .subscribeOn(scheduler);
    }

    @Override
    public Completable moveFiles(Collection<FileSource> files,
                                 @Nullable Long fromFolderId,
                                 @Nullable Long toFolderId) {
        return verifyFolderMove(fromFolderId, toFolderId, files)
                .andThen(Single.zip(getFullFolderPath(fromFolderId),
                        getFullFolderPath(toFolderId),
                        foldersDao.extractAllCompositionsFromFiles(files, settingsRepository.isDisplayFileNameEnabled()),
                        (fromPath, toPath, compositions) -> {
                            List<FilePathComposition> updateCompositions = filesDataSource.moveCompositionsToFolder(compositions, fromPath, toPath);
                            compositionsDao.updateFilesPath(updateCompositions);
                            return TRIGGER;
                        }))
                .ignoreElement()
                .doOnComplete(() -> foldersDao.updateFolderId(files, toFolderId))
                .subscribeOn(scheduler);
    }

    @Override
    public Completable moveFilesToNewDirectory(Collection<FileSource> files,
                                               @Nullable Long fromFolderId,
                                               @Nullable Long targetParentFolderId,
                                               String directoryName) {
        return Completable.fromRunnable(() -> {
            if (foldersDao.isFolderWithNameExists(targetParentFolderId, directoryName)) {
                throw new FileExistsException();
            }
        }).andThen(Single.zip(getFullFolderPath(fromFolderId),
                getFullFolderPath(targetParentFolderId),
                foldersDao.extractAllCompositionsFromFiles(files, settingsRepository.isDisplayFileNameEnabled()),
                (fromPath, toParentPath, compositions) -> {
                    List<FilePathComposition> updatedCompositions = new LinkedList<>();
                    String name = filesDataSource.moveCompositionsToNewFolder(compositions,
                            fromPath,
                            toParentPath,
                            directoryName,
                            updatedCompositions);

                    compositionsDao.updateFilesPath(updatedCompositions);

                    return foldersDao.createFolder(targetParentFolderId, name);
                }))
                .doOnSuccess(folderId -> foldersDao.updateFolderId(files, folderId))
                .ignoreElement()
                .subscribeOn(scheduler);
    }

    @Override
    public Completable updateAlbumName(String name, long albumId) {
        return checkAlbumExists(name)
                .andThen(Single.fromCallable(() -> albumsDao.getCompositionsInAlbum(albumId)))
                .flatMap(compositions -> sourceEditor.setCompositionsAlbum(compositions, name))
                .doOnSuccess(compositions -> {
                    albumsDao.updateAlbumName(name, albumId);
                    for (CompositionId composition: compositions) {
                        runSystemRescan(composition);
                    }
                })
                .ignoreElement()
                .subscribeOn(scheduler);
    }

    @Override
    public Completable updateAlbumArtist(String newArtistName, long albumId) {
        return Single.fromCallable(() -> albumsDao.getCompositionsInAlbum(albumId))
                .flatMap(compositions -> sourceEditor.setCompositionsAlbumArtist(compositions, newArtistName))
                .doOnSuccess(compositions -> {
                    albumsDao.updateAlbumArtist(albumId, newArtistName);
                    for (CompositionId composition: compositions) {
                        runSystemRescan(composition);
                    }
                })
                .ignoreElement()
                .subscribeOn(scheduler);
    }

    @Override
    public Completable updateArtistName(String name, long artistId) {
        Set<CompositionId> compositionsToScan = new LinkedHashSet<>();
        return checkArtistExists(name)

                .andThen(Single.fromCallable(() -> artistsDao.getCompositionsByArtist(artistId)))
                .doOnSuccess(compositionsToScan::addAll)
                .flatMapObservable(Observable::fromIterable)
                .flatMapCompletable(composition -> sourceEditor.setCompositionAuthor(composition, name))

                .andThen(Single.fromCallable(() -> albumsDao.getAllAlbumsForArtist(artistId)))
                .flatMapObservable(Observable::fromIterable)
                .flatMapCompletable(album -> Single.fromCallable(() -> albumsDao.getCompositionsInAlbum(album.getId()))
                        .doOnSuccess(compositionsToScan::addAll)
                        .flatMapObservable(Observable::fromIterable)
                        .flatMapCompletable(composition -> sourceEditor.setCompositionAlbumArtist(composition, name))
                )
                .doOnComplete(() -> {
                    artistsDao.updateArtistName(name, artistId);

                    for (CompositionId composition: compositionsToScan) {
                        runSystemRescan(composition);
                    }
                })
                .subscribeOn(scheduler);
    }

    @Override
    public Completable updateGenreName(String name, long genreId) {
        return checkGenreExists(name)
                .andThen(Single.fromCallable(() -> genresDao.getGenreName(genreId)))
                .flatMapCompletable(oldName -> Single.fromCallable(() -> genresDao.getCompositionsInGenre(genreId))
                        .flatMapObservable(Observable::fromIterable)
                        .flatMapCompletable(composition -> sourceEditor.changeCompositionGenre(composition, oldName, name))
                        .doOnComplete(() -> {
                            genresDao.updateGenreName(name, genreId);
                            storageGenresProvider.updateGenreName(oldName, name);
                        })
                )
                .subscribeOn(scheduler);
    }

    @Override
    public Single<String[]> getCompositionFileGenres(FullComposition composition) {
        return sourceEditor.getCompositionGenres(composition)
                .subscribeOn(scheduler);
    }

    @Override
    public Completable changeCompositionAlbumArt(FullComposition composition, ImageSource imageSource) {
        return sourceEditor.changeCompositionAlbumArt(composition, imageSource)
                .doOnSuccess(newSize -> onCompositionFileChanged(composition, newSize))
                .ignoreElement()
                .timeout(CHANGE_COVER_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS, Completable.error(new EditorTimeoutException()))
                .subscribeOn(scheduler);
    }

    @Override
    public Completable removeCompositionAlbumArt(FullComposition composition) {
        return sourceEditor.removeCompositionAlbumArt(composition)
                .doOnSuccess(newSize -> onCompositionFileChanged(composition, newSize))
                .ignoreElement()
                .timeout(CHANGE_COVER_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS, Completable.error(new EditorTimeoutException()))
                .subscribeOn(scheduler);
    }

    /**
     * Album-artist in android system and in common file has conflicts. This function
     * updates media library by real file source tags.
     */
    @Override
    public Completable updateTagsFromSource(FullComposition composition) {
        return sourceEditor.getFullTags(composition)
                .flatMapCompletable(tags -> updateCompositionTags(composition, tags))
                .subscribeOn(scheduler);
    }

    private Completable updateCompositionTags(FullComposition composition,
                                              CompositionSourceTags tags) {
        return Completable.fromAction(() -> {
            compositionsDao.updateCompositionBySourceTags(composition, tags);
//            long id = composition.getId();
//
//            String tagTitle = tags.getTitle();
//            if (!isEmpty(tagTitle) && !Objects.equals(composition.getTitle(), tagTitle)) {
//                compositionsDao.updateTitle(id, tagTitle);
//            }
//
//            String tagArtist = tags.getArtist();
//            if (!isEmpty(tagArtist) && !Objects.equals(composition.getArtist(), tagArtist)) {
//                compositionsDao.updateArtist(id, tagArtist);
//            }
//
//            String tagAlbum = tags.getAlbum();
//            if (!isEmpty(tagAlbum) && !Objects.equals(composition.getAlbum(), tagAlbum)) {
//                compositionsDao.updateAlbum(id, tagAlbum);
//            }
//
//            String tagAlbumArtist = tags.getAlbumArtist();
//            if (!isEmpty(tagAlbumArtist) && !Objects.equals(composition.getAlbumArtist(), tagAlbumArtist)) {
//                compositionsDao.updateAlbumArtist(id, tagAlbumArtist);
//            }
//
//            String tagLyrics = tags.getLyrics();
//            if (!isEmpty(tagLyrics) && !Objects.equals(composition.getLyrics(), tagLyrics)) {
//                compositionsDao.updateLyrics(id, tagLyrics);
//            }
        });
    }

    private void onCompositionFileChanged(FullComposition composition, long newSize) {
        compositionsDao.updateModifyTimeAndSize(composition.getId(), newSize, new Date());
        runSystemRescan(composition);
    }

    private Completable checkAlbumExists(String name) {
        return Completable.fromAction(() -> {
            if (albumsDao.isAlbumExists(name)) {
                throw new AlbumAlreadyExistsException();
            }
        });
    }

    private Completable checkArtistExists(String name) {
        return Completable.fromAction(() -> {
            if (artistsDao.isArtistExists(name)) {
                throw new ArtistAlreadyExistsException();
            }
        });
    }

    private Completable checkGenreExists(String name) {
        return Completable.fromAction(() -> {
            if (genresDao.isGenreExists(name)) {
                throw new GenreAlreadyExistsException();
            }
        });
    }

    private Single<String> getFullFolderPath(@Nullable Long folderId) {
        return Single.fromCallable(() -> {
            StringBuilder sbPath = new StringBuilder();
            String rootFolderPath = stateRepository.getRootFolderPath();
            if (rootFolderPath != null) {
                sbPath.append(rootFolderPath);
            }
            if (folderId != null) {
                if (sbPath.length() != 0) {
                    sbPath.append('/');
                }
                sbPath.append(foldersDao.getFullFolderPath(folderId));
            }
            return sbPath.toString();
        });
    }

    private Completable verifyFolderMove(@Nullable Long fromFolderId,
                                         @Nullable Long toFolderId,
                                         Collection<FileSource> files) {
        return Completable.fromAction(() -> {
            if (Objects.equals(fromFolderId, toFolderId)) {
                throw new MoveInTheSameFolderException("move in the same folder");
            }
            for (FileSource fileSource: files) {
                if (fileSource instanceof FolderFileSource) {
                    FolderFileSource folder = (FolderFileSource) fileSource;
                    long folderId = folder.getId();

                    List<Long> childFoldersId = foldersDao.getAllChildFoldersId(folderId);
                    if (Objects.equals(toFolderId, folderId) || childFoldersId.contains(toFolderId)) {
                        throw new MoveFolderToItselfException("moving and destination folders matches");
                    }
                    String name = foldersDao.getFolderName(folderId);
                    if (foldersDao.getChildFoldersNames(toFolderId).contains(name)) {
                        throw new DuplicateFolderNamesException();
                    }
                }
            }

        });
    }

    private void runSystemRescan(CompositionId composition) {
        Long storageId = composition.getStorageId();
        if (storageId != null) {
            storageMusicProvider.scanMedia(storageId);
        }
    }

    private void runSystemRescan(FullComposition composition) {
        Long storageId = composition.getStorageId();
        if (storageId != null) {
            storageMusicProvider.scanMedia(storageId);
        }
    }
}
