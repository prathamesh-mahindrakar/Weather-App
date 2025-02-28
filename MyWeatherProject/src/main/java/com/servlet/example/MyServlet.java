package com.servlet.example;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

@WebServlet("/MyServlet")
public class MyServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public MyServlet() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.getWriter().append("Served at: ").append(request.getContextPath());
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // OpenWeatherMap API Key (Ensure it's valid)
        String apiKey = "b12835045abd338e67d1f3eb211c87d9";

        // Get city name from user input
        String city = request.getParameter("city");

        if (city == null || city.trim().isEmpty()) {
            request.setAttribute("error", "City name cannot be empty.");
            request.getRequestDispatcher("index.jsp").forward(request, response);
            return;
        }

        // Encode the city name to handle spaces
        String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=" + encodedCity + "&appid=" + apiKey;

        HttpURLConnection connection = null;
        Scanner scanner = null;

        try {
            URL url = new URL(apiUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);  // Set timeout for better performance
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) { // Check for errors in API response
                request.setAttribute("error", "Failed to fetch weather data. Error code: " + responseCode);
                request.getRequestDispatcher("index.jsp").forward(request, response);
                return;
            }

            // Read API response
            InputStreamReader reader = new InputStreamReader(connection.getInputStream());
            scanner = new Scanner(reader);
            StringBuilder responseContent = new StringBuilder();

            while (scanner.hasNext()) {
                responseContent.append(scanner.nextLine());
            }

            // Parse JSON response
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(responseContent.toString(), JsonObject.class);

            // Extract weather details
            long dateTimestamp = jsonObject.get("dt").getAsLong() * 1000;
            String date = new Date(dateTimestamp).toString();

            double temperatureKelvin = jsonObject.getAsJsonObject("main").get("temp").getAsDouble();
            int temperatureCelsius = (int) (temperatureKelvin - 273.15);

            int humidity = jsonObject.getAsJsonObject("main").get("humidity").getAsInt();
            double windSpeed = jsonObject.getAsJsonObject("wind").get("speed").getAsDouble();
            String weatherCondition = jsonObject.getAsJsonArray("weather").get(0).getAsJsonObject().get("main").getAsString();

            // Set attributes for JSP
            request.setAttribute("date", date);
            request.setAttribute("city", city);
            request.setAttribute("temperature", temperatureCelsius);
            request.setAttribute("weatherCondition", weatherCondition);
            request.setAttribute("humidity", humidity);
            request.setAttribute("windSpeed", windSpeed);
            request.setAttribute("weatherData", responseContent.toString());

            // Forward to JSP page
            request.getRequestDispatcher("index.jsp").forward(request, response);

        } catch (IOException e) {
            request.setAttribute("error", "Error fetching weather data: " + e.getMessage());
            request.getRequestDispatcher("index.jsp").forward(request, response);
        } finally {
            if (scanner != null) {
                scanner.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
