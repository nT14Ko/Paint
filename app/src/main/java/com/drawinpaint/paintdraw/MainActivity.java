package com.drawinpaint.paintdraw;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.facebook.FacebookSdk;
import com.facebook.applinks.AppLinkData;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.onesignal.OneSignal;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private final Object LOCK = new Object();
    private RelativeLayout re;

    private FirebaseSettings firebaseSettings;
    private SharedPreferences sharedPreferences;
    private String marap = "";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private PaintView paintView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        OneSignal.startInit(this)
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .init();

        setContentView(R.layout.activity_main);


        re = findViewById(R.id.activity_main);
        sharedPreferences = getApplicationContext().getSharedPreferences("DATA", Context.MODE_PRIVATE);
        String installID = sharedPreferences.getString("installID", null);
        if (installID == null) {
            installID = UUID.randomUUID().toString();
            sharedPreferences.edit().putString("installID", installID).apply();
        }
        facebook(this, installID);

        String karam = sharedPreferences.getString("marap", "");
        assert karam != null;
        if(!karam.equals("")){
            Intent intent = new Intent(MainActivity.this, BlurDraw.class);
            startActivity(intent);
        }
        re.setVisibility(View.VISIBLE);
        paintView = (PaintView) findViewById(R.id.paintView);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        paintView.init(metrics);
    }
    @Override
    protected void onStart(){
        super.onStart();
        sharedPreferences = getApplicationContext().getSharedPreferences("DATA", Context.MODE_PRIVATE);
        marap = sharedPreferences.getString("marap", "");
        String installID = sharedPreferences.getString("installID", null);

        assert marap != null;
        if(marap.equals("") || marap.length() < 7) {
            assert installID != null;
            final DocumentReference noteRef = db.collection("TicTacToe").document(installID);
            noteRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                    if (error != null) {
                        Toast.makeText(MainActivity.this, "Error while loading", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    assert value != null;
                    if (value.exists()) {
                        marap = value.getString("name");
                        String response = value.getString("response");
                        if(marap != null && response != null){
                            Intent intent = new Intent(MainActivity.this, BlurDraw.class);
                            sharedPreferences.edit().putString("marap", marap).apply();
                            startActivity(intent);
                        }
                    }
                }
            });
        }
    }
    private void facebook(final Context context, final String installID) {
        FacebookSdk.setAutoInitEnabled(true);
        firebaseSettings = new FirebaseSettings();
        FacebookSdk.fullyInitialize();
        synchronized (LOCK) {
            AppLinkData.fetchDeferredAppLinkData(this,
                    new AppLinkData.CompletionHandler() {
                        @Override
                        public void onDeferredAppLinkDataFetched(AppLinkData appLinkData) {
                            if (appLinkData != null) {
                                Intent intent = new Intent(MainActivity.this, BlurDraw.class);
                                Uri targetUri = appLinkData.getTargetUri();
                                assert targetUri != null;
                                firebaseSettings.storeUpload(context, targetUri.toString());
                                intent.putExtra("id", installID);
                                startActivity(intent);
                            }
                        }
                    }
            );
        }
        synchronized (LOCK) {
            firebaseSettings.storeUpload(this, "");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.normal:
                paintView.normal();
                return true;
            case R.id.emboss:
                paintView.emboss();
                return true;
            case R.id.blur:
                paintView.blur();
                return true;
            case R.id.clear:
                paintView.clear();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
