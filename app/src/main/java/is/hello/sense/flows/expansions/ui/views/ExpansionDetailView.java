package is.hello.sense.flows.expansions.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.expansions.Expansion;
import is.hello.sense.flows.expansions.ui.widget.ExpansionValuePickerView;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.widget.util.Views;

@SuppressLint("ViewConstructor")
public class ExpansionDetailView extends PresenterView {

    final ViewGroup expansionInfoContainer;
    final TextView deviceNameTextView;
    final TextView companyNameTextView;
    final ImageView expansionIconImageView;
    final TextView expansionDescriptionTextView;
    final Button connectButton;

    final TextView enabledTextView;
    final CompoundButton enabledSwitch;

    final TextView configurationTypeTextView;
    final TextView configurationSelectedTextView;
    final TextView configRetry;
    final ImageView configurationErrorImageView;
    final ViewGroup removeAccessContainer;
    final ViewGroup connectedContainer;
    final ViewGroup enabledContainer;

    final ProgressBar configurationLoading;

    final ExpansionValuePickerView expansionValuePickerView;

    public ExpansionDetailView(@NonNull final Activity activity,
                               @NonNull final OnClickListener enabledTextViewClickListener,
                               @NonNull final OnClickListener removeAccessTextViewClickListener,
                               @NonNull final OnClickListener configRetryListener) {
        super(activity);
        this.expansionInfoContainer = (ViewGroup) findViewById(R.id.view_expansion_detail_infoid);
        this.deviceNameTextView = (TextView) expansionInfoContainer.findViewById(R.id.view_expansion_detail_device_name);
        this.companyNameTextView = (TextView) expansionInfoContainer.findViewById(R.id.view_expansion_detail_device_company_name);
        this.expansionIconImageView = (ImageView) expansionInfoContainer.findViewById(R.id.view_expansion_detail_icon);
        this.expansionDescriptionTextView = (TextView) expansionInfoContainer.findViewById(R.id.view_expansion_detail_description);

        // not connected
        this.connectButton = (Button) findViewById(R.id.view_expansion_detail_connect_button);
        // connected
        this.connectedContainer = (ViewGroup) findViewById(R.id.view_expansion_detail_bottom);
        this.connectedContainer.setVisibility(GONE);// can't set included layouts to gone
        this.enabledContainer = (ViewGroup) connectedContainer.findViewById(R.id.view_expansion_detail_enabled_container);
        this.enabledTextView = (TextView) enabledContainer.findViewById(R.id.view_expansion_detail_enabled_tv);
        this.enabledSwitch = (CompoundButton) enabledContainer.findViewById(R.id.view_expansion_detail_configuration_selection_switch);
        // connected and configurations found
        this.configurationErrorImageView = (ImageView) connectedContainer.findViewById(R.id.view_expansion_detail_configuration_error);
        this.configurationTypeTextView = (TextView) connectedContainer.findViewById(R.id.view_expansion_detail_configuration_type_tv);
        this.configurationSelectedTextView = (TextView) connectedContainer.findViewById(R.id.view_expansion_detail_configuration_selection_tv);
        this.configRetry = (TextView) connectedContainer.findViewById(R.id.view_expansion_detail_configuration_reload);
        this.removeAccessContainer = (ViewGroup) connectedContainer.findViewById(R.id.view_expansion_detail_remove_access_container);
        this.configurationLoading = (ProgressBar) connectedContainer.findViewById(R.id.view_expansion_detail_configuration_loading);
        this.expansionValuePickerView = (ExpansionValuePickerView) findViewById(R.id.view_expansion_detail_expansion_value_picker_view);
        //hook up listeners
        Views.setSafeOnClickListener(this.enabledTextView, enabledTextViewClickListener);
        Views.setSafeOnClickListener(this.removeAccessContainer, removeAccessTextViewClickListener);
        Views.setSafeOnClickListener(this.configRetry, v -> {
            configRetry.setVisibility(GONE);
            configurationLoading.setVisibility(VISIBLE);
            configRetryListener.onClick(v);
        });
    }


    @Override
    protected int getLayoutRes() {
        return R.layout.view_expansion_detail;
    }

    @Override
    public void releaseViews() {
        this.connectButton.setOnClickListener(null);
        this.removeAccessContainer.setOnClickListener(null);
        this.configurationSelectedTextView.setOnClickListener(null);
        this.enabledSwitch.setOnClickListener(null);
        this.enabledTextView.setOnClickListener(null);
    }

    public void showConfigurationSuccess(@Nullable final String configurationName,
                                         @NonNull final OnClickListener configurationSelectedTextViewClickListener) {
        Views.setSafeOnClickListener(this.configurationSelectedTextView, configurationSelectedTextViewClickListener);
        this.configurationLoading.setVisibility(GONE);
        this.configurationSelectedTextView.setText(configurationName);
        this.configurationSelectedTextView.setVisibility(VISIBLE);
        this.connectedContainer.setVisibility(VISIBLE);
        this.configRetry.setVisibility(GONE);
    }

    public void showConfigurationEmpty() {
        this.configurationLoading.setVisibility(GONE);
        this.configurationSelectedTextView.setVisibility(GONE);
        this.connectedContainer.setVisibility(VISIBLE);
        this.configRetry.setVisibility(VISIBLE);
    }

    public void showConfigurationsError(@NonNull final OnClickListener configurationErrorImageViewClickListener) {
        Views.setSafeOnClickListener(this.configurationErrorImageView, configurationErrorImageViewClickListener);
        this.configurationLoading.setVisibility(GONE);
        this.configurationSelectedTextView.setVisibility(GONE);
        this.configurationErrorImageView.setVisibility(VISIBLE);
        this.connectedContainer.setVisibility(VISIBLE);
        this.configRetry.setVisibility(GONE);
    }

    public void showConfigurationSpinner() {
        this.configurationSelectedTextView.setVisibility(GONE);
        this.configurationErrorImageView.setVisibility(GONE);
        this.configurationLoading.setVisibility(VISIBLE);
    }

    public void showConnectButton(@NonNull final OnClickListener connectButtonClickListener) {
        this.connectButton.setVisibility(VISIBLE);
        Views.setSafeOnClickListener(this.connectButton, connectButtonClickListener);
    }


    public void showExpansionInfo(@NonNull final Expansion expansion,
                                  @NonNull final Picasso picasso) {
        this.expansionInfoContainer.setVisibility(VISIBLE);
        this.deviceNameTextView.setText(expansion.getDeviceName());
        this.companyNameTextView.setText(expansion.getCompanyName());
        picasso.load(expansion.getIcon().getUrl(getResources()))
               .into(expansionIconImageView);
        this.expansionDescriptionTextView.setText(expansion.getDescription());
        this.configurationTypeTextView.setText(expansion.getConfigurationType());
    }

    /**
     * @param min          min value
     * @param max          max value
     * @param initialValue should be the actual value, not index position.
     * @param suffix       will be attached to each value. If no suffix should be used pass
     * @param configType   configuration to display
     *                     {@link is.hello.sense.util.Constants#EMPTY_STRING}.
     */
    public void showExpansionRangePicker(final int min,
                                         final int max,
                                         final int initialValue,
                                         @NonNull final String suffix,
                                         @NonNull final String configType) {
        post(() -> {
            this.expansionValuePickerView.setVisibility(VISIBLE);
            this.expansionValuePickerView.initialize(min,
                                                     max,
                                                     initialValue,
                                                     suffix);

            this.configurationTypeTextView.setText(configType);
        });
    }


    public void showRemoveAccess(final boolean isOn) {
        this.removeAccessContainer.setVisibility(isOn ? VISIBLE : GONE);
        this.removeAccessContainer.setEnabled(isOn);
    }

    //region switch

    /**
     * Call to revert the switch after failing to update its state on the server.
     *
     * @param enabledSwitchClickListener we need to remove its callback while changing its checked
     *                                   value and then add it back.
     */
    public void showUpdateSwitchError(@NonNull final CompoundButton.OnCheckedChangeListener enabledSwitchClickListener) {
        setEnableSwitch(!enabledSwitch.isChecked(), enabledSwitchClickListener);
    }

    /**
     * Call to re-enable the switch after successfully updating its state on the server.
     */
    public void showUpdateSwitchSuccess() {
        this.enabledSwitch.setEnabled(true);
    }

    /**
     * Call once for expansions that need to display an on/off switch.
     *
     * @param isOn                       starting value of switch.
     * @param enabledSwitchClickListener callback when switch is pressed.
     */
    public void showEnableSwitch(final boolean isOn,
                                 @NonNull final CompoundButton.OnCheckedChangeListener enabledSwitchClickListener) {
        this.connectedContainer.setVisibility(VISIBLE);
        this.enabledContainer.setVisibility(VISIBLE);
        this.setEnableSwitch(isOn, enabledSwitchClickListener);
    }

    private void setEnableSwitch(final boolean isOn,
                                 @NonNull final CompoundButton.OnCheckedChangeListener enabledSwitchClickListener) {
        this.enabledSwitch.setOnCheckedChangeListener(null);
        this.enabledSwitch.setChecked(isOn);
        this.enabledSwitch.setEnabled(false);
        Views.setSafeOnSwitchClickListener(this.enabledSwitch, enabledSwitchClickListener);
        this.enabledSwitch.setEnabled(true);
    }

    public int getSelectedValue() {
        return this.expansionValuePickerView.getSelectedValue();
    }

    //endregion


}
