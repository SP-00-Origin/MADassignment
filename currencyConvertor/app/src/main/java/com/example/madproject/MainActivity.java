package com.example.madproject;

import android.os.Bundle;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });

        Spinner spinnerFrom = findViewById(R.id.spinnerFrom);
        Spinner spinnerTo = findViewById(R.id.spinnerTo);

// Currency list
        String[] currencies = {"INR", "USD", "JPY", "EUR"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                currencies
        );

        spinnerFrom.setAdapter(adapter);
        spinnerTo.setAdapter(adapter);

// Default selections
        spinnerFrom.setSelection(0); // INR
        spinnerTo.setSelection(1);   // USD

    EditText editAmount = findViewById(R.id.editAmount);
    Button btnConvert = findViewById(R.id.btnConvert);
    TextView textResult = findViewById(R.id.textResult);

        //Base currency is USD
        double usdToInr = 83.0;
        double usdToJpy = 150.0;
        double usdToEur = 0.92;

        btnConvert.setOnClickListener(v -> {

            String from = spinnerFrom.getSelectedItem().toString();
            String to = spinnerTo.getSelectedItem().toString();

            String input = editAmount.getText().toString();

            if (input.isEmpty()) {
                textResult.setText("Enter amount");
                return;
            }

            double amount = Double.parseDouble(input);
            double amountInUSD = 0;
            double finalResult = 0;

            // FROM → USD
            switch (from) {
                case "USD":
                    amountInUSD = amount;
                    break;
                case "INR":
                    amountInUSD = amount / usdToInr;
                    break;
                case "JPY":
                    amountInUSD = amount / usdToJpy;
                    break;
                case "EUR":
                    amountInUSD = amount / usdToEur;
                    break;
            }

            // USD → TO
            switch (to) {
                case "USD":
                    finalResult = amountInUSD;
                    break;
                case "INR":
                    finalResult = amountInUSD * usdToInr;
                    break;
                case "JPY":
                    finalResult = amountInUSD * usdToJpy;
                    break;
                case "EUR":
                    finalResult = amountInUSD * usdToEur;
                    break;
            }

            textResult.setText(String.format("%.2f %s", finalResult, to));
        });
}

    }

