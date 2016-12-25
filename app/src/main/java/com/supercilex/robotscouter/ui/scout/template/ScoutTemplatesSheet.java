package com.supercilex.robotscouter.ui.scout.template;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.RobotScouter;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.util.FirebaseCopier;
import com.supercilex.robotscouter.ui.BottomSheetBase;
import com.supercilex.robotscouter.util.Constants;

public class ScoutTemplatesSheet extends BottomSheetBase {
    private static final String TAG = "ScoutTemplatesSheet";

//    private ScoutPagerAdapter mPagerAdapter;

    public static void show(FragmentManager manager, Team team) {
        ScoutTemplatesSheet sheet = new ScoutTemplatesSheet();
        sheet.setArguments(team.getBundle());
        sheet.show(manager, TAG);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_scout_templates, container, false);

        final Team team = Team.getTeam(getArguments());
        String templateKey = team.getTemplateKey();
        if (TextUtils.isEmpty(templateKey)) {
            DatabaseReference newTemplateRef = Constants.FIREBASE_SCOUT_TEMPLATES.push();
            templateKey = newTemplateRef.getKey();
            final String finalTemplateKey = templateKey;
            new FirebaseCopier(Constants.FIREBASE_DEFAULT_TEMPLATE, newTemplateRef) {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    super.onDataChange(snapshot);
                    team.updateTemplateKey(finalTemplateKey);
                }
            }.performTransformation();
        }

        getChildFragmentManager()
                .beginTransaction()
                .add(R.id.root, ScoutTemplateFragment.newInstance(templateKey))
                .commit();

//        TabLayout tabLayout = (TabLayout) rootView.findViewById(R.id.tabs);
//        mPagerAdapter = new ScoutTemplatesPagerAdapter(
//                getChildFragmentManager(),
//                tabLayout,
//                Constants.FIREBASE_SCOUT_TEMPLATES);
//        ViewPager viewPager = (ViewPager) rootView.findViewById(R.id.viewpager);
//        viewPager.setAdapter(mPagerAdapter);
//        tabLayout.setupWithViewPager(viewPager);

        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        mPagerAdapter.cleanup();
        RobotScouter.getRefWatcher(getActivity()).watch(this);
    }
}
