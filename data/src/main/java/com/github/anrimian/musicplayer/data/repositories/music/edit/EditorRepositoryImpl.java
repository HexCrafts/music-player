package com.github.anrimian.musicplayer.data.repositories.music.edit;

import com.github.anrimian.musicplayer.data.database.dao.albums.AlbumsDaoWrapper;
import com.github.anrimian.musicplayer.data.database.dao.artist.ArtistsDaoWrapper;
import com.github.anrimian.musicplayer.data.database.dao.genre.GenresDaoWrapper;
import com.github.anrimian.musicplayer.data.storage.providers.albums.StorageAlbumsProvider;
import com.github.anrimian.musicplayer.data.storage.providers.artist.StorageArtistsProvider;
import com.github.anrimian.musicplayer.data.storage.providers.genres.StorageGenresProvider;
import com.github.anrimian.musicplayer.data.storage.providers.music.StorageMusicDataSource;
import com.github.anrimian.musicplayer.data.storage.providers.music.StorageMusicProvider;
import com.github.anrimian.musicplayer.domain.models.composition.Composition;
import com.github.anrimian.musicplayer.domain.models.composition.FullComposition;
import com.github.anrimian.musicplayer.domain.repositories.EditorRepository;
import com.github.anrimian.musicplayer.domain.utils.FileUtils;
import com.github.anrimian.musicplayer.domain.utils.Objects;

import java.io.File;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;

public class EditorRepositoryImpl implements EditorRepository {

    private final CompositionSourceEditor sourceEditor = new CompositionSourceEditor();

    private final StorageMusicDataSource storageMusicDataSource;
    private final AlbumsDaoWrapper albumsDao;
    private final ArtistsDaoWrapper artistsDao;
    private final GenresDaoWrapper genresDao;
    private final StorageMusicProvider storageMusicProvider;
    private final StorageGenresProvider storageGenresProvider;
    private final StorageArtistsProvider storageArtistsProvider;
    private final StorageAlbumsProvider storageAlbumsProvider;
    private final Scheduler scheduler;

    public EditorRepositoryImpl(StorageMusicDataSource storageMusicDataSource,
                                AlbumsDaoWrapper albumsDao,
                                ArtistsDaoWrapper artistsDao,
                                GenresDaoWrapper genresDao,
                                StorageMusicProvider storageMusicProvider,
                                StorageGenresProvider storageGenresProvider,
                                StorageArtistsProvider storageArtistsProvider,
                                StorageAlbumsProvider storageAlbumsProvider,
                                Scheduler scheduler) {
        this.storageMusicDataSource = storageMusicDataSource;
        this.albumsDao = albumsDao;
        this.artistsDao = artistsDao;
        this.genresDao = genresDao;
        this.storageMusicProvider = storageMusicProvider;
        this.storageGenresProvider = storageGenresProvider;
        this.storageArtistsProvider = storageArtistsProvider;
        this.storageAlbumsProvider = storageAlbumsProvider;
        this.scheduler = scheduler;
    }

    @Override
    public Completable changeCompositionGenre(FullComposition composition, String newGenre) {
        return sourceEditor.setCompositionGenre(composition.getFilePath(), newGenre)
                .andThen(storageMusicDataSource.updateCompositionGenre(composition, newGenre))
                .subscribeOn(scheduler);
    }

    @Override
    public Completable changeCompositionAuthor(FullComposition composition, String newAuthor) {
        return sourceEditor.setCompositionAuthor(composition.getFilePath(), newAuthor)
                .andThen(storageMusicDataSource.updateCompositionAuthor(composition, newAuthor))
                .subscribeOn(scheduler);
    }

    @Override
    public Completable changeCompositionAlbumArtist(FullComposition composition, String newAuthor) {
        return sourceEditor.setCompositionAlbumArtist(composition.getFilePath(), newAuthor)
                .andThen(storageMusicDataSource.updateCompositionAlbumArtist(composition, newAuthor))
                .subscribeOn(scheduler);
    }

    @Override
    public Completable changeCompositionAlbum(FullComposition composition, String newAlbum) {
        return sourceEditor.setCompositionAlbum(composition.getFilePath(), newAlbum)
                .andThen(storageMusicDataSource.updateCompositionAlbum(composition, newAlbum))
                .subscribeOn(scheduler);
    }

    @Override
    public Completable changeCompositionTitle(FullComposition composition, String title) {
        return sourceEditor.setCompositionTitle(composition.getFilePath(), title)
                .andThen(storageMusicDataSource.updateCompositionTitle(composition, title))
                .subscribeOn(scheduler);
    }

    @Override
    public Completable changeCompositionFileName(FullComposition composition, String fileName) {
        return Single.fromCallable(() -> FileUtils.getChangedFilePath(composition.getFilePath(), fileName))
                .flatMap(newPath -> renameFile(composition.getFilePath(), newPath))
                .flatMapCompletable(newPath -> storageMusicDataSource.updateCompositionFilePath(composition, newPath))
                .subscribeOn(scheduler);
    }

    @Override
    public Completable changeCompositionsFilePath(List<Composition> compositions) {
        return storageMusicDataSource.updateCompositionsFilePath(compositions)
                .subscribeOn(scheduler);
    }

    @Override
    public Single<String> changeFolderName(String filePath, String folderName) {
        return Single.fromCallable(() -> FileUtils.getChangedFilePath(filePath, folderName))
                .flatMap(newPath -> renameFile(filePath, newPath))
                .subscribeOn(scheduler);
    }

    @Override
    public Single<String> moveFile(String filePath, String oldPath, String newPath) {
        if (Objects.equals(oldPath, newPath)) {
            return Single.error(new MoveInTheSameFolderException("move in the same folder"));
        }
        return Single.fromCallable(() -> FileUtils.getChangedFilePath(filePath, oldPath, newPath))
                .flatMap(path -> renameFile(filePath, path))
                .subscribeOn(scheduler);
    }

    @Override
    public Completable createFile(String path) {
        return Completable.fromAction(() -> {
            File file = new File(path);
            if (file.exists()) {
                throw new FileExistsException();
            }
            if (!file.mkdir()) {
                throw new Exception("file not created");
            }
        }).subscribeOn(scheduler);
    }

    @Override
    public Completable updateAlbumName(String name, long albumId) {
        return Single.fromCallable(() -> albumsDao.getCompositionsInAlbum(albumId))
                .flatMapObservable(Observable::fromIterable)
                .flatMapCompletable(composition -> sourceEditor.setCompositionAlbum(composition.getFilePath(), name))
                .doOnComplete(() -> {
                    String oldName = albumsDao.getAlbumName(albumId);
                    String artist = albumsDao.getAlbumArtist(albumId);
                    albumsDao.updateAlbumName(name, albumId);
                    storageAlbumsProvider.updateAlbumName(oldName, artist, name);//not working
                })
                .subscribeOn(scheduler);
    }

    @Override
    public Completable updateAlbumArtist(String newArtistName, long albumId) {
        return Single.fromCallable(() -> albumsDao.getCompositionsInAlbum(albumId))
                .flatMapObservable(Observable::fromIterable)
                .flatMapCompletable(composition -> sourceEditor.setCompositionAlbumArtist(composition.getFilePath(), newArtistName))
                .doOnComplete(() -> {
                    String albumName = albumsDao.getAlbumName(albumId);
                    String oldArtist = albumsDao.getAlbumArtist(albumId);
                    albumsDao.updateAlbumArtist(albumId, newArtistName);
                    storageAlbumsProvider.updateAlbumArtist(albumName, oldArtist, newArtistName);//not working
                })
                .subscribeOn(scheduler);
    }

    @Override
    public Completable updateArtistName(String name, long artistId) {
        return Single.fromCallable(() -> artistsDao.getCompositionsByArtist(artistId))
                .flatMapObservable(Observable::fromIterable)
                .flatMapCompletable(composition -> sourceEditor.setCompositionAuthor(composition.getFilePath(), name)
                        .doOnComplete(() -> {
                            Long storageId = composition.getStorageId();
                            if (storageId != null) {
                                storageMusicProvider.updateCompositionArtist(composition.getStorageId(), name);
                            }
                        }))
                //edit all composition album artist
                .andThen(Single.fromCallable(() -> albumsDao.getAllAlbumsForArtist(artistId)))
                .flatMapObservable(Observable::fromIterable)
                .flatMapCompletable(album -> Single.fromCallable(() -> albumsDao.getCompositionsInAlbum(album.getId()))
                        .flatMapObservable(Observable::fromIterable)
                        .flatMapCompletable(composition -> sourceEditor.setCompositionAlbumArtist(composition.getFilePath(), name)
                                .doOnComplete(() -> {
                                    Long storageId = composition.getStorageId();
                                    if (storageId != null) {
                                        storageMusicProvider.updateCompositionAlbumArtist(composition.getStorageId(), name);
                                    }
                                }))
                )
                .doOnComplete(() -> {
//                    String oldName = artistsDao.getArtistName(artistId);
                    artistsDao.updateArtistName(name, artistId);
//                    storageArtistsProvider.updateArtistName(oldName, name);
                })
                .subscribeOn(scheduler);
    }

    @Override
    public Completable updateGenreName(String name, long genreId) {
        return Single.fromCallable(() -> genresDao.getCompositionsInGenre(genreId))
                .flatMapObservable(Observable::fromIterable)
                .flatMapCompletable(composition -> sourceEditor.setCompositionGenre(composition.getFilePath(), name))
                .doOnComplete(() -> {
                    String oldName = genresDao.getGenreName(genreId);
                    genresDao.updateGenreName(name, genreId);
                    storageGenresProvider.updateGenreName(oldName, name);
                })
                .subscribeOn(scheduler);
    }

    private Single<String> renameFile(String oldPath, String newPath) {
        return Single.create(emitter -> {

            File oldFile = new File(oldPath);
            File newFile = new File(newPath);
            if (oldFile.renameTo(newFile)) {
                emitter.onSuccess(newPath);
            } else {
                emitter.onError(new Exception("file not renamed"));
            }
        });
    }



}
