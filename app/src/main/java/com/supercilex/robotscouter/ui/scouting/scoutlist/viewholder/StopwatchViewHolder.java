package com.supercilex.robotscouter.ui.scouting.scoutlist.viewholder;

import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.transition.AutoTransition;
import android.support.transition.Transition;
import android.support.transition.TransitionManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.github.rubensousa.gravitysnaphelper.GravitySnapHelper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.crash.FirebaseCrash;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Metric;
import com.supercilex.robotscouter.ui.scouting.MetricViewHolderBase;
import com.supercilex.robotscouter.util.AsyncTaskExecutor;
import com.supercilex.robotscouter.util.ui.RecyclerPoolHolder;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

        mToggleStopwatch.setOnClickListener(this);

        LinearLayoutManager manager = new LinearLayoutManager(
                itemView.getContext(), LinearLayoutManager.HORIZONTAL, false);
        manager.setInitialPrefetchItemCount(5);
        mCycles.setLayoutManager(manager);
        mCycles.setAdapter(new Adapter());
        new GravitySnapHelper(Gravity.START).attachToRecyclerView(mCycles);

        FragmentManager fragmentManager = ((FragmentActivity) itemView.getContext()).getSupportFragmentManager();
        for (Fragment fragment : fragmentManager.getFragments()) {
            if (fragment instanceof RecyclerPoolHolder) {
                mCycles.setRecycledViewPool(((RecyclerPoolHolder) fragment).getRecyclerPool());
                break;
            }
        }
    }

    @Override
    protected void bind() {
        super.bind();
        setText(R.string.metric_stopwatch_start_title);

        mCycles.setHasFixedSize(false);
        mCycles.getAdapter().notifyDataSetChanged();

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
            setText(R.string.metric_stopwatch_stop_title, "0:00");
            mTimer = new Timer(this);
        } else {
            ArrayList<Long> newCycles = new ArrayList<>(getMetric().getValue());
            newCycles.add(mTimer.cancel());
            getMetric().setValue(newCycles);

            RecyclerView.Adapter adapter = mCycles.getAdapter();
            int size = getMetric().getValue().size();

            // Force RV to request layout when adding first item
            mCycles.setHasFixedSize(size > 1);

            if (size == 2) {
                // Add the average metric
                adapter.notifyItemInserted(0);
                adapter.notifyItemInserted(size);
            } else {
                // Account for the average card being there or not
                adapter.notifyItemInserted(size == 1 ? 0 : size);
                adapter.notifyItemChanged(0);
            }
        }
    }

    private void setText(@StringRes int id, Object... formatArgs) {
        mToggleStopwatch.setText(itemView.getResources().getString(id, formatArgs));
    }

    private static final class Timer implements OnSuccessListener<Void>, OnFailureListener {
        private static final int GAME_TIME = 3;
        private static final String COLON = ":";
        private static final String LEADING_ZERO = "0";

        private static Transition sTransition;

        private final long mStartTimeMillis = System.currentTimeMillis();

        private TaskCompletionSource<Void> mTimerTask;
        private boolean mIsRunning;

        private WeakReference<StopwatchViewHolder> mHolder;

        static {
            Transition transition = new AutoTransition();
            transition.excludeTarget(RecyclerView.class, true);
            sTransition = transition;
        }

        public Timer(StopwatchViewHolder holder) {
            mHolder = new WeakReference<>(holder);
            mIsRunning = true;
            TIMERS.put((Metric.Stopwatch) (Object) holder.getMetric(), this);

            updateStyle();

            Tasks.<Void>forResult(null).addOnSuccessListener(AsyncTaskExecutor.INSTANCE, this);
        }

        public static String getFormattedTime(long nanos) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(nanos);

            String formattedTime =
                    minutes + COLON + (TimeUnit.MILLISECONDS.toSeconds(nanos) - (minutes * 60));
            String[] split = formattedTime.split(COLON);
            if (split[1].length() <= 1) { // NOPMD
                formattedTime = split[0] + COLON + LEADING_ZERO + split[1];
            }

            return formattedTime;
        }

        public void setHolder(StopwatchViewHolder holder) {
            mHolder = new WeakReference<>(holder);
            updateStyle();
        }

        public void updateButtonTime() {
            setText(R.string.metric_stopwatch_stop_title, getFormattedTime(getElapsedTime()));
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
            setText(R.string.metric_stopwatch_start_title);

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
                Crashlytics.logException(e);
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

            TransitionManager.beginDelayedTransition((ViewGroup) holder.itemView, sTransition);

            // There's a bug pre-L where changing the view state doesn't update the vector drawable.
            // Because of that, calling View#setActivated(isRunning) doesn't update the background
            // color and we end up with unreadable text.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;

            Button stopwatch = holder.mToggleStopwatch;
            stopwatch.setTextColor(mIsRunning ? ContextCompat.getColor(
                    stopwatch.getContext(), R.color.colorAccent) : Color.WHITE);
            stopwatch.setActivated(mIsRunning);
            stopwatch.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    mIsRunning ? R.drawable.ic_timer_off_accent_24dp : R.drawable.ic_timer_white_24dp,
                    0,
                    0,
                    0);
        }
    }

    private class Adapter extends RecyclerView.Adapter<DataHolder> {
        // Don't conflict with metric types since the pool is shared
        private static final int DATA_ITEM = 1000;
        private static final int AVERAGE_ITEM = 1001;

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
                    holder.bind(StopwatchViewHolder.this, cycles.get(realIndex));
                } else {
                    holder.bind(StopwatchViewHolder.this, cycles.get(position));
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

        public boolean containsAverageItems() {
            return getMetric().getValue().size() > 1;
        }
    }

    private static class DataHolder extends RecyclerView.ViewHolder
            implements View.OnCreateContextMenuListener, MenuItem.OnMenuItemClickListener {
        protected TextView mTitle;
        protected TextView mValue;

        private StopwatchViewHolder mHolder;

        public DataHolder(View itemView) {
            super(itemView);
            mTitle = itemView.findViewById(android.R.id.text1);
            mValue = itemView.findViewById(android.R.id.text2);

            itemView.setOnCreateContextMenuListener(this);
        }

        public void bind(StopwatchViewHolder holder, long nanoTime) {
            mHolder = holder;

            mTitle.setText(itemView.getContext().getString(
                    R.string.metric_stopwatch_cycle_title, getRealPosition() + 1));
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
            Metric<List<Long>> metric = mHolder.getMetric();
            boolean hadAverage = hasAverage();

            ArrayList<Long> newCycles = new ArrayList<>(metric.getValue());
            newCycles.remove(getRealPosition());
            metric.setValue(newCycles);

            RecyclerView.Adapter adapter = mHolder.mCycles.getAdapter();
            int size = metric.getValue().size();

            // Force RV to request layout when removing last item
            mHolder.mCycles.setHasFixedSize(size > 0);

            adapter.notifyItemRemoved(getAdapterPosition());
            if (hadAverage && size == 1) {
                // Remove the average card
                adapter.notifyItemRemoved(0);
            } else if (size > 1) {
                adapter.notifyItemChanged(0);
            }

            return true;
        }

        private int getRealPosition() {
            return hasAverage() ? getAdapterPosition() - 1 : getAdapterPosition();
        }

        private boolean hasAverage() {
            return ((Adapter) mHolder.mCycles.getAdapter()).containsAverageItems();
        }
    }

    private static class AverageHolder extends DataHolder {
        public AverageHolder(View itemView) {
            super(itemView);
            mTitle.setText(R.string.metric_stopwatch_cycle_average_title);

            itemView.setOnCreateContextMenuListener(null);
        }

        public void bind(List<Long> cycles) {
            long sum = 0;
            for (Long duration : cycles) sum += duration;
            long nanos = cycles.isEmpty() ? sum : sum / cycles.size();
            mValue.setText(Timer.getFormattedTime(nanos));
        }
    }
}
