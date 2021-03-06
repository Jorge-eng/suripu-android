package is.hello.sense.flows.settings.ui.activities;

import android.app.Fragment;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.flows.expansions.ui.activities.ExpansionSettingsActivity;
import is.hello.sense.flows.home.ui.activities.HomeActivity;
import is.hello.sense.flows.nightmode.interactors.NightModeInteractor;
import is.hello.sense.flows.nightmode.ui.activities.NightModeActivity;
import is.hello.sense.flows.notification.ui.activities.NotificationActivity;
import is.hello.sense.flows.settings.ui.fragments.AppSettingsFragment;
import is.hello.sense.flows.settings.ui.views.AppSettingsView;
import is.hello.sense.flows.voice.ui.activities.VoiceSettingsActivity;
import is.hello.sense.functional.Functions;
import is.hello.sense.ui.activities.appcompat.FragmentNavigationActivity;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.fragments.settings.AccountSettingsFragment;
import is.hello.sense.ui.fragments.settings.DeviceListFragment;
import is.hello.sense.ui.fragments.support.SupportFragment;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Distribution;
import is.hello.sense.util.Share;

public class AppSettingsActivity extends FragmentNavigationActivity
        implements FragmentNavigation {


    @Inject
    NightModeInteractor nightModeInteractor;

    @Override
    protected void onCreate(@NonNull final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //todo decide if should make an extensible subclass NightModeActivity
        final int initialConfigMode = nightModeInteractor.getConfigMode(getResources());
        bindAndSubscribe(nightModeInteractor.currentNightMode,
                         mode -> {
                             if (nightModeInteractor.requiresRecreate(initialConfigMode,
                                                                      getResources())) {
                                 HomeActivity.recreateTaskStack(this);
                             }},
                         Functions.LOG_ERROR);
    }

    //region NavigationActivity
    @Override
    protected void onCreateAction() {
        showAppSettingsFragment();
    }
    //endregion

    //region NavigationDelegate
    @Override
    public final void flowFinished(@NonNull final Fragment fragment,
                                   final int responseCode,
                                   @Nullable final Intent result) {
        if (fragment instanceof AppSettingsFragment) {
            switch (responseCode) {
                case AppSettingsView.INDEX_ACCOUNT:
                    showAccountSettingsFragment();
                    break;
                case AppSettingsView.INDEX_DEVICES:
                    showDevicesFragment();
                    break;
                case AppSettingsView.INDEX_NOTIFICATIONS:
                    showNotifications();
                    break;
                case AppSettingsView.INDEX_EXPANSIONS:
                    showExpansions();
                    break;
                case AppSettingsView.INDEX_VOICE:
                    showVoiceFragment();
                    break;
                case AppSettingsView.INDEX_NIGHT_MODE:
                    showNightModeSettings();
                    break;
                case AppSettingsView.INDEX_SUPPORT:
                    showSupportFragment();
                    break;
                case AppSettingsView.INDEX_SHARE:
                    showShare();
                    break;
                case AppSettingsView.INDEX_DEBUG:
                    showDebugActivity();
                    break;
            }
        }
    }

    private void showNightModeSettings() {
        startActivity(new Intent(this, NightModeActivity.class));
    }
    //endregion

    //region methods
    public void showAppSettingsFragment() {
        pushFragment(new AppSettingsFragment(), null, false);
    }

    public void showAccountSettingsFragment() {
        showFragment(AccountSettingsFragment.class, R.string.label_account, true);
    }

    public void showDevicesFragment() {
        showFragment(DeviceListFragment.class, R.string.label_devices, false);
    }

    public void showNotifications() {
        NotificationActivity.startActivity(this);
    }

    public void showExpansions() {
        startActivity(new Intent(this, ExpansionSettingsActivity.class));
    }

    public void showVoiceFragment() {
        startActivity(new Intent(this, VoiceSettingsActivity.class));
    }

    public void showSupportFragment() {
        showFragment(SupportFragment.class, R.string.action_support, false);
    }

    public void showShare() {
        Analytics.trackEvent(Analytics.Backside.EVENT_TELL_A_FRIEND_TAPPED, null);
        Share.text(getString(R.string.tell_a_friend_body))
             .withSubject(getString(R.string.tell_a_friend_subject))
             .send(this);
    }

    public void showDebugActivity() {
        Distribution.startDebugActivity(this);
    }

    private void showFragment(@NonNull final Class<? extends Fragment> fragmentClass,
                              @StringRes final int titleRes,
                              final boolean lockOrientation) {
        final is.hello.sense.ui.common.FragmentNavigationActivity.Builder builder =
                new is.hello.sense.ui.common.FragmentNavigationActivity.Builder(this);
        builder.setDefaultTitle(titleRes);
        builder.setFragmentClass(fragmentClass);
        if (lockOrientation) {
            builder.setOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        startActivity(builder.toIntent());

    }
    //endregion

}
