package is.hello.sense.flows.expansions.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.expansions.Configuration;
import is.hello.sense.api.model.v2.expansions.Expansion;
import is.hello.sense.flows.expansions.ui.views.ConfigSelectionView;
import is.hello.sense.interactors.ConfigurationsInteractor;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.ui.adapter.ConfigurationAdapter;

public class ConfigSelectionFragment extends PresenterFragment<ConfigSelectionView>
        implements ArrayRecyclerAdapter.OnItemClickedListener<Configuration> {

    @Inject
    ConfigurationsInteractor configurationsInteractor;

    private static final String ARG_EXPANSION = ConfigSelectionFragment.class + "ARG_EXPANSION";

    private ConfigurationAdapter adapter;
    private Expansion expansion;

    public static ConfigSelectionFragment newInstance(@NonNull final Expansion expansion){
        final ConfigSelectionFragment fragment = new ConfigSelectionFragment();
        final Bundle args = new Bundle();
        args.putSerializable(ARG_EXPANSION, expansion);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void initializePresenterView() {
        if(presenterView == null){
            this.adapter = new ConfigurationAdapter(new ArrayList<>(2));
            this.adapter.setOnItemClickedListener(this);
            presenterView = new ConfigSelectionView(getActivity(), adapter);
        }
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        handleArgs(getArguments());
        bindAndSubscribe(configurationsInteractor.configSubject,
                         this::bindConfigurations,
                         this::presentError);
    }

    private void handleArgs(@Nullable final Bundle arguments) {
        if(arguments != null){
            this.expansion = (Expansion) arguments.getSerializable(ARG_EXPANSION);
            if(expansion != null) {
                presenterView.setTitle(getString(R.string.expansions_configuration_selection_title_format, expansion.getServiceName()));
                presenterView.setSubtitle(getString(R.string.expansions_configuration_selection_subtitle_format, expansion.getCategory().name()));
                configurationsInteractor.setExpansionId(expansion.getId());
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        configurationsInteractor.update();
    }

    @Override
    protected void onRelease() {
        super.onRelease();
        if(this.adapter != null){
            adapter.setOnItemClickedListener(null);
            adapter.clear();
            adapter = null;
        }
    }

    @Override
    public void onItemClicked(final int position, @NonNull final Configuration item) {
        adapter.setSelectedItem(position);
    }

    public void bindConfigurations(@Nullable final List<Configuration> configurations){
        if(configurations == null) {
            this.adapter.clear();
        } else {
            if(configurations.isEmpty()){
                if(expansion != null) {
                    //todo until server updates expansion with configuration name field just using category
                    configurations.add(new Configuration.Empty(getString(R.string.expansions_configuration_selection_item_missing_title_format, expansion.getCategory()),
                                                               getString(R.string.expansions_configuration_selection_item_missing_subtitle_format, expansion.getServiceName(), expansion.getCategory()),
                                                               R.drawable.icon_warning));
                } else {
                    configurations.add(new Configuration.Empty());
                }
            }
            this.adapter.replaceAll(configurations);
        }
    }

    public void presentError(@NonNull final Throwable e){
        //todo
    }
}