package com.github.anrimian.musicplayer.data.database.entities;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import static com.github.anrimian.musicplayer.data.database.AppDatabase.SHUFFLED_PLAY_QUEUE;

@Deprecated
@Entity(tableName = SHUFFLED_PLAY_QUEUE)
public class ShuffledPlayQueueEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long audioId;

    private int position;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getAudioId() {
        return audioId;
    }

    public void setAudioId(long audioId) {
        this.audioId = audioId;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
