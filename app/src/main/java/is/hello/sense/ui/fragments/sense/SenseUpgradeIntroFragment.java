package is.hello.sense.ui.fragments.sense;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.presenters.BasePresenter;
import is.hello.sense.presenters.SenseUpgradeIntroPresenter;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.ui.fragments.BasePresenterFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSimpleStepView;

public class SenseUpgradeIntroFragment extends BasePresenterFragment
        implements SenseUpgradeIntroPresenter.Output,
        OnBackPressedInterceptor {

    @Inject
    SenseUpgradeIntroPresenter presenter;

    private OnboardingSimpleStepView view;

    @Override
    public BasePresenter getPresenter() {
        return presenter;
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        this.view = new OnboardingSimpleStepView(this, inflater)
                .setHeadingText(R.string.title_upgrade_sense_voice)
                .setSubheadingText(R.string.info_upgrade_sense_voice)
                .setDiagramImage(R.drawable.edu_room_check)
                .setPrimaryButtonText(R.string.action_continue)
                .setPrimaryOnClickListener(presenter::onPrimaryClicked)
                .setSecondaryButtonText(R.string.action_upgrade_sense_voice_help)
                .setSecondaryOnClickListener(presenter::onSecondaryClicked)
                .setToolbarWantsBackButton(true);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(this.view != null) {
            this.view.destroy();
            this.view = null;
        }

    }

    @Override
    public boolean onInterceptBackPressed(@NonNull final Runnable defaultBehavior) {
        cancelFlow();
        return true;
    }
}
