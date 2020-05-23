package com.example.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.InputStreamReader;

public class NewsActivity extends AppCompatActivity {

    WebView wvNews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);

        wvNews = (WebView) findViewById(R.id.wvNews);
        wvNews.getSettings().setJavaScriptEnabled(true);
        wvNews.setWebViewClient(new WebViewClient());

        Intent intent = getIntent();
        wvNews.loadData(intent.getStringExtra("content"), "text/html", "UTF-8");
    }
}
