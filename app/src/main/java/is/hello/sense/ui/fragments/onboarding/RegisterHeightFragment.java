package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.SenseApplication;
import is.hello.sense.api.model.Account;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.AccountEditor;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.widget.ScaleView;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.units.UnitOperations;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;

public class RegisterHeightFragment extends SenseFragment {
    @Inject
    PreferencesInteractor preferences;

    private ScaleView scale;
    private TextView scaleReading;

    private boolean hasAnimated = false;

    public RegisterHeightFragment() {
        SenseApplication.getInstance().inject(this);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            this.hasAnimated = savedInstanceState.getBoolean("hasAnimated", false);
        }

        if (savedInstanceState == null && getActivity() instanceof OnboardingActivity) {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_HEIGHT, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_onboarding_register_height, container, false);

        this.scale = (ScaleView) view.findViewById(R.id.fragment_onboarding_register_height_scale);
        this.scaleReading = (TextView) view.findViewById(R.id.fragment_onboarding_register_height_scale_reading);

        final boolean defaultMetric = UnitFormatter.isDefaultLocaleMetric();
        final boolean useCentimeters = preferences.getBoolean(PreferencesInteractor.USE_CENTIMETERS, defaultMetric);
        if (useCentimeters) {
            scale.setOnValueChangedListener(centimeters -> {
                scaleReading.setText(getString(R.string.height_cm_fmt, centimeters));
            });
        } else {
            scale.setOnValueChangedListener(centimeters -> {
                final long totalInches = UnitOperations.centimetersToInches(centimeters);
                final long feet = totalInches / 12;
                final long inches = totalInches % 12;
                scaleReading.setText(getString(R.string.height_inches_fmt, feet, inches));
            });
        }

        final Account account = AccountEditor.getContainer(this).getAccount();
        if (account.getHeight() != null) {
            scale.setValue(account.getHeight(), true);
        }

        final Button nextButton = (Button) view.findViewById(R.id.fragment_onboarding_next);
        Views.setTimeOffsetOnClickListener(nextButton, ignored -> next());

        final Button skipButton = (Button) view.findViewById(R.id.fragment_onboarding_skip);
        if (AccountEditor.getWantsSkipButton(this)) {
            Views.setTimeOffsetOnClickListener(skipButton, ignored -> skip());
        } else {
            skipButton.setVisibility(View.INVISIBLE);
            nextButton.setText(R.string.action_done);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        final Account account = AccountEditor.getContainer(this).getAccount();
        if (!hasAnimated && account.getHeight() != null) {
            scale.setValue(scale.getMinValue(), true);
            scale.postDelayed(() -> {
                if (scale != null) {
                    scale.animateToValue(account.getHeight());
                }
            }, 250);
            this.hasAnimated = true;
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("hasAnimated", true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        scale.onDestroyView();
        scale = null;
        scaleReading = null;
    }

    public void skip() {
        Analytics.trackEvent(Analytics.Onboarding.EVENT_SKIP,
                             Analytics.createProperties(Analytics.Onboarding.PROP_SKIP_SCREEN, "height"));
        AccountEditor.getContainer(this).onAccountUpdated(this);
    }

    public void next() {
        try {
            final AccountEditor.Container container = AccountEditor.getContainer(this);
            if (!scale.isAnimating()) {
                container.getAccount().setHeight(scale.getValue());
            }
            container.onAccountUpdated(this);
        } catch (NumberFormatException e) {
            Logger.warn(RegisterHeightFragment.class.getSimpleName(), "Invalid input fed to height fragment, ignoring.", e);
        }
    }
}
