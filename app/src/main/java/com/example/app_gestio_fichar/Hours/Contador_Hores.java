package com.example.app_gestio_fichar.Hours;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.app_gestio_fichar.CSV.Info_horari;
import com.example.app_gestio_fichar.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDateTime;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class Contador_Hores extends AppCompatActivity {

    private Button countBtn;
    private TextView textViewNif;
    private TextView textViewEmail;
    private TextView worked_hours;
    private TextView textView2;
    private TextView textView3;
    private TextView textView4;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth;
    private Timer timer;
    private Handler handler = new Handler();
    private Validador validador;
    private Context context;
    private Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contador_hores);

        countBtn = findViewById(R.id.countBtn);
        textViewNif = findViewById(R.id.TextViewDNI);
        textViewEmail = findViewById(R.id.TextViewEmail);
        worked_hours = findViewById(R.id.worked_hours);
        textView2 = findViewById(R.id.textView2);
        textView3 = findViewById(R.id.textView3);
        textView4 = findViewById(R.id.textView4);
        mAuth = FirebaseAuth.getInstance();
        validador = new Validador();
        context = this;
        String uriString = getIntent().getStringExtra("file_uri");
        uri = Uri.parse(uriString);

        if (uri != null && !uri.getPath().isEmpty()) {
            textView2.setText("uri: " + uri.getPath());
        } else {
            textView2.setText("uri es null");
        }

        countBtn.setOnClickListener(v -> contar(v));

    }

    public void contar(View view) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            textViewNif.setText("Ha pillat el usuari: " + currentUser.getEmail());
            db.collection("Empleats").document(currentUser.getEmail()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String ruta_horari = documentSnapshot.getString("ruta_horari");

                            try {

                                Info_horari horari = new Info_horari();
                                horari = horari.llegirCSV(uri, getContentResolver(), LocalDateTime.now());

                                // logs

                                textView4.setText(horari.toString());

                                //logs

                                if (horari != null) {
                                    int dia = horari.getDia();
                                    String[] hores = horari.getHores().toArray(new String[0]);
                                    String[] x = horari.getX().toArray(new String[0]);

                                    // Usar los datos recopilados para determinar si es la hora de trabajo
                                    boolean isWorkingTime = validador.isWorkingTime(dia, hores, x, LocalDateTime.now().getHour(), LocalDateTime.now().getMinute());

                                    if (isWorkingTime) {
                                        // Iniciar el temporizador para contar las horas
                                        if (timer == null) {
                                            worked_hours.setText("0");
                                            timer = new Timer();
                                            timer.scheduleAtFixedRate(new TimerTask() {
                                                @Override
                                                public void run() {
                                                    updateUI(documentSnapshot, 1);
                                                }
                                            }, 0, TimeUnit.HOURS.toMillis(1));

                                            countBtn.setEnabled(false);
                                        }
                                    } else {
                                        textView3.setText("No es hora de trabajo según el horario");
                                    }
                                } else {
                                    textView3.setText("Error al obtener el horario");
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.e("Contador_Hores", "Error al obtener datos de Firebase", e);
                            }
                        }
                    });
        } else {
            textView4.setText("No se ha obtenido el usuario");
        }

    }

    private void updateUI(DocumentSnapshot documentSnapshot, long hoursToAdd) {
        try {
            String workedHoursString = documentSnapshot.getString("worked_hours");
            long workedhours = Long.parseLong(workedHoursString);
            workedhours += hoursToAdd;
            final String finalWorkedHours = String.valueOf(workedhours);  // Cambiado a String
            handler.post(() -> worked_hours.setText(finalWorkedHours));
        } catch (NumberFormatException e) {
            Log.e("Contador_Hores", "Error al convertir 'worked_hours' a número", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
