package com.example.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.database.sqlite.SQLiteTransactionListener;
import android.net.UrlQuerySanitizer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.renderscript.ScriptGroup;
import android.util.JsonToken;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    ListView lvNewsThreads;
    ArrayList<String> newsTitle = new ArrayList<>();
    ArrayList<String> newsContent = new ArrayList<>();
    SQLiteDatabase myDatabase;
    ArrayAdapter<String> myAdapter;

    public class getNews extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... urls) {
            String html="";
            URL url = null;
            HttpURLConnection connection = null;

            try {
                url = new URL(urls[0]);
                connection = (HttpURLConnection) url.openConnection();

                InputStream in = connection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);

                int data = reader.read();
                while (data != -1){
                    char cur = (char) data;
                    html += cur;
                    data = reader.read();
                }

                JSONArray jsonArray = new JSONArray(html);
                int jl=10;
                if (jsonArray.length()<10)
                    jl = jsonArray.length();

                myDatabase.execSQL("DELETE FROM article");

                for (int i=0; i<jl; i++){
                    String articleId = jsonArray.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + articleId
                            +".json?print=pretty");
                    connection = (HttpURLConnection) url.openConnection();

                    in = connection.getInputStream();
                    reader = new InputStreamReader(in);

                    String articleInfo = "";

                    data = reader.read();
                    while (data != -1){
                        char cur = (char) data;
                        articleInfo += cur;
                        data = reader.read();
                    }

                    JSONObject jsonObject = new JSONObject(articleInfo);
                    if (!jsonObject.isNull("title") && !jsonObject.isNull("url")){
                        String articleTitle = jsonObject.getString("title");
                        String articleUrl = jsonObject.getString("url");

                        url = new URL(articleUrl);
                        connection = (HttpURLConnection) url.openConnection();
                        in = connection.getInputStream();
                        reader = new InputStreamReader(in);
                        data = reader.read();
                        String articleContent = "";
                        while (data != -1){
                            char cur = (char) data;
                            articleContent += cur;
                            data = reader.read();
                        }

                        String sql = "INSERT INTO article (articleID, title, content) VALUES (?, ?, ?)";
                        SQLiteStatement statement = myDatabase.compileStatement(sql);

                        statement.bindString(1, articleId);
                        statement.bindString(2, articleTitle);
                        statement.bindString(3, articleContent);

                        statement.execute();
                    }
                }
                return html;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListView();
        }
    }

    public void updateListView() {
        Cursor c = myDatabase.rawQuery("SELECT * FROM article", null);

        int titleIndex = c.getColumnIndex("title");
        int contentIndex = c.getColumnIndex("content");

        if (c.moveToFirst()) {
            newsTitle.clear();
            newsContent.clear();

            do {
                newsTitle.add(c.getString(titleIndex));
                newsContent.add(c.getString(contentIndex));
            } while (c.moveToNext());

            myAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myDatabase = this.openOrCreateDatabase("NewsFeed", MODE_PRIVATE, null);
        myDatabase.execSQL("CREATE TABLE IF NOT EXISTS article (id INTEGER PRIMARY KEY, articleID INTEGER" +
                ", title VARCHAR, content VARCHAR)");

        getNews news = new getNews();
        try {
            String html = news.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty").get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        lvNewsThreads = (ListView) findViewById(R.id.lvNewsThreads);
        myAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, newsTitle);
        lvNewsThreads.setAdapter(myAdapter);

        lvNewsThreads.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), NewsActivity.class);
                intent.putExtra("content", newsContent.get(position));
                startActivity(intent);
            }
        });

        updateListView();
    }
}