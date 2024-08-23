package com.dharven.fts;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.dharven.fts.adapter.HistoryAdapter;
import com.dharven.fts.repository.History;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView historyListView;
    private FirebaseFirestore db;
    private HistoryAdapter adapter;
    private List<History> historyList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        historyListView = findViewById(R.id.historyListView);
        db = FirebaseFirestore.getInstance();
        historyList = new ArrayList<>();
        adapter = new HistoryAdapter(historyList, this);
        historyListView.setAdapter(adapter);
        historyListView.setLayoutManager(new LinearLayoutManager(this));
        fetchHistoryData();
    }

    private void fetchHistoryData() {
        db.collection("users").document(getSharedPreferences("MyPrefs", MODE_PRIVATE).getString("email", "")).collection("history")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Convert the document to a History object
                            History history = document.toObject(History.class);
                            historyList.add(history);
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(HistoryActivity.this, "Error getting documents: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
