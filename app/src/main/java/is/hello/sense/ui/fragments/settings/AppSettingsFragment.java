package is.hello.sense.ui.fragments.settings;

import android.app.Fragment;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.ui.common.FragmentNavigationActivity;
import is.hello.sense.ui.fragments.UndersideTabFragment;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Analytics;

import static android.widget.LinearLayout.LayoutParams;

public class AppSettingsFragment extends UndersideTabFragment {
    private final LayoutParams itemTextLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    private LayoutParams dividerLayoutParams;

    private LinearLayout itemContainer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Resources resources = getResources();
        this.dividerLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, resources.getDimensionPixelSize(R.dimen.divider_size));

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.TopView.EVENT_SETTINGS, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_app_settings, container, false);

        this.itemContainer = (LinearLayout) view.findViewById(R.id.fragment_app_settings_container);
        addItem(R.string.label_account, true, ignored -> showFragment(AccountSettingsFragment.class, R.string.label_account));
        addItem(R.string.label_devices, true, ignored -> showFragment(DeviceListFragment.class, R.string.label_devices));
        addItem(R.string.label_notifications, true, ignored -> showFragment(NotificationsSettingsFragment.class, R.string.label_notifications));
        addItem(R.string.label_units_and_time, false, ignored -> showFragment(UnitSettingsFragment.class, R.string.label_units_and_time));

        TextView footer = (TextView) view.findViewById(R.id.footer_help);
        Styles.initializeSupportFooter(getActivity(), footer);

        TextView version = (TextView) view.findViewById(R.id.fragment_app_settings_version);
        version.setText(getString(R.string.app_version_fmt, getString(R.string.app_name), BuildConfig.VERSION_NAME));

        return view;
    }

    @Override
    public void onSwipeInteractionDidFinish() {

    }

    @Override
    public void onUpdate() {

    }

    public void addItem(@StringRes int titleRes, boolean wantsDivider, @NonNull View.OnClickListener onClick) {
        TextView itemView = Styles.createItemView(getActivity(), titleRes, R.style.AppTheme_Text_Body_Light, onClick);
        itemContainer.addView(itemView, itemTextLayoutParams);

        if (wantsDivider) {
            View divider = new View(getActivity());
            divider.setBackgroundResource(R.color.border);
            itemContainer.addView(divider, dividerLayoutParams);
        }
    }

    private void showFragment(@NonNull Class<? extends Fragment> fragmentClass, @StringRes int titleRes) {
        Bundle intentArguments = FragmentNavigationActivity.getArguments(getString(titleRes), fragmentClass, null);
        Intent intent = new Intent(getActivity(), FragmentNavigationActivity.class);
        intent.putExtras(intentArguments);
        startActivity(intent);
    }
}
