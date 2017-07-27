package com.supercilex.robotscouter.ui.scouting.scout.viewholder;

import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.transition.TransitionManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.crash.FirebaseCrash;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Metric;
import com.supercilex.robotscouter.ui.scouting.MetricViewHolderBase;
import com.supercilex.robotscouter.util.AsyncTaskExecutor;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.supercilex.robotscouter.util.ConstantsKt.SINGLE_ITEM;

public class StopwatchViewHolder extends MetricViewHolderBase<Metric<List<Long>>, List<Long>, TextView>
        implements View.OnClickListener {
    private static final Map<Metric.Stopwatch, Timer> TIMERS = new ConcurrentHashMap<>();

    private final Button mToggleStopwatch;
    private final RecyclerView mCycles;

    private Timer mTimer;

    public StopwatchViewHolder(View itemView) {
        super(itemView);
        mToggleStopwatch = itemView.findViewById(R.id.stopwatch);
        mCycles = itemView.findViewById(R.id.list);
    }

    @Override
    protected void bind() {
        super.bind();
        mToggleStopwatch.setOnClickListener(this);
        setText(R.string.start_stopwatch);

        LinearLayoutManager manager =
                new LinearLayoutManager(itemView.getContext(),
                                        LinearLayoutManager.HORIZONTAL,
                                        false);
        manager.setInitialPrefetchItemCount(5);
        mCycles.setLayoutManager(manager);
        mCycles.setAdapter(new Adapter());

        Timer timer = TIMERS.get(getMetric());
        if (timer != null) {
            timer.setHolder(this);
            mTimer = timer;
            mTimer.updateButtonTime();
        }
    }

    @Override
    public void onClick(View v) {
        if (mTimer == null) {
            setText(R.string.stop_stopwatch, "0:00");
            mTimer = new Timer(this);
        } else {
            ArrayList<Long> newCycles = new ArrayList<>(getMetric().getValue());
            newCycles.add(mTimer.cancel());
            updateMetricValue(newCycles);
        }
    }

    private void setText(@StringRes int id, Object... formatArgs) {
        mToggleStopwatch.setText(itemView.getResources().getString(id, formatArgs));
    }

    private static class Timer implements OnSuccessListener<Void>, OnFailureListener {
        private static final int GAME_TIME = 3;
        private static final String COLON = ":";
        private static final String LEADING_ZERO = "0";

        private final long mStartTimeMillis = System.currentTimeMillis();

        private TaskCompletionSource<Void> mTimerTask;
        private boolean mIsRunning;

        private WeakReference<StopwatchViewHolder> mHolder;

        public Timer(StopwatchViewHolder holder) {
            mHolder = new WeakReference<>(holder);
            mIsRunning = true;
            TIMERS.put((Metric.Stopwatch) (Object) holder.getMetric(), this);

            updateStyle();

            TaskCompletionSource<Void> start = new TaskCompletionSource<>();
            start.getTask().addOnSuccessListener(AsyncTaskExecutor.INSTANCE, this);
            start.setResult(null);
        }

        public static String getFormattedTime(long nanos) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(nanos);

            String formattedTime =
                    minutes + COLON + (TimeUnit.MILLISECONDS.toSeconds(nanos) - (minutes * 60));
            String[] split = formattedTime.split(COLON);
            if (split[1].length() <= SINGLE_ITEM) {
                formattedTime = split[0] + COLON + LEADING_ZERO + split[1];
            }

            return formattedTime;
        }

        public void setHolder(StopwatchViewHolder holder) {
            mHolder = new WeakReference<>(holder);
            updateStyle();
        }

        public void updateButtonTime() {
            setText(R.string.stop_stopwatch, getFormattedTime(getElapsedTime()));
        }

        /** @return The time since this class was instantiated and then cancelled */
        public long cancel() {
            mIsRunning = false;
            StopwatchViewHolder holder = mHolder.get();
            if (holder != null) holder.mTimer = null;
            for (Map.Entry<Metric.Stopwatch, Timer> entry : TIMERS.entrySet()) {
                if (entry.getValue().equals(this)) TIMERS.remove(entry.getKey());
            }

            mTimerTask.trySetException(new CancellationException());
            updateStyle();
            setText(R.string.start_stopwatch);

            return getElapsedTime();
        }

        @Override
        public void onSuccess(Void aVoid) {
            mTimerTask = new TaskCompletionSource<>();
            Task<Void> task = mTimerTask.getTask();
            task.addOnSuccessListener(AsyncTaskExecutor.INSTANCE, this);
            task.addOnFailureListener(this);
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(1));

                final long seconds = TimeUnit.MILLISECONDS.toSeconds(getElapsedTime());
                if (!mIsRunning || TimeUnit.SECONDS.toMinutes(seconds) >= GAME_TIME) {
                    mTimerTask.trySetException(
                            new TimeoutException("Cycle time is longer than game time."));
                    return;
                }

                StopwatchViewHolder holder = mHolder.get();
                if (holder != null) {
                    new Handler(holder.itemView.getContext()
                                        .getMainLooper()).post(this::updateButtonTime);
                }
            } catch (InterruptedException e) {
                FirebaseCrash.report(e);
                mTimerTask.setException(e);
            }
            mTimerTask.setResult(null);
        }

        @Override
        public void onFailure(@NonNull Exception e) {
            cancel();
        }

        /** @return the time since this class was instantiated in milliseconds */
        private long getElapsedTime() {
            return System.currentTimeMillis() - mStartTimeMillis;
        }

        private void setText(@StringRes int id, Object... formatArgs) {
            StopwatchViewHolder holder = mHolder.get();
            if (holder != null) holder.setText(id, formatArgs);
        }

        private void updateStyle() {
            StopwatchViewHolder holder = mHolder.get();
            if (holder == null) return;

            TransitionManager.beginDelayedTransition((ViewGroup) holder.itemView);

            // There's a bug pre-L where changing the view state doesn't update the vector drawable.
            // Because of that, calling View#setActivated(isRunning) doesn't update the background
            // color and we end up with unreadable text.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;

            Button stopwatch = holder.mToggleStopwatch;
            stopwatch.setTextColor(mIsRunning ? ContextCompat.getColor(
                    stopwatch.getContext(), R.color.colorAccent) : Color.WHITE);
            stopwatch.setActivated(mIsRunning);
            stopwatch.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    mIsRunning ? R.drawable.ic_timer_off_color_accent_24dp : R.drawable.ic_timer_white_24dp,
                    0,
                    0,
                    0);
        }
    }

    private class Adapter extends RecyclerView.Adapter<DataHolder> {
        private static final int DATA_ITEM = 0;
        private static final int AVERAGE_ITEM = 1;

        @Override
        public DataHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.scout_stopwatch_cycle_item, parent, false);

            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            view.setLayoutParams(params);

            return viewType == DATA_ITEM ? new DataHolder(view) : new AverageHolder(view);
        }

        @Override
        public void onBindViewHolder(DataHolder holder, int position) {
            List<Long> cycles = getMetric().getValue();
            if (holder instanceof AverageHolder) {
                ((AverageHolder) holder).bind(cycles);
            } else {
                if (containsAverageItems()) {
                    int realIndex = position - 1;
                    holder.bind(cycles.get(realIndex), realIndex);
                } else {
                    holder.bind(cycles.get(position), position);
                }
            }
        }

        @Override
        public int getItemCount() {
            int size = getMetric().getValue().size();
            return containsAverageItems() ? size + 1 : size;
        }

        @Override
        public int getItemViewType(int position) {
            return containsAverageItems() && position == 0 ? AVERAGE_ITEM : DATA_ITEM;
        }

        private boolean containsAverageItems() {
            return getMetric().getValue().size() > 1;
        }
    }

    private class DataHolder extends RecyclerView.ViewHolder
            implements View.OnCreateContextMenuListener, MenuItem.OnMenuItemClickListener {
        protected TextView mTitle;
        protected TextView mValue;
        private int mIndex;

        public DataHolder(View itemView) {
            super(itemView);
            mTitle = itemView.findViewById(android.R.id.text1);
            mValue = itemView.findViewById(android.R.id.text2);
        }

        public void bind(long nanoTime, int index) {
            mIndex = index;

            itemView.setOnCreateContextMenuListener(this);
            mTitle.setText(itemView.getContext().getString(R.string.cycle_item, mIndex + 1));
            mValue.setText(Timer.getFormattedTime(nanoTime));
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu,
                                        View v,
                                        ContextMenu.ContextMenuInfo menuInfo) {
            menu.add(R.string.delete).setOnMenuItemClickListener(this);
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            ArrayList<Long> newCycles = new ArrayList<>(getMetric().getValue());
            newCycles.remove(mIndex);
            updateMetricValue(newCycles);
            return true;
        }
    }

    private class AverageHolder extends DataHolder {
        public AverageHolder(View itemView) {
            super(itemView);
        }

        public void bind(List<Long> cycles) {
            mTitle.setText(R.string.average);

            long sum = 0;
            for (Long duration : cycles) sum += duration;
            long nanos = cycles.isEmpty() ? sum : sum / cycles.size();
            mValue.setText(Timer.getFormattedTime(nanos));
        }
    }
}
