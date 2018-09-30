package com.github.anrimian.musicplayer.ui.playlist_screens.playlists.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.github.anrimian.musicplayer.domain.models.playlist.PlayList;
import com.github.anrimian.musicplayer.ui.utils.OnItemClickListener;
import com.github.anrimian.musicplayer.ui.utils.views.recycler_view.diff_utils.SimpleDiffCallback;

import java.util.List;

import static android.support.v7.util.DiffUtil.calculateDiff;
import static com.github.anrimian.musicplayer.domain.models.utils.PlayListHelper.hasChanges;

public class PlayListsAdapter extends RecyclerView.Adapter<PlayListViewHolder> {

    private final List<PlayList> playLists;

    private OnItemClickListener<PlayList> onItemClickListener;

    public PlayListsAdapter(List<PlayList> playLists) {
        this.playLists = playLists;
    }

    @NonNull
    @Override
    public PlayListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new PlayListViewHolder(LayoutInflater.from(parent.getContext()),
                parent,
                onItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull PlayListViewHolder holder, int position) {
        PlayList playList = playLists.get(position);
        holder.bind(playList);
    }

    @Override
    public int getItemCount() {
        return playLists.size();
    }

    public void setOnItemClickListener(OnItemClickListener<PlayList> onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }
}
