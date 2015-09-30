package is.hello.sense.ui.fragments.settings;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Account;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.adapter.SettingsRecyclerAdapter;
import is.hello.sense.ui.common.AccountEditor;
import is.hello.sense.ui.common.FragmentNavigationActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterBirthdayFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterGenderFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterHeightFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterWeightFragment;
import is.hello.sense.ui.recycler.InsetItemDecoration;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;

public class AccountSettingsFragment extends InjectionFragment implements AccountEditor.Container {
    private static final int REQUEST_CODE_PASSWORD = 0x20;
    private static final int REQUEST_CODE_ERROR = 0xE3;

    @Inject AccountPresenter accountPresenter;
    @Inject DateFormatter dateFormatter;
    @Inject UnitFormatter unitFormatter;
    @Inject PreferencesPresenter preferences;

    private ProgressBar loadingIndicator;

    private SettingsRecyclerAdapter.DetailItem nameItem;
    private SettingsRecyclerAdapter.DetailItem emailItem;

    private SettingsRecyclerAdapter.DetailItem birthdayItem;
    private SettingsRecyclerAdapter.DetailItem genderItem;
    private SettingsRecyclerAdapter.DetailItem heightItem;
    private SettingsRecyclerAdapter.DetailItem weightItem;

    private SettingsRecyclerAdapter.ToggleItem enhancedAudioItem;

    private Account currentAccount;
    private @Nullable Account.Preferences accountPreferences;
    private RecyclerView recyclerView;
    private SettingsRecyclerAdapter adapter;


    //region Lifecycle

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            this.currentAccount = (Account) savedInstanceState.getSerializable("currentAccount");
        } else {
            Analytics.trackEvent(Analytics.TopView.EVENT_ACCOUNT, null);
        }

        accountPresenter.update();
        addPresenter(accountPresenter);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.static_recycler, container, false);

        this.loadingIndicator = (ProgressBar) view.findViewById(R.id.static_recycler_view_loading);

        this.recyclerView = (RecyclerView) view.findViewById(R.id.static_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setItemAnimator(null);

        this.adapter = new SettingsRecyclerAdapter(getActivity());

        final int extraPadding = getResources().getDimensionPixelSize(R.dimen.gap_outer);
        final InsetItemDecoration decoration = new InsetItemDecoration();
        recyclerView.addItemDecoration(decoration);

        decoration.addItemInset(adapter.getItemCount(), new Rect(0, extraPadding, 0, 0));
        this.nameItem = new SettingsRecyclerAdapter.DetailItem(R.drawable.icon_settings_user_guide,
                                                               getString(R.string.missing_data_placeholder),
                                                               null,
                                                               this::changeName);
        adapter.add(nameItem);
        this.emailItem = new SettingsRecyclerAdapter.DetailItem(R.drawable.icon_settings_email,
                                                                getString(R.string.missing_data_placeholder),
                                                                null,
                                                                this::changeEmail);
        adapter.add(emailItem);

        decoration.addItemInset(adapter.getItemCount(), new Rect(0, 0, 0, extraPadding));
        final SettingsRecyclerAdapter.DetailItem passwordItem =
                new SettingsRecyclerAdapter.DetailItem(R.drawable.icon_settings_lock,
                                                       getString(R.string.title_change_password),
                                                       null,
                                                       this::changePassword);
        adapter.add(passwordItem);


        this.birthdayItem = new SettingsRecyclerAdapter.DetailItem(R.drawable.icon_settings_calendar,
                                                                   getString(R.string.label_dob),
                                                                   getString(R.string.missing_data_placeholder),
                                                                   this::changeBirthDate);
        adapter.add(birthdayItem);
        this.genderItem = new SettingsRecyclerAdapter.DetailItem(R.drawable.icon_settings_gender,
                                                                 getString(R.string.label_gender),
                                                                 getString(R.string.missing_data_placeholder),
                                                                 this::changeGender);
        adapter.add(genderItem);

        this.heightItem = new SettingsRecyclerAdapter.DetailItem(R.drawable.icon_settings_height,
                                                                 getString(R.string.label_height),
                                                                 getString(R.string.missing_data_placeholder),
                                                                 this::changeHeight);
        adapter.add(heightItem);

        this.weightItem = new SettingsRecyclerAdapter.DetailItem(R.drawable.icon_settings_weight,
                                                                 getString(R.string.label_weight),
                                                                 getString(R.string.missing_data_placeholder),
                                                                 this::changeWeight);
        adapter.add(weightItem);

        decoration.addItemInset(adapter.getItemCount(), new Rect(0, extraPadding, 0, 0));
        this.enhancedAudioItem = new SettingsRecyclerAdapter.ToggleItem(getString(R.string.label_enhanced_audio),
                                                                        false,
                                                                        this::changeEnhancedAudio);
        adapter.add(enhancedAudioItem);

        decoration.addItemInset(adapter.getItemCount(), new Rect(0, 0, 0, extraPadding));
        adapter.add(new SettingsRecyclerAdapter.TextItem(getString(R.string.info_enhanced_audio), null));

        decoration.addItemInset(adapter.getItemCount(), new Rect(0, 0, 0, extraPadding));
        final SettingsRecyclerAdapter.DetailItem signOutItem =
                new SettingsRecyclerAdapter.DetailItem(R.drawable.icon_settings_signout,
                                                       getString(R.string.action_log_out),
                                                       null,
                                                       this::signOut);
        adapter.add(signOutItem);

        recyclerView.setAdapter(adapter);

        loadingIndicator.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.INVISIBLE);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(accountPresenter.account,
                         this::bindAccount,
                         this::accountUnavailable);

        bindAndSubscribe(accountPresenter.preferences(),
                         this::bindAccountPreferences,
                         Functions.LOG_ERROR);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        this.loadingIndicator = null;

        this.nameItem = null;
        this.emailItem = null;

        this.birthdayItem = null;
        this.genderItem = null;
        this.heightItem = null;
        this.weightItem = null;

        this.enhancedAudioItem = null;

        this.recyclerView = null;
        this.adapter = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable("currentAccount", currentAccount);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PASSWORD && resultCode == Activity.RESULT_OK) {
            accountPresenter.update();
        } else if (requestCode == REQUEST_CODE_ERROR && resultCode == Activity.RESULT_OK) {
            getActivity().finish();
        }
    }

    //endregion


    //region Glue

    public FragmentNavigationActivity getNavigationContainer() {
        return (FragmentNavigationActivity) getActivity();
    }

    private void showLoadingIndicator() {
        recyclerView.setVisibility(View.INVISIBLE);
        loadingIndicator.setVisibility(View.VISIBLE);
    }

    private void hideLoadingIndicator() {
        recyclerView.setVisibility(View.VISIBLE);
        loadingIndicator.setVisibility(View.GONE);
    }

    //endregion


    //region Binding Data

    public void bindAccount(@NonNull Account account) {
        nameItem.text = account.getName();
        emailItem.text = account.getEmail();

        birthdayItem.detail = dateFormatter.formatAsLocalizedDate(account.getBirthDate());
        genderItem.detail = getString(account.getGender().nameRes);

        final CharSequence weight = unitFormatter.formatWeight(account.getWeight());
        weightItem.detail = weight.toString();

        final CharSequence height = unitFormatter.formatHeight(account.getHeight());
        heightItem.detail = height.toString();

        this.currentAccount = account;
        adapter.notifyDataSetChanged();

        hideLoadingIndicator();
    }

    public void accountUnavailable(Throwable e) {
        loadingIndicator.setVisibility(View.GONE);
        final ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(e).build();
        errorDialogFragment.setTargetFragment(this, REQUEST_CODE_ERROR);
        errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }

    public void bindAccountPreferences(@NonNull Account.Preferences preferences) {
        this.accountPreferences = preferences;
        enhancedAudioItem.active = preferences.enhancedAudioEnabled;
        enhancedAudioItem.notifyChanged();
    }

    //endregion


    //region Basic Info

    public void changeName() {
        final ChangeNameFragment fragment = new ChangeNameFragment();
        fragment.setTargetFragment(this, 0x00);
        getNavigationContainer().overlayFragmentAllowingStateLoss(fragment, getString(R.string.action_change_name), true);
    }

    public void changeEmail() {
        final ChangeEmailFragment fragment = new ChangeEmailFragment();
        fragment.setTargetFragment(this, REQUEST_CODE_PASSWORD);
        getNavigationContainer().pushFragmentAllowingStateLoss(fragment, getString(R.string.title_change_email), true);
    }

    public void changePassword() {
        final ChangePasswordFragment fragment = ChangePasswordFragment.newInstance(currentAccount.getEmail());
        getNavigationContainer().pushFragmentAllowingStateLoss(fragment, getString(R.string.title_change_password), true);
    }

    //endregion


    //region Demographics

    public void changeBirthDate() {
        final OnboardingRegisterBirthdayFragment fragment = new OnboardingRegisterBirthdayFragment();
        AccountEditor.setWantsSkipButton(fragment, false);
        fragment.setTargetFragment(this, 0x00);
        getNavigationContainer().overlayFragmentAllowingStateLoss(fragment, getString(R.string.label_dob), true);
    }

    public void changeGender() {
        final OnboardingRegisterGenderFragment fragment = new OnboardingRegisterGenderFragment();
        AccountEditor.setWantsSkipButton(fragment, false);
        fragment.setTargetFragment(this, 0x00);
        getNavigationContainer().overlayFragmentAllowingStateLoss(fragment, getString(R.string.label_gender), true);
    }

    public void changeHeight() {
        final OnboardingRegisterHeightFragment fragment = new OnboardingRegisterHeightFragment();
        AccountEditor.setWantsSkipButton(fragment, false);
        fragment.setTargetFragment(this, 0x00);
        getNavigationContainer().overlayFragmentAllowingStateLoss(fragment, getString(R.string.label_height), true);
    }

    public void changeWeight() {
        final OnboardingRegisterWeightFragment fragment = new OnboardingRegisterWeightFragment();
        AccountEditor.setWantsSkipButton(fragment, false);
        fragment.setTargetFragment(this, 0x00);
        getNavigationContainer().overlayFragmentAllowingStateLoss(fragment, getString(R.string.label_weight), true);
    }

    //endregion


    //region Preferences

    public void changeEnhancedAudio() {
        if (accountPreferences == null) {
            return;
        }

        accountPreferences.enhancedAudioEnabled = !enhancedAudioItem.active;
        enhancedAudioItem.active = accountPreferences.enhancedAudioEnabled;
        enhancedAudioItem.notifyChanged();

        showLoadingIndicator();
        bindAndSubscribe(accountPresenter.updatePreferences(accountPreferences),
                         ignored -> {
                             preferences.edit()
                                        .putBoolean(PreferencesPresenter.ENHANCED_AUDIO_ENABLED,
                                                    accountPreferences.enhancedAudioEnabled)
                                        .apply();
                             hideLoadingIndicator();
                         },
                         e -> {
                             accountPreferences.enhancedAudioEnabled = !accountPreferences.enhancedAudioEnabled;
                             enhancedAudioItem.active = accountPreferences.enhancedAudioEnabled;
                             enhancedAudioItem.notifyChanged();
                             accountUnavailable(e);
                         });
    }

    //endregion


    //region Actions

    public void signOut() {
        Analytics.trackEvent(Analytics.TopView.EVENT_SIGN_OUT, null);

        final SenseAlertDialog signOutDialog = new SenseAlertDialog(getActivity());
        signOutDialog.setTitle(R.string.dialog_title_log_out);
        signOutDialog.setMessage(R.string.dialog_message_log_out);
        signOutDialog.setNegativeButton(android.R.string.cancel, null);
        signOutDialog.setPositiveButton(R.string.action_log_out, (dialog, which) -> {
            Analytics.trackEvent(Analytics.Global.EVENT_SIGNED_OUT, null);
            // Let the dialog finish dismissing before we block the main thread.
            recyclerView.post(() -> {
                getActivity().finish();
                accountPresenter.logOut();
            });
        });
        signOutDialog.setButtonDestructive(DialogInterface.BUTTON_POSITIVE, true);
        signOutDialog.show();
    }

    //endregion


    //region Updates

    @NonNull
    @Override
    public Account getAccount() {
        return currentAccount;
    }

    @Override
    public void onAccountUpdated(@NonNull SenseFragment updatedBy) {
        stateSafeExecutor.execute(() -> {
            LoadingDialogFragment.show(getFragmentManager());
            bindAndSubscribe(accountPresenter.saveAccount(currentAccount),
                             ignored -> {
                                 LoadingDialogFragment.close(getFragmentManager());
                                 updatedBy.getFragmentManager().popBackStackImmediate();
                             },
                             e -> {
                                 LoadingDialogFragment.close(getFragmentManager());
                                 ErrorDialogFragment.presentError(getFragmentManager(), e);
                             });
        });
    }

    //endregion
}
