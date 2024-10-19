package com.example.boom;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private OnBackPressedCallback mBackPressedCallback;
    private Timer mTimer;
    private boolean doubleBackToExitPressedOnce;
    private boolean pageLoadFailed = false;
    private WebView webView;
    private List<String> urlList;
    private Random random;
    private PowerManager.WakeLock wakeLock;
    private String phoneNumber;
    private int delayBeforeJsExecution = 3000;
    private int delayAfterJsExecution = 3000;
    private boolean isPaused = false;
    private int successCount = 0;
    private String message;

    private Button pauseButton;
    private TextView beforeDelayEditText;
    private TextView afterDelayEditText;
    private TextView phoneNumberEditText;
    private TextView messageStatusLabel;
    private TextView sendCountLabel;

    private SharedPreferences sharedPreferences;

    private final Handler taskHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // 在这里执行自定义的返回逻辑
                // 比如弹出提示对话框，询问用户是否确定退出
                // 或者直接关闭Activity
                if (doubleBackToExitPressedOnce) {
                    // 如果是第二次点击，直接退出应用
                    finish();
                    return;
                }
                doubleBackToExitPressedOnce = true;
                mTimer = new Timer();
                mTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        doubleBackToExitPressedOnce = false;
                    }
                }, 2000); // 设置时间为 2 秒

                // 提示用户再次点击退出应用
                Toast.makeText(MainActivity.this, "再按一次退出应用", Toast.LENGTH_SHORT).show();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, mBackPressedCallback);

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        connectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
            @Override
            public void onLost(@NonNull Network network) {
                runOnUiThread(() -> {
                    isPaused = true;
                    pauseButton.setText("继续");
                    Toast.makeText(MainActivity.this, "网络断开，暂停执行", Toast.LENGTH_SHORT).show();
                });
            }
        });

        phoneNumberEditText = findViewById(R.id.phoneNumberEditText);
        beforeDelayEditText = findViewById(R.id.beforeDelay);
        afterDelayEditText = findViewById(R.id.afterDelay);
        Button startButton = findViewById(R.id.startButton);
        pauseButton = findViewById(R.id.pauseButton);
        messageStatusLabel = findViewById(R.id.messageStatusLabel);
        sendCountLabel = findViewById(R.id.sendCountLabel);

        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        phoneNumberEditText.setText(sharedPreferences.getString("PhoneNumber", "15021306159"));
        beforeDelayEditText.setText(String.valueOf(sharedPreferences.getInt("BeforeDelay", 3000)));
        afterDelayEditText.setText(String.valueOf(sharedPreferences.getInt("AfterDelay", 3000)));

        random = new Random();
        urlList = new ArrayList<>();
        sendCountLabel.setText("已发送 0 条  |  剩余 " + Urls.URL_LIST.length + " 条");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 获取WakeLock
        acquireWakeLock();

        startButton.setOnClickListener(v -> {
            taskHandler.removeCallbacksAndMessages(null);
            phoneNumber = phoneNumberEditText.getText().toString();
            if (phoneNumber.isEmpty()) {
                phoneNumberEditText.setError("请输入手机号码");
                return;
            }

            try {
                delayBeforeJsExecution = Integer.parseInt(beforeDelayEditText.getText().toString());
                delayAfterJsExecution = Integer.parseInt(afterDelayEditText.getText().toString());
            } catch (NumberFormatException e) {
                Toast.makeText(this, "延时格式错误", Toast.LENGTH_SHORT).show();
            }

            // 保存数据到 SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("PhoneNumber", phoneNumber);
            editor.putInt("BeforeDelay", delayBeforeJsExecution);
            editor.putInt("AfterDelay", delayAfterJsExecution);
            editor.apply();

            messageStatusLabel.setText("待发消息");
            messageStatusLabel.setTextColor(Color.BLUE);
            isPaused = false;
            pauseButton.setText("暂停");
            hideKeyboard();
            urlList.clear();
            successCount = 0;
            urlList.addAll(Arrays.asList(Urls.URL_LIST));
            sendCountLabel.setText("已发送 0 条  |  剩余 " + urlList.size() + " 条");
            // 加载第一个随机URL
            loadNextRandomUrl();
        });

        pauseButton.setOnClickListener(v -> {
            isPaused = !isPaused;
            pauseButton.setText(isPaused ? "继续" : "暂停");
            if (!isPaused) {
                loadNextRandomUrl();
            }
        });
    }

    private void setWebViewConfig() {
        if (webView != null) {
            // 配置WebView设置
            WebSettings webSettings = webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setSupportZoom(false);
            webSettings.setGeolocationEnabled(false);
            webSettings.setDomStorageEnabled(false);

            // 设置WebViewClient
            webView.setWebViewClient(new WebViewClient() {

                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);
                    pageLoadFailed = false;
                    if (!url.equals("about:blank")) {
                        message = Message.generateMessage(phoneNumber);
                        messageStatusLabel.setText(message);
                        messageStatusLabel.setTextColor(Color.BLUE);
                    }
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    if (!pageLoadFailed && !isPaused && message != null && !url.equals("about:blank")) {
                        String safeMessage = JSONObject.quote(message);
                        String javascript = "var textarea = document.querySelector('.imlp-component-newtypebox-textarea');" +
                                "if (textarea) {" +
                                "   textarea.value = " + safeMessage + ";" +
                                "   textarea.dispatchEvent(new Event('input'));" +
                                "   var sendButton = document.querySelector('.imlp-component-newtypebox-send');" +
                                "   if (sendButton) {" +
                                "       sendButton.click();" +
                                "       'jsSuccess';" +
                                "   }" +
                                "}";
                        // 在页面加载完成后的3秒发送消息
                        taskHandler.postDelayed(() -> webView.evaluateJavascript(javascript, value -> taskHandler.postDelayed(() -> {
                            // 发送消息0.25秒后处理结果
                            String resultJS = "document.body.innerText;";
                            webView.evaluateJavascript(resultJS, result -> {
                                if (result.contains(message)) {
                                    successCount++;
                                    messageStatusLabel.setTextColor(Color.parseColor("#669900"));
                                    System.out.println("任务js success");
                                } else {
                                    messageStatusLabel.setTextColor(Color.RED);
                                    System.out.println("任务js fail");
                                }
                                updateSendCountLabel();
                                String closePassModJS = "var mask = document.querySelector('.imlp-component-mask');" +
                                        "if (mask) {" +
                                        "   mask.style.display = 'none';" +
                                        "}" +
                                        "var passModDialog = document.querySelector('.passMod_dialog-wrapper.passMod_show');" +
                                        "if (passModDialog) {" +
                                        "   passModDialog.style.display = 'none';" +
                                        "}";
                                webView.evaluateJavascript(closePassModJS, value2 -> {
                                    // 任务完成后等待3秒供用户观察
                                    taskHandler.postDelayed(() -> loadNextRandomUrl(), delayAfterJsExecution);
                                });
                            });
                        }, 250)), delayBeforeJsExecution);
                    }
                }

                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    super.onReceivedError(view, request, error);
                    pageLoadFailed = true;
                    if (!isPaused) {
                        webView.stopLoading();
                        taskHandler.removeCallbacksAndMessages(null);
                        messageStatusLabel.setText("页面加载失败");
                        messageStatusLabel.setTextColor(Color.RED);
                        loadNextRandomUrl();
                        System.out.println("任务error");
                    }
                }
            });
        }
    }

    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Boom:WakeLock");
        }
        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }
    }

    private void recreateWebView() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }

        ConstraintLayout constraintLayout = findViewById(R.id.main);
        webView = new WebView(this);
        setWebViewConfig();
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
                ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
        );
        params.topToBottom = R.id.startButton; // 确保 WebView 在 startButton 下方
        params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;

        // 将 WebView 添加到布局中
        constraintLayout.addView(webView, params);
    }

    private void loadNextRandomUrl() {
        if (!isPaused && !urlList.isEmpty()) {
            if (successCount % 10 == 0) {
                System.out.println("WebView 重新创建" + successCount + "次, ");
                recreateWebView();
            }
            // 从剩余的URL中随机选择一个
            int randomIndex = random.nextInt(urlList.size());
            String randomUrl = urlList.get(randomIndex);

            //加载前清空WebView的内容
            webView.clearHistory();
            webView.clearCache(true);
            webView.loadUrl("about:blank");
            webView.loadUrl(randomUrl);
            urlList.remove(randomIndex);
            System.out.println("任务进行中");
        } else if (urlList.isEmpty()) {
            System.out.println("任务完成");
            releaseWakeLock();
        }
    }

    private void updateSendCountLabel() {
        sendCountLabel.setText("已发送 " + successCount + " 条  |  剩余 " + urlList.size() + " 条");
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseWakeLock();
        taskHandler.removeCallbacksAndMessages(null);
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        if (mBackPressedCallback != null) {
            mBackPressedCallback.remove();
        }
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }
}