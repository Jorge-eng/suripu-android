package is.hello.sense.flows.expansions.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.api.model.v2.expansions.Expansion;
import is.hello.sense.flows.expansions.interactors.ExpansionsInteractor;
import is.hello.sense.flows.expansions.ui.views.ExpansionListView;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.ui.adapter.ExpansionAdapter;

public class ExpansionListFragment extends PresenterFragment<ExpansionListView>
        implements ArrayRecyclerAdapter.OnItemClickedListener<Expansion> {
    public static final String EXPANSION_ID_KEY = ExpansionListFragment.class.getSimpleName() + ".expansion_id_key";
    @Inject
    ExpansionsInteractor expansionsInteractor;
    @Inject
    Picasso picasso;
    private ExpansionAdapter adapter;

    @Override
    public void initializePresenterView() {
        if (presenterView == null) {
            this.adapter = new ExpansionAdapter(new ArrayList<>(2), picasso);
            this.adapter.setOnItemClickedListener(this);
            presenterView = new ExpansionListView(getActivity(), adapter);
        }
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindAndSubscribe(expansionsInteractor.expansions,
                         this::bindExpansions,
                         this::presentError);
    }

    @Override
    public void onResume() {
        super.onResume();
        expansionsInteractor.update();
    }

    @Override
    protected void onRelease() {
        super.onRelease();
        if (this.adapter != null) {
            adapter.setOnItemClickedListener(null);
            adapter.clear();
            adapter = null;
        }
    }

    @Override
    public void onItemClicked(final int position, @NonNull final Expansion item) {
        final Intent intent = new Intent();
        intent.putExtra(EXPANSION_ID_KEY, item.getId());
        finishFlowWithResult(Activity.RESULT_OK, intent);

    }

    public void bindExpansions(@Nullable final List<Expansion> expansions) {
        if (expansions == null) {
            this.adapter.clear();
        } else {
            this.adapter.replaceAll(expansions);
        }
    }

    public void presentError(@NonNull final Throwable e) {
        //todo
    }

}
