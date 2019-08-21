package com.github.anrimian.musicplayer.ui.common.error.parser;

import android.content.Context;

import androidx.annotation.StringRes;

import com.github.anrimian.musicplayer.R;
import com.github.anrimian.musicplayer.data.models.exceptions.PlayListAlreadyDeletedException;
import com.github.anrimian.musicplayer.data.models.exceptions.PlayListNotCreatedException;
import com.github.anrimian.musicplayer.data.repositories.music.edit.MoveInTheSameFolderException;
import com.github.anrimian.musicplayer.domain.business.analytics.Analytics;
import com.github.anrimian.musicplayer.domain.models.exceptions.FileNodeNotFoundException;
import com.github.anrimian.musicplayer.domain.models.exceptions.StorageTimeoutException;
import com.github.anrimian.musicplayer.domain.utils.validation.ValidateError;
import com.github.anrimian.musicplayer.domain.utils.validation.ValidateException;
import com.github.anrimian.musicplayer.ui.common.error.ErrorCommand;

import java.io.FileNotFoundException;
import java.util.List;

/**
 * Created on 29.10.2017.
 */

public class DefaultErrorParser implements ErrorParser {

    private final Context context;
    private final Analytics analytics;

    public DefaultErrorParser(Context context, Analytics analytics) {
        this.context = context;
        this.analytics = analytics;
    }

    @Override
    public ErrorCommand parseError(Throwable throwable) {
        if (throwable instanceof ValidateException) {
            ValidateException exception = (ValidateException) throwable;
            List<ValidateError> validateErrors = exception.getValidateErrors();
            for (ValidateError validateError: validateErrors) {
                switch (validateError.getCause()) {
                    case EMPTY_NAME: {
                        return new ErrorCommand(getString(R.string.name_can_not_be_empty));
                    }
                }
            }
        }
        if (throwable instanceof PlayListNotCreatedException) {
            return new ErrorCommand(getString(R.string.play_list_with_this_name_already_exists));
        }
        if (throwable instanceof PlayListAlreadyDeletedException) {
            return new ErrorCommand(getString(R.string.play_not_exists));
        }
        if (throwable instanceof FileNodeNotFoundException || throwable instanceof FileNotFoundException) {
            return new ErrorCommand(getString(R.string.file_not_found));
        }
        if (throwable instanceof MoveInTheSameFolderException) {
            return new ErrorCommand(getString(R.string.move_in_the_same_folder_error));
        }
        if (throwable instanceof NullPointerException) {
            logException(throwable);
            return new ErrorCommand(getString(R.string.internal_app_error));
        }
        if (throwable instanceof StorageTimeoutException) {
            return new ErrorCommand(getString(R.string.storage_timeout_error_message));
        }
        logException(throwable);
        return new ErrorCommand(getString(R.string.unexpected_error, throwable.getMessage()));
    }

    @Override
    public void logError(Throwable throwable) {
        logException(throwable);
    }

    private void logException(Throwable throwable) {
        analytics.processNonFatalError(throwable);
    }

    private String getString(@StringRes int resId) {
        return context.getString(resId);
    }

    private String getString(@StringRes int resId, Object... formatArgs) {
        return context.getString(resId, formatArgs);
    }
}
