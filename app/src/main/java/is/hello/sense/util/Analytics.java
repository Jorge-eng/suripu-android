package is.hello.sense.util;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDelegate;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Severity;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;

import org.joda.time.DateTime;

import java.util.Locale;
import java.util.Set;

import is.hello.commonsense.util.Errors;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.BuildConfig;
import is.hello.sense.SenseApplication;
import is.hello.sense.api.gson.Enums;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.model.SenseDevice;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.ui.handholding.TutorialOverlayView;

public class Analytics {
    public static final String LOG_TAG = Analytics.class.getSimpleName();
    public static final String PLATFORM = "android";

    /**
     * Memory leak warning from Android Studio
     */
    private static @Nullable com.segment.analytics.Analytics segment;

    public interface OnEventListener {
        void onSuccess();
    }

    public interface Global {

        /**
         * iOS | android
         */
        String TRAIT_PLATFORM = "Platform";

        /**
         * The version code for the current build. Was
         * provided by Mixpanel before switching to Segment.
         */
        String TRAIT_APP_RELEASE = "Android App Release";

        /**
         * The version name for the current build. Was
         * provided by Mixpanel before switching to Segment.
         */
        String TRAIT_APP_VERSION = "Android App Version";

        /**
         * The model of the device the app is running on. Was
         * provided by Mixpanel before switching to Segment.
         */
        String TRAIT_DEVICE_MODEL = "Android Device Model";

        /**
         * The manufacturer of the device the app is running on.
         * Was provided by Mixpanel before switching to Segment.
         */
        String TRAIT_DEVICE_MANUFACTURER = "Android Device Manufacturer";

        /**
         * The version of the analytics library in use. Was
         * provided by Mixpanel before switching to Segment.
         */
        String TRAIT_LIB_VERSION = "Android Lib Version";

        /**
         * The user's preferred country code. Was provided by Mixpanel before switching to Segment.
         */
        String TRAIT_COUNTRY_CODE = "Country Code";

        /**
         * The account id of the user
         */
        String TRAIT_ACCOUNT_ID = "Account Id";

        /**
         * The account email of the user
         */
        String TRAIT_ACCOUNT_EMAIL = "email";

        /**
         * The id of the user's Sense.
         */
        String TRAIT_SENSE_ID = "Sense Id";

        /**
         * The last bonded hardware version of the user's Sense.
         * Do not set after factory reset or unpair like for sense id.
         * Should be one of {@link is.hello.sense.api.model.SenseDevice.HardwareVersion}
         */
        String TRAIT_SENSE_VERSION = "Sense Version";

        /**
         * The default value after factory reset or unpaired
         * used for {@link Analytics.Global#TRAIT_SENSE_ID}
         */
        String PROP_SENSE_UNPAIRED = "unpaired";


        /**
         * Anytime an error is encountered, even if it came from server.  MAKE SURE you don't log Error in a loop ... I've seen it happen where 10,000 events get logged :)
         */
        String EVENT_ERROR = "Error";
        String PROP_ERROR_MESSAGE = "message";
        String PROP_ERROR_TYPE = "type";
        String PROP_ERROR_CONTEXT = "context";
        String PROP_ERROR_OPERATION = "operation";
        String EVENT_WARNING = "Warning";

        /**
         * When the user signs in
         */
        String EVENT_SIGNED_IN = "Signed In";

        /**
         * When the user signs out
         */
        String EVENT_SIGNED_OUT = "Signed Out";


        /**
         * When the user opens the app
         */
        String APP_LAUNCHED = "App Launched";


        /**
         * When the user agrees to using high power scans.
         */
        String EVENT_TURN_ON_HIGH_POWER = "High power mode enabled";

        /**
         * When the app is activated by a third party through the AlarmClock intents.
         */
        String EVENT_ALARM_CLOCK_INTENT = "Alarm clock intent";
        String PROP_ALARM_CLOCK_INTENT_NAME = "name";


        String PROP_BLUETOOTH_PAIRED_DEVICE_COUNT = "Paired device count";
        String PROP_BLUETOOTH_CONNECTED_DEVICE_COUNT = "Connected device count";
        String PROP_BLUETOOTH_HEADSET_CONNECTED = "Headset connected";
        String PROP_BLUETOOTH_A2DP_CONNECTED = "A2DP connected";
        String PROP_BLUETOOTH_HEALTH_DEVICE_CONNECTED = "Health device connected";

        String EVENT_SHARE= "Share";
        String PROP_INSIGHT= "Insight";
        String PROP_INSIGHT_CATEGORY= "Category";
        String PROP_TYPE = "Type";

        /**
         * Whenever user taps on a "help" button
         */
        String EVENT_HELP = "Help";
        String PROP_HELP_STEP = "help_step";

        /**
         * Whenever directed to features section of hello site
         */
        String EVENT_VIEW_FEATURES = "View Features";
    }

    public interface Onboarding {

        /**
         * Whenever user taps on a "play" button within the Onboarding flow
         */
        String EVENT_PLAY_VIDEO = "Play Video";

        /**
         * Whenever user taps on a "skip" button within the onboarding flow
         */
        String EVENT_SKIP = "Onboarding Skip";
        String EVENT_SKIP_IN_APP = "Skip";
        String PROP_SKIP_SCREEN = "Screen";

        /**
         * Whenever user taps a "back" button within the onboarding flow.
         */
        String EVENT_BACK = "Back";

        /**
         * When the user lands on Sign In screen.
         */
        String EVENT_SIGN_IN = "Sign In Start";

        /**
         * When user lands on the Have Sense ready? screen
         */
        String EVENT_START = "Onboarding Start";

        /**
         * When the user swipes the intro pages
         */
        String EVENT_INTRO_SWIPED = "Onboarding intro swiped";

        String PROP_SCREEN = "screen";

        /**
         * When the user lands on the unsupported device screen.
         *
         * @see #PROP_DEVICE_SUPPORT_LEVEL
         */
        String EVENT_UNSUPPORTED_DEVICE = "Onboarding Unsupported Device";

        String PROP_DEVICE_SUPPORT_LEVEL = "device_support_level";

        /**
         * When user lands on the Sign Up screen
         */
        String EVENT_ACCOUNT = "Onboarding Account";
        /**
         * When user successfully uploads profile picture during onboarding
         */
        String EVENT_CHANGE_PROFILE_PHOTO = "Onboarding Change Profile Photo";

        /**
         * When user successfully deletes profile picture during onboarding
         */
        String EVENT_DELETE_PROFILE_PHOTO = "Onboarding Delete Profile Photo";

        /**
         * When user taps "I don't have a Sense" button.
         */
        String EVENT_NO_SENSE = "I don't have a Sense";

        /**
         * User lands on Birthday screen (do not log if user comes from Settings)
         */
        String EVENT_BIRTHDAY = "Onboarding Birthday";
        /**
         * User lands on Gender screen (do not log if user comes from Settings)
         */
        String EVENT_GENDER = "Onboarding Gender";
        /**
         * User lands on Height screen (do not log if user comes from Settings)
         */
        String EVENT_HEIGHT = "Onboarding Height";

        /**
         * User lands on Weight screen (do not log if user comes from Settings)
         */
        String EVENT_WEIGHT = "Onboarding Weight";

        /**
         * User lands on Location screen (do not log if user comes from Settings)
         */
        String EVENT_LOCATION = "Onboarding Location";

        /**
         * User lands on Enhanced Audio screen
         */
        String EVENT_SENSE_AUDIO = "Onboarding Sense Audio";

        /**
         * When user lands on the pairing mode help screen (not glowing purple)
         */
        String EVENT_PAIRING_MODE_HELP = "Onboarding Pairing Mode Help";

        /**
         * When user lands on the Setting up Sense screen
         */
        String EVENT_SENSE_SETUP = "Onboarding Sense Setup";

        /**
         * When user lands on the "Pair your Sense" screen
         */
        String EVENT_PAIR_SENSE = "Onboarding Pair Sense";

        /**
         * When the user successfully pairs a sense
         */
        String EVENT_SENSE_PAIRED = "Onboarding Sense Paired";

        /**
         * When user lands on the screen to scan for wifi
         */
        String EVENT_WIFI = "Onboarding WiFi";

        /**
         * When the user implicitly scans for wifi networks.
         */
        String EVENT_WIFI_SCAN = "Onboarding WiFi Scan";

        /**
         * When the user explicitly rescans for wifi networks.
         */
        String EVENT_WIFI_RESCAN = "Onboarding WiFi Rescan";

        /**
         * When the user lands on the "Enter Wifi Password" screen
         */
        String EVENT_WIFI_PASSWORD = "Onboarding WiFi Password";

        String PROP_WIFI_IS_OTHER = "Is Other";

        String PROP_WIFI_RSSI = "RSSI";

        /**
         * When the user or the app sends WiFi credentials to Sense.
         */
        String EVENT_WIFI_CREDENTIALS_SUBMITTED = "Onboarding WiFi Credentials Submitted";

        String PROP_WIFI_SECURITY_TYPE = "Security Type";

        /**
         * Internal logging updates from Sense.
         *
         * @see #PROP_SENSE_WIFI_STATUS
         */
        String EVENT_SENSE_WIFI_UPDATE = "Onboarding Sense WiFi Update";

        String PROP_SENSE_WIFI_STATUS = "status";
        String PROP_SENSE_WIFI_HTTP_RESPONSE_CODE = "http_response_code";
        String PROP_SENSE_WIFI_SOCKET_ERROR_CODE = "socket_error_code";

        /**
         * When the user lands on the "Sleep Pill" intro screen.
         */
        String EVENT_PILL_INTRO = "Onboarding Sleep Pill";
        String EVENT_PILL_INTRO_IN_APP = "Sleep Pill";

        /**
         * When user lands on the "Pairing your Sleep Pill" screen
         */
        String EVENT_PAIR_PILL = "Onboarding Pair Pill";

        /**
         * When user lands on the "Pairing your Sleep Pill" screen
         */
        String EVENT_PILL_PAIRED = "Onboarding Pill Paired";

        /**
         * When user lands on screen where it asks user to place the pill on the pillow
         */
        String EVENT_PILL_PLACEMENT = "Onboarding Pill Placement";

        /**
         * When user lands on the screen that explains what the colors of Sense mean.  also known as 'before you sleep"
         */
        String EVENT_SENSE_COLORS = "Onboarding Sense Colors";

        /**
         * When user is shown the Room Check screen
         */
        String EVENT_ROOM_CHECK = "Onboarding Room Check";

        /**
         * When error occurs during fetching room conditions
         */
        String ERROR_MSG_ROOM_CHECK = "Room check";

        /**
         * When user is asked to set up their smart alarm during onboarding
         */
        String EVENT_FIRST_ALARM = "Onboarding First Alarm";

        /**
         * When user has voice feature and enters screen to test voice command
         */
        String EVENT_VOICE_TUTORIAL = "Onboarding Voice Tutorial";

        /**
         * When user has voice feature and presses skip button
         */
        String EVENT_VOICE_TUTORIAL_SKIP = "Onboarding Voice Tutorial Skip";

        /**
         * Status of voice command returned
         */
        String EVENT_VOICE_COMMAND = "Onboarding Voice Command";
        String PROP_VOICE_COMMAND_STATUS = "status";

        /**
         * When user lands on the last onboarding Screen
         */
        String EVENT_END = "Onboarding End";

        /**
         * When the user long presses on the help button and accesses our secret support menu.
         */
        String EVENT_SUPPORT_OPTIONS = "Support options activated";

        String EVENT_PAIR_PILL_RETRY = "Onboarding Pair Pill Retry";
    }

    public interface Timeline {
        String EVENT_TIMELINE = "Timeline";
        String PROP_DATE = "date";

        String EVENT_TIMELINE_SWIPE = "Timeline swipe";

        String EVENT_SLEEP_SCORE_BREAKDOWN = "Sleep Score breakdown";
        String EVENT_SHARE = "Share Timeline";


        String EVENT_ZOOMED_OUT = "Timeline zoomed out";
        String EVENT_ZOOMED_IN = "Timeline zoomed in";

        String EVENT_TAP = "Timeline tap";
        String EVENT_TIMELINE_EVENT_TAPPED = "Timeline Event tapped";
        String EVENT_LONG_PRESS_EVENT = "Long press sleep duration bar";

        String EVENT_ADJUST_TIME = "Timeline event time adjust";
        String EVENT_CORRECT = "Timeline event correct";
        String EVENT_INCORRECT = "Timeline event incorrect";
        String EVENT_REMOVE = "Timeline event remove";
        String PROP_TYPE = "Type";


        String EVENT_SYSTEM_ALERT = "System Alert";

        String PROP_SYSTEM_ALERT_TYPE = "Type";
        int SYSTEM_ALERT_TYPE_PILL_LOW_BATTERY = 5;
        int SYSTEM_ALERT_TYPE_PILL_FIRMWARE_UPDATE_AVAILABLE = 8;

        String EVENT_SYSTEM_ALERT_ACTION = "System Alert Action";

        String PROP_EVENT_SYSTEM_ALERT_ACTION = "Action";
        String PROP_EVENT_SYSTEM_ALERT_ACTION_NOW = "now";
        String PROP_EVENT_SYSTEM_ALERT_ACTION_LATER = "later";
    }

    public interface Backside {

        String EVENT_CURRENT_CONDITIONS = "Current Conditions";
        String EVENT_SENSOR_HISTORY = "Sensor History";
        String PROP_SENSOR_NAME = "sensor_name";

        String EVENT_TRENDS = "Trends";

        String EVENT_INSIGHT_DETAIL = "Insight Detail";
        String EVENT_QUESTION = "Question";
        String EVENT_SKIP_QUESTION = "Skip Question";
        String EVENT_ANSWER_QUESTION = "Answer Question";

        String EVENT_ALARMS = "Alarms";
        String EVENT_NEW_ALARM = "New Alarm";
        String EVENT_ALARM_SAVED = "Alarm Saved";
        String PROP_ALARM_DAYS_REPEATED = "days_repeated";
        String PROP_ALARM_ENABLED = "enabled";
        String PROP_ALARM_IS_SMART = "smart_alarm";
        String PROP_ALARM_HOUR = "hour";
        String PROP_ALARM_MINUTE = "minute";

        String EVENT_ALARM_ON_OFF = "Alarm On/Off";
        String EVENT_EDIT_ALARM = "Edit Alarm";

        String EVENT_SETTINGS = "Settings";
        String EVENT_ACCOUNT = "Account";


        String EVENT_DEVICES = "Devices";
        String EVENT_SENSE_DETAIL = "Sense detail";
        String EVENT_REPLACE_SENSE = "Replace Sense";
        String EVENT_PUT_INTO_PAIRING_MODE = "Put into Pairing Mode";
        String EVENT_FACTORY_RESET = "Factory Reset";
        String EVENT_EDIT_WIFI = "Edit WiFi";
        String EVENT_SENSE_ADVANCED = "Sense advanced tapped";
        String EVENT_TIME_ZONE = "Time Zone";
        String EVENT_TIME_ZONE_CHANGED = "Time Zone Changed";
        String PROP_TIME_ZONE = "tz";

        String EVENT_PILL_DETAIL = "Pill detail";
        String EVENT_REPLACE_PILL = "Replace Pill";
        String EVENT_REPLACE_BATTERY = "Replace Battery";
        String EVENT_PILL_ADVANCED = "Pill advanced tapped";

        String EVENT_TROUBLESHOOTING_LINK = "Troubleshooting link";
        String PROP_TROUBLESHOOTING_ISSUE = "issue";

        String EVENT_NOTIFICATIONS = "Notifications";
        String EVENT_UNITS_TIME = "Units/Time";
        String EVENT_SIGN_OUT = "Sign Out";

        String EVENT_HELP = "Settings Help";
        String EVENT_CONTACT_SUPPORT = "Contact Support";
        String EVENT_TELL_A_FRIEND_TAPPED = "Tell a friend tapped";

        String EVENT_CHANGE_TRENDS_TIMESCALE = "Change trends timescale";
        String EVENT_TIMESCALE = "timescale";
        String EVENT_TIMESCALE_WEEK = "week";
        String EVENT_TIMESCALE_MONTH = "month";
        String EVENT_TIMESCALE_QUARTER = "quarter";

        /**
         * User views the Voice tab list of commands
         */
        String EVENT_VOICE_TAB = "Voice";

        /**
         * User taps to see all examples of a "category" from the list
         */
        String EVENT_VOICE_EXAMPLES = "Voice Examples";

        String PROP_VOICE_EXAMPLES = "category";
    }

    public interface General{
        String EVENT_HELP = "Help";
        /**
         * When user lands on the No BLE screen
         */
        String EVENT_NO_BLE = "No BLE";

    }

    public interface SleepSounds {
        String EVENT_SLEEP_SOUNDS = "Sleep sounds";
        String EVENT_SLEEP_SOUNDS_PLAY = "Play sleep sound";
        String EVENT_SLEEP_SOUNDS_STOP = "Stop sleep sound";
        String PROP_SLEEP_SOUNDS_SOUND_ID = "sound id";
        String PROP_SLEEP_SOUNDS_DURATION_ID = "duration id";
        String PROP_SLEEP_SOUNDS_VOLUME = "volume";
    }

    public interface StoreReview {
        String SHOWN = "App review shown";
        String START = "App review start";
        String ENJOY_SENSE = "Enjoy Sense";
        String DO_NOT_ENJOY_SENSE = "Do not enjoy Sense";
        String HELP_FROM_APP_REVIEW = "Help from app review";
        String RATE_APP = "Rate app";
        String RATE_APP_AMAZON = "Rate on Amazon";
        String DO_NOT_ASK_TO_RATE_APP_AGAIN = "Do not ask to rate app again";
        String FEEDBACK_FROM_APP_REVIEW = "Feedback from app review";
        String APP_REVIEW_COMPLETED_WITH_NO_ACTION = "App review completed with no action";
        String APP_REVIEW_SKIP = "App review skip";
    }

    public interface Permissions {
        String EVENT_WE_NEED_LOCATION = "Location Permission Explanation";
        String EVENT_LOCATION_DISABLED = "Location Not Granted";
        String EVENT_LOCATION_MORE_INFO = "Location Permission More Info";
        String EVENT_WE_NEED_STORAGE = "External Storage Permission Explanation";
        String EVENT_STORAGE_DISABLED = "External Storage Not Granted";
        String EVENT_STORAGE_MORE_INFO = "External Storage Permission More Info";
        String EVENT_GALLERY_MORE_INFO = "Photo Library More Info";
    }

    public interface Account {
        String EVENT_CHANGE_EMAIL = "Change Email";
        String EVENT_CHANGE_PASSWORD = "Change password";
        String EVENT_CHANGE_NAME = "Change Name";
        String EVENT_CHANGE_PROFILE_PHOTO = "Change Profile Photo";
        String EVENT_DELETE_PROFILE_PHOTO = "Delete Profile Photo";
    }

    public interface ProfilePhoto {
        String PROP_SOURCE = "source";

        enum Source implements Enums.FromString {
            FACEBOOK("facebook"),
            CAMERA("camera"),
            GALLERY("photo library"),
            UNKNOWN("unknown");
            private final String src;
            Source(@NonNull final String source){
                this.src = source;
            }

            public static Source fromString(@NonNull final String value){
                return Enums.fromString(value, values(), UNKNOWN);
            }

        }

    }

    /**
     * Breadcrumb end events are tracked when {@link is.hello.sense.ui.handholding.Tutorial#wasDismissed(Context)}
     * usually during {@link TutorialOverlayView#interactionCompleted()}
     */
    public interface Breadcrumb {
        String PROP_SOURCE = "source";
        String PROP_DESCRIPTION = "description";

        enum Source{
            ACCOUNT("account"),
            TRENDS("trends"),
            INSIGHTS("insights"),
            TIMELINE("timeline"),
            SENSOR_GRAPH("sensor graph");
            private final String src;
            Source(@NonNull final String source){
                this.src = source;
            }
        }

        enum Description{
            SWIPE_TIMELINE("swipe timeline"),
            ZOOM_OUT_TIMELINE("zoom out timeline"),
            TAP_INSIGHT_CARD("tap insight card"),
            TAP_NAME("tap name"),
            SCRUB_GRAPH("scrub graph");
            private final String desc;
            Description(@NonNull final String desc){
                this.desc = desc;
            }
        }

    }

    public interface Settings {
        /**
         * When user lands on the pairing mode help screen (not glowing purple)
         */
        String EVENT_PAIRING_MODE_HELP = "Pairing Mode Help";

        /**
         * When user lands on the Setting up Sense screen
         */
        String EVENT_SENSE_SETUP = "Sense Setup";

        /**
         * When user lands on the "Pair your Sense" screen
         */
        String EVENT_PAIR_SENSE = "Pair Sense";

        /**
         * When the user successfully pairs a sense
         */
        String EVENT_SENSE_PAIRED = "Sense Paired";

        /**
         * When user lands on the screen to scan for wifi
         */
        String EVENT_WIFI = "WiFi";

        /**
         * When the user implicitly scans for wifi networks
         */
        String EVENT_WIFI_SCAN = "WiFi Scan";

        /**
         * When the user explicitly rescans for wifi networks
         */
        String EVENT_WIFI_RESCAN = "WiFi Rescan";

        /**
         * When the user lands on the "Enter Wifi Password" screen
         */
        String EVENT_WIFI_PASSWORD = "WiFi Password";

        /**
         * When the user or the app sends WiFi credentials to Sense
         */
        String EVENT_WIFI_CREDENTIALS_SUBMITTED = "WiFi Credentials Submitted";

        /**
         * Internal logging updates from Sense in app.
         */
        String EVENT_SENSE_WIFI_UPDATE = "Sense WiFi Update";

        /**
         * When user lands on the "Pairing your Sleep Pill" screen inside the app
         */
        String EVENT_PILL_PAIRED = "Pill Paired";


        /**
         * When user lands on the "Pairing your Sleep Pill" screen inside the app
         */
        String EVENT_PAIR_PILL = "Pair Pill";

        String EVENT_PAIR_PILL_RETRY = "Pair Pill Retry";

    }

    /**
     *  {@link this#EVENT_START} - Fire when user reaches "Updating your Sleep Pill" screen after tapping "Update" button or "Update Sleep Pill firmware" row or "Update Now" pop-up button
     *
     *  {@link this#EVENT_OTA_START} - Fire when Pill OTA firmware update starts to transfer
     *
     *  {@link this#EVENT_OTA_COMPLETE} - Fire when Pill OTA firmware update completes transfer
     */
    public interface PillUpdate {
        String EVENT_START = "Pill Update Start";
        String EVENT_OTA_START = "Pill Update OTA Start";
        String EVENT_OTA_COMPLETE = "Pill Update Complete";

        interface Error {
            String PHONE_BATTERY_LOW = "Pill Update Phone Battery Low";
            String PILL_NOT_DETECTED = "Pill Update Pill Not Detected";
            String PILL_TOO_FAR = "Pill Update Pill Too Far";
            String PILL_OTA_FAIL = "Pill Update OTA Failed";
        }

    }

    /**
     *  {@link this#EVENT_ENTER} - fire when user lands on the sense update required screen
     *
     *  {@link this#EVENT_START} - fire when user taps on continue and a force ota is triggered
     *
     *  {@link this#EVENT_STATUS} - Fire when the status changes from the previous status.\nProperties: { {@link this#PROPERTY_NAME}: <enum>value from server</enum>}
     *
     *  {@link this#EVENT_END} - Fire when the status returned becomes 'COMPLETE'
     */
    public interface SenseOTA {
        String EVENT_ENTER = "Sense DFU";
        String EVENT_START = "Sense DFU begin";
        String EVENT_STATUS = "Sense DFU Status";
        String EVENT_END = "Sense DFU end";
        String PROPERTY_NAME = "status";
    }

    public interface Upgrade {
        String ERROR_SENSE_REQUIRED = "No Previously Paired Sense Found";
        String ERROR_SWAP_API_STATUS = "Swap Api Status was not OK";

        /**
         * When user lands on the pairing mode help screen (not glowing purple)
         */
        String EVENT_PAIRING_MODE_HELP = "Upgrade Pairing Mode Help";

        /**
         * When user lands on the screen to scan for wifi in the app
         */
        String EVENT_WIFI = "Upgrade WiFi";

        /**
         * When the user implicitly scans for wifi networks.
         */
        String EVENT_WIFI_SCAN = "Upgrade WiFi Scan";

        /**
         * When the user explicitly rescans for wifi networks
         */
        String EVENT_WIFI_RESCAN = "Upgrade WiFi Rescan";

        /**
         * When the user lands on the "Enter Wifi Password" screen
         */
        String EVENT_WIFI_PASSWORD = "Upgrade WiFi Password";

        String PROP_WIFI_IS_OTHER = "Is Other";

        String PROP_WIFI_RSSI = "RSSI";

        /**
         * When user sends WiFi credentials
         */
        String EVENT_WIFI_CREDENTIALS_SUBMITTED = "Upgrade WiFi Credentials Submitted";

        String PROP_WIFI_SECURITY_TYPE = "Security Type";

        /**
         * Internal logging updates from Sense in update flow.
         *
         * @see #PROP_SENSE_WIFI_STATUS
         */
        String EVENT_SENSE_WIFI_UPDATE = "Upgrade Sense WiFi Update";

        String PROP_SENSE_WIFI_STATUS = "status";
        String PROP_SENSE_WIFI_HTTP_RESPONSE_CODE = "http_response_code";
        String PROP_SENSE_WIFI_SOCKET_ERROR_CODE = "socket_error_code";

        String EVENT_PAIR_SENSE = "Upgrade Pair Sense";
        String EVENT_SENSE_PAIRED = "Upgrade Sense Paired";

        String EVENT_SWAP_ACCOUNTS_REQUEST = "Upgrade Swap Accounts Request";
        String EVENT_SWAPPED_ACCOUNTS = "Upgrade Account Swapped";
        /**
         * User taps to factory reset during at end of upgrade flow
         */
        String EVENT_FACTORY_RESET = "Upgrade Factory Reset";

        /**
         * User taps "set up" button
         */
        String EVENT_SENSE_VOICE_START = "Upgrade Sense Voice Start";

        String EVENT_PAIR_PILL = "Upgrade Pair Pill";

        String EVENT_PAIR_PILL_RETRY = "Upgrade Pair Pill Retry";

        String EVENT_PILL_PAIRED = "Upgrade Pill Paired";

        String EVENT_HELP = "Upgrade Help";

        /**
         * "Purchase sense with voice" button tapped.
         */
        String EVENT_PURCHASE_SENSE_VOICE = "Purchase Sense Voice";

        String EVENT_UPGRADE_SENSE = "Upgrade Sense";

    }

    public interface Notification {

        /**
         * tracked when app was launched from the notification
         */
        String EVENT_OPEN = "Open Notification";

        /**
         * value should correspond to what is in the payload for {@link is.hello.sense.notifications.Notification#type}
         */
        String PROP_TYPE = "type";

        /**
         * value should correspond to what is in the payload for {@link is.hello.sense.notifications.Notification#detail}
         */
        String PROP_DETAIL = "detail";

    }

    public interface NightMode {

        String TRAIT = "Night Mode";
        /**
         * This property is sent and updated when user taps on / changes one of the options.
         * If user hasn't made a selection, the property should not appear
         */
        String EVENT_CHANGED = "Changed Night Mode";

        String PROP_SETTING = "setting";

        String PROP_ON = "on";
        String PROP_OFF = "off";
        String PROP_AUTO = "auto";
    }

    private static String getNightModeProperty(@AppCompatDelegate.NightMode final int mode) {
        final String property;
        switch (mode) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                property = Analytics.NightMode.PROP_OFF;
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                property = Analytics.NightMode.PROP_ON;
                break;
            case AppCompatDelegate.MODE_NIGHT_AUTO:
                property = Analytics.NightMode.PROP_AUTO;
                break;
            default:
                property = NightMode.PROP_OFF;
        }
        return property;
    }


    //region Lifecycle

    public static void initialize(@NonNull final Context context) {
        final com.segment.analytics.Analytics.Builder builder =
                new com.segment.analytics.Analytics.Builder(context, BuildConfig.SEGMENT_API_KEY);
        builder.flushQueueSize(1);
        if (BuildConfig.DEBUG) {
            builder.logLevel(com.segment.analytics.Analytics.LogLevel.VERBOSE);
        }
        Analytics.segment = builder.build();
        com.segment.analytics.Analytics.setSingletonInstance(segment);
    }

    @SuppressWarnings("UnusedParameters")
    public static void onResume(@NonNull final Activity activity) {
    }

    @SuppressWarnings("UnusedParameters")
    public static void onPause(@NonNull final Activity activity) {
        if (segment == null) {
            return;
        }

        segment.flush();
    }

    //endregion


    //region User Identity

    private static Traits createBaseTraits() {
        final Traits traits = new Traits();
        traits.put(Global.TRAIT_PLATFORM, PLATFORM);
        traits.put(Global.TRAIT_APP_RELEASE, Integer.toString(BuildConfig.VERSION_CODE, 10));
        traits.put(Global.TRAIT_APP_VERSION, BuildConfig.VERSION_NAME);
        traits.put(Global.TRAIT_DEVICE_MODEL, Build.MODEL);
        traits.put(Global.TRAIT_DEVICE_MANUFACTURER, Build.MANUFACTURER);
        traits.put(Global.TRAIT_LIB_VERSION, com.segment.analytics.core.BuildConfig.VERSION_NAME);
        traits.put(Global.TRAIT_COUNTRY_CODE, Locale.getDefault().getCountry());
        return traits;
    }

    /**
     * @param accountId to track user
     * @param includeSegment to use segmentation analytics
     * @return true if user tracking started successfully
     */
    public static boolean trackUserIdentifier(@NonNull final String accountId,
                                              final boolean includeSegment) {
        if (accountId.isEmpty()) {
            Logger.warn(Analytics.LOG_TAG, "Unable to begin session for empty accountId");
            return false;
        }
        Logger.info(Analytics.LOG_TAG, "Began session for " + accountId);

        if (!SenseApplication.isRunningInRobolectric()) {
            Bugsnag.setUserId(accountId);

            if (includeSegment && segment != null) {
                segment.identify(accountId);
                segment.flush();
            }
        }

        return true;
    }

    public static void trackRegistration(@NonNull final String accountId,
                                         @Nullable final String name,
                                         @Nullable final String email,
                                         @NonNull final DateTime created) {
        Logger.info(LOG_TAG, "Tracking user sign up { accountId: '" + accountId +
                "', name: '" + name + "', email: '" + email + "', created: '" + created + "' }");
        if (segment == null) {
            return;
        }

        Analytics.trackEvent(Analytics.Global.EVENT_SIGNED_IN, null);

        segment.alias(accountId);

        final Traits traits = createBaseTraits();
        traits.putCreatedAt(created.toString());
        traits.putName(name);
        traits.put(Global.TRAIT_ACCOUNT_ID, accountId);
        traits.put(Global.TRAIT_ACCOUNT_EMAIL, email);
        segment.identify(traits);
        segment.flush();

        trackUserIdentifier(accountId, false);
    }

    public static void trackSignIn(@NonNull final String accountId,
                                   @Nullable final String name,
                                   @Nullable final String email) {
        if (segment == null) {
            return;
        }

        trackUserIdentifier(accountId, true);
        Analytics.trackEvent(Analytics.Global.EVENT_SIGNED_IN, null);

        final Traits traits = createBaseTraits();
        traits.put(Global.TRAIT_ACCOUNT_ID, accountId);

        if (name != null) {
            traits.putName(name);
        }

        if (email != null) {
            traits.put(Global.TRAIT_ACCOUNT_EMAIL, email);
        }

        segment.identify(traits);
        segment.flush();
    }

    public static void backFillUserInfo(@Nullable final String name, @Nullable final String email) {
        if (segment == null) {
            return;
        }

        final Traits traits = createBaseTraits();
        if (name != null) {
            traits.putName(name);
        }

        if (email != null) {
            traits.put(Global.TRAIT_ACCOUNT_EMAIL, email);
        }

        segment.identify(traits);
        segment.flush();
    }

    public static void trackNightMode(@AppCompatDelegate.NightMode final int mode) {
        final String property = getNightModeProperty(mode);
        trackTrait(NightMode.TRAIT, property);
        trackEvent(Analytics.NightMode.EVENT_CHANGED,
                   Analytics.createProperties(Analytics.NightMode.PROP_SETTING, property));
    }

    public static void signOut() {
        if (segment == null) {
            return;
        }

        segment.reset();
    }

    public static void resetSenseTraits(){
        setSenseId(Global.PROP_SENSE_UNPAIRED);
        // do not reset sense version so it will serve as last bonded sense version
        //setSenseVersion(Global.PROP_SENSE_UNPAIRED);
    }

    public static void setSenseId(@Nullable final String senseId) {
        if (!trackTrait(Global.TRAIT_SENSE_ID, senseId)) {
            return;
        }

        final Context context = SenseApplication.getInstance();
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit()
                   .putString(PreferencesInteractor.PAIRED_SENSE_ID, senseId)
                   .apply();
    }

    public static void setSenseVersion(@Nullable final SenseDevice.HardwareVersion hardwareVersion) {
        final Context context = SenseApplication.getInstance();
        trackTrait(Global.TRAIT_SENSE_VERSION,
                   hardwareVersion != null ?
                           context.getString(hardwareVersion.nameRes) : null
                  );
    }

    /**
     * @return true if trait will be tracked by analytics or false if invalid
     */
    private static boolean trackTrait(@NonNull final String trait, @Nullable final String prop){
        Logger.info(LOG_TAG, "Tracking " + trait + " " + prop);
        if (segment == null || prop == null) {
            return false;
        }

        final Traits traits = new Traits();
        traits.put(trait, prop);
        segment.identify(traits);
        return true;
    }

    public static String getSenseId() {
        final Context context = SenseApplication.getInstance();
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(PreferencesInteractor.PAIRED_SENSE_ID, "");
    }

    //endregion


    //region Events

    public static @NonNull Properties createProperties(@NonNull final Object... pairs) {
        if ((pairs.length % 2) != 0) {
            throw new IllegalArgumentException("even number of arguments required");
        }

        final Properties properties = new Properties();
        for (int i = 0; i < pairs.length; i += 2) {
            properties.put(pairs[i].toString(), pairs[i + 1]);
        }
        return properties;
    }

    private static boolean isConnected(final int connectionState) {
        return (connectionState == BluetoothAdapter.STATE_CONNECTING ||
                connectionState == BluetoothAdapter.STATE_CONNECTED);
    }

    public static @NonNull Properties createBluetoothTrackingProperties(@NonNull final Context context) {
        int bondedCount = 0,
            connectedCount = 0;

        boolean headsetConnected = false,
                a2dpConnected = false,
                healthConnected = false;

        final BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            final BluetoothAdapter adapter = bluetoothManager.getAdapter();

            if (adapter != null && adapter.isEnabled()) {
                final Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
                bondedCount = bondedDevices.size();
                for (final BluetoothDevice bondedDevice : bondedDevices) {
                    final int gattConnectionState =
                            bluetoothManager.getConnectionState(bondedDevice, BluetoothProfile.GATT);
                    if (isConnected(gattConnectionState)) {
                        connectedCount++;
                    }
                }

                headsetConnected = isConnected(adapter.getProfileConnectionState(BluetoothProfile.HEADSET));
                a2dpConnected = isConnected(adapter.getProfileConnectionState(BluetoothProfile.A2DP));
                healthConnected = isConnected(adapter.getProfileConnectionState(BluetoothProfile.HEALTH));
            }
        }

        return createProperties(Global.PROP_BLUETOOTH_PAIRED_DEVICE_COUNT, bondedCount,
                                Global.PROP_BLUETOOTH_CONNECTED_DEVICE_COUNT, connectedCount,
                                Global.PROP_BLUETOOTH_HEADSET_CONNECTED, headsetConnected,
                                Global.PROP_BLUETOOTH_A2DP_CONNECTED, a2dpConnected,
                                Global.PROP_BLUETOOTH_HEALTH_DEVICE_CONNECTED, healthConnected);
    }

    public static @NonNull Properties createProfilePhotoTrackingProperties(@NonNull final ProfilePhoto.Source source){
        return createProperties(ProfilePhoto.PROP_SOURCE, source.src);
    }

    public static @NonNull Properties createBreadcrumbTrackingProperties(@NonNull final Breadcrumb.Source source,
                                                                         @NonNull final Breadcrumb.Description description){
        return createProperties(Breadcrumb.PROP_SOURCE, source.src,
                                Breadcrumb.PROP_DESCRIPTION, description.desc);
    }

    public static void trackEvent(@NonNull final String event, @Nullable final Properties properties) {
        if (segment == null) {
            return;
        }

        segment.track(event, properties);

        Logger.analytic(event, properties);
    }

    public static void trackError(@NonNull final String message,
                                  @Nullable final String errorType,
                                  @Nullable final String errorContext,
                                  @Nullable final String errorOperation,
                                  final boolean isWarning) {

        final Properties properties = createProperties(Global.PROP_ERROR_MESSAGE, message,
                                                       Global.PROP_ERROR_TYPE, errorType,
                                                       Global.PROP_ERROR_CONTEXT, errorContext,
                                                       Global.PROP_ERROR_OPERATION, errorOperation);
        String event = Global.EVENT_ERROR;
        if (isWarning){
            event = Global.EVENT_WARNING;
        }
        trackEvent(event, properties);
    }

    public static void trackError(@Nullable final Throwable e, @Nullable final String errorOperation) {
        final StringRef message = Errors.getDisplayMessage(e);
        final String messageString;
        if (message != null && SenseApplication.getInstance() != null) {
            messageString = message.resolve(SenseApplication.getInstance());
        } else {
            messageString = "Unknown";
        }
        trackError(messageString, Errors.getType(e), Errors.getContextInfo(e), errorOperation, ApiException.isNetworkError(e));
    }

    public static void trackUnexpectedError(@Nullable final Throwable e) {
        if (e != null && !SenseApplication.isRunningInRobolectric()) {
            Bugsnag.notify(e, Severity.WARNING);
        }
    }

    //endregion
}