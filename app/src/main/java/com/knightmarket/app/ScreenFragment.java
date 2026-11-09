package com.knightmarket.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/** Shows one initial UI layout and wires its buttons from MainActivity's route table. */
public class ScreenFragment extends Fragment {
    private static final String ARG_LAYOUT = "layout";

    public static ScreenFragment of(int layoutRes) {
        ScreenFragment f = new ScreenFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_LAYOUT, layoutRes);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(requireArguments().getInt(ARG_LAYOUT), container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MainActivity activity = (MainActivity) requireActivity();
        for (int id : MainActivity.BACK_IDS) {
            View v = view.findViewById(id);
            if (v != null) v.setOnClickListener(x -> activity.getSupportFragmentManager().popBackStack());
        }
        int[][] routes = MainActivity.ROUTES.get(requireArguments().getInt(ARG_LAYOUT));
        if (routes == null) return;
        for (int[] r : routes) {
            View v = view.findViewById(r[0]);
            if (v != null) v.setOnClickListener(x -> activity.show(r[1]));
        }
    }
}
