package is.hello.sense.graph.presenters;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains one or more child presenter objects, allowing a containing object
 * to send lifecycle events to all of its presenters with a single method call.
 *
 * @see is.hello.sense.graph.presenters.Presenter
 */
public class PresenterContainer {
    private final List<Presenter> presenters = new ArrayList<>();

    //region Lifecycle

    public void onContainerDestroyed() {
        for (Presenter presenter : presenters) {
            presenter.onContainerDestroyed();
        }
    }

    public void onContainerResumed() {
        for (Presenter presenter : presenters) {
            presenter.onContainerResumed();
        }
    }

    public void onTrimMemory(int level) {
        for (Presenter presenter : presenters) {
            presenter.onTrimMemory(level);
        }
    }

    public void onRestoreState(@NonNull Bundle inState) {
        for (Presenter presenter : presenters) {
            if (presenter.isStateRestored()) {
                continue;
            }

            Parcelable savedState = inState.getParcelable(presenter.getSavedStateKey());
            presenter.onRestoreState(savedState);
        }
    }

    public void onSaveState(Bundle outState) {
        for (Presenter presenter : presenters) {
            Parcelable savedState = presenter.onSaveState();
            outState.putParcelable(presenter.getSavedStateKey(), savedState);
        }
    }

    //endregion


    /**
     * Add a child presenter to the container.
     */
    public void addPresenter(@NonNull Presenter presenter) {
        presenters.add(presenter);
    }

    /**
     * Remove a child presenter from the container, ignoring if it was not a child.
     */
    public void removePresenter(@NonNull Presenter presenter) {
        presenters.remove(presenter);
    }

    /**
     * Returns all of the child presenters in the container.
     * <p/>
     * The returned list should be lazily created if possible.
     */
    public @NonNull List<Presenter> getPresenters() {
        return presenters;
    }
}
