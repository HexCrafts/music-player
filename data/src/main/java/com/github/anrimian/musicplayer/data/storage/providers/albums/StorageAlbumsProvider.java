package com.github.anrimian.musicplayer.data.storage.providers.albums;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore.Audio.Albums;

import androidx.annotation.Nullable;

import com.github.anrimian.musicplayer.data.database.entities.albums.ShortAlbum;
import com.github.anrimian.musicplayer.data.utils.db.CursorWrapper;
import com.github.anrimian.musicplayer.data.utils.rx.content_observer.RxContentObserver;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.Observable;

public class StorageAlbumsProvider {

    private final ContentResolver contentResolver;

    public StorageAlbumsProvider(Context context) {
        contentResolver = context.getContentResolver();
    }

    public Observable<Map<ShortAlbum, StorageAlbum>> getAlbumsObservable() {
        return RxContentObserver.getObservable(contentResolver, Albums.EXTERNAL_CONTENT_URI)
                .map(o -> getAlbums());
    }

    public Map<ShortAlbum, StorageAlbum> getAlbums() {
        try(Cursor cursor = contentResolver.query(Albums.EXTERNAL_CONTENT_URI,
                new String[] {
                        Albums._ID,
                        Albums.ALBUM,
//                        Albums.ALBUM_ID,
//                        Albums.ALBUM_KEY,
                        Albums.FIRST_YEAR,
                        Albums.LAST_YEAR,
                        Albums.ARTIST,
//                        Albums.ARTIST_ID
                },
                null,
                null,
                null)) {
            if (cursor == null) {
                return new HashMap<>();
            }
            CursorWrapper cursorWrapper = new CursorWrapper(cursor);
            Map<ShortAlbum, StorageAlbum> artists = new HashMap<>(cursor.getCount());
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToPosition(i);

                StorageAlbum item = getAlbumFromCursor(cursorWrapper);
                if (item != null) {
                    artists.put(new ShortAlbum(item.getAlbum(), item.getArtist()), item);
                }
            }
            return artists;
        }
    }

    public void updateAlbumName(String oldName, String artist, String name) {
        ContentValues cv = new ContentValues();
        cv.put(Albums.ALBUM, name);
        contentResolver.update(Albums.EXTERNAL_CONTENT_URI,
                cv,
                Albums.ALBUM + " = ? AND " + Albums.ARTIST + " = ?",
                new String[] { oldName, artist });
    }

    public void updateAlbumArtist(String albumName, String oldArtist, String newArtistName) {
        ContentValues cv = new ContentValues();
        cv.put(Albums.ARTIST, newArtistName);
        contentResolver.update(Albums.EXTERNAL_CONTENT_URI,
                cv,
                Albums.ALBUM + " = ? AND " + Albums.ARTIST + " = ?",
                new String[] { albumName, oldArtist });
    }

    @Nullable
    private StorageAlbum getAlbumFromCursor(CursorWrapper cursorWrapper) {
        String name = cursorWrapper.getString(Albums.ALBUM);
        if (name == null) {
            return null;
        }
        String artist = cursorWrapper.getString(Albums.ARTIST);
        if (artist != null && artist.equals("<unknown>")) {
            artist = null;
        }

        return new StorageAlbum(
                cursorWrapper.getLong(Albums._ID),
                name,
                artist,
                cursorWrapper.getInt(Albums.FIRST_YEAR),
                cursorWrapper.getInt(Albums.LAST_YEAR)
        );
    }

}
