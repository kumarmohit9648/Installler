package com.mohit.installer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.mohit.installer.databinding.ActivityBasicBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class BasicActivity extends AppCompatActivity {

    static final int REQUEST_INSTALL = 1;
    static final int REQUEST_UNINSTALL = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityBasicBinding binding = ActivityBasicBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.unknownSource.setOnClickListener(mUnknownSourceListener);
        binding.mySource.setOnClickListener(mMySourceListener);
        binding.uninstall.setOnClickListener(mUninstallListener);
        binding.uninstallResult.setOnClickListener(mUninstallResultListener);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == REQUEST_INSTALL) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Install succeeded!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "Install canceled!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Install Failed!", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_UNINSTALL) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Uninstall succeeded!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "Uninstall canceled!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Uninstall Failed!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private final View.OnClickListener mUnknownSourceListener = v -> {
        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        intent.setData(getApkUri("Videoder_14.4.2.apk"));
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    };

    private final View.OnClickListener mMySourceListener = v -> {
        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        intent.setData(getApkUri("HelloActivity.apk"));
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        intent.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME,
                getApplicationInfo().packageName);
        startActivityForResult(intent, REQUEST_INSTALL);
    };

    private final View.OnClickListener mUninstallListener = v -> {
        Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
        intent.setData(Uri.parse(
                "package:com.mohit.installer.BasicActivity"));
        startActivity(intent);
    };

    private final View.OnClickListener mUninstallResultListener = v -> {
        Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
        intent.setData(Uri.parse(
                "package:com.mohit.installer.BasicActivity"));
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        startActivityForResult(intent, REQUEST_UNINSTALL);
    };

    /**
     * Returns a Uri pointing to the APK to install.
     */
    private Uri getApkUri(String assetName) {
        // Before N, a MODE_WORLD_READABLE file could be passed via the ACTION_INSTALL_PACKAGE
        // Intent. Since N, MODE_WORLD_READABLE files are forbidden, and a FileProvider is
        // recommended.
        boolean useFileProvider = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
        // Copy the given asset out into a file so that it can be installed.
        // Returns the path to the file.
        String tempFilename = "tmp.apk";
        byte[] buffer = new byte[16384];
        int fileMode = useFileProvider ? Context.MODE_PRIVATE : Context.MODE_WORLD_READABLE;
        try (InputStream is = getAssets().open(assetName);
             FileOutputStream fout = openFileOutput(tempFilename, fileMode)) {
            int n;
            while ((n = is.read(buffer)) >= 0) {
                fout.write(buffer, 0, n);
            }
        } catch (IOException e) {
            Log.i("InstallApk", "Failed to write temporary APK file", e);
        }
        if (useFileProvider) {
            File toInstall = new File(this.getFilesDir(), tempFilename);
            return FileProvider.getUriForFile(
                    this, "com.example.android.apis.installapkprovider", toInstall);
        } else {
            return Uri.fromFile(getFileStreamPath(tempFilename));
        }
    }
}
