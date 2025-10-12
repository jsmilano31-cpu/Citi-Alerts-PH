package com.example.citialertsph;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HotlinesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_hotlines, container, false);

        // === POLICE ===
        setDialAction(view, R.id.tv_dispatch_911, "911");
        setDialAction(view, R.id.tv_bcpo_hq, "0344321865");
        setDialAction(view, R.id.tv_btao, "0347098080");

        // === FIRE ===
        setDialAction(view, R.id.tv_bfp_bacolod, "0344345022");
        setDialAction(view, R.id.tv_amity_fire, "0344336113");

        // === HEALTH ===
        setDialAction(view, R.id.tv_clmmrh, "0344332695");
        setDialAction(view, R.id.tv_riverside, "0344337331");
        setDialAction(view, R.id.tv_doctors_hospital, "0344342828");

        // === DISASTER / RESCUE ===
        setDialAction(view, R.id.tv_drrmo, "0344323875");
        setDialAction(view, R.id.tv_redcross, "0344346010");
        setDialAction(view, R.id.tv_coastguard, "0344344997");

        // === UTILITIES ===
        setDialAction(view, R.id.tv_ceneco, "0344341000");
        setDialAction(view, R.id.tv_baciwad, "0344343743");

        return view;
    }

    private void setDialAction(View parent, int textViewId, final String phoneNumber) {
        TextView tv = parent.findViewById(textViewId);
        if (tv != null) {
            tv.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + phoneNumber));
                startActivity(intent);
            });
        }
    }
}
