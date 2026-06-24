package com.sos.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        final EditText editNum = (EditText) findViewById(R.id.editNum);
        final Button btnSave = (Button) findViewById(R.id.btnSave);
        final Button btnDelete = (Button) findViewById(R.id.btnDelete);

        // پرانا نمبر لوڈ کریں
        editNum.setText(getSharedPreferences("SOS_PREFS", MODE_PRIVATE).getString("SOS_NUM", ""));

        // بٹن کا رنگ تبدیل کرنے کا فیچر
        editNum.addTextChangedListener(new TextWatcher() {
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					btnSave.setBackgroundColor(s.length() == 11 ? Color.parseColor("#4CAF50") : Color.GRAY);
				}
				public void afterTextChanged(Editable s) {}
			});

        // Save بٹن
        btnSave.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if(editNum.getText().length() == 11) {
						getSharedPreferences("SOS_PREFS", MODE_PRIVATE).edit().putString("SOS_NUM", editNum.getText().toString()).apply();
						// کامیابی کا پیغام
						Toast.makeText(SettingsActivity.this, "نمبر کامیابی سے محفوظ ہو گیا!", Toast.LENGTH_SHORT).show();
						finish();
					} else {
						Toast.makeText(SettingsActivity.this, "براہ کرم صحیح 11 ہندسوں کا نمبر درج کریں", Toast.LENGTH_SHORT).show();
					}
				}
			});

        // Delete بٹن
        btnDelete.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					getSharedPreferences("SOS_PREFS", MODE_PRIVATE).edit().remove("SOS_NUM").apply();
					editNum.setText("");
					btnSave.setBackgroundColor(Color.GRAY);
					// ڈیلیٹ ہونے کا پیغام
					Toast.makeText(SettingsActivity.this, "نمبر ڈیلیٹ ہو گیا!", Toast.LENGTH_SHORT).show();
				}
			});
    }
}

