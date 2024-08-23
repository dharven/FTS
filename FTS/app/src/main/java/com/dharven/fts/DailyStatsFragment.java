package com.dharven.fts;

import static android.content.Context.MODE_PRIVATE;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.dharven.fts.R;
import com.dharven.fts.repository.History;
import com.dharven.fts.repository.User;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DailyStatsFragment extends Fragment implements SensorEventListener {

    private PieChart caloriesChart;
    private PieChart stepsChart;

    private TextView NoDataTextView, age, height, weight, goalWeight, bmi, pre_weight;
    private FirebaseFirestore db;

    private User user;
    private SensorManager sensorManager;
    private boolean running = false;
    private float totalSteps = 0f;
    private float previousTotalSteps = 0f;

    private long lastSavedDate;
    int currentSteps;
    int totalConsumedCalories;

    private Button updateButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_daily_stats, container, false);

        caloriesChart = view.findViewById(R.id.calories);
        stepsChart = view.findViewById(R.id.steps);
        NoDataTextView = view.findViewById(R.id.no_data);
        age = view.findViewById(R.id.Age);
        weight = view.findViewById(R.id.weight);
        goalWeight = view.findViewById(R.id.GoalWeight);
        height = view.findViewById(R.id.Height);
        bmi = view.findViewById(R.id.bmi);
        pre_weight = view.findViewById(R.id.pre_weight);
        updateButton = view.findViewById(R.id.update);

        setupPieChart(caloriesChart, "Calories");
        setupPieChart(stepsChart, "Steps");

        db = FirebaseFirestore.getInstance();

        fetchData();
        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        loadData();
        checkIfNewDay();


        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // update data from weight and goal weight and age and height
                String ageStr = age.getText().toString();
                String weightStr = weight.getText().toString();
                String goalWeightStr = goalWeight.getText().toString();
                String heightStr = height.getText().toString();
                if (ageStr.isEmpty() || weightStr.isEmpty() || goalWeightStr.isEmpty() || heightStr.isEmpty()) {
                    Toast.makeText(requireContext(), "Please fill all the fields", Toast.LENGTH_SHORT).show();
                    return;
                }
                int ageInt = Integer.parseInt(ageStr);
                double weightInt = Double.parseDouble(weightStr);
                double goalWeightInt = Double.parseDouble(goalWeightStr);
                int heightInt = Integer.parseInt(heightStr);

                // now update the user data in firestore
                FirebaseFirestore.getInstance().collection("users").document(requireActivity().getSharedPreferences("MyPrefs", MODE_PRIVATE).getString("email", "")).update("age", ageInt, "weight", weightInt, "goalWeight", goalWeightInt, "height", heightInt).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(requireContext(), "Data updated successfully", Toast.LENGTH_SHORT).show();
                        double heightInMeters = heightInt / 100.0;
                        double bmiText = weightInt/ (heightInMeters * heightInMeters);
                        String bmiStr = String.format("%.2f", bmiText);
                        bmi.setText("BMI: "+bmiStr);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(requireContext(), "Failed to update data", Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });
        FirebaseFirestore.getInstance().collection("users").document(requireActivity().getSharedPreferences("MyPrefs", MODE_PRIVATE).getString("email", "")).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    user = documentSnapshot.toObject(User.class);
                    age.setText(user.getAge() + "");
                    height.setText(user.getHeight() + "");
                    weight.setText(user.getWeight() + "");
                    goalWeight.setText(user.getGoalWeight() + "");
                    double heightInMeters = user.getHeight() / 100.0;
                    double bmiText = user.getWeight() / (heightInMeters * heightInMeters);
                    String bmiStr = String.format("%.2f", bmiText);
                    bmi.setText("BMI: "+bmiStr);
                    double caloriesBurnt = 0.04 * user.getWeight() * currentSteps;
                    double netCalories;
                    if (totalConsumedCalories != 0) {
                        netCalories = totalConsumedCalories - caloriesBurnt;
                    } else {
                        netCalories = caloriesBurnt;
                    }
                    double weightChange = netCalories / 7700;
                    double predictedWeight = user.getWeight() + weightChange;
                    String pre_weight_str = String.format("%.2f", predictedWeight);
                    pre_weight.setText("Predicted Weight:" + pre_weight_str + " Kg");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });

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
                        List<PieEntry> entries = new ArrayList<>();
                        List<PieEntry> entries2 = new ArrayList<>();

                        // Get the start of today
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(Calendar.HOUR_OF_DAY, 0);
                        calendar.set(Calendar.MINUTE, 0);
                        calendar.set(Calendar.SECOND, 0);
                        calendar.set(Calendar.MILLISECOND, 0);
                        Date startOfToday = calendar.getTime();

                        // Get the end of today
                        calendar.add(Calendar.DAY_OF_MONTH, 1);
                        Date endOfToday = calendar.getTime();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            History history = document.toObject(History.class);
                            long time = history.getDate();
                            Date date = new Date(time);
                            totalConsumedCalories += history.getCalories();
                            // Check if the date is today
                            if (date.after(startOfToday) && date.before(endOfToday)) {
                                NoDataTextView.setVisibility(View.GONE);
                                caloriesChart.setVisibility(View.VISIBLE);
                                stepsChart.setVisibility(View.VISIBLE);
                                entries.add(new PieEntry(Float.parseFloat(history.getCalories() + ""), "Cal: " + history.getCalories()));
                                entries2.add(new PieEntry(history.getSteps(), "Steps: " + history.getSteps()));
                            }
                        }

                        if (entries.isEmpty() && entries2.isEmpty()) {
                            NoDataTextView.setText("No data available for today");
                            NoDataTextView.setVisibility(View.VISIBLE);
                            caloriesChart.setVisibility(View.GONE);
                            stepsChart.setVisibility(View.GONE);
                        } else {
                            loadPieChartData(caloriesChart, new ArrayList<>(entries));
                            loadPieChartData(stepsChart, new ArrayList<>(entries2));
                        }
                    } else {
                        Toast.makeText(getActivity(), "Error getting documents: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        running = true;

        // Get the step counter sensor
        Sensor stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (stepSensor == null) {
            // No sensor found, show a toast
            Toast.makeText(requireContext(), "No sensor detected on this device", Toast.LENGTH_SHORT).show();
        } else {
            // Register the listener
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (running) {
            totalSteps = event.values[0];

            // Calculate the current steps by subtracting the previous total steps
            currentSteps = (int) (totalSteps - previousTotalSteps);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // We do not need to implement this for step counting
    }

    private void saveData(long date, float steps) {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong("lastSavedDate", date);
        editor.putFloat("previousTotalSteps", steps);
        editor.apply();
    }

    private void loadData() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        previousTotalSteps = sharedPreferences.getFloat("previousTotalSteps", 0f);
        lastSavedDate = sharedPreferences.getLong("lastSavedDate", 0);
    }
    private void checkIfNewDay() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(lastSavedDate);
        int lastSavedDay = calendar.get(Calendar.DAY_OF_YEAR);

        calendar.setTimeInMillis(System.currentTimeMillis());
        int today = calendar.get(Calendar.DAY_OF_YEAR);

        if (today != lastSavedDay) {
            previousTotalSteps = totalSteps;
            saveData(System.currentTimeMillis(), previousTotalSteps);
        }
    }
}