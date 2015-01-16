package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.AccountPreference;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;

public class OnboardingRegisterAudioFragment extends InjectionFragment {
    @Inject ApiService apiService;
    
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return new OnboardingSimpleStepViewBuilder(this, inflater, container)
                .setHeadingText(R.string.onboarding_title_enhanced_audio)
                .setSubheadingText(R.string.onboarding_info_enhanced_audio)
                .setDiagramImage(R.drawable.onboarding_enhanced_audio)
                .setPrimaryButtonText(R.string.action_enable_enhanced_audio)
                .setPrimaryOnClickListener(this::optIn)
                .setSecondaryOnClickListener(this::optOut)
                .setToolbarWantsBackButton(false)
                .setToolbarOnHelpClickListener(ignored -> UserSupport.showForOnboardingStep(getActivity(), UserSupport.OnboardingStep.ENHANCED_AUDIO))
                .create();
    }


    public void optIn(@NonNull View sender) {
        updateEnhancedAudioEnabled(false);
    }

    public void optOut(@NonNull View sender) {
        updateEnhancedAudioEnabled(false);
    }

    private void updateEnhancedAudioEnabled(boolean enabled) {
        LoadingDialogFragment.show(getFragmentManager());

        AccountPreference preferenceUpdate = new AccountPreference(AccountPreference.Key.ENHANCED_AUDIO);
        preferenceUpdate.setEnabled(enabled);
        bindAndSubscribe(apiService.updatePreference(preferenceUpdate),
                         ignored -> {
                             LoadingDialogFragment.close(getFragmentManager());
                             ((OnboardingActivity) getActivity()).showSetupSense();
                         },
                         e -> {
                             LoadingDialogFragment.close(getFragmentManager());
                             ErrorDialogFragment.presentError(getFragmentManager(), e);
                         });
    }
}
