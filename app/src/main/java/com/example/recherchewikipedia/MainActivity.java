package com.example.recherchewikipedia;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


public class MainActivity extends AppCompatActivity
{

    EditText zonepourentrerlarecherche;
    Button bouttonpourlancerlarecherche;
    TextView sectiondaffichageduresultat;
    ProgressBar recherche;

    Handler handler = new Handler(Looper.getMainLooper());



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        zonepourentrerlarecherche = findViewById(R.id.zonepourentrerlarecherche);
        bouttonpourlancerlarecherche = findViewById(R.id.bouttonpourlancerlarecherche);
        sectiondaffichageduresultat = findViewById(R.id.sectiondaffichageduresultat);
        recherche = findViewById(R.id.recherche);


        bouttonpourlancerlarecherche.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                String mot = zonepourentrerlarecherche.getText().toString().trim();

                if (mot.isEmpty())
                {
                    Toast.makeText(MainActivity.this, "Veuillez entrer quelque chose : ", Toast.LENGTH_SHORT).show();
                    return;
                }

                lancementdelarecherche(mot);
            }

        });


    }



    void desactivationdelaverificationdecertificatssl()
    {

        try
        {

            TrustManager[] confiance = new TrustManager[]
                    {

                    new X509TrustManager()
                    {

                        public X509Certificate[] getAcceptedIssuers()
                        {
                            return new X509Certificate[0];
                        }

                        public void checkClientTrusted(X509Certificate[] c, String a) {}
                        public void checkServerTrusted(X509Certificate[] c, String a) {}

                    }

            };


            SSLContext contexteSSL = SSLContext.getInstance("TLS");
            contexteSSL.init(null, confiance, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(contexteSSL.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }


    }


    void lancementdelarecherche(String mot)
    {

        recherche.setVisibility(View.VISIBLE);
        sectiondaffichageduresultat.setText("");
        bouttonpourlancerlarecherche.setEnabled(false);

        Thread thread = new Thread(new Runnable()
        {

            @Override
            public void run()
            {
                String texteResultat;

                try
                {
                    desactivationdelaverificationdecertificatssl();

                    String motEncode = URLEncoder.encode(mot, "UTF-8");
                    String adresse = "https://fr.wikipedia.org/api/rest_v1/page/summary/" + motEncode;

                    URL url = new URL(adresse);
                    HttpsURLConnection connexion = (HttpsURLConnection) url.openConnection();
                    connexion.setRequestMethod("GET");
                    connexion.setConnectTimeout(8000);
                    connexion.setReadTimeout(8000);
                    connexion.setRequestProperty("User-Agent", "Mozilla/5.0");

                    int code = connexion.getResponseCode();


                    if (code == 200)
                    {
                        BufferedReader lecteur = new BufferedReader(new InputStreamReader(connexion.getInputStream()));
                        StringBuilder contenu = new StringBuilder();
                        String ligne;

                        while ((ligne = lecteur.readLine()) != null)
                        {
                            contenu.append(ligne);
                        }

                        lecteur.close();

                        JSONObject json = new JSONObject(contenu.toString());
                        String titre = json.optString("title", "");
                        String resume = json.optString("extract", "Aucun résultat n'a été trouvé sur Wikipedia.");
                        texteResultat = "📖 Wikipedia : " + titre + "\n\n" + resume;


                    }
                    else if (code == 404)
                    {
                        texteResultat = "Aucune réponse n'a été trouvé sur Wikipedia pour ce mot. Essayez autre chose.";
                    }
                    else
                    {
                        texteResultat = "Erreur de serveur (code " + code + "). Réessayez plus tard.";
                    }

                } catch (Exception e)
                {
                    texteResultat = "Erreur : " + e.getMessage();
                }


                final String affichage = texteResultat;
                handler.post(new Runnable()
                {

                    @Override
                    public void run()
                    {
                        recherche.setVisibility(View.GONE);
                        bouttonpourlancerlarecherche.setEnabled(true);
                        sectiondaffichageduresultat.setText(affichage);
                    }

                });


            }

        });



        thread.start();
    }



}