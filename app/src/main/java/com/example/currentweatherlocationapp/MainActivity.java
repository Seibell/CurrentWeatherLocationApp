package com.example.currentweatherlocationapp;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private FusedLocationProviderClient mFusedLocationClient;

    private static final String API_KEY = "ec104ce4828c00949c5c4f5871a5e982";
    private static final ArrayList<String> cityNames = new ArrayList<>(Arrays.asList("New York", "Singapore", "Mumbai", "Delhi", "Sydney", "Melbourne"));

    private ArrayList<WeatherData> weatherDataList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        loadSavedWeatherData();
        getLocation();

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new WeatherDataAdapter(weatherDataList));
        recyclerView.setHasFixedSize(true);
    }

    private void getLocation() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        } else {
            if (isNetworkConnected()) {
                mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            getCurrentWeather(location.getLatitude(), location.getLongitude());
                            fetchWeatherDataForCities();
                        }
                    }
                });
            } else {
                loadSavedWeatherData();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            }
        }
    }

    private void getCurrentWeather(double latitude, double longitude) {
        String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + latitude + "&lon=" + longitude + "&units=metric&appid=" + API_KEY;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> loadSavedWeatherData());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonData = Objects.requireNonNull(response.body()).string();
                    saveWeatherData(jsonData, System.currentTimeMillis());
                    parseCurrentWeatherData(jsonData);
                }
            }
        });
    }

    private void getCurrentWeather(String cityName) {
        String url = "https://api.openweathermap.org/data/2.5/weather?q=" + cityName + "&units=metric&appid=" + API_KEY;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> loadSavedWeatherData());
            }

            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonData = Objects.requireNonNull(response.body()).string();
                    WeatherData weatherData = parseWeatherData(jsonData);
                    runOnUiThread(() -> {
                        weatherDataList.add(weatherData);
                        weatherDataList.sort((a, b) -> b.getCityName().compareTo(a.getCityName()));
                        RecyclerView recyclerView = findViewById(R.id.recyclerView);
                        recyclerView.getAdapter().notifyDataSetChanged();
                    });
                }
            }
        });
    }
    private WeatherData parseWeatherData(String jsonData) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(jsonData, JsonObject.class);
        WeatherData weatherData = new WeatherData();
        weatherData.setCityName(jsonObject.get("name").getAsString());
        JsonObject mainObject = jsonObject.getAsJsonObject("main");
        weatherData.setFeelsLike(mainObject.get("feels_like").getAsDouble());
        weatherData.setTemperature(mainObject.get("temp").getAsDouble());
        weatherData.setMinTemperature(mainObject.get("temp_min").getAsDouble());
        weatherData.setMaxTemperature(mainObject.get("temp_max").getAsDouble());
        weatherData.setHumidity(mainObject.get("humidity").getAsInt());
        weatherData.setPressure(mainObject.get("pressure").getAsInt());

        JsonObject weatherObject = jsonObject.getAsJsonArray("weather").get(0).getAsJsonObject();
        weatherData.setWeatherDescription(weatherObject.get("description").getAsString());

        return weatherData;
    }

    private void fetchWeatherDataForCities() {
        for (String cityName : cityNames) {
            getCurrentWeather(cityName);
        }
    }

    private void parseCurrentWeatherData(String jsonData) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(jsonData, JsonObject.class);

        WeatherData weatherData = new WeatherData();
        weatherData.setCityName(jsonObject.get("name").getAsString());
        JsonObject mainObject = jsonObject.getAsJsonObject("main");
        weatherData.setTemperature(mainObject.get("temp").getAsDouble());
        weatherData.setFeelsLike(mainObject.get("feels_like").getAsDouble());
        weatherData.setMinTemperature(mainObject.get("temp_min").getAsDouble());
        weatherData.setMaxTemperature(mainObject.get("temp_max").getAsDouble());
        weatherData.setHumidity(mainObject.get("humidity").getAsInt());
        weatherData.setPressure(mainObject.get("pressure").getAsInt());

        JsonObject weatherObject = jsonObject.getAsJsonArray("weather").get(0).getAsJsonObject();
        weatherData.setWeatherDescription(weatherObject.get("description").getAsString());

        runOnUiThread(() -> updateWeatherUI(weatherData));
    }

    private void updateWeatherUI(WeatherData weatherData) {
        TextView weatherLocation = findViewById(R.id.tv_current_location_name);
        weatherLocation.setText(String.format(Locale.getDefault(), "%s", weatherData.getCityName()));

        TextView weatherText = findViewById(R.id.tv_current_weather_condition);
        weatherText.setText(String.format(Locale.getDefault(), "Weather: %s\nTemperature: %.1f°C\nFeels Like: %.1f°C\nMin Temperature: %.1f°C\nMax Temperature: %.1f°C\nHumidity: %d%%\nPressure: %dhPa\n",
                weatherData.getWeatherDescription(),
                weatherData.getTemperature(),
                weatherData.getFeelsLike(),
                weatherData.getMinTemperature(),
                weatherData.getMaxTemperature(),
                weatherData.getHumidity(),
                weatherData.getPressure()));
        weatherText.append("Time: Current");
    }

    private void saveWeatherData(String jsonData, long timestamp) {
        SharedPreferences sharedPreferences = getSharedPreferences("weather_preferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("weather_data", jsonData);
        editor.putLong("weather_timestamp", timestamp);
        editor.apply();
    }

    private void loadSavedWeatherData() {
        SharedPreferences sharedPreferences = getSharedPreferences("weather_preferences", MODE_PRIVATE);
        String jsonData = sharedPreferences.getString("weather_data", null);
        long timestamp = sharedPreferences.getLong("weather_timestamp", 0);

        if (jsonData != null) {
            parseCurrentWeatherData(jsonData);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String dateString = sdf.format(new Date(timestamp));

            TextView weatherText = findViewById(R.id.tv_current_weather_condition);
            weatherText.append("Time: " + dateString);
        }
    }

    private boolean isNetworkConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }
}