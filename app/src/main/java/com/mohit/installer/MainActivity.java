package com.mohit.installer;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mohit.installer.databinding.ActivityMainBinding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String PACKAGE_INSTALLED_ACTION =
            "com.example.android.apis.content.SESSION_API_PACKAGE_INSTALLED";

    private ActivityMainBinding binding;
    private int count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Watch for button clicks.
        binding.install.setOnClickListener(v -> {
            PackageInstaller.Session session = null;
            try {
                PackageInstaller packageInstaller = getPackageManager().getPackageInstaller();
                PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                        PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                int sessionId = packageInstaller.createSession(params);
                session = packageInstaller.openSession(sessionId);
                addApkToInstallSession("my_app.apk", session);
                // Create an install status receiver.
                Context context = MainActivity.this;
                Intent intent = new Intent(context, MainActivity.class);
                intent.setAction(PACKAGE_INSTALLED_ACTION);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
                IntentSender statusReceiver = pendingIntent.getIntentSender();
                // Commit the session (this will start the installation workflow).
                session.commit(statusReceiver);
            } catch (IOException e) {
                throw new RuntimeException("Couldn't install package", e);
            } catch (RuntimeException e) {
                if (session != null) {
                    session.abandon();
                }
                throw e;
            }
        });
    }

    private void addApkToInstallSession(String assetName, PackageInstaller.Session session)
            throws IOException {
        // It's recommended to pass the file size to openWrite(). Otherwise installation may fail
        // if the disk is almost full.
        try (OutputStream packageInSession = session.openWrite("package", 0, -1);
             InputStream is = getAssets().open(assetName)) {
            byte[] buffer = new byte[16384];
            int n;
            count = 1;
            while ((n = is.read(buffer)) >= 0) {
                packageInSession.write(buffer, 0, n);
                Log.d(TAG, "addApkToInstallSession: " + count++);
            }
        }
    }

    // Note: this Activity must run in singleTop launchMode for it to be able to receive the intent
    // in onNewIntent().
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Bundle extras = intent.getExtras();
        if (PACKAGE_INSTALLED_ACTION.equals(intent.getAction())) {
            int status = extras.getInt(PackageInstaller.EXTRA_STATUS);
            String message = extras.getString(PackageInstaller.EXTRA_STATUS_MESSAGE);
            switch (status) {
                case PackageInstaller.STATUS_PENDING_USER_ACTION:
                    // This test app isn't privileged, so the user has to confirm the install.
                    Intent confirmIntent = (Intent) extras.get(Intent.EXTRA_INTENT);
                    startActivity(confirmIntent);
                    break;
                case PackageInstaller.STATUS_SUCCESS:
                    Toast.makeText(this, "Install succeeded!", Toast.LENGTH_SHORT).show();
                    break;
                case PackageInstaller.STATUS_FAILURE:
                case PackageInstaller.STATUS_FAILURE_ABORTED:
                case PackageInstaller.STATUS_FAILURE_BLOCKED:
                case PackageInstaller.STATUS_FAILURE_CONFLICT:
                case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
                case PackageInstaller.STATUS_FAILURE_INVALID:
                case PackageInstaller.STATUS_FAILURE_STORAGE:
                    Toast.makeText(this, "Install failed! " + status + ", " + message,
                            Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(this, "Unrecognized status received from installer: " + status,
                            Toast.LENGTH_SHORT).show();
            }
        }
    }
}
