package is.hello.sense.mvp.presenters;

import android.support.annotation.CallSuper;

import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.adapter.StaticFragmentAdapter;

public abstract class ControllerPresenterFragment<T extends PresenterView> extends PresenterFragment<T>
        implements StaticFragmentAdapter.Controller {
    private boolean isVisible = false;

    @CallSuper
    @Override
    public void setVisibleToUser(final boolean isVisible) {
        debugLog("setVisibleToUser: [ "+isVisible+" ]");
        this.isVisible = isVisible;
    }

    @CallSuper
    @Override
    public boolean isVisibleToUser() {
        return isVisible;
    }
}