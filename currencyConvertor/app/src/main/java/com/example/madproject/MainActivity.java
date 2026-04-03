package com.example.madproject;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;
import android.content.Intent;
import android.text.TextWatcher;
import android.text.Editable;

public class MainActivity extends AppCompatActivity {

        private double convert(String from, String to, double amount,
        double usdToInr, double usdToJpy, double usdToEur) {

            double amountInUSD = 0;

            switch (from) {
                case "USD": amountInUSD = amount; break;
                case "INR": amountInUSD = amount / usdToInr; break;
                case "JPY": amountInUSD = amount / usdToJpy; break;
                case "EUR": amountInUSD = amount / usdToEur; break;
            }

            switch (to) {
                case "USD": return amountInUSD;
                case "INR": return amountInUSD * usdToInr;
                case "JPY": return amountInUSD * usdToJpy;
                case "EUR": return amountInUSD * usdToEur;
            }

            return 0;
        }
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", false);

        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        EditText editTop = findViewById(R.id.editAmountTop);
        EditText editBottom = findViewById(R.id.editAmountBottom);

        Spinner spinnerTop = findViewById(R.id.spinnerTop);
        Spinner spinnerBottom = findViewById(R.id.spinnerBottom);

// Currency list
        String[] currencies = {"INR", "USD", "JPY", "EUR"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                currencies
        );

        //Base currency is USD
        double usdToInr = 83.0;
        double usdToJpy = 150.0;
        double usdToEur = 0.92;


        spinnerTop.setAdapter(adapter);
        spinnerBottom.setAdapter(adapter);

// Default selections
        spinnerTop.setSelection(0); // INR
        spinnerBottom.setSelection(1);   // USD

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String input = editTop.getText().toString();

                if (!input.isEmpty()) {
                    double amount = Double.parseDouble(input);

                    String from = spinnerTop.getSelectedItem().toString();
                    String to = spinnerBottom.getSelectedItem().toString();

                    double result = convert(from, to, amount, usdToInr, usdToJpy, usdToEur);

                    editBottom.setText(String.format("%.2f", result));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerTop.setOnItemSelectedListener(listener);
        spinnerBottom.setOnItemSelectedListener(listener);




        editTop.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (s.length() == 0) {
                    editBottom.setText("");
                    return;
                }

                double amount = Double.parseDouble(s.toString());

                String from = spinnerTop.getSelectedItem().toString();
                String to = spinnerBottom.getSelectedItem().toString();

                double result = convert(from, to, amount, usdToInr, usdToJpy, usdToEur);

                editBottom.setAlpha(0f);
                editBottom.setText(String.format("%.2f", result));
                editBottom.animate().alpha(1f).setDuration(150);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });





        Button btnSettings = findViewById(R.id.btnSettings);

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingActivity.class);
            startActivity(intent);
        });
}

    }

