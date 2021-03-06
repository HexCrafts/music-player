package com.github.anrimian.musicplayer.ui.library.genres.list;

import com.github.anrimian.musicplayer.domain.models.genres.Genre;
import com.github.anrimian.musicplayer.domain.models.order.Order;
import com.github.anrimian.musicplayer.ui.common.error.ErrorCommand;
import com.github.anrimian.musicplayer.ui.utils.moxy.SingleStateByTagStrategy;

import java.util.List;

import moxy.MvpView;
import moxy.viewstate.strategy.StateStrategyType;
import moxy.viewstate.strategy.alias.AddToEndSingle;
import moxy.viewstate.strategy.alias.OneExecution;

public interface GenresListView extends MvpView {

    String LIST_STATE = "list_state";
    String RENAME_STATE = "rename_state";

    @StateStrategyType(value = SingleStateByTagStrategy.class, tag = LIST_STATE)
    void showEmptyList();

    @StateStrategyType(value = SingleStateByTagStrategy.class, tag = LIST_STATE)
    void showEmptySearchResult();

    @StateStrategyType(value = SingleStateByTagStrategy.class, tag = LIST_STATE)
    void showList();

    @StateStrategyType(value = SingleStateByTagStrategy.class, tag = LIST_STATE)
    void showLoading();

    @StateStrategyType(value = SingleStateByTagStrategy.class, tag = LIST_STATE)
    void showLoadingError(ErrorCommand errorCommand);

    @StateStrategyType(value = SingleStateByTagStrategy.class, tag = RENAME_STATE)
    void showRenameProgress();

    @StateStrategyType(value = SingleStateByTagStrategy.class, tag = RENAME_STATE)
    void hideRenameProgress();

    @AddToEndSingle
    void submitList(List<Genre> genres);

    @OneExecution
    void showSelectOrderScreen(Order order);

    @OneExecution
    void showErrorMessage(ErrorCommand errorCommand);
}
