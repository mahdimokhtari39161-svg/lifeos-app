package com.sharenow.android;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.google.zxing.common.BitMatrix;

public class MainActivity extends Activity {
    private static final int PICK_FILES_REQUEST = 4101;
    private static final int WIFI_PERMISSION_REQUEST = 4102;
    private static final int PICK_FOLDER_REQUEST = 4103;
    private static final int INSTALL_PERMISSION_REQUEST = 4104;
    private static final int TCP_PORT = 47890;
    private static final int DISCOVERY_PORT = 47891;
    private static final int BUFFER_SIZE = 1024 * 1024;
    private static final long MAX_FILE_SIZE = 200L * 1024L * 1024L * 1024L;

    private static final int BG = Color.rgb(7, 27, 44);
    private static final int CARD = Color.rgb(17, 45, 66);
    private static final int TEAL = Color.rgb(19, 184, 166);
    private static final int WHITE = Color.WHITE;
    private static final int MUTED = Color.rgb(177, 198, 211);

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService transferExecutor = Executors.newSingleThreadExecutor();
    private final SecureRandom secureRandom = new SecureRandom();
    private final List<PickedFile> pickedFiles = new ArrayList<>();
    private final Map<String, String> discoveredPeers = new LinkedHashMap<>();
    private final List<ReceivedBundle> receivedBundles = new ArrayList<>();

    private LinearLayout peerList;
    private TextView selectedFilesView;
    private TextView receiverInfoView;
    private TextView statusView;
    private TextView progressText;
    private ProgressBar progressBar;
    private EditText ipInput;
    private EditText pinInput;
    private Button receiveButton;
    private Button hotspotButton;
    private Button sameWifiButton;
    private Button stopButton;
    private Button discoverButton;
    private Button installReceivedButton;
    private String hotspotSsid = "";
    private String hotspotPassword = "";
    private String pendingQrSsid = "";
    private String pendingQrPassword = "";
    private ReceivedBundle pendingReceivedInstall;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback wifiNetworkCallback;

    private WifiManager wifiManager;
    private WifiManager.LocalOnlyHotspotReservation hotspotReservation;
    private WifiManager.MulticastLock multicastLock;
    private ServerSocket serverSocket;
    private volatile boolean receiving = false;
    private volatile boolean discoveryRunning = false;
    private volatile boolean broadcasterRunning = false;
    private volatile String sessionPin = "";
    private Thread serverThread;
    private Thread discoveryThread;
    private Thread broadcasterThread;
    private PowerManager.WakeLock transferWakeLock;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        buildInterface();
        setStatus("آماده؛ برای شروع، ارسال یا دریافت را انتخاب کنید.");
    }

    private void buildInterface() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(18), dp(18), dp(28));
        page.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(page, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView brand = text("ShareNow", 31, WHITE, true);
        brand.setGravity(Gravity.CENTER);
        page.addView(brand, matchWrap());

        TextView subtitle = text("انتقال سریع، مستقیم و امن بین دو گوشی", 14, MUTED, false);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subtitleParams = matchWrap();
        subtitleParams.bottomMargin = dp(18);
        page.addView(subtitle, subtitleParams);

        LinearLayout receiveCard = card(page);
        addSectionTitle(receiveCard, "دریافت فایل", "گوشی شما مقصد انتقال می‌شود");
        hotspotButton = button("ساخت شبکهٔ آفلاین و شروع دریافت", true);
        receiveCard.addView(hotspotButton, matchWrap());
        hotspotButton.setOnClickListener(v -> requestWifiPermissionAndStartHotspot());

        sameWifiButton = button("استفاده از Wi-Fi فعلی", false);
        LinearLayout.LayoutParams sameParams = matchWrap();
        sameParams.topMargin = dp(8);
        receiveCard.addView(sameWifiButton, sameParams);
        sameWifiButton.setOnClickListener(v -> startReceiveOnCurrentWifi());

        LinearLayout receiverTools = new LinearLayout(this);
        receiverTools.setOrientation(LinearLayout.HORIZONTAL);
        receiverTools.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams toolsParams = matchWrap();
        toolsParams.topMargin = dp(8);
        receiveCard.addView(receiverTools, toolsParams);

        Button wifiSettings = button("تنظیمات Wi-Fi", false);
        receiverTools.addView(wifiSettings, new LinearLayout.LayoutParams(0, dp(46), 1));
        wifiSettings.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            } catch (Exception e) {
                showToast("تنظیمات Wi-Fi در این گوشی در دسترس نیست.");
            }
        });

        stopButton = button("توقف دریافت", false);
        LinearLayout.LayoutParams stopParams = new LinearLayout.LayoutParams(0, dp(46), 1);
        stopParams.leftMargin = dp(8);
        receiverTools.addView(stopButton, stopParams);
        stopButton.setOnClickListener(v -> stopReceiveSession());

        receiverInfoView = text("هنوز حالت دریافت فعال نشده است.", 13, MUTED, false);
        receiverInfoView.setPadding(0, dp(12), 0, 0);
        receiveCard.addView(receiverInfoView, matchWrap());

        Button showQrButton = button("نمایش QR برای اتصال گوشی فرستنده", false);
        LinearLayout.LayoutParams qrParams = matchWrap();
        qrParams.topMargin = dp(8);
        receiveCard.addView(showQrButton, qrParams);
        showQrButton.setOnClickListener(v -> showConnectionQr());

        installReceivedButton = button("نصب برنامه‌ها و بازی‌های دریافتی", false);
        LinearLayout.LayoutParams installReceivedParams = matchWrap();
        installReceivedParams.topMargin = dp(8);
        receiveCard.addView(installReceivedButton, installReceivedParams);
        installReceivedButton.setVisibility(View.GONE);
        installReceivedButton.setOnClickListener(v -> showReceivedBundles());

        LinearLayout sendCard = card(page);
        LinearLayout.LayoutParams sendCardParams = matchWrap();
        sendCardParams.topMargin = dp(14);
        sendCard.setLayoutParams(sendCardParams);
        addSectionTitle(sendCard, "ارسال فایل", "هر نوع فایل؛ انتخاب تکی یا چندتایی");

        Button chooseButton = button("انتخاب عکس، فیلم، سند و فایل‌ها", true);
        sendCard.addView(chooseButton, matchWrap());
        chooseButton.setOnClickListener(v -> openFilePicker());

        Button chooseFolderButton = button("انتخاب یک پوشهٔ کامل", false);
        LinearLayout.LayoutParams folderButtonParams = matchWrap();
        folderButtonParams.topMargin = dp(8);
        sendCard.addView(chooseFolderButton, folderButtonParams);
        chooseFolderButton.setOnClickListener(v -> openFolderPicker());

        Button chooseAppsButton = button("انتخاب برنامه‌ها و بازی‌های نصب‌شده", false);
        LinearLayout.LayoutParams appsParams = matchWrap();
        appsParams.topMargin = dp(8);
        sendCard.addView(chooseAppsButton, appsParams);
        chooseAppsButton.setOnClickListener(v -> showInstalledAppsDialog());

        selectedFilesView = text("هنوز فایلی انتخاب نشده است.", 13, MUTED, false);
        LinearLayout.LayoutParams pickedParams = matchWrap();
        pickedParams.topMargin = dp(8);
        pickedParams.bottomMargin = dp(12);
        sendCard.addView(selectedFilesView, pickedParams);

        ipInput = editText("IP گوشی دریافت‌کننده (مثلاً 192.168.43.1)");
        sendCard.addView(ipInput, matchWrap());

        pinInput = editText("کد امنیتی ۸ رقمی نمایش‌داده‌شده در گوشی مقصد");
        LinearLayout.LayoutParams pinParams = matchWrap();
        pinParams.topMargin = dp(8);
        sendCard.addView(pinInput, pinParams);
        pinInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        pinInput.setGravity(Gravity.CENTER);

        Button scanQrButton = button("اسکن QR گوشی دریافت‌کننده", true);
        LinearLayout.LayoutParams scanParams = matchWrap();
        scanParams.topMargin = dp(8);
        sendCard.addView(scanQrButton, scanParams);
        scanQrButton.setOnClickListener(v -> startQrScanner());

        discoverButton = button("پیدا کردن خودکار گوشی‌های نزدیک", false);
        LinearLayout.LayoutParams discoverParams = matchWrap();
        discoverParams.topMargin = dp(8);
        sendCard.addView(discoverButton, discoverParams);
        discoverButton.setOnClickListener(v -> startDiscovery());

        peerList = new LinearLayout(this);
        peerList.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams peersParams = matchWrap();
        peersParams.topMargin = dp(6);
        sendCard.addView(peerList, peersParams);

        Button sendButton = button("ارسال امن فایل‌ها", true);
        LinearLayout.LayoutParams sendParams = matchWrap();
        sendParams.topMargin = dp(12);
        sendCard.addView(sendButton, sendParams);
        sendButton.setOnClickListener(v -> sendSelectedFiles());

        LinearLayout statusCard = card(page);
        LinearLayout.LayoutParams statusParams = matchWrap();
        statusParams.topMargin = dp(14);
        statusCard.setLayoutParams(statusParams);
        addSectionTitle(statusCard, "وضعیت انتقال", "پیشرفت و نتیجهٔ عملیات");
        statusView = text("آماده", 14, WHITE, false);
        statusCard.addView(statusView, matchWrap());

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        LinearLayout.LayoutParams progressParams = matchWrap();
        progressParams.topMargin = dp(10);
        statusCard.addView(progressBar, progressParams);

        progressText = text("۰٪", 12, MUTED, false);
        progressText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams progressTextParams = matchWrap();
        progressTextParams.topMargin = dp(4);
        statusCard.addView(progressText, progressTextParams);

        TextView footnote = text(
                "اتصال QR: گوشی مقصد کد را نمایش می‌دهد و گوشی فرستنده آن را اسکن می‌کند. برنامه‌ها و بازی‌ها به صورت بستهٔ نصب منتقل می‌شوند؛ اطلاعات داخلی بازی‌ها منتقل نمی‌شود.",
                12, MUTED, false);
        footnote.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footParams = matchWrap();
        footParams.topMargin = dp(18);
        page.addView(footnote, footParams);

        setContentView(scroll);
    }

    private LinearLayout card(LinearLayout parent) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable background = new GradientDrawable();
        background.setColor(CARD);
        background.setCornerRadius(dp(18));
        box.setBackground(background);
        parent.addView(box, matchWrap());
        return box;
    }

    private void addSectionTitle(LinearLayout parent, String title, String description) {
        TextView heading = text(title, 19, WHITE, true);
        parent.addView(heading, matchWrap());
        TextView detail = text(description, 12, MUTED, false);
        LinearLayout.LayoutParams detailParams = matchWrap();
        detailParams.topMargin = dp(3);
        detailParams.bottomMargin = dp(12);
        parent.addView(detail, detailParams);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button button(String label, boolean primary) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setTextColor(primary ? BG : WHITE);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(primary ? TEAL : Color.rgb(37, 68, 88));
        shape.setCornerRadius(dp(13));
        button.setBackground(shape);
        button.setMinHeight(dp(48));
        button.setPadding(dp(12), dp(8), dp(12), dp(8));
        return button;
    }

    private EditText editText(String hint) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setTextSize(14);
        input.setTextColor(WHITE);
        input.setHintTextColor(MUTED);
        input.setHint(hint);
        input.setPadding(dp(12), dp(8), dp(12), dp(8));
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Color.rgb(8, 33, 51));
        shape.setCornerRadius(dp(11));
        shape.setStroke(dp(1), Color.rgb(48, 83, 105));
        input.setBackground(shape);
        return input;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }


    // QR pairing. The receiver encodes its local IP, port, one-time PIN and hotspot credentials.
    private void startQrScanner() {
        try {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
            integrator.setPrompt("کد ShareNow گوشی دریافت‌کننده را اسکن کنید");
            integrator.setBeepEnabled(false);
            integrator.setOrientationLocked(true);
            integrator.initiateScan();
        } catch (Exception e) {
            showToast("اسکنر QR در دسترس نیست: " + safeMessage(e));
        }
    }

    private void showConnectionQr() {
        if (!receiving || sessionPin.isEmpty()) {
            showToast("ابتدا در گوشی مقصد حالت دریافت را فعال کنید.");
            return;
        }
        String ip = getLocalIpAddress();
        try {
            String payload = "SHARENOW|2|" + ip + "|" + TCP_PORT + "|" + sessionPin + "|"
                    + android.util.Base64.encodeToString(hotspotSsid.getBytes("UTF-8"), android.util.Base64.NO_WRAP)
                    + "|"
                    + android.util.Base64.encodeToString(hotspotPassword.getBytes("UTF-8"), android.util.Base64.NO_WRAP);
            BitMatrix matrix = new MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, 720, 720);
            Bitmap bitmap = Bitmap.createBitmap(720, 720, Bitmap.Config.ARGB_8888);
            for (int y = 0; y < 720; y++) {
                for (int x = 0; x < 720; x++) {
                    bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setGravity(Gravity.CENTER);
            box.setPadding(dp(12), dp(12), dp(12), dp(12));
            ImageView image = new ImageView(this);
            image.setImageBitmap(bitmap);
            image.setAdjustViewBounds(true);
            box.addView(image, new LinearLayout.LayoutParams(dp(250), dp(250)));
            TextView info = text("IP: " + ip + "\nکد امنیتی: " + sessionPin
                    + "\nگوشی فرستنده این کد را در ShareNow اسکن کند.", 14, BG, true);
            info.setGravity(Gravity.CENTER);
            box.addView(info, matchWrap());
            new AlertDialog.Builder(this)
                    .setTitle("اتصال سریع ShareNow")
                    .setView(box)
                    .setPositiveButton("بستن", null)
                    .show();
        } catch (Exception e) {
            showToast("ساخت QR ناموفق بود: " + safeMessage(e));
        }
    }

    private void handleConnectionQr(String contents) {
        try {
            String[] fields = contents.split("\\|", -1);
            if (fields.length < 5 || !fields[0].equals("SHARENOW") || !fields[1].equals("2")) {
                showToast("این کد QR مربوط به اتصال ShareNow نیست.");
                return;
            }
            String ip = fields[2];
            int port = Integer.parseInt(fields[3]);
            String pin = fields[4];
            if (port != TCP_PORT || pin.length() != 8 || !ip.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")) {
                showToast("اطلاعات اتصال داخل QR معتبر نیست.");
                return;
            }
            ipInput.setText(ip);
            pinInput.setText(pin);

            String ssid = fields.length > 5
                    ? new String(android.util.Base64.decode(fields[5], android.util.Base64.NO_WRAP), "UTF-8") : "";
            String password = fields.length > 6
                    ? new String(android.util.Base64.decode(fields[6], android.util.Base64.NO_WRAP), "UTF-8") : "";

            if (!ssid.isEmpty() && Build.VERSION.SDK_INT >= 29) {
                if (Build.VERSION.SDK_INT >= 33
                        && checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                    pendingQrSsid = ssid;
                    pendingQrPassword = password;
                    requestPermissions(new String[]{Manifest.permission.NEARBY_WIFI_DEVICES}, WIFI_PERMISSION_REQUEST);
                } else {
                    connectToQrHotspot(ssid, password);
                }
            } else if (!ssid.isEmpty()) {
                setStatus("QR خوانده شد. در Wi-Fi به شبکهٔ " + ssid + " وصل شوید. رمز: " + password
                        + "\nپس از اتصال، به ShareNow برگردید و ارسال را بزنید.");
                try { startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)); } catch (Exception ignored) { }
            } else {
                setStatus("گوشی مقصد شناسایی شد؛ حالا فایل‌ها را انتخاب و ارسال کنید.");
                showToast("اطلاعات اتصال ثبت شد؛ هر دو گوشی باید روی یک Wi-Fi باشند.");
            }
        } catch (Exception e) {
            showToast("خواندن QR ناموفق بود: " + safeMessage(e));
        }
    }

    private void connectToQrHotspot(String ssid, String password) {
        try {
            if (connectivityManager == null) {
                showToast("مدیریت اتصال Wi-Fi در این گوشی در دسترس نیست.");
                return;
            }
            if (wifiNetworkCallback != null) {
                try { connectivityManager.unregisterNetworkCallback(wifiNetworkCallback); }
                catch (Exception ignored) { }
                wifiNetworkCallback = null;
            }
            WifiNetworkSpecifier.Builder specifierBuilder = new WifiNetworkSpecifier.Builder().setSsid(ssid);
            if (password != null && !password.isEmpty()) specifierBuilder.setWpa2Passphrase(password);
            WifiNetworkSpecifier specifier = specifierBuilder.build();
            NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(android.net.NetworkCapabilities.TRANSPORT_WIFI)
                    .removeCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .setNetworkSpecifier(specifier)
                    .build();

            setStatus("درخواست اتصال به شبکهٔ " + ssid + "؛ تأیید پنجرهٔ Android را انجام دهید.");
            wifiNetworkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    connectivityManager.bindProcessToNetwork(network);
                    mainHandler.post(() -> {
                        setStatus("گوشی‌ها متصل شدند؛ فایل‌ها یا برنامه‌ها را انتخاب و ارسال کنید.");
                        showToast("اتصال Wi-Fi برقرار شد. آمادهٔ ارسال هستید.");
                    });
                }
                @Override
                public void onUnavailable() {
                    mainHandler.post(() -> setStatus("اتصال خودکار Wi-Fi انجام نشد. در تنظیمات به " + ssid
                            + " وصل شوید. رمز: " + password));
                }
                @Override
                public void onLost(Network network) {
                    mainHandler.post(() -> setStatus("اتصال Wi-Fi قطع شد."));
                }
            };
            connectivityManager.requestNetwork(request, wifiNetworkCallback, 30_000);
        } catch (SecurityException e) {
            setStatus("Android اجازهٔ اتصال نداد؛ شبکه را از تنظیمات Wi-Fi انتخاب کنید.");
            try { startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)); } catch (Exception ignored) { }
        } catch (Exception e) {
            setStatus("اتصال Wi-Fi ناموفق بود: " + safeMessage(e));
        }
    }

    private void showInstalledAppsDialog() {
        List<AppEntry> apps = getInstalledApps();
        if (apps.isEmpty()) {
            showToast("برنامهٔ قابل انتقالی پیدا نشد.");
            return;
        }
        String[] labels = new String[apps.size()];
        boolean[] checked = new boolean[apps.size()];
        for (int i = 0; i < apps.size(); i++) {
            AppEntry app = apps.get(i);
            labels[i] = (app.isGame ? "🎮 " : "📱 ") + app.label + "  •  " + app.versionName;
        }
        new AlertDialog.Builder(this)
                .setTitle("انتخاب برنامه‌ها و بازی‌ها")
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setNegativeButton("انصراف", null)
                .setPositiveButton("ساخت بسته و افزودن به ارسال", (dialog, which) -> {
                    List<AppEntry> selected = new ArrayList<>();
                    for (int i = 0; i < apps.size(); i++) if (checked[i]) selected.add(apps.get(i));
                    if (selected.isEmpty()) {
                        showToast("هیچ برنامه‌ای انتخاب نشد.");
                        return;
                    }
                    setStatus("در حال آماده‌سازی بستهٔ برنامه‌ها و بازی‌ها...");
                    transferExecutor.execute(() -> createAppBundles(selected));
                })
                .show();
    }

    private List<AppEntry> getInstalledApps() {
        List<AppEntry> result = new ArrayList<>();
        PackageManager pm = getPackageManager();
        try {
            List<ApplicationInfo> installed = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            for (ApplicationInfo info : installed) {
                if (info.packageName.equals(getPackageName())) continue;
                if (pm.getLaunchIntentForPackage(info.packageName) == null) continue;
                File base = new File(info.sourceDir == null ? "" : info.sourceDir);
                if (!base.isFile() || !base.canRead()) continue;
                String label;
                try { label = pm.getApplicationLabel(info).toString(); }
                catch (Exception e) { label = info.packageName; }
                String version = "";
                try {
                    PackageInfo pi = pm.getPackageInfo(info.packageName, 0);
                    version = pi.versionName == null ? "" : pi.versionName;
                } catch (Exception ignored) { }
                boolean game = Build.VERSION.SDK_INT >= 26 && info.category == ApplicationInfo.CATEGORY_GAME;
                result.add(new AppEntry(info, label, version, game));
            }
        } catch (Exception e) {
            publishStatus("خواندن برنامه‌های نصب‌شده ناموفق بود: " + safeMessage(e));
        }
        result.sort((a, b) -> {
            if (a.isGame != b.isGame) return a.isGame ? -1 : 1;
            return a.label.compareToIgnoreCase(b.label);
        });
        return result;
    }

    private void createAppBundles(List<AppEntry> apps) {
        List<PickedFile> created = new ArrayList<>();
        try {
            File folder = new File(getCacheDir(), "app-bundles");
            if (!folder.exists() && !folder.mkdirs()) throw new IOException("Cannot create app bundle folder");
            for (AppEntry app : apps) {
                File bundle = new File(folder, safeFileName(app.label) + "-" + app.info.packageName + ".sharenowapp");
                Properties metadata = new Properties();
                metadata.setProperty("packageName", app.info.packageName);
                metadata.setProperty("label", app.label);
                metadata.setProperty("versionName", app.versionName == null ? "" : app.versionName);
                metadata.setProperty("isGame", Boolean.toString(app.isGame));

                try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(bundle), BUFFER_SIZE))) {
                    zip.putNextEntry(new ZipEntry("bundle.properties"));
                    metadata.store(zip, "ShareNow Android app bundle");
                    zip.closeEntry();
                    addApkToBundle(zip, new File(app.info.sourceDir), "base.apk");
                    String[] splits = app.info.splitSourceDirs;
                    if (splits != null) {
                        for (int i = 0; i < splits.length; i++) {
                            addApkToBundle(zip, new File(splits[i]), "split_" + i + ".apk");
                        }
                    }
                }
                created.add(new PickedFile(Uri.fromFile(bundle), app.label + ".sharenowapp", bundle.length(), ""));
            }
            mainHandler.post(() -> {
                pickedFiles.addAll(created);
                updatePickedSummary();
                setStatus("آماده‌سازی برنامه‌ها کامل شد؛ اکنون گوشی مقصد را اسکن یا انتخاب کنید.");
                showToast(created.size() + " بستهٔ برنامه/بازی آمادهٔ ارسال شد.");
            });
        } catch (Exception e) {
            publishStatus("ساخت بستهٔ برنامه ناموفق بود: " + safeMessage(e));
        }
    }

    private void addApkToBundle(ZipOutputStream zip, File apk, String entryName) throws IOException {
        if (!apk.isFile() || !apk.canRead()) throw new IOException("فایل نصب قابل خواندن نیست: " + apk.getName());
        zip.putNextEntry(new ZipEntry(entryName));
        try (InputStream input = new BufferedInputStream(new java.io.FileInputStream(apk), BUFFER_SIZE)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int count;
            while ((count = input.read(buffer)) != -1) zip.write(buffer, 0, count);
        }
        zip.closeEntry();
    }

    private void registerReceivedBundle(FileTarget target, String fileName) {
        if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".sharenowapp")) return;
        ReceivedBundle bundle = new ReceivedBundle(fileName, target.mediaUri, target.file);
        mainHandler.post(() -> {
            receivedBundles.add(bundle);
            if (installReceivedButton != null) {
                installReceivedButton.setVisibility(View.VISIBLE);
                installReceivedButton.setText("نصب برنامه‌ها و بازی‌های دریافتی (" + receivedBundles.size() + ")");
            }
        });
    }

    private void showReceivedBundles() {
        if (receivedBundles.isEmpty()) {
            showToast("بستهٔ برنامه‌ای دریافت نشده است.");
            return;
        }
        String[] names = new String[receivedBundles.size()];
        for (int i = 0; i < names.length; i++) names[i] = receivedBundles.get(i).name;
        new AlertDialog.Builder(this)
                .setTitle("انتخاب بسته برای نصب")
                .setItems(names, (dialog, which) -> installReceivedBundle(receivedBundles.get(which)))
                .setNegativeButton("بستن", null)
                .show();
    }

    private void installReceivedBundle(ReceivedBundle bundle) {
        if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
            pendingReceivedInstall = bundle;
            Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + getPackageName()));
            try {
                startActivityForResult(settings, INSTALL_PERMISSION_REQUEST);
            } catch (Exception e) {
                showToast("برای نصب، مجوز نصب از این برنامه را در تنظیمات Android فعال کنید.");
            }
            return;
        }
        transferExecutor.execute(() -> {
            try {
                File sourceZip = new File(getCacheDir(), "incoming-" + System.currentTimeMillis() + ".sharenowapp");
                try (InputStream input = bundle.open(this);
                     OutputStream output = new BufferedOutputStream(new FileOutputStream(sourceZip), BUFFER_SIZE)) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int count;
                    while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                }

                File staging = new File(getCacheDir(), "install-" + System.currentTimeMillis());
                if (!staging.mkdirs()) throw new IOException("Cannot create install staging folder");
                Properties metadata = new Properties();
                List<File> apkFiles = new ArrayList<>();
                try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(new java.io.FileInputStream(sourceZip), BUFFER_SIZE))) {
                    ZipEntry entry;
                    while ((entry = zip.getNextEntry()) != null) {
                        String name = entry.getName();
                        if (entry.isDirectory()) continue;
                        if (name.equals("bundle.properties")) {
                            metadata.load(zip);
                        } else if (name.equals("base.apk") || (name.startsWith("split_") && name.endsWith(".apk"))) {
                            File outputFile = new File(staging, name);
                            try (OutputStream output = new BufferedOutputStream(new FileOutputStream(outputFile), BUFFER_SIZE)) {
                                byte[] buffer = new byte[BUFFER_SIZE];
                                int count;
                                while ((count = zip.read(buffer)) != -1) output.write(buffer, 0, count);
                            }
                            apkFiles.add(outputFile);
                        }
                        zip.closeEntry();
                    }
                }
                final String packageName = metadata.getProperty("packageName", "");
                File baseApk = new File(staging, "base.apk");
                PackageInfo archiveInfo = baseApk.isFile()
                        ? getPackageManager().getPackageArchiveInfo(baseApk.getAbsolutePath(), 0) : null;
                if (packageName.isEmpty() || archiveInfo == null || !baseApk.isFile() || apkFiles.isEmpty()) {
                    throw new IOException("بستهٔ برنامه ناقص یا نامعتبر است.");
                }
                if (!packageName.equals(archiveInfo.packageName)) {
                    throw new IOException("شناسهٔ برنامه با بستهٔ نصب مطابقت ندارد.");
                }
                apkFiles.sort((a, b) -> a.getName().equals("base.apk") ? -1 : (b.getName().equals("base.apk") ? 1 : a.getName().compareTo(b.getName())));

                PackageInstaller installer = getPackageManager().getPackageInstaller();
                PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                params.setAppPackageName(packageName);
                int sessionId = installer.createSession(params);
                try (PackageInstaller.Session session = installer.openSession(sessionId)) {
                    for (File apk : apkFiles) {
                        try (InputStream input = new BufferedInputStream(new java.io.FileInputStream(apk), BUFFER_SIZE);
                             OutputStream output = session.openWrite(apk.getName(), 0, apk.length())) {
                            byte[] buffer = new byte[BUFFER_SIZE];
                            int count;
                            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                            session.fsync(output);
                        }
                    }
                    Intent callback = new Intent(this, InstallStatusReceiver.class);
                    callback.setAction("com.sharenow.INSTALL_STATUS." + sessionId);
                    PendingIntent pending = PendingIntent.getBroadcast(this, sessionId, callback,
                            PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0));
                    session.commit(pending.getIntentSender());
                } catch (Exception e) {
                    installer.abandonSession(sessionId);
                    throw e;
                }
                sourceZip.delete();
                publishStatus("درخواست نصب " + metadata.getProperty("label", packageName)
                        + " به Android فرستاده شد؛ تأیید نصب را در پنجرهٔ سیستم انجام دهید.");
            } catch (Exception e) {
                publishStatus("نصب بستهٔ برنامه ناموفق بود: " + safeMessage(e));
            }
        });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, PICK_FILES_REQUEST);
        } catch (Exception e) {
            showToast("انتخاب‌گر فایل در این گوشی در دسترس نیست.");
        }
    }

    private void openFolderPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, PICK_FOLDER_REQUEST);
        } catch (Exception e) {
            showToast("انتخاب پوشه در این گوشی در دسترس نیست.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IntentIntegrator.REQUEST_CODE) {
            IntentResult scan = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (scan != null && scan.getContents() != null) handleConnectionQr(scan.getContents());
            return;
        }
        if (requestCode == INSTALL_PERMISSION_REQUEST) {
            if (Build.VERSION.SDK_INT >= 26 && getPackageManager().canRequestPackageInstalls()
                    && pendingReceivedInstall != null) {
                ReceivedBundle bundle = pendingReceivedInstall;
                pendingReceivedInstall = null;
                installReceivedBundle(bundle);
            } else {
                showToast("مجوز نصب برنامه از منابع ناشناس فعال نشد.");
            }
            return;
        }
        if (resultCode != RESULT_OK || data == null) return;

        if (requestCode == PICK_FILES_REQUEST) {
            pickedFiles.clear();
            try {
                if (data.getClipData() != null) {
                    for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                        Uri uri = data.getClipData().getItemAt(i).getUri();
                        addPickedUri(uri, data.getFlags());
                    }
                } else if (data.getData() != null) {
                    addPickedUri(data.getData(), data.getFlags());
                }
            } catch (Exception e) {
                showToast("خواندن فهرست فایل‌ها ناموفق بود: " + safeMessage(e));
            }
            updatePickedSummary();
            return;
        }

        if (requestCode == PICK_FOLDER_REQUEST && data.getData() != null) {
            pickedFiles.clear();
            Uri treeUri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                String rootId = DocumentsContract.getTreeDocumentId(treeUri);
                collectFolder(treeUri, rootId, "");
            } catch (Exception e) {
                showToast("خواندن پوشه ناموفق بود: " + safeMessage(e));
            }
            updatePickedSummary();
        }
    }

    private void collectFolder(Uri treeUri, String documentId, String relativePath) throws IOException {
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId);
        String[] projection = {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
        };
        Cursor cursor = getContentResolver().query(childrenUri, projection, null, null, null);
        if (cursor == null) return;
        try {
            while (cursor.moveToNext()) {
                String childId = cursor.getString(0);
                String name = cursor.getString(1);
                String mime = cursor.getString(2);
                long size = cursor.isNull(3) ? -1 : cursor.getLong(3);
                String safePart = safeFileName(name);
                if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
                    collectFolder(treeUri, childId, relativePath + safePart + "/");
                } else {
                    Uri childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId);
                    if (size < 0) size = measureUri(childUri);
                    pickedFiles.add(new PickedFile(childUri, name == null ? "file" : name, size, relativePath));
                }
            }
        } finally {
            cursor.close();
        }
    }

    private void updatePickedSummary() {
        long total = 0;
        for (PickedFile file : pickedFiles) total += file.size;
        if (pickedFiles.isEmpty()) {
            selectedFilesView.setText("هیچ فایلی انتخاب نشد.");
        } else {
            selectedFilesView.setText(pickedFiles.size() + " فایل انتخاب شد • مجموع " + formatBytes(total)
                    + "\nساختار پوشه‌ها هنگام دریافت حفظ می‌شود.");
        }
    }

    private void addPickedUri(Uri uri, int flags) throws IOException {
        try {
            getContentResolver().takePersistableUriPermission(uri,
                    flags & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
        } catch (Exception ignored) {
            // Some document providers do not offer persistable permissions.
        }

        String name = "file";
        long size = -1;
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (nameIndex >= 0) name = cursor.getString(nameIndex);
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex);
            }
        } finally {
            if (cursor != null) cursor.close();
        }

        if (size < 0) size = measureUri(uri);
        pickedFiles.add(new PickedFile(uri, name == null ? "file" : name, size, ""));
    }

    private long measureUri(Uri uri) throws IOException {
        long total = 0;
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) throw new IOException("Cannot open selected file");
            int count;
            while ((count = input.read(buffer)) != -1) total += count;
        }
        return total;
    }

    private void requestWifiPermissionAndStartHotspot() {
        String permission = Build.VERSION.SDK_INT >= 33
                ? Manifest.permission.NEARBY_WIFI_DEVICES
                : Manifest.permission.ACCESS_FINE_LOCATION;

        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{permission}, WIFI_PERMISSION_REQUEST);
        } else {
            startLocalOnlyHotspot();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WIFI_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (!pendingQrSsid.isEmpty() && Build.VERSION.SDK_INT >= 29) {
                    String ssid = pendingQrSsid;
                    String password = pendingQrPassword;
                    pendingQrSsid = "";
                    pendingQrPassword = "";
                    connectToQrHotspot(ssid, password);
                } else {
                    startLocalOnlyHotspot();
                }
            } else {
                pendingQrSsid = "";
                pendingQrPassword = "";
                setStatus("مجوز Wi-Fi نزدیک داده نشد. می‌توانید از Wi-Fi فعلی استفاده کنید.");
            }
        }
    }

    private void startLocalOnlyHotspot() {
        if (Build.VERSION.SDK_INT < 26) {
            setStatus("این نسخهٔ اندروید از ساخت شبکهٔ محلی خودکار پشتیبانی نمی‌کند؛ از Wi-Fi فعلی استفاده کنید.");
            return;
        }
        if (hotspotReservation != null) {
            setStatus("شبکهٔ آفلاین از قبل فعال است.");
            return;
        }

        setStatus("در حال ساخت شبکهٔ Wi-Fi بدون اینترنت...");
        try {
            wifiManager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {
                @Override
                public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                    hotspotReservation = reservation;
                    String ssid = "ShareNow";
                    String password = "در تنظیمات Wi-Fi گوشی بررسی کنید";
                    try {
                        if (Build.VERSION.SDK_INT >= 30) {
                            android.net.wifi.SoftApConfiguration config = reservation.getSoftApConfiguration();
                            if (config != null) {
                                if (config.getSsid() != null) ssid = config.getSsid().toString();
                                if (config.getPassphrase() != null) password = config.getPassphrase();
                            }
                        } else {
                            WifiConfiguration config = reservation.getWifiConfiguration();
                            if (config != null) {
                                if (config.SSID != null) ssid = config.SSID.replace("\"", "");
                                if (config.preSharedKey != null) password = config.preSharedKey.replace("\"", "");
                            }
                        }
                    } catch (Exception ignored) {
                        // Keep generic fallback values if the platform hides hotspot details.
                    }

                    final String finalSsid = ssid;
                    final String finalPassword = password;
                    hotspotSsid = finalSsid;
                    hotspotPassword = finalPassword;
                    startReceiveTransport();
                    mainHandler.postDelayed(() -> {
                        String ip = getLocalIpAddress();
                        receiverInfoView.setText("شبکه: " + finalSsid
                                + "\nرمز Wi-Fi: " + finalPassword
                                + "\nکد امنیتی انتقال: " + sessionPin
                                + "\nIP این گوشی: " + ip
                                + "\nبرای اتصال سریع، در گوشی فرستنده QR را اسکن کنید.");
                        setStatus("شبکهٔ آفلاین آماده است؛ منتظر گوشی فرستنده هستیم.");
                    }, 700);
                }

                @Override
                public void onStopped() {
                    hotspotReservation = null;
                    mainHandler.post(() -> setStatus("شبکهٔ آفلاین متوقف شد."));
                }

                @Override
                public void onFailed(int reason) {
                    mainHandler.post(() -> {
                        setStatus("ساخت Hotspot ناموفق بود. از گزینهٔ Wi-Fi فعلی استفاده کنید.");
                        showToast("Hotspot در این لحظه در دسترس نیست؛ شاید نقطهٔ اتصال گوشی روشن باشد.");
                    });
                }
            }, mainHandler);
        } catch (SecurityException e) {
            setStatus("مجوز ساخت شبکه داده نشده است. از تنظیمات Wi-Fi فعلی استفاده کنید.");
        } catch (Exception e) {
            setStatus("ساخت شبکه ناموفق بود: " + safeMessage(e));
        }
    }

    private void startReceiveOnCurrentWifi() {
        startReceiveTransport();
        hotspotSsid = "";
        hotspotPassword = "";
        receiverInfoView.setText("کد امنیتی انتقال: " + sessionPin
                + "\nIP این گوشی: " + getLocalIpAddress()
                + "\nهر دو گوشی باید به یک شبکهٔ Wi-Fi متصل باشند؛ سپس QR را اسکن کنید.");
        setStatus("حالت دریافت روی Wi-Fi فعلی فعال شد.");
    }

    private boolean isWifiPermissionGranted() {
        if (Build.VERSION.SDK_INT >= 33) {
            return checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED;
        }
        return Build.VERSION.SDK_INT < 23
                || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void startReceiveTransport() {
        if (receiving) return;
        sessionPin = String.format(Locale.US, "%08d", secureRandom.nextInt(100_000_000));
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(TCP_PORT));
            receiving = true;
            startBroadcaster();
            serverThread = new Thread(this::acceptConnections, "ShareNow-server");
            serverThread.start();
            setStatus("سرور انتقال آماده است؛ کد امنیتی را فقط به فرستنده بدهید.");
        } catch (IOException e) {
            receiving = false;
            setStatus("باز کردن درگاه انتقال ناموفق بود: " + safeMessage(e));
        }
    }

    private void acceptConnections() {
        while (receiving) {
            try {
                ServerSocket currentServer = serverSocket;
                if (currentServer == null) return;
                Socket client = currentServer.accept();
                client.setSoTimeout(120_000);
                handleClient(client);
            } catch (SocketException e) {
                if (receiving) publishStatus("ارتباط شبکه قطع شد: " + safeMessage(e));
                return;
            } catch (Exception e) {
                publishStatus("انتقال دریافت نشد: " + safeMessage(e));
            }
        }
    }

    private void handleClient(Socket client) throws Exception {
        acquireTransferWakeLock();
        FileTarget currentTarget = null;
        try (Socket socket = client;
             DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream(), BUFFER_SIZE));
             DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream(), BUFFER_SIZE))) {

            byte[] salt = new byte[16];
            byte[] authNonce = new byte[12];
            input.readFully(salt);
            input.readFully(authNonce);
            int authLength = input.readInt();
            if (authLength < 16 || authLength > 256) throw new IOException("Invalid authentication proof");
            byte[] authProof = new byte[authLength];
            input.readFully(authProof);

            SecretKey key = deriveKey(sessionPin, salt);
            boolean authenticated = false;
            try {
                Cipher authCipher = Cipher.getInstance("AES/GCM/NoPadding");
                authCipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, authNonce));
                byte[] proofText = authCipher.doFinal(authProof);
                authenticated = MessageDigest.isEqual(
                        proofText, "SHARENOW_AUTH_V2".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } catch (Exception ignored) {
                authenticated = false;
            }
            output.writeBoolean(authenticated);
            output.flush();
            if (!authenticated) {
                publishStatus("یک اتصال با کد امنیتی نادرست رد شد.");
                return;
            }

            int fileCount = input.readInt();
            if (fileCount < 1 || fileCount > 500) throw new IOException("Invalid file count");
            long totalBytes = 0;
            for (int i = 0; i < fileCount; i++) {
                String relativeName = input.readUTF();
                String originalName = relativeName.substring(relativeName.lastIndexOf('/') + 1);
                long plainSize = input.readLong();
                if (plainSize < 0 || plainSize > MAX_FILE_SIZE) throw new IOException("File size is not supported");
                byte[] nonce = new byte[12];
                input.readFully(nonce);

                currentTarget = createFileTarget(relativeName);
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, nonce));
                MessageDigest digest = MessageDigest.getInstance("SHA-256");

                long encryptedRemaining = plainSize + 16L;
                long written = 0;
                byte[] buffer = new byte[BUFFER_SIZE];
                while (encryptedRemaining > 0) {
                    int request = (int) Math.min((long) buffer.length, encryptedRemaining);
                    int read = input.read(buffer, 0, request);
                    if (read < 0) throw new IOException("Connection ended before file completed");
                    encryptedRemaining -= read;

                    byte[] plain = cipher.update(buffer, 0, read);
                    if (plain != null && plain.length > 0) {
                        currentTarget.output.write(plain);
                        digest.update(plain);
                        written += plain.length;
                    }
                    final long fileWritten = written;
                    final int fileIndex = i;
                    publishProgress("دریافت: " + originalName,
                            fileCount, fileIndex, totalBytes + fileWritten, plainSize);
                }

                byte[] finalPlain = cipher.doFinal();
                if (finalPlain != null && finalPlain.length > 0) {
                    currentTarget.output.write(finalPlain);
                    digest.update(finalPlain);
                    written += finalPlain.length;
                }
                byte[] expectedDigest = new byte[32];
                input.readFully(expectedDigest);
                boolean valid = written == plainSize && MessageDigest.isEqual(digest.digest(), expectedDigest);

                if (valid) {
                    FileTarget finishedTarget = currentTarget;
                    finishedTarget.finish();
                    registerReceivedBundle(finishedTarget, originalName);
                    currentTarget = null;
                    totalBytes += plainSize;
                    output.writeBoolean(true);
                    output.flush();
                    publishStatus("دریافت شد: " + originalName + " • " + formatBytes(plainSize));
                } else {
                    currentTarget.abort();
                    currentTarget = null;
                    output.writeBoolean(false);
                    output.flush();
                    throw new IOException("Integrity check failed for " + originalName);
                }
            }
            publishProgress("همهٔ فایل‌ها با موفقیت دریافت شدند.", 1, 1, 1, 1);
            publishStatus("انتقال کامل شد؛ فایل‌ها در Downloads/ShareNow ذخیره شدند.");
        } finally {
            if (currentTarget != null) currentTarget.abort();
            releaseTransferWakeLock();
        }
    }

    private FileTarget createFileTarget(String suppliedPath) throws IOException {
        String normalized = suppliedPath == null ? "received_file" : suppliedPath.replace('\\', '/');
        String[] parts = normalized.split("/");
        List<String> safeParts = new ArrayList<>();
        for (String part : parts) {
            if (part == null || part.trim().isEmpty() || part.equals(".") || part.equals("..")) continue;
            safeParts.add(safeFileName(part));
        }
        if (safeParts.isEmpty()) safeParts.add("received_file");
        String name = safeParts.get(safeParts.size() - 1);
        String relativeFolders = "";
        for (int i = 0; i < safeParts.size() - 1; i++) {
            relativeFolders += safeParts.get(i) + "/";
        }

        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType(name));
            values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/ShareNow/" + relativeFolders);
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IOException("Cannot create destination file");
            OutputStream output = getContentResolver().openOutputStream(uri, "w");
            if (output == null) {
                getContentResolver().delete(uri, null, null);
                throw new IOException("Cannot open destination file");
            }
            return new FileTarget(output, uri, null);
        }

        File base = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (base == null) throw new IOException("Download folder is not available");
        File folder = new File(base, "ShareNow");
        if (!relativeFolders.isEmpty()) folder = new File(folder, relativeFolders);
        if (!folder.exists() && !folder.mkdirs()) throw new IOException("Cannot create download folder");
        File file = uniqueFile(folder, name);
        return new FileTarget(new FileOutputStream(file), null, file);
    }

    private File uniqueFile(File folder, String name) {
        File candidate = new File(folder, name);
        if (!candidate.exists()) return candidate;
        int dot = name.lastIndexOf('.');
        String stem = dot > 0 ? name.substring(0, dot) : name;
        String extension = dot > 0 ? name.substring(dot) : "";
        int index = 1;
        while (candidate.exists()) {
            candidate = new File(folder, stem + " (" + index++ + ")" + extension);
        }
        return candidate;
    }

    private String mimeType(String name) {
        int dot = name.lastIndexOf('.');
        if (dot >= 0 && dot < name.length() - 1) {
            String type = android.webkit.MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(name.substring(dot + 1).toLowerCase(Locale.ROOT));
            if (type != null) return type;
        }
        return "application/octet-stream";
    }

    private String safeFileName(String name) {
        if (name == null || name.trim().isEmpty()) return "received_file";
        String safe = name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (safe.length() > 180) safe = safe.substring(0, 180);
        if (safe.equals(".") || safe.equals("..")) return "received_file";
        return safe;
    }

    private void sendSelectedFiles() {
        if (pickedFiles.isEmpty()) {
            showToast("ابتدا فایل یا فایل‌هایی را انتخاب کنید.");
            return;
        }
        String ip = ipInput.getText().toString().trim();
        String pin = pinInput.getText().toString().trim();
        if (ip.isEmpty() || pin.length() != 8) {
            showToast("IP مقصد و کد امنیتی ۸ رقمی را وارد کنید.");
            return;
        }
        final List<PickedFile> files = new ArrayList<>(pickedFiles);
        setStatus("در حال اتصال امن به گوشی مقصد...");
        progressBar.setProgress(0);
        transferExecutor.execute(() -> transferFiles(ip, pin, files));
    }

    private void transferFiles(String ip, String pin, List<PickedFile> files) {
        acquireTransferWakeLock();
        long totalBytes = 0;
        for (PickedFile file : files) totalBytes += file.size;
        long completedBytes = 0;

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, TCP_PORT), 15_000);
            socket.setSoTimeout(120_000);
            socket.setSendBufferSize(2 * 1024 * 1024);
            socket.setReceiveBufferSize(2 * 1024 * 1024);
            DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream(), BUFFER_SIZE));
            DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream(), BUFFER_SIZE));

            byte[] salt = new byte[16];
            byte[] authNonce = new byte[12];
            secureRandom.nextBytes(salt);
            secureRandom.nextBytes(authNonce);
            SecretKey key = deriveKey(pin, salt);
            Cipher authCipher = Cipher.getInstance("AES/GCM/NoPadding");
            authCipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, authNonce));
            byte[] authProof = authCipher.doFinal(
                    "SHARENOW_AUTH_V2".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            output.write(salt);
            output.write(authNonce);
            output.writeInt(authProof.length);
            output.write(authProof);
            output.flush();
            if (!input.readBoolean()) throw new IOException("کد امنیتی اشتباه است یا نشست مقصد منقضی شده است.");

            output.writeInt(files.size());
            output.flush();

            for (int i = 0; i < files.size(); i++) {
                PickedFile file = files.get(i);
                if (file.size < 0 || file.size > MAX_FILE_SIZE) {
                    throw new IOException("حجم این فایل پشتیبانی نمی‌شود: " + file.name);
                }

                byte[] nonce = new byte[12];
                secureRandom.nextBytes(nonce);
                String transferPath = file.relativePath + safeFileName(file.name);
                output.writeUTF(transferPath);
                output.writeLong(file.size);
                output.write(nonce);
                output.flush();

                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                long sentForFile = 0;

                try (InputStream source = getContentResolver().openInputStream(file.uri)) {
                    if (source == null) throw new IOException("فایل قابل خواندن نیست: " + file.name);
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int read;
                    while ((read = source.read(buffer)) != -1) {
                        digest.update(buffer, 0, read);
                        byte[] encrypted = cipher.update(buffer, 0, read);
                        if (encrypted != null && encrypted.length > 0) output.write(encrypted);
                        sentForFile += read;
                        publishProgress("ارسال: " + file.name, files.size(), i,
                                completedBytes + sentForFile, totalBytes);
                    }
                }

                if (sentForFile != file.size) {
                    throw new IOException("اندازهٔ فایل هنگام انتقال تغییر کرد: " + file.name);
                }
                byte[] finalEncrypted = cipher.doFinal();
                if (finalEncrypted != null && finalEncrypted.length > 0) output.write(finalEncrypted);
                output.write(digest.digest());
                output.flush();

                if (!input.readBoolean()) throw new IOException("تأیید سلامت فایل از گوشی مقصد دریافت نشد.");
                completedBytes += file.size;
            }

            publishProgress("انتقال همهٔ فایل‌ها کامل شد.", files.size(), files.size(), totalBytes, totalBytes);
            publishStatus("ارسال موفق بود؛ " + files.size() + " فایل به گوشی مقصد رسید.");
            mainHandler.post(() -> showToast("انتقال با موفقیت انجام شد."));
        } catch (Exception e) {
            publishStatus("ارسال ناموفق بود: " + safeMessage(e));
        } finally {
            releaseTransferWakeLock();
        }
    }

    private SecretKey deriveKey(String pin, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, 150_000, 256);
        try {
            byte[] raw = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
            return new SecretKeySpec(raw, "AES");
        } finally {
            spec.clearPassword();
        }
    }

    private void startDiscovery() {
        if (discoveryRunning) {
            showToast("جست‌وجو از قبل فعال است.");
            return;
        }
        discoveredPeers.clear();
        peerList.removeAllViews();
        discoveryRunning = true;
        acquireMulticastLock();

        discoveryThread = new Thread(() -> {
            DatagramSocket socket = null;
            try {
                socket = new DatagramSocket(null);
                socket.setReuseAddress(true);
                socket.bind(new InetSocketAddress(DISCOVERY_PORT));
                socket.setSoTimeout(1200);
                byte[] buffer = new byte[512];
                while (discoveryRunning) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    try {
                        socket.receive(packet);
                        String message = new String(packet.getData(), packet.getOffset(),
                                packet.getLength(), java.nio.charset.StandardCharsets.UTF_8);
                        if (message.startsWith("SHARENOW|1|")) {
                            String ip = packet.getAddress().getHostAddress();
                            if (!isOwnAddress(ip)) {
                                String deviceName = message.substring("SHARENOW|1|".length()).trim();
                                addDiscoveredPeer(ip, deviceName.isEmpty() ? "گوشی ShareNow" : deviceName);
                            }
                        }
                    } catch (java.net.SocketTimeoutException ignored) {
                        // Keep listening until the user stops discovery.
                    }
                }
            } catch (Exception e) {
                publishStatus("جست‌وجوی دستگاه‌ها ناموفق بود: " + safeMessage(e));
            } finally {
                if (socket != null) socket.close();
                releaseMulticastLock();
            }
        }, "ShareNow-discovery");
        discoveryThread.start();
        setStatus("در حال جست‌وجوی گوشی‌های ShareNow روی شبکهٔ محلی...");
    }

    private void addDiscoveredPeer(String ip, String deviceName) {
        mainHandler.post(() -> {
            if (discoveredPeers.containsKey(ip)) return;
            discoveredPeers.put(ip, deviceName);
            TextView peer = text("📱  " + deviceName + "    " + ip + "    • انتخاب", 13, WHITE, false);
            peer.setPadding(dp(10), dp(10), dp(10), dp(10));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.rgb(8, 33, 51));
            bg.setCornerRadius(dp(9));
            peer.setBackground(bg);
            LinearLayout.LayoutParams params = matchWrap();
            params.topMargin = dp(5);
            peerList.addView(peer, params);
            peer.setOnClickListener(v -> {
                ipInput.setText(ip);
                setStatus("گوشی مقصد انتخاب شد: " + ip + "؛ کد امنیتی را وارد کنید.");
            });
        });
    }

    private boolean isOwnAddress(String address) {
        for (String ip : getLocalIpAddresses()) {
            if (ip.equals(address)) return true;
        }
        return false;
    }

    private void startBroadcaster() {
        if (broadcasterRunning) return;
        broadcasterRunning = true;
        broadcasterThread = new Thread(() -> {
            while (broadcasterRunning) {
                String beacon = "SHARENOW|1|" + Build.MODEL.replace("|", " ");
                byte[] bytes = beacon.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                try (DatagramSocket socket = new DatagramSocket()) {
                    socket.setBroadcast(true);
                    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                    while (interfaces != null && interfaces.hasMoreElements()) {
                        NetworkInterface network = interfaces.nextElement();
                        try {
                            if (!network.isUp() || network.isLoopback()) continue;
                            for (java.net.InterfaceAddress address : network.getInterfaceAddresses()) {
                                InetAddress broadcast = address.getBroadcast();
                                if (broadcast != null) {
                                    DatagramPacket packet = new DatagramPacket(
                                            bytes, bytes.length, broadcast, DISCOVERY_PORT);
                                    socket.send(packet);
                                }
                            }
                        } catch (Exception ignored) {
                            // Skip interfaces that do not support broadcast.
                        }
                    }
                } catch (Exception e) {
                    publishStatus("اعلان حضور روی شبکه ناموفق بود: " + safeMessage(e));
                }
                try {
                    Thread.sleep(900);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "ShareNow-beacon");
        broadcasterThread.start();
    }

    private void stopDiscovery() {
        discoveryRunning = false;
        if (discoveryThread != null) discoveryThread.interrupt();
        releaseMulticastLock();
    }

    private void stopReceiveSession() {
        receiving = false;
        broadcasterRunning = false;
        if (broadcasterThread != null) broadcasterThread.interrupt();
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
            serverSocket = null;
        }
        if (hotspotReservation != null) {
            try {
                hotspotReservation.close();
            } catch (Exception ignored) {
            }
            hotspotReservation = null;
        }
        sessionPin = "";
        hotspotSsid = "";
        hotspotPassword = "";
        receiverInfoView.setText("حالت دریافت متوقف شده است.");
        setStatus("دریافت متوقف شد.");
    }

    private List<String> getLocalIpAddresses() {
        List<String> addresses = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface network = interfaces.nextElement();
                try {
                    if (!network.isUp() || network.isLoopback()) continue;
                    for (InetAddress address : Collections.list(network.getInetAddresses())) {
                        if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                            String ip = address.getHostAddress();
                            if (!addresses.contains(ip)) addresses.add(ip);
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return addresses;
    }

    private String getLocalIpAddress() {
        List<String> addresses = getLocalIpAddresses();
        for (String ip : addresses) {
            if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) return ip;
        }
        return addresses.isEmpty() ? "در انتظار اتصال Wi-Fi" : addresses.get(0);
    }

    private void acquireMulticastLock() {
        try {
            if (wifiManager == null) return;
            if (multicastLock == null) multicastLock = wifiManager.createMulticastLock("ShareNowDiscovery");
            if (!multicastLock.isHeld()) multicastLock.acquire();
        } catch (Exception ignored) {
        }
    }

    private void releaseMulticastLock() {
        try {
            if (multicastLock != null && multicastLock.isHeld()) multicastLock.release();
        } catch (Exception ignored) {
        }
    }

    private synchronized void acquireTransferWakeLock() {
        try {
            if (transferWakeLock == null) {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                transferWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ShareNow:Transfer");
                transferWakeLock.setReferenceCounted(false);
            }
            if (!transferWakeLock.isHeld()) transferWakeLock.acquire(3 * 60 * 60 * 1000L);
        } catch (Exception ignored) {
        }
    }

    private synchronized void releaseTransferWakeLock() {
        try {
            if (transferWakeLock != null && transferWakeLock.isHeld()) transferWakeLock.release();
        } catch (Exception ignored) {
        }
    }

    private void publishStatus(String message) {
        mainHandler.post(() -> setStatus(message));
    }

    private void publishProgress(String message, int count, int index, long completed, long total) {
        int percent;
        if (total <= 0) {
            percent = 0;
        } else {
            percent = (int) Math.max(0, Math.min(100, (completed * 100.0d) / total));
        }
        final int value = percent;
        mainHandler.post(() -> {
            setStatus(message);
            progressBar.setProgress(value);
            progressText.setText(value + "٪");
        });
    }

    private void setStatus(String message) {
        if (statusView != null) statusView.setText(message);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double value = bytes;
        String[] units = {"KB", "MB", "GB", "TB"};
        int index = -1;
        do {
            value /= 1024.0;
            index++;
        } while (value >= 1024 && index < units.length - 1);
        return String.format(Locale.US, "%.1f %s", value, units[index]);
    }

    private String safeMessage(Throwable error) {
        String message = error.getMessage();
        return message == null || message.trim().isEmpty()
                ? error.getClass().getSimpleName()
                : message;
    }

    @Override
    protected void onDestroy() {
        stopDiscovery();
        if (receiving) stopReceiveSession();
        releaseTransferWakeLock();
        if (connectivityManager != null) {
            try { connectivityManager.bindProcessToNetwork(null); } catch (Exception ignored) { }
            if (wifiNetworkCallback != null) {
                try { connectivityManager.unregisterNetworkCallback(wifiNetworkCallback); } catch (Exception ignored) { }
                wifiNetworkCallback = null;
            }
        }
        transferExecutor.shutdownNow();
        super.onDestroy();
    }


    private static final class AppEntry {
        final ApplicationInfo info;
        final String label;
        final String versionName;
        final boolean isGame;
        AppEntry(ApplicationInfo info, String label, String versionName, boolean isGame) {
            this.info = info;
            this.label = label;
            this.versionName = versionName;
            this.isGame = isGame;
        }
    }

    private static final class ReceivedBundle {
        final String name;
        final Uri uri;
        final File file;
        ReceivedBundle(String name, Uri uri, File file) {
            this.name = name;
            this.uri = uri;
            this.file = file;
        }
        InputStream open(Context context) throws IOException {
            if (uri != null) {
                InputStream input = context.getContentResolver().openInputStream(uri);
                if (input == null) throw new IOException("فایل دریافتی قابل خواندن نیست.");
                return input;
            }
            if (file != null && file.isFile()) return new java.io.FileInputStream(file);
            throw new IOException("فایل بسته پیدا نشد.");
        }
    }

    private static final class PickedFile {
        final Uri uri;
        final String name;
        final long size;
        final String relativePath;

        PickedFile(Uri uri, String name, long size, String relativePath) {
            this.uri = uri;
            this.name = name;
            this.size = size;
            this.relativePath = relativePath == null ? "" : relativePath;
        }
    }

    private final class FileTarget {
        final OutputStream output;
        final Uri mediaUri;
        final File file;
        boolean closed = false;

        FileTarget(OutputStream output, Uri mediaUri, File file) {
            this.output = output;
            this.mediaUri = mediaUri;
            this.file = file;
        }

        void finish() throws IOException {
            if (!closed) {
                output.flush();
                output.close();
                closed = true;
            }
            if (mediaUri != null) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.IS_PENDING, 0);
                getContentResolver().update(mediaUri, values, null, null);
            }
        }

        void abort() {
            try {
                if (!closed) output.close();
            } catch (Exception ignored) {
            }
            closed = true;
            try {
                if (mediaUri != null) getContentResolver().delete(mediaUri, null, null);
                if (file != null) file.delete();
            } catch (Exception ignored) {
            }
        }
    }
}
