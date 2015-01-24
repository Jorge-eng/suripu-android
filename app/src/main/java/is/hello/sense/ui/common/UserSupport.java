package is.hello.sense.ui.common;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.api.model.Enums;
import is.hello.sense.util.Analytics;

public class UserSupport {
    private static final String SUPPORT_URL = "https://support.hello.is";

    public static void show(@NonNull Context from) {
        Analytics.trackEvent(Analytics.EVENT_HELP, null);
        from.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(SUPPORT_URL)));
    }

    public static void showForOnboardingStep(@NonNull Context from, @NonNull OnboardingStep onboardingStep) {
        Analytics.trackEvent(Analytics.EVENT_HELP, Analytics.createProperties(Analytics.PROP_HELP_STEP, onboardingStep.toString()));
        from.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(SUPPORT_URL)));
    }

    public static void showForDeviceIssue(@NonNull Context from, @NonNull DeviceIssue issue) {
        Analytics.trackEvent(Analytics.EVENT_HELP, Analytics.createProperties(Analytics.PROP_HELP_STEP, issue));
        from.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(SUPPORT_URL)));
    }


    public static enum DeviceIssue {
        WIFI_CONNECTIVITY,
        SENSE_MISSING,
        SLEEP_PILL_MISSING,
    }

    public static enum OnboardingStep {
        INFO,
        BLUETOOTH,
        ENHANCED_AUDIO,
        SETUP_SENSE,
        WIFI_SCAN,
        SIGN_INTO_WIFI,
        PILL_PLACEMENT;


        public static OnboardingStep fromString(@Nullable String string) {
            return Enums.fromString(string, values(), INFO);
        }
    }
}