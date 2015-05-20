package is.hello.sense.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.ui.animation.AnimatorConfig;
import is.hello.sense.ui.animation.AnimatorContext;
import is.hello.sense.ui.animation.PropertyAnimatorProxy;
import is.hello.sense.ui.widget.FragmentPageView.Position;

public final class FragmentPageTitleStrip extends FrameLayout implements FragmentPageView.Decor {
    private final TextView textView1;
    private final TextView textView2;

    private final GradientDrawable fadeGradient;
    private final int fadeGradientWidth;

    private boolean textViewsSwapped = false;
    private Position swipeDirection;

    //region Lifecycle

    public FragmentPageTitleStrip(@NonNull Context context) {
        this(context, null);
    }

    public FragmentPageTitleStrip(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FragmentPageTitleStrip(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setFocusable(false);
        ViewCompat.setImportantForAccessibility(this, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);

        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        this.textView1 = createTextView();
        addView(textView1, layoutParams);

        this.textView2 = createTextView();
        textView2.setVisibility(INVISIBLE);
        addView(textView2, layoutParams);

        this.fadeGradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{
            getResources().getColor(R.color.background_timeline),
            Color.TRANSPARENT,
        });
        this.fadeGradientWidth = getResources().getDimensionPixelSize(R.dimen.gap_medium);
    }

    //endregion


    //region Edge Fading

    @Override
    public void draw(@NonNull Canvas canvas) {
        super.draw(canvas);

        int width = canvas.getWidth(),
            height = canvas.getHeight();

        fadeGradient.setBounds(0, 0, fadeGradientWidth, height);
        fadeGradient.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        fadeGradient.draw(canvas);

        fadeGradient.setBounds(width - fadeGradientWidth, 0, width, height);
        fadeGradient.setOrientation(GradientDrawable.Orientation.RIGHT_LEFT);
        fadeGradient.draw(canvas);
    }


    //endregion


    //region Internal Views

    private TextView createTextView() {
        TextView textView = new TextView(getContext());
        textView.setTextAppearance(getContext(), R.style.AppTheme_Text_ScreenTitle);
        textView.setGravity(Gravity.CENTER);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setSingleLine();
        return textView;
    }

    private TextView getForegroundTextView() {
        if (textViewsSwapped) {
            return textView2;
        } else {
            return textView1;
        }
    }

    private TextView getBackgroundTextView() {
        if (textViewsSwapped) {
            return textView1;
        } else {
            return textView2;
        }
    }

    private void swapTextViews() {
        this.textViewsSwapped = !textViewsSwapped;

        TextView background = getBackgroundTextView();
        background.setVisibility(INVISIBLE);
        background.setText(null);

        textView1.setTranslationX(0f);
        textView2.setTranslationX(0f);
    }

    //endregion


    //region Attributes

    public void setDimmed(boolean dimmed) {
        int textColor;
        if (dimmed) {
            textColor = getResources().getColor(R.color.text_dim);
        } else {
            textColor = getResources().getColor(R.color.text_dark);
        }
        textView1.setTextColor(textColor);
        textView2.setTextColor(textColor);
    }

    //endregion


    //region Decor

    @Override
    public void onSetOnScreenTitle(@Nullable CharSequence title) {
        getForegroundTextView().setText(title);
    }

    @Override
    public void onSetOffScreenTitle(@Nullable CharSequence title) {
        getBackgroundTextView().setText(title);
    }

    @Override
    public void onSwipeBegan() {
        this.swipeDirection = null;
    }

    @Override
    public void onSwipeMoved(float newAmount) {
        View background = getBackgroundTextView(),
             foreground = getForegroundTextView();

        if (swipeDirection == null) {
            background.setVisibility(VISIBLE);
        }

        this.swipeDirection = newAmount > 0.0 ? Position.BEFORE : Position.AFTER;


        float foregroundX = getMeasuredWidth() * newAmount;
        foreground.setTranslationX(foregroundX);

        float backgroundX;
        if (newAmount > 0f) {
            backgroundX = foregroundX - background.getMeasuredWidth();
        } else {
            backgroundX = foregroundX + background.getMeasuredWidth();
        }
        background.setTranslationX(backgroundX);
    }

    @Override
    public void onSwipeSnappedBack(long duration, @NonNull AnimatorConfig animatorConfig, @Nullable AnimatorContext animatorContext) {
        if (animatorContext == null) {
            return;
        }

        animatorContext.transaction(animatorConfig.withDuration(duration), 0, f -> {
            float viewWidth = getMeasuredWidth();
            f.animate(getForegroundTextView())
             .x(0f);

            f.animate(getBackgroundTextView())
             .x(swipeDirection == Position.BEFORE ? -viewWidth : viewWidth);
        }, finished -> {
            if (!finished) {
                return;
            }

            this.swipeDirection = null;
        });
    }

    @Override
    public void onSwipeCompleted(long duration, @NonNull AnimatorConfig animatorConfig, @Nullable AnimatorContext animatorContext) {
        if (animatorContext == null) {
            return;
        }

        animatorContext.transaction(animatorConfig.withDuration(duration), 0, f -> {
            float viewWidth = getMeasuredWidth();
            f.animate(getForegroundTextView())
             .x(swipeDirection == Position.BEFORE ? viewWidth : -viewWidth);

            f.animate(getBackgroundTextView())
             .x(0f);
        }, finished -> {
            if (!finished) {
                return;
            }

            swapTextViews();
            this.swipeDirection = null;
        });
    }

    @Override
    public void onSwipeConclusionInterrupted() {
        PropertyAnimatorProxy.stop(textView1, textView2);
    }

    //endregion
}
