package com.dharven.fts;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.dharven.fts.R;
import com.dharven.fts.repository.History;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeeklyStatsFragment extends Fragment {

    private PieChart caloriesChart;
    private PieChart stepsChart;

    private TextView NoDataTextView;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_weekly_stats, container, false);

        caloriesChart = view.findViewById(R.id.calories);
        stepsChart = view.findViewById(R.id.steps);
        NoDataTextView = view.findViewById(R.id.no_data);

        setupPieChart(caloriesChart, "Calories");
        setupPieChart(stepsChart, "Steps");

        db = FirebaseFirestore.getInstance();

        fetchData();

        return view;
    }

    private void setupPieChart(PieChart pieChart, String centerText) {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);

        pieChart.setDragDecelerationFrictionCoef(0.95f);

        pieChart.setHoleColor(Color.BLACK);
        pieChart.setDrawHoleEnabled(true);

        pieChart.setTransparentCircleColor(Color.WHITE);
        pieChart.setTransparentCircleAlpha(110);

        pieChart.setHoleRadius(58f);
        pieChart.setTransparentCircleRadius(61f);

        pieChart.setDrawCenterText(true);
        pieChart.setCenterText(centerText);
        pieChart.setCenterTextSize(18f);
        pieChart.setCenterTextColor(Color.WHITE);

        pieChart.setRotationAngle(0);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);

        pieChart.animateY(1400, Easing.EaseInOutQuad);

        Legend l = pieChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        l.setOrientation(Legend.LegendOrientation.VERTICAL);
        l.setDrawInside(false);
        l.setXEntrySpace(7f);
        l.setYEntrySpace(0f);
        l.setYOffset(0f);

        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setEntryLabelTextSize(12f);
    }

    private void loadPieChartData(PieChart pieChart, ArrayList<PieEntry> dataEntries) {
        PieDataSet dataSet = new PieDataSet(dataEntries, "Data Set");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        ArrayList<Integer> colors = new ArrayList<>();
        for (int c : ColorTemplate.VORDIPLOM_COLORS)
            colors.add(c);
        for (int c : ColorTemplate.JOYFUL_COLORS)
            colors.add(c);
        for (int c : ColorTemplate.COLORFUL_COLORS)
            colors.add(c);
        for (int c : ColorTemplate.LIBERTY_COLORS)
            colors.add(c);
        for (int c : ColorTemplate.PASTEL_COLORS)
            colors.add(c);
        colors.add(ColorTemplate.getHoloBlue());

        dataSet.setColors(colors);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChart));
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.BLACK);
        pieChart.setData(data);

        pieChart.invalidate();
    }

    private void fetchData() {
        String email = getActivity().getSharedPreferences("MyPrefs", getActivity().MODE_PRIVATE).getString("email", "");
        db.collection("users").document(email).collection("history")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Map<String, Float> caloriesMap = new HashMap<>();
                        Map<String, Integer> stepsMap = new HashMap<>();

                        // Get the start of the week (Monday)
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                        calendar.set(Calendar.HOUR_OF_DAY, 0);
                        calendar.set(Calendar.MINUTE, 0);
                        calendar.set(Calendar.SECOND, 0);
                        calendar.set(Calendar.MILLISECOND, 0);
                        Date startOfWeek = calendar.getTime();

                        // Get the end of the week (Sunday)
                        calendar.add(Calendar.DAY_OF_MONTH, 6);
                        Date endOfWeek = calendar.getTime();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            History history = document.toObject(History.class);
                            long time = history.getDate();
                            Date date = new Date(time);

                            // Check if the date is within the week
                            if (date.after(startOfWeek) && date.before(endOfWeek)) {
                                String day = new java.text.SimpleDateFormat("EEEE").format(date);

                                // Sum up calories and steps for each day
                                float calories = Float.parseFloat(caloriesMap.getOrDefault(day, 0f) + history.getCalories()+"");
                                caloriesMap.put(day, calories);
                                stepsMap.put(day, stepsMap.getOrDefault(day, 0) + history.getSteps());
                            }
                        }

                        if (caloriesMap.isEmpty() && stepsMap.isEmpty()) {
                            NoDataTextView.setText("No data available for this week");
                            NoDataTextView.setVisibility(View.VISIBLE);
                            caloriesChart.setVisibility(View.GONE);
                            stepsChart.setVisibility(View.GONE);
                        } else {
                            NoDataTextView.setVisibility(View.GONE);
                            caloriesChart.setVisibility(View.VISIBLE);
                            stepsChart.setVisibility(View.VISIBLE);

                            ArrayList<PieEntry> caloriesEntries = new ArrayList<>();
                            ArrayList<PieEntry> stepsEntries = new ArrayList<>();

                            for (String day : caloriesMap.keySet()) {
                                caloriesEntries.add(new PieEntry(caloriesMap.get(day), day));
                                stepsEntries.add(new PieEntry(stepsMap.get(day), day));
                            }

                            loadPieChartData(caloriesChart, caloriesEntries);
                            loadPieChartData(stepsChart, stepsEntries);
                        }
                    } else {
                        Toast.makeText(getActivity(), "Error getting documents: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
