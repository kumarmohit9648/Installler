package com.mohit.installer;

import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mohit.installer.databinding.ActivityMainBinding;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements OnAttachmentDownloadListener {

    private static final String TAG = "MainActivity";
    private static final String PACKAGE_INSTALLED_ACTION =
            "com.example.android.apis.content.SESSION_API_PACKAGE_INSTALLED";

    private ActivityMainBinding binding;

    private static final String BROADCAST_ACTION =
            "com.android.packageinstaller.ACTION_INSTALL_COMMIT";
    private static final String BROADCAST_SENDER_PERMISSION =
            "android.permission.INSTALL_PACKAGES";
    // private ApplicationInfo mAppInfo;
    private Uri mPackageURI;
    // private ProgressBar mProgressBar;
    // private View mOkPanel;
    // private TextView mStatusTextView;
    // private TextView mExplanationTextView;
    // private Button mDoneButton;
    // private Button mLaunchButton;
    private final int INSTALL_COMPLETE = 1;
    private Intent mLaunchIntent;
    private static final int DLG_OUT_OF_SPACE = 1;
    private CharSequence mLabel;
    private HandlerThread mInstallThread;
    private Handler mInstallHandler;

    /*private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case INSTALL_COMPLETE:
                    if (getIntent().getBooleanExtra(Intent.EXTRA_RETURN_RESULT, false)) {
                        Intent result = new Intent();
                        result.putExtra(Intent.EXTRA_INSTALL_RESULT, msg.arg1);
                        setResult(msg.arg1 == PackageInstaller.STATUS_SUCCESS
                                        ? Activity.RESULT_OK : Activity.RESULT_FIRST_USER,
                                result);
                        clearCachedApkIfNeededAndFinish();
                        return;
                    }
                    // Update the status text
                    // mProgressBar.setVisibility(View.GONE);
                    // Show the ok button
                    int centerTextLabel;
                    int centerExplanationLabel = -1;
                    if (msg.arg1 == PackageInstaller.STATUS_SUCCESS) {
                        mLaunchButton.setVisibility(View.VISIBLE);
                        centerTextLabel = R.string.install_done;
                        // Enable or disable launch button
                        mLaunchIntent = getPackageManager().getLaunchIntentForPackage(
                                mAppInfo.packageName);
                        boolean enabled = false;
                        if (mLaunchIntent != null) {
                            List<ResolveInfo> list = getPackageManager().
                                    queryIntentActivities(mLaunchIntent, 0);
                            if (list != null && list.size() > 0) {
                                enabled = true;
                            }
                        }
                        if (enabled) {
                            mLaunchButton.setOnClickListener(InstallAppProgress.this);
                        } else {
                            mLaunchButton.setEnabled(false);
                        }
                    } else if (msg.arg1 == PackageInstaller.STATUS_FAILURE_STORAGE) {
                        showDialogInner(DLG_OUT_OF_SPACE);
                        return;
                    } else {
                        // Generic error handling for all other error codes.
                        centerExplanationLabel = getExplanationFromErrorCode(msg.arg1);
                        centerTextLabel = R.string.install_failed;
                        mLaunchButton.setVisibility(View.GONE);
                    }
                    if (centerExplanationLabel != -1) {
                        mExplanationTextView.setText(centerExplanationLabel);
                    } else {

                    }
                    mDoneButton.setOnClickListener(InstallAppProgress.this);
                    mOkPanel.setVisibility(View.VISIBLE);
                    break;
                default:
                    break;
            }
        }
    };*/

    public static final String ACTION_INSTALL_COMPLETE = "com.sureshjoshi.android.kioskexample.INSTALL_COMPLETE";
    public static final String ACTION_EXIT_KIOSK_MODE = "com.sureshjoshi.android.kioskexample.EXIT_KIOSK_MODE";

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (ACTION_INSTALL_COMPLETE.equals(action)) {
                int result = intent.getIntExtra(PackageInstaller.EXTRA_STATUS,
                        PackageInstaller.STATUS_FAILURE);
                String packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME);
                // Timber.d("PackageInstallerCallback: result= %s, packageName = %s", result, packageName);
                switch (result) {
                    case PackageInstaller.STATUS_PENDING_USER_ACTION: {
                        // this should not happen in M, but will happen in L and L-MR1
                        startActivity((Intent) intent.getParcelableExtra(Intent.EXTRA_INTENT));
                    }
                    break;
                    case PackageInstaller.STATUS_SUCCESS: {
                        // Timber.d("Package %s installation complete", packageName);
                    }
                    break;
                    default: {
                        // Timber.e("Install failed.");
                        return;
                    }
                }
            } else if (ACTION_EXIT_KIOSK_MODE.equals(action)) {
                enableKioskMode(false);
            }
        }
    };

    private DevicePolicyManager mDpm;
    private boolean mIsKioskEnabled = false;

    private void enableKioskMode(boolean enabled) {
        try {
            if (enabled) {
                if (mDpm.isLockTaskPermitted(this.getPackageName())) {
                    startLockTask();
                    mIsKioskEnabled = true;
                    // mButton.setText(getString(R.string.exit_kiosk_mode));
                } else {
                    // Toast.makeText(getApplicationContext(), getString(R.string.kiosk_not_permitted), Toast.LENGTH_SHORT).show();
                }
            } else {
                stopLockTask();
                mIsKioskEnabled = false;
                // mButton.setText(getString(R.string.enter_kiosk_mode));
            }
        } catch (Exception e) {
            // TODO: Log and handle appropriately
        }
    }

    /*private int getExplanationFromErrorCode(int errCode) {
        Log.d(TAG, "Installation error code: " + errCode);
        switch (errCode) {
            case PackageInstaller.STATUS_FAILURE_BLOCKED:
                return R.string.install_failed_blocked;
            case PackageInstaller.STATUS_FAILURE_CONFLICT:
                return R.string.install_failed_conflict;
            case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
                return R.string.install_failed_incompatible;
            case PackageInstaller.STATUS_FAILURE_INVALID:
                return R.string.install_failed_invalid_apk;
            default:
                return -1;
        }
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mInstallThread = new HandlerThread("InstallThread");
        mInstallThread.start();
        mInstallHandler = new Handler(mInstallThread.getLooper());

        /*IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_RESTARTED);
        registerReceiver(
                mBroadcastReceiver, intentFilter, BROADCAST_SENDER_PERMISSION, null *//*scheduler*//*);*/

        ComponentName deviceAdmin = new ComponentName(this, AdminReceiver.class);
        mDpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (!mDpm.isAdminActive(deviceAdmin)) {
            Toast.makeText(getApplicationContext(), "getString(R.string.not_device_admin)", Toast.LENGTH_SHORT).show();
        }

        if (mDpm.isDeviceOwnerApp(getPackageName())) {
            mDpm.setLockTaskPackages(deviceAdmin, new String[]{getPackageName()});
        } else {
            Toast.makeText(getApplicationContext(), "getString(R.string.not_device_owner)", Toast.LENGTH_SHORT).show();
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_INSTALL_COMPLETE);
        intentFilter.addAction(ACTION_EXIT_KIOSK_MODE);
        registerReceiver(mIntentReceiver, intentFilter);

        // Watch for button clicks.
        binding.install.setOnClickListener(v -> {
            binding.progressHorizontal.setProgress(0);
            binding.progressHorizontal.setVisibility(View.VISIBLE);
            Api api = Api.createClient(MainActivity.this);
            api.getApk(AppConstant.URL).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    PackageInstaller.Session session = null;
                    String filename = "my_app.apk";
                    try {
                        try (FileOutputStream fos = openFileOutput(filename, Context.MODE_PRIVATE)) {
                            fos.write(readAllBytes(response.body().byteStream()));
                            fos.flush();
                        }
                        String path = getApplicationContext().getFilesDir().toString() + "/my_app.apk";
                        Log.d(TAG, "onResponse: " + path);

                        PackageInstaller packageInstaller = getPackageManager().getPackageInstaller();
                        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                                PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                        int sessionId = packageInstaller.createSession(params);
                        session = packageInstaller.openSession(sessionId);
                        addApkToInstallSession(path, session);
                        // Create an install status receiver.
                        Context context = MainActivity.this;
                        Intent intent = new Intent(context, MainActivity.class);
                        intent.setAction(PACKAGE_INSTALLED_ACTION);
                        // PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
                        // IntentSender statusReceiver = pendingIntent.getIntentSender();
                        // Commit the session (this will start the installation workflow).
                        session.commit(createIntentSender(getApplicationContext(), sessionId));

                    } catch (IOException e) {
                        throw new RuntimeException("Couldn't install package", e);
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    } catch (RuntimeException e) {
                        if (session != null) {
                            session.abandon();
                        }
                        throw e;
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    binding.progressHorizontal.setVisibility(View.GONE);
                    Log.d(TAG, "onFailure: " + t.getMessage());
                }
            });
        });
    }

    private static IntentSender createIntentSender(Context context, int sessionId) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                new Intent(ACTION_INSTALL_COMPLETE),
                0);
        return pendingIntent.getIntentSender();
    }

    void onPackageInstalled(int statusCode) {
//        Message msg = mHandler.obtainMessage(INSTALL_COMPLETE);
//        msg.arg1 = statusCode;
//        mHandler.sendMessage(msg);
    }

    /*int getInstallFlags(String packageName) {
        PackageManager pm = getPackageManager();
        try {
            PackageInfo pi =
                    pm.getPackageInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
            if (pi != null) {
                return PackageManager.INSTALL_REPLACE_EXISTING;
            }
        } catch (PackageManager.NameNotFoundException e) {

        }
        return 0;
    }*/

    /*private void doPackageStage(PackageManager pm, PackageInstaller.SessionParams params) {
        final PackageInstaller packageInstaller = pm.getPackageInstaller();
        PackageInstaller.Session session = null;
        try {
            final String packageLocation = mPackageURI.getPath();
            final File file = new File(packageLocation);
            final int sessionId = packageInstaller.createSession(params);
            final byte[] buffer = new byte[65536];

            session = packageInstaller.openSession(sessionId);

            final InputStream in = new FileInputStream(file);
            final long sizeBytes = file.length();
            final OutputStream out = session.openWrite("PackageInstaller", 0, sizeBytes);
            try {
                int c;
                while ((c = in.read(buffer)) != -1) {
                    out.write(buffer, 0, c);
                    if (sizeBytes > 0) {
                        final float fraction = ((float) c / (float) sizeBytes);
                        session.addProgress(fraction);
                    }
                }
                session.fsync(out);
            } finally {
                IOUtils.closeQuietly(in);
                IOUtils.closeQuietly(out);
            }

            // Create a PendingIntent and use it to generate the IntentSender
            Intent broadcastIntent = new Intent(BROADCAST_ACTION);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    MainActivity.this *//*context*//*,
                    sessionId,
                    broadcastIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            session.commit(pendingIntent.getIntentSender());
        } catch (IOException e) {
            onPackageInstalled(PackageInstaller.STATUS_FAILURE);
        } finally {
            IOUtils.closeQuietly(session);
        }
    }*/

    private void initialize() {
        // final PackageUtil.AppSnippet as;
        // final PackageManager pm = getPackageManager();
        // final int installFlags = getInstallFlags(mAppInfo.packageName);

        /*if ((installFlags & PackageManager.INSTALL_REPLACE_EXISTING) != 0) {
            Log.w(TAG, "Replacing package:" + mAppInfo.packageName);
        }
        if ("package".equals(mPackageURI.getScheme())) {
            as = new PackageUtil.AppSnippet(pm.getApplicationLabel(mAppInfo),
                    pm.getApplicationIcon(mAppInfo));
        } else {
            final File sourceFile = new File(mPackageURI.getPath());
            as = PackageUtil.getAppSnippet(this, mAppInfo, sourceFile);
        }*/
        // mLabel = as.label;
        // PackageUtil.initSnippetForNewApp(this, as, R.id.app_snippet);
        // mProgressBar.setIndeterminate(true);
        // Hide button till progress is being displayed
        // mOkPanel.setVisibility(View.INVISIBLE);

        /*if ("package".equals(mPackageURI.getScheme())) {
            try {
                pm.installExistingPackage(mAppInfo.packageName);
                onPackageInstalled(PackageInstaller.STATUS_SUCCESS);
            } catch (PackageManager.NameNotFoundException e) {
                onPackageInstalled(PackageInstaller.STATUS_FAILURE_INVALID);
            }
        } else {*/
        final PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL);
//            params.referrerUri = getIntent().getParcelableExtra(Intent.EXTRA_REFERRER);
//            params.originatingUri = getIntent().getParcelableExtra(Intent.EXTRA_ORIGINATING_URI);
//            params.originatingUid = getIntent().getIntExtra(Intent.EXTRA_ORIGINATING_UID,
//                    UID_UNKNOWN);

//            File file = new File(mPackageURI.getPath());
//            try {
//                PackageLite pkg = PackageParser.parsePackageLite(file, 0);
//                params.setAppPackageName(pkg.packageName);
//                params.setInstallLocation(pkg.installLocation);
//                params.setSize(
//                        PackageHelper.calculateInstalledSize(pkg, false, params.abiOverride));
//            } catch (PackageParser.PackageParserException e) {
//                Log.e(TAG, "Cannot parse package " + file + ". Assuming defaults.");
//                Log.e(TAG, "Cannot calculate installed size " + file + ". Try only apk size.");
//                params.setSize(file.length());
//            } catch (IOException e) {
//                Log.e(TAG, "Cannot calculate installed size " + file + ". Try only apk size.");
//                params.setSize(file.length());
//            }
//
//            mInstallHandler.post(() -> doPackageStage(pm, params));
        // }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mIntentReceiver);
        mInstallThread.getLooper().quitSafely();
    }

    /*public void onClick(View v) {
        if (v == mDoneButton) {
            if (mAppInfo.packageName != null) {
                Log.i(TAG, "Finished installing " + mAppInfo.packageName);
            }
            clearCachedApkIfNeededAndFinish();
        } else if (v == mLaunchButton) {
            try {
                startActivity(mLaunchIntent);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "Could not start activity", e);
            }
            clearCachedApkIfNeededAndFinish();
        }
    }*/

    /*public void onCancel(DialogInterface dialog) {
        clearCachedApkIfNeededAndFinish();
    }*/

    private void clearCachedApkIfNeededAndFinish() {
        // If we are installing from a content:// the apk is copied in the cache
        // dir and passed in here. As we aren't started for a result because our
        // caller needs to be able to forward the result, here we make sure the
        // staging file in the cache dir is removed.
        if (mPackageURI != null) {
            if ("file".equals(mPackageURI.getScheme()) && mPackageURI.getPath() != null
                    && mPackageURI.getPath().startsWith(getCacheDir().toString())) {
                File file = new File(mPackageURI.getPath());
                file.delete();
            }
        }
        finish();
    }

    private void addApkToInstallSession(String path, PackageInstaller.Session session)
            throws IOException {
        // It's recommended to pass the file size to openWrite(). Otherwise installation may fail
        // if the disk is almost full.
        try (OutputStream packageInSession = session.openWrite("package", 0, -1);
             InputStream is = new FileInputStream(new File(path))) {
            byte[] buffer = new byte[16384];
            int n;
            while ((n = is.read(buffer)) >= 0) {
                packageInSession.write(buffer, 0, n);
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

    public static byte[] readAllBytes(InputStream inputStream) throws IOException {
        final int bufLen = 4 * 0x400; // 4KB
        byte[] buf = new byte[bufLen];
        int readLen;
        IOException exception = null;

        try {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                while ((readLen = inputStream.read(buf, 0, bufLen)) != -1)
                    outputStream.write(buf, 0, readLen);

                return outputStream.toByteArray();
            }
        } catch (IOException e) {
            exception = e;
            throw e;
        } finally {
            if (exception == null) inputStream.close();
            else try {
                inputStream.close();
            } catch (IOException e) {
                exception.addSuppressed(e);
            }
        }
    }

    @Override
    public void onAttachmentDownloadedSuccess() {

    }

    @Override
    public void onAttachmentDownloadedError() {

    }

    @Override
    public void onAttachmentDownloadedFinished() {

    }

    @Override
    public void onAttachmentDownloadUpdate(float percent) {
        if (percent > 0) {
            Log.d(TAG, "onAttachmentDownloadUpdate: " + percent);
            binding.progressHorizontal.setProgress((int) percent);
        }
    }

    @Override
    public void onBackPressed() {
        clearCachedApkIfNeededAndFinish();
    }
}
