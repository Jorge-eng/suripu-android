package is.hello.sense.util;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.support.annotation.NonNull;

import is.hello.commonsense.util.StringRef;
import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;

/**
 * Only fetches current battery levels on creation. To refresh values use {@link this#refresh(Context)}
 */
public class BatteryUtil {

    private Intent batteryStatus;

    public static ErrorDialogFragment getErrorDialog(){
        return new ErrorDialogFragment.Builder()
                .withTitle(R.string.error_phone_battery_low_title)
                .withMessage(StringRef.from(R.string.error_phone_battery_low_message))
                .build();
    }

    public BatteryUtil(@NonNull final Context context){
       refresh(context);
    }

    public boolean canPerformOperation(@NonNull final Operation operation){
        return operation.minBatteryPercentage <= getBatteryPercentage()
                && (!operation.requiresCharging() || isPluggedInAndCharging());
    }

    public boolean isPluggedInAndCharging(){
        final int chargeType = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        final int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return chargeType != 0 && (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL);
    }

    public double getBatteryPercentage(){
        final int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        final int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        Logger.debug(BatteryUtil.class.getSimpleName(), "battery level " + level);
        return level / (double) scale;
    }

    public void refresh(@NonNull final Context context){
        final IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        this.batteryStatus = context.registerReceiver(null, filter);
    }

    public static class Operation {
        final double minBatteryPercentage;
        final boolean requiresCharging;

        public static Operation pillUpdateOperationNoCharge() {
            return new Operation(0.20, false);
        }

        public static Operation pillUpdateOperationWithCharge(){
            return new Operation(0.15, true);
        }

        public Operation(){
            this(0, false);
        }

        public Operation(final double minimumBatteryPercentage, final boolean requiresCharging){
            this.minBatteryPercentage = minimumBatteryPercentage;
            this.requiresCharging = requiresCharging;
            runAssertions();
        }

        private void runAssertions(){
            if(BuildConfig.DEBUG) {
                if(minBatteryPercentage > 1 || minBatteryPercentage < 0){
                    throw new NumberFormatException("minBatteryPercentage must be between 0 - 1");
                }
            }
        }

        public boolean requiresCharging() {
            return requiresCharging;
        }
    }
}
