package com.selfawarelab.restapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.DisposableSubscriber;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.Query;

public class MainActivity extends AppCompatActivity {
    final String BASE_URL = "https://api.pagerduty.com/";

    Map<String, String> map = new HashMap<>();

    List<String> userString = new ArrayList<>();
    List<User> userList = new ArrayList<>();

    ListView listView;
    ArrayAdapter<String> arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = (ListView) findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                User u = userList.get(position);
                Toast.makeText(MainActivity.this, "Contact Method Count: " + u.contactMethods.size(), Toast.LENGTH_SHORT).show();

                String text = "";
                for(ContactMethod m : u.contactMethods) {
                    text += m.type + " ";
                }

                Toast.makeText(MainActivity.this, "Contact Method Types: " + text, Toast.LENGTH_SHORT).show();

            }
        });

        webSetup();
    }

    private void webSetup() {
        map.put("Accept", "application/vnd.pagerduty+json;version=2");
        map.put("Authorization", "Token token=y_NbAkKc66ryYTWUXYEu");

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create()) // Learn about this
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WebService service = retrofit.create(WebService.class);

        Flowable<Users> flowable = service.getUsers(map, "contact_methods");
        flowable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new DisposableSubscriber<Users>() {
                    @Override
                    public void onNext(Users users) {
                        Log.d("TAG", "User: " + users.users.size());

                        userList = users.users;

                        for(User u : users.users) {
                            String text = u.idString + " Name: " + u.nameString;
                            userString.add(text);
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        Log.d("TAG", "error: " + t.getLocalizedMessage());

                    }

                    @Override
                    public void onComplete() {
                        Log.d("TAG", "Complete");

                        arrayAdapter.addAll(userString);
                        listView.setAdapter(arrayAdapter);
                        arrayAdapter.notifyDataSetChanged();
                    }
                });
    }

    // Web Stuff
    interface WebService {
        @GET("users")
        Flowable<Users> getUsers(@HeaderMap Map<String, String> headers, @Query("include[]") String param);
    }

    // JSON stuff
    class Users {
        @SerializedName("users")
        @Expose
        public List<User> users;
    }

    class ContactMethod {
        @SerializedName("type")
        @Expose
        public String type;
    }

    class User {
        @SerializedName("name")
        @Expose
        public String nameString;

        @SerializedName("id")
        @Expose
        public String idString;

        @SerializedName("contact_methods")
        @Expose
        public List<ContactMethod> contactMethods;
    }

}
