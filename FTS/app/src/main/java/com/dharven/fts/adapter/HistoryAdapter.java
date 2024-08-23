package com.dharven.fts.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dharven.fts.R;
import com.dharven.fts.repository.History;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private List<History> historyList;
    private Context context;

    private boolean isMarqueeEnabled = true;

    public HistoryAdapter(List<History> historyList, Context context) {
        this.historyList = historyList;
        this.context = context;
    }

    @NonNull
    @Override
    public HistoryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryAdapter.ViewHolder holder, int position) {
        History history = historyList.get(position);
        holder.date.setText("Date: "+new SimpleDateFormat("dd/MM/yyyy").format(new Date(history.getDate())));
        holder.calories.setText("Calories: "+String.valueOf(history.getCalories()));
        holder.steps.setText("Steps: "+String.valueOf(history.getSteps()));
        holder.advise.setText("Advise: "+ history.getAdvise());

        holder.itemView.setOnClickListener(v -> toggleMarquee(holder.advise));

    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    private void toggleMarquee(TextView adviseTextView) {
        if (isMarqueeEnabled) {
            // Disable marquee and expand text
            adviseTextView.setEllipsize(null);
            adviseTextView.setSingleLine(false);
            adviseTextView.setSelected(false);  // Stop marquee
        } else {
            // Enable marquee and collapse text
            adviseTextView.setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE);
            adviseTextView.setSingleLine(true);
            adviseTextView.setSelected(true);  // Start marquee
        }
        isMarqueeEnabled = !isMarqueeEnabled;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView date, calories, steps, advise;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.date);
            calories = itemView.findViewById(R.id.calories);
            steps = itemView.findViewById(R.id.steps);
            advise = itemView.findViewById(R.id.advise);
        }
    }
}
