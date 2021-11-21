package com.lau.koubaissitechnews;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    SQLiteDatabase db;
    ArrayAdapter ad;
    ArrayList<String> names = new ArrayList<String>();
    ArrayList<String> texts = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = this.openOrCreateDatabase("TechArticles", MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleId INTEGER, title VARCHAR, content VARCHAR)");

        DownloadTask task = new DownloadTask();
        try {
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        } catch (Exception e) {e.printStackTrace();}

        ListView lv = findViewById(R.id.lv);
        ad = new ArrayAdapter(this, android.R.layout.simple_list_item_1, names);
        lv.setAdapter(ad);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                Intent i1 = new Intent(getApplicationContext(), ArticleActivity.class);
                i1.putExtra("content", texts.get(i));
                startActivity(i1);
            }
        });

        List();
    }

    public class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {

            String s = "";
            String id = "";
            String name = "";
            String text = "";

            int total = 20;
            URL url;
            HttpURLConnection urlConnection = null;

            try {

                url = new URL(strings[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream stream = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(stream);
                int data = reader.read();

                while (data != -1) {

                    char cur = (char) data;
                    s += cur;
                    data = reader.read();
                }

                JSONArray jr = new JSONArray(s);
                if (jr.length() < 20) total = jr.length();
                db.execSQL("DELETE FROM articles");

                for (int i = 0; i < total; i++) {

                    id = jr.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + id + ".json?print=pretty");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    stream = urlConnection.getInputStream();
                    reader = new InputStreamReader(stream);
                    data = reader.read();
                    String s1 = "";

                    while (data != -1) {

                        char cur = (char) data;
                        s1 += cur;
                        data = reader.read();
                    }
                    JSONObject jo = new JSONObject(s1);

                    if (!jo.isNull("title") && !jo.isNull("url")) {

                        name = jo.getString("title");
                        String link = jo.getString("url");
                        url = new URL(link);
                        urlConnection = (HttpURLConnection) url.openConnection();
                        stream = urlConnection.getInputStream();
                        reader = new InputStreamReader(stream);
                        data = reader.read();

                        while (data != -1) {

                            char cur = (char) data;
                            text += cur;
                            data = reader.read();
                        }

                        String str = "INSERT INTO articles (articleId, title, content) VALUES (?, ?, ?)";
                        SQLiteStatement res = db.compileStatement(str);

                        res.bindString(1, id);
                        res.bindString(2, name);
                        res.bindString(3, text);
                        res.execute();
                    }
                }
                return s;

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            List();
        }
    }

    public void List() {

        Cursor c = db.rawQuery("SELECT * FROM articles", null);
        int textIndex = c.getColumnIndex("content");
        int nameIndex = c.getColumnIndex("title");

        if (c.moveToFirst()) {

            names.clear();
            texts.clear();

            do {

                names.add(c.getString(nameIndex));
                texts.add(c.getString(textIndex));

            } while (c.moveToNext());

            ad.notifyDataSetChanged();
        }

    }
}