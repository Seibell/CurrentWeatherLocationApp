package com.example.currentweatherlocationapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class WeatherDataAdapter extends RecyclerView.Adapter<WeatherDataAdapter.ViewHolder> {
    private List<WeatherData> weatherDataList;

    public WeatherDataAdapter(List<WeatherData> weatherDataList) {
        this.weatherDataList = weatherDataList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.weather_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WeatherData weatherData = weatherDataList.get(position);
        holder.bind(weatherData);
    }

    @Override
    public int getItemCount() {
        return weatherDataList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView cityName, weatherInfo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cityName = itemView.findViewById(R.id.city_name);
            weatherInfo = itemView.findViewById(R.id.weather_info);
        }

        public void bind(WeatherData weatherData) {
            cityName.setText(weatherData.getCityName());
            weatherInfo.setText(String.format(Locale.getDefault(), "Weather: %s\nTemperature: %.1f°C\nFeels Like: %.1f°C\nMin Temperature: %.1f°C\nMax Temperature: %.1f°C\nHumidity: %d%%\nPressure: %dhPa\n",
                    weatherData.getWeatherDescription(),
                    weatherData.getTemperature(),
                    weatherData.getFeelsLike(),
                    weatherData.getMinTemperature(),
                    weatherData.getMaxTemperature(),
                    weatherData.getHumidity(),
                    weatherData.getPressure()));
        }
    }
}