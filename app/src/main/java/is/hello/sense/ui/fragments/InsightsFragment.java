package is.hello.sense.ui.fragments;

import android.animation.Animator;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.squareup.picasso.Picasso;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Question;
import is.hello.sense.api.model.v2.Insight;
import is.hello.sense.graph.presenters.DeviceIssuesPresenter;
import is.hello.sense.graph.presenters.InsightsPresenter;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.QuestionsPresenter;
import is.hello.sense.graph.presenters.questions.ReviewQuestionProvider;
import is.hello.sense.rating.LocalUsageTracker;
import is.hello.sense.ui.adapter.InsightsAdapter;
import is.hello.sense.ui.adapter.ParallaxRecyclerScrollListener;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.InsightInfoFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.dialogs.QuestionsDialogFragment;
import is.hello.sense.ui.handholding.Tutorial;
import is.hello.sense.ui.handholding.TutorialOverlayView;
import is.hello.sense.ui.recycler.CardItemDecoration;
import is.hello.sense.ui.recycler.FadingEdgesItemDecoration;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;
import rx.Observable;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class InsightsFragment extends UndersideTabFragment
        implements SwipeRefreshLayout.OnRefreshListener, InsightsAdapter.InteractionListener,
        InsightInfoFragment.Parent {
    private static final float UNFOCUSED_CONTENT_SCALE = 0.90f;
    private static final float FOCUSED_CONTENT_SCALE = 1f;
    private static final float UNFOCUSED_CONTENT_ALPHA = 0.95f;
    private static final float FOCUSED_CONTENT_ALPHA = 1f;

    @Inject InsightsPresenter insightsPresenter;
    @Inject DateFormatter dateFormatter;
    @Inject LocalUsageTracker localUsageTracker;
    @Inject DeviceIssuesPresenter deviceIssuesPresenter;
    @Inject PreferencesPresenter preferences;
    @Inject QuestionsPresenter questionsPresenter;
    @Inject Picasso picasso;

    private InsightsAdapter insightsAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;

    private @Nullable TutorialOverlayView tutorialOverlayView;
    private @Nullable InsightsAdapter.InsightViewHolder selectedInsightHolder;

    private boolean questionLoaded = false, insightsLoaded = false;
    private @Nullable Question currentQuestion;
    private @NonNull List<Insight> insights = Collections.emptyList();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        deviceIssuesPresenter.bindScope(getScope());
        addPresenter(deviceIssuesPresenter);
        addPresenter(insightsPresenter);
        addPresenter(questionsPresenter);

        LocalBroadcastManager.getInstance(getActivity())
                             .registerReceiver(REVIEW_ACTION_RECEIVER,
                                               new IntentFilter(ReviewQuestionProvider.ACTION_COMPLETED));

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.TopView.EVENT_MAIN_VIEW, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_insights, container, false);

        this.swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.fragment_insights_refresh_container);
        swipeRefreshLayout.setOnRefreshListener(this);
        Styles.applyRefreshLayoutStyle(swipeRefreshLayout);

        this.progressBar = (ProgressBar) view.findViewById(R.id.fragment_insights_progress);

        final Resources resources = getResources();
        this.recyclerView = (RecyclerView) view.findViewById(R.id.fragment_insights_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new CardItemDecoration(resources));
        recyclerView.addOnScrollListener(new ParallaxRecyclerScrollListener());
        recyclerView.setItemAnimator(null);

        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new FadingEdgesItemDecoration(layoutManager, resources,
                                                                     FadingEdgesItemDecoration.Style.ROUNDED_EDGES));

        this.insightsAdapter = new InsightsAdapter(getActivity(), dateFormatter, this, picasso);
        recyclerView.setAdapter(insightsAdapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Combining these into a single Observable results in error
        // handling more or less breaking. Keep them separate until
        // we actually merge the endpoints on the backend.

        bindAndSubscribe(insightsPresenter.insights,
                         this::bindInsights,
                         this::insightsUnavailable);

        bindAndSubscribe(questionsPresenter.question,
                         this::bindQuestion,
                         this::questionUnavailable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (tutorialOverlayView != null) {
            tutorialOverlayView.dismiss(false);
            this.tutorialOverlayView = null;
        }

        insightsPresenter.unbindScope();

        this.insightsAdapter = null;
        this.recyclerView = null;
        this.swipeRefreshLayout = null;
        this.progressBar = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(getActivity())
                             .unregisterReceiver(REVIEW_ACTION_RECEIVER);
    }

    @Override
    public void onSwipeInteractionDidFinish() {
    }

    @Override
    public void onUpdate() {
        if (!insightsPresenter.bindScope(getScope())) {
            swipeRefreshLayout.setRefreshing(true);
            insightsPresenter.update();
        }

        updateQuestion();
    }


    //region Data Binding

    @Override
    public void onRefresh() {
        this.insights = Collections.emptyList();
        this.insightsLoaded = false;
        this.currentQuestion = null;
        this.questionLoaded = false;

        swipeRefreshLayout.setRefreshing(true);
        insightsPresenter.update();
        updateQuestion();
    }

    /**
     * Pushes data into the adapter once both questions and insights have loaded.
     * <p>
     * Unfortunately this cannot be done through RxJava, if either the questions
     * or the insights request fails, the compound Observable will fail and stop
     * emitting values, even if the requests succeed on retry.
     */
    private void bindPendingIfReady() {
        if (!questionLoaded || !insightsLoaded) {
            return;
        }

        progressBar.setVisibility(View.GONE);

        insightsAdapter.bindQuestion(currentQuestion);
        insightsAdapter.bindInsights(insights);

        final Activity activity = getActivity();
        if (!isPostOnboarding() && tutorialOverlayView == null &&
                Tutorial.TAP_INSIGHT_CARD.shouldShow(activity)) {
            this.tutorialOverlayView = new TutorialOverlayView(activity,
                                                               Tutorial.TAP_INSIGHT_CARD);
            tutorialOverlayView.setOnDismiss(() -> {
                this.tutorialOverlayView = null;
            });
            tutorialOverlayView.setAnchorContainer(getView());
            tutorialOverlayView.postShow(R.id.activity_home_container);
        }
    }

    private void bindInsights(@NonNull List<Insight> insights) {
        this.insights = insights;
        this.insightsLoaded = true;
        bindPendingIfReady();
    }

    private void insightsUnavailable(@Nullable Throwable e) {
        progressBar.setVisibility(View.GONE);
        insightsAdapter.insightsUnavailable(e);
    }

    private void bindQuestion(@Nullable Question question) {
        this.currentQuestion = question;
        this.questionLoaded = true;
        bindPendingIfReady();
    }

    private void questionUnavailable(@Nullable Throwable e) {
        progressBar.setVisibility(View.GONE);
        insightsAdapter.questionUnavailable(e);
    }

    //endregion


    //region Insights

    private Animator createRecyclerEnter() {
        return animatorFor(recyclerView)
                .scale(UNFOCUSED_CONTENT_SCALE)
                .alpha(UNFOCUSED_CONTENT_ALPHA)
                .addOnAnimationCompleted(finished -> {
                    // If we don't reset this now, Views#getFrameInWindow(View, Rect) will
                    // return a subtly broken value, and the exit transition will be broken.
                    if (recyclerView != null) {
                        recyclerView.setScaleX(FOCUSED_CONTENT_SCALE);
                        recyclerView.setScaleY(FOCUSED_CONTENT_SCALE);
                        recyclerView.setAlpha(FOCUSED_CONTENT_ALPHA);
                    }
                });
    }

    private Animator createRecyclerExit() {
        return animatorFor(recyclerView)
                .addOnAnimationWillStart(() -> {
                    // Ensure visual consistency.
                    if (recyclerView != null) {
                        recyclerView.setScaleX(UNFOCUSED_CONTENT_SCALE);
                        recyclerView.setScaleY(UNFOCUSED_CONTENT_SCALE);
                        recyclerView.setAlpha(UNFOCUSED_CONTENT_ALPHA);
                    }
                })
                .scale(FOCUSED_CONTENT_SCALE)
                .alpha(FOCUSED_CONTENT_ALPHA);
    }

    @Override
    @Nullable
    public InsightInfoFragment.SharedState provideSharedState(boolean isEnter) {
        if (selectedInsightHolder != null && getActivity() != null) {
            final InsightInfoFragment.SharedState state = new InsightInfoFragment.SharedState();
            Views.getFrameInWindow(selectedInsightHolder.itemView, state.cardRectInWindow);
            Views.getFrameInWindow(selectedInsightHolder.image, state.imageRectInWindow);
            state.imageParallaxPercent = selectedInsightHolder.image.getParallaxPercent();
            if (recyclerView != null) {
                if (isEnter) {
                    state.parentAnimator = createRecyclerEnter();
                } else {
                    state.parentAnimator = createRecyclerExit();
                }
            }
            return state;
        } else {
            return null;
        }
    }

    @Nullable
    @Override
    public Drawable getInsightImage() {
        if (selectedInsightHolder != null) {
            return selectedInsightHolder.image.getDrawable();
        } else {
            return null;
        }
    }

    @Override
    public void onInsightClicked(@NonNull InsightsAdapter.InsightViewHolder viewHolder) {
        final Insight insight = viewHolder.getInsight();
        if (insight.isError()) {
            return;
        }

        Analytics.trackEvent(Analytics.TopView.EVENT_INSIGHT_DETAIL, null);
        Tutorial.TAP_INSIGHT_CARD.markShown(getActivity());

        // InsightsFragment lives inside of a child fragment manager, whose root view is inset
        // on the bottom to make space for the open timeline. We go right to the root fragment
        // manager to keep things simple.
        final FragmentManager fragmentManager = getActivity().getFragmentManager();
        final InsightInfoFragment infoFragment = InsightInfoFragment.newInstance(insight,
                                                                                 getResources());
        infoFragment.setTargetFragment(this, 0x0);
        infoFragment.show(fragmentManager,
                          R.id.activity_home_container,
                          InsightInfoFragment.TAG);

        this.selectedInsightHolder = viewHolder;
    }

    //endregion


    //region Questions

    public void updateQuestion() {
        final Observable<Boolean> stageOne = deviceIssuesPresenter.latest().map(issue -> {
            return (issue == DeviceIssuesPresenter.Issue.NONE &&
                    localUsageTracker.isUsageAcceptableForRatingPrompt() &&
                    !preferences.getBoolean(PreferencesPresenter.DISABLE_REVIEW_PROMPT, false));
        });
        stageOne.subscribe(showReview -> {
                               if (showReview) {
                                   questionsPresenter.setSource(QuestionsPresenter.Source.REVIEW);
                               } else {
                                   questionsPresenter.setSource(QuestionsPresenter.Source.API);
                               }
                               questionsPresenter.update();
                           },
                           e -> {
                               Logger.warn(getClass().getSimpleName(),
                                           "Could not determine device status", e);
                               questionsPresenter.update();
                           });
    }

    @Override
    public void onDismissLoadingIndicator() {
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onSkipQuestion() {
        LoadingDialogFragment.show(getFragmentManager());
        bindAndSubscribe(questionsPresenter.skipQuestion(false),
                         ignored -> {
                             LoadingDialogFragment.close(getFragmentManager());
                         },
                         e -> {
                             LoadingDialogFragment.close(getFragmentManager());
                             ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(e, getResources()).build();
                             errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
                         });
    }

    @Override
    public void onAnswerQuestion() {
        final QuestionsDialogFragment dialogFragment = new QuestionsDialogFragment();
        dialogFragment.showAllowingStateLoss(getActivity().getFragmentManager(), QuestionsDialogFragment.TAG);
        insightsAdapter.clearCurrentQuestion();
    }

    //endregion


    private final BroadcastReceiver REVIEW_ACTION_RECEIVER = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int response = intent.getIntExtra(ReviewQuestionProvider.EXTRA_RESPONSE,
                                                    ReviewQuestionProvider.RESPONSE_SUPPRESS_TEMPORARILY);
            switch (response) {
                case ReviewQuestionProvider.RESPONSE_WRITE_REVIEW:
                    stateSafeExecutor.execute(() -> UserSupport.showProductPage(getActivity()));
                    preferences.edit()
                               .putBoolean(PreferencesPresenter.DISABLE_REVIEW_PROMPT, true)
                               .apply();
                    break;

                case ReviewQuestionProvider.RESPONSE_SEND_FEEDBACK:
                    stateSafeExecutor.execute(() -> UserSupport.showContactForm(getActivity()));
                    localUsageTracker.incrementAsync(LocalUsageTracker.Identifier.SKIP_REVIEW_PROMPT);
                    break;

                case ReviewQuestionProvider.RESPONSE_SHOW_HELP:
                    stateSafeExecutor.execute(() -> UserSupport.showUserGuide(getActivity()));
                    localUsageTracker.incrementAsync(LocalUsageTracker.Identifier.SKIP_REVIEW_PROMPT);
                    break;

                case ReviewQuestionProvider.RESPONSE_SUPPRESS_TEMPORARILY:
                    localUsageTracker.incrementAsync(LocalUsageTracker.Identifier.SKIP_REVIEW_PROMPT);
                    break;

                case ReviewQuestionProvider.RESPONSE_SUPPRESS_PERMANENTLY:
                    localUsageTracker.incrementAsync(LocalUsageTracker.Identifier.SKIP_REVIEW_PROMPT);
                    preferences.edit()
                               .putBoolean(PreferencesPresenter.DISABLE_REVIEW_PROMPT, true)
                               .apply();
                    break;
            }
        }
    };
}
