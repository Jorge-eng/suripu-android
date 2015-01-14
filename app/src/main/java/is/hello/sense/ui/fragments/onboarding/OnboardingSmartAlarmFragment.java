package is.hello.sense.ui.fragments.onboarding;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.graph.presenters.SmartAlarmPresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.activities.SmartAlarmDetailActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;

public class OnboardingSmartAlarmFragment extends InjectionFragment {
    private static final int EDIT_REQUEST_CODE = 0x31;

    @Inject SmartAlarmPresenter smartAlarmPresenter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Analytics.trackEvent(Analytics.EVENT_ONBOARDING_FIRST_ALARM, null);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_smart_alarm, container, false);

        Button setAlarm = (Button) view.findViewById(R.id.fragment_onboarding_smart_alarm_set);
        Views.setSafeOnClickListener(setAlarm, this::createNewAlarm);

        Button skip = (Button) view.findViewById(R.id.fragment_onboarding_smart_alarm_skip);
        Views.setSafeOnClickListener(skip, ignored -> complete());

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            complete();
        }
    }


    public void createNewAlarm(@NonNull View sender) {
        Intent newAlarm = new Intent(getActivity(), SmartAlarmDetailActivity.class);
        startActivityForResult(newAlarm, EDIT_REQUEST_CODE);
    }

    public void complete() {
        LoadingDialogFragment.close(getFragmentManager());
        ((OnboardingActivity) getActivity()).show2ndPillIntroduction();
    }
}
