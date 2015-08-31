package is.hello.sense.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import is.hello.buruberi.util.Errors;
import is.hello.buruberi.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.model.Insight;
import is.hello.sense.api.model.Question;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;

public class InsightsAdapter extends RecyclerView.Adapter<InsightsAdapter.BaseViewHolder> {
    @VisibleForTesting static final int TYPE_QUESTION = 0;
    @VisibleForTesting static final int TYPE_INSIGHT = 1;

    private final Context context;
    private final LayoutInflater inflater;
    private final DateFormatter dateFormatter;
    private final InteractionListener interactionListener;

    private @Nullable List<Insight> insights;
    private Question currentQuestion;

    public InsightsAdapter(@NonNull Context context,
                           @NonNull DateFormatter dateFormatter,
                           @NonNull InteractionListener interactionListener) {
        this.context = context;
        this.dateFormatter = dateFormatter;
        this.inflater = LayoutInflater.from(context);
        this.interactionListener = interactionListener;
    }


    //region Bindings

    public void bindData(@NonNull Pair<List<Insight>, Question> data) {
        interactionListener.onDismissLoadingIndicator();

        this.insights = data.first;
        this.currentQuestion = data.second;

        notifyDataSetChanged();
    }

    public void dataUnavailable(Throwable e) {
        Analytics.trackError(e, "Loading Insights");
        Logger.error(getClass().getSimpleName(), "Could not load insights", e);

        interactionListener.onDismissLoadingIndicator();
        this.insights = new ArrayList<>();
        this.currentQuestion = null;

        StringRef messageRef = Errors.getDisplayMessage(e);
        String message = messageRef != null
                ? messageRef.resolve(context)
                : context.getString(R.string.dialog_error_generic_message);
        Insight errorInsight = Insight.createError(message);
        insights.add(errorInsight);

        notifyDataSetChanged();
    }


    public void clearCurrentQuestion() {
        this.currentQuestion = null;
        notifyDataSetChanged();
    }

    //endregion


    //region Adapter

    @Override
    public int getItemCount() {
        int count = 0;
        if (insights != null) {
            count += insights.size();
        }

        if (currentQuestion != null) {
            count++;
        }
        
        return count;
    }

    public Insight getInsightItem(int position) {
        if (insights != null) {
            int adjustedPosition = currentQuestion != null ? position - 1 : position;
            return insights.get(adjustedPosition);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 && currentQuestion != null) {
            return TYPE_QUESTION;
        } else {
            return TYPE_INSIGHT;
        }
    }

    //endregion


    //region Views


    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_QUESTION: {
                View view = inflater.inflate(R.layout.sub_fragment_new_question, parent, false);
                return new QuestionViewHolder(view);
            }
            case TYPE_INSIGHT: {
                View view = inflater.inflate(R.layout.item_insight, parent, false);
                return new InsightViewHolder(view);
            }
            default: {
                throw new IllegalArgumentException();
            }
        }
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        holder.bind(position);
    }


    abstract class BaseViewHolder extends RecyclerView.ViewHolder {
        BaseViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        abstract void bind(int position);
    }

    class QuestionViewHolder extends BaseViewHolder {
        final TextView title;

        QuestionViewHolder(@NonNull View view) {
            super(view);

            this.title = (TextView) view.findViewById(R.id.sub_fragment_new_question_title);

            Button skip = (Button) view.findViewById(R.id.sub_fragment_new_question_skip);
            Views.setSafeOnClickListener(skip, this::skip);

            Button answer = (Button) view.findViewById(R.id.sub_fragment_new_question_answer);
            Views.setSafeOnClickListener(answer, this::answer);
        }

        void skip(@NonNull View ignored) {
            interactionListener.onSkipQuestion();
        }

        void answer(@NonNull View ignored) {
            interactionListener.onAnswerQuestion();
        }

        @Override
        void bind(int ignored) {
            title.setText(currentQuestion.getText());
        }
    }

    class InsightViewHolder extends BaseViewHolder implements View.OnClickListener {
        final TextView body;
        final TextView date;
        final View previewDivider;
        final TextView preview;

        InsightViewHolder(@NonNull View view) {
            super(view);

            this.body = (TextView) view.findViewById(R.id.item_insight_body);
            this.date = (TextView) view.findViewById(R.id.item_insight_date);
            this.previewDivider = view.findViewById(R.id.item_insight_preview_divider);
            this.preview = (TextView) view.findViewById(R.id.item_insight_preview);

            view.setOnClickListener(this);
        }

        @Override
        void bind(int position) {
            Insight insight = getInsightItem(position);

            DateTime insightCreated = insight.getCreated();
            if (insightCreated == null && Insight.CATEGORY_IN_APP_ERROR.equals(insight.getCategory())) {
                date.setText(R.string.dialog_error_title);
            } else {
                CharSequence insightDate = dateFormatter.formatAsRelativeTime(insightCreated);
                date.setText(insightDate);
            }

            if (!TextUtils.isEmpty(insight.getInfoPreview())) {
                previewDivider.setVisibility(View.VISIBLE);
                preview.setVisibility(View.VISIBLE);
                preview.setText(insight.getInfoPreview());
            } else {
                previewDivider.setVisibility(View.GONE);
                preview.setVisibility(View.GONE);
            }

            body.setText(insight.getMessage());
        }


        @Override
        public void onClick(View ignored) {
            // View dispatches OnClickListener#onClick(View) calls on
            // the next looper cycle. It's possible for the adapter's
            // containing recycler view to update and invalidate a
            // view holder before the callback fires.
            final int adapterPosition = getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                Insight insight = getInsightItem(adapterPosition);
                interactionListener.onInsightClicked(insight);
            }
        }
    }

    //endregion


    public interface InteractionListener {
        void onDismissLoadingIndicator();
        void onSkipQuestion();
        void onAnswerQuestion();
        void onInsightClicked(@NonNull Insight insight);
    }
}
