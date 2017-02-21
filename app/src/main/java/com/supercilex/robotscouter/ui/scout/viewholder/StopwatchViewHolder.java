package com.supercilex.robotscouter.ui.scout.viewholder;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
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
import com.supercilex.robotscouter.data.model.metrics.StopwatchMetric;
import com.supercilex.robotscouter.util.AsyncTaskExecutor;
import com.supercilex.robotscouter.util.Constants;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class StopwatchViewHolder extends ScoutViewHolderBase<List<Long>, TextView> implements View.OnClickListener {
    private static final Map<StopwatchMetric, Timer> TIMERS = new ConcurrentHashMap<>();

    private Button mToggleStopwatch;
    private RecyclerView mCycles;

    private Timer mTimer;

    public StopwatchViewHolder(View itemView) {
        super(itemView);
        mToggleStopwatch = (Button) itemView.findViewById(R.id.stopwatch);
        mCycles = (RecyclerView) itemView.findViewById(R.id.list);
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

        Timer timer = TIMERS.get(mMetric);
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
            long nanoElapsed = mTimer.cancel();

            ArrayList<Long> newCycles = new ArrayList<>(mMetric.getValue());
            newCycles.add(nanoElapsed);
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

        private static final long START_NANO_TIME = System.nanoTime();

        private TaskCompletionSource<Void> mTimerTask;
        private boolean mIsRunning;

        private WeakReference<StopwatchViewHolder> mHolder;

        public Timer(StopwatchViewHolder holder) {
            mHolder = new WeakReference<>(holder);
            mIsRunning = true;
            TIMERS.put((StopwatchMetric) holder.mMetric, this);

            TaskCompletionSource<Void> start = new TaskCompletionSource<>();
            start.getTask().addOnSuccessListener(new AsyncTaskExecutor(), this);
            start.setResult(null);
        }

        private static String getFormattedTime(long nanos) {
            long seconds = TimeUnit.NANOSECONDS.toSeconds(nanos);

            long minutes = TimeUnit.NANOSECONDS.toMinutes(nanos);
            String formattedTime = minutes + COLON + (seconds - (minutes * 60));
            String[] split = formattedTime.split(COLON);
            if (split[1].length() <= Constants.SINGLE_ITEM) {
                formattedTime = split[0] + COLON + LEADING_ZERO + split[1];
            }

            return formattedTime;
        }

        public void setHolder(StopwatchViewHolder holder) {
            mHolder = new WeakReference<>(holder);
        }

        public void updateButtonTime() {
            setText(R.string.stop_stopwatch, getFormattedTime(getElapsedTime()));
        }

        public long cancel() {
            mIsRunning = false;
            StopwatchViewHolder holder = mHolder.get();
            if (holder != null) holder.mTimer = null;
            for (Map.Entry<StopwatchMetric, Timer> entry : TIMERS.entrySet()) {
                if (entry.getValue().equals(this)) TIMERS.remove(entry.getKey());
            }

            mTimerTask.trySetException(new CancellationException());
            setText(R.string.start_stopwatch);

            return getElapsedTime();
        }

        @Override
        public void onSuccess(Void aVoid) {
            mTimerTask = new TaskCompletionSource<>();
            Task<Void> task = mTimerTask.getTask();
            task.addOnSuccessListener(new AsyncTaskExecutor(), this);
            task.addOnFailureListener(this);
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(1));

                final long seconds = TimeUnit.NANOSECONDS.toSeconds(getElapsedTime());
                if (!mIsRunning || TimeUnit.SECONDS.toMinutes(seconds) >= GAME_TIME) {
                    mTimerTask.trySetException(
                            new TimeoutException("Cycle time is longer than game time."));
                    return;
                }

                StopwatchViewHolder holder = mHolder.get();
                if (holder != null) {
                    new Handler(holder.itemView.getContext().getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            updateButtonTime();
                        }
                    });
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

        private long getElapsedTime() {
            return System.nanoTime() - START_NANO_TIME;
        }

        private void setText(@StringRes int id, Object... formatArgs) {
            StopwatchViewHolder holder = mHolder.get();
            if (holder != null) {
                holder.setText(id, formatArgs);
            }
        }
    }

    private class Adapter extends RecyclerView.Adapter<Holder> {
        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            view.setLayoutParams(params);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            holder.bind();
        }

        @Override
        public int getItemCount() {
            return mMetric.getValue().size() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            return R.layout.scout_stopwatch_cycle_item;
        }
    }

    private class Holder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener, MenuItem.OnMenuItemClickListener {
        private final TextView mText1;
        private final TextView mText2;

        public Holder(View itemView) {
            super(itemView);
            mText1 = (TextView) itemView.findViewById(android.R.id.text1);
            mText2 = (TextView) itemView.findViewById(android.R.id.text2);
        }

        private void bind() {
            List<Long> cycles = mMetric.getValue();
            if (getAdapterPosition() == 0) {
                mText1.setText(itemView.getContext().getString(R.string.average));

                long sum = 0;
                for (Long duration : cycles) {
                    sum += duration;
                }
                long nanos = cycles.isEmpty() ? sum : sum / cycles.size();
                mText2.setText(Timer.getFormattedTime(nanos));
            } else {
                int realPosition = getAdapterPosition() - 1;

                mText1.setText(itemView.getContext()
                                       .getString(R.string.cycle_item, realPosition + 1));
                long duration = cycles.get(realPosition);
                mText2.setText(Timer.getFormattedTime(duration));

                itemView.setOnCreateContextMenuListener(this);
            }
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu,
                                        View v,
                                        ContextMenu.ContextMenuInfo menuInfo) {
            menu.add(R.string.delete).setOnMenuItemClickListener(this);
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            ArrayList<Long> newCycles = new ArrayList<>(mMetric.getValue());
            newCycles.remove(getAdapterPosition() - 1);
            updateMetricValue(newCycles);
            return true;
        }
    }
}
