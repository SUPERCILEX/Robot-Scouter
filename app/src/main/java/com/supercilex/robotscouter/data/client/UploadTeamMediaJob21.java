package com.supercilex.robotscouter.data.client;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.supercilex.robotscouter.data.remote.TbaUploader;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.util.JobHelper;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class UploadTeamMediaJob21 extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {
        TeamHelper oldTeamHelper = JobHelper.parseRawBundle(params.getExtras());
        TbaUploader.upload(oldTeamHelper.getTeam(), this)
                .addOnSuccessListener(newTeam -> {
                    oldTeamHelper.updateMedia(newTeam);
                    jobFinished(params, false);
                })
                .addOnFailureListener(e -> jobFinished(params, true));
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
