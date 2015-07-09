package is.hello.sense.ui.widget.timeline;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.SpannableStringBuilder;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.TimelineEvent;

public class TimelineInfoPopup {
    public static final long VISIBLE_DURATION = 2000;

    private final Activity activity;
    private final PopupWindow popupWindow;
    private final TextView contents;

    public TimelineInfoPopup(@NonNull Activity activity) {
        this.activity = activity;

        this.popupWindow = new PopupWindow(activity);
        popupWindow.setAnimationStyle(R.style.WindowAnimations_PopSlideAndFade);
        popupWindow.setTouchable(true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new ColorDrawable()); // Required for touch to dismiss
        popupWindow.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        this.contents = new TextView(activity);
        contents.setTextAppearance(activity, R.style.AppTheme_Text_Timeline);
        contents.setBackgroundResource(R.drawable.background_timeline_info_popup);

        Resources resources = activity.getResources();
        int paddingHorizontal = resources.getDimensionPixelSize(R.dimen.gap_medium),
            paddingVertical = resources.getDimensionPixelSize(R.dimen.gap_small);
        contents.setPadding(
            contents.getPaddingLeft() + paddingHorizontal,
            contents.getPaddingTop() + paddingVertical,
            contents.getPaddingRight() + paddingHorizontal,
            contents.getPaddingBottom() + paddingVertical
        );

        popupWindow.setContentView(contents);
    }

    private int getNavigationBarHeight() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Display display = activity.getWindowManager().getDefaultDisplay();

            Point realSize = new Point();
            display.getRealSize(realSize);

            Point visibleArea = new Point();
            display.getSize(visibleArea);

            // Status bar is counted as part of the display height,
            // so the delta just gives us the navigation bar height.
            return realSize.y - visibleArea.y;
        } else {
            return 0;
        }
    }

    public void bindEvent(@NonNull TimelineEvent event) {
        CharSequence prefix = activity.getText(R.string.timeline_popup_info_prefix);
        CharSequence sleepDepth = activity.getText(event.getSleepState().stringRes);

        SpannableStringBuilder reading = new SpannableStringBuilder(prefix).append(sleepDepth);
        contents.setText(reading);
    }

    public void show(@NonNull View fromView) {
        View parent = (View) fromView.getParent();
        int parentHeight = parent.getMeasuredHeight();
        int fromViewMiddle = (fromView.getTop() + fromView.getBottom()) / 2;
        int bottomInset = (parentHeight - fromViewMiddle) + getNavigationBarHeight();
        int leftInset = activity.getResources().getDimensionPixelSize(R.dimen.timeline_segment_event_vertical_inset);
        popupWindow.showAtLocation(parent, Gravity.BOTTOM | Gravity.START, leftInset, bottomInset);

        contents.postDelayed(this::dismiss, VISIBLE_DURATION);
    }

    public void dismiss() {
        popupWindow.dismiss();
    }
}
