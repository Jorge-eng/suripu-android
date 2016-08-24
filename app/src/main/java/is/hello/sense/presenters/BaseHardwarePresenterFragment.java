package is.hello.sense.presenters;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import is.hello.sense.presenters.outputs.BaseHardwareOutput;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.fragments.BasePresenterFragment;

public class BaseHardwarePresenterFragment extends BasePresenterFragment
        implements BaseHardwareOutput {

    @Override
    public void hideBlockingActivity(@StringRes final int text, @Nullable final Runnable onCompletion) {
        LoadingDialogFragment.closeWithMessageTransition(getFragmentManager(),
                                            () -> {
                                                loadingDialogFragment = null;
                                                if (onCompletion != null) {
                                                    presenter.execute(onCompletion);
                                                }
                                            },
                                            text);
    }

    @Override
    public void hideBlockingActivity(final boolean success, @NonNull final Runnable onCompletion) {
        if (success) {
            LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(), () -> {
                this.loadingDialogFragment = null;
                presenter.execute(onCompletion);
            });
        } else {
            LoadingDialogFragment.close(getFragmentManager());
            this.loadingDialogFragment = null;
            onCompletion.run();
        }
    }
}
