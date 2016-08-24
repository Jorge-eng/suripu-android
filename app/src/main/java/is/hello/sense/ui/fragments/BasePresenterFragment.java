package is.hello.sense.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.util.Logger;

public abstract class BasePresenterFragment extends ScopedInjectionFragment {

    protected boolean animatorContextFromActivity = false;
    protected LoadingDialogFragment loadingDialogFragment;

    @Nullable
    protected AnimatorContext animatorContext;

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        if (animatorContext == null && context instanceof AnimatorContext.Scene) {
            this.animatorContext = ((AnimatorContext.Scene) context).getAnimatorContext();
            this.animatorContextFromActivity = true;
        }
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        if (animatorContext == null && activity instanceof AnimatorContext.Scene) {
            this.animatorContext = ((AnimatorContext.Scene) activity).getAnimatorContext();
            this.animatorContextFromActivity = true;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.animatorContext = null;
        this.animatorContextFromActivity = false;
    }

    public @NonNull
    AnimatorContext getAnimatorContext() {
        if (animatorContext == null) {
            this.animatorContext = new AnimatorContext(getClass().getSimpleName());
            Logger.debug(getClass().getSimpleName(), "Creating animator context");
        }

        return animatorContext;
    }
}
