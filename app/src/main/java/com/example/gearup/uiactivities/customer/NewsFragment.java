package com.example.gearup.uiactivities.customer;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gearup.BuildConfig;
import com.example.gearup.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NewsFragment extends Fragment {

    private RecyclerView newsRecyclerView;
    private NewsAdapter newsAdapter;
    private List<String> newsItems;
    private OkHttpClient client = new OkHttpClient();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_news, container, false);

        newsRecyclerView = view.findViewById(R.id.newsRecyclerView);
        newsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        newsItems = new ArrayList<>();
        newsAdapter = new NewsAdapter(newsItems);
        newsRecyclerView.setAdapter(newsAdapter);

        fetchNews();
        return view;
    }

    private void fetchNews() {
        String apiKey = BuildConfig.GOOGLE_CSE_API_KEY; // Add to BuildConfig
        String cx = BuildConfig.GOOGLE_CSE_CX; // Add to BuildConfig
        String url = "https://www.googleapis.com/customsearch/v1?key=" + apiKey + "&cx=" + cx + "&q=car+updates+technology+accidents";

        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("NewsFragment", "Failed to fetch news: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonResponse = new JSONObject(response.body().string());
                        JSONArray items = jsonResponse.getJSONArray("items");
                        newsItems.clear();
                        for (int i = 0; i < items.length() && i < 5; i++) { // Limit to 5 items
                            JSONObject item = items.getJSONObject(i);
                            String title = item.getString("title");
                            newsItems.add(title);
                        }
                        requireActivity().runOnUiThread(() -> newsAdapter.notifyDataSetChanged());
                    } catch (JSONException e) {
                        Log.e("NewsFragment", "JSON parsing error: " + e.getMessage());
                    }
                }
            }
        });
    }

    static class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {
        private List<String> newsItems;

        NewsAdapter(List<String> newsItems) {
            this.newsItems = newsItems;
        }

        @NonNull
        @Override
        public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_news, parent, false);
            return new NewsViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
            holder.newsTextView.setText(newsItems.get(position));
        }

        @Override
        public int getItemCount() {
            return newsItems.size();
        }

        static class NewsViewHolder extends RecyclerView.ViewHolder {
            TextView newsTextView;

            NewsViewHolder(@NonNull View itemView) {
                super(itemView);
                newsTextView = itemView.findViewById(R.id.newsTextView);
            }
        }
    }
}