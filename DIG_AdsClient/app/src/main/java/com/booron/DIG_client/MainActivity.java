package com.booron.DIG_AdsClient; // Ваш ТОЧНЫЙ package name
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    // Тег для логирования, удобно для фильтрации в Logcat
    private static final String TAG = "DIG_AdsClient";
    // Имя файла для сохранения настроек (deviceId и IP-адрес)
    private static final String PREFS_NAME = "DIG_AdsClientPrefs";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_SERVER_IP = "server_ip"; // Ключ для сохранения IP

    // Переменная для базового URL сервера.
    private String currentBaseUrl; // Будет загружаться из SharedPreferences или вводиться с UI

    // Интервалы обновления плейлиста и отправки Heartbeat в миллисекундах
    private static final long PLAYLIST_UPDATE_INTERVAL_MS = 5 * 60 * 1000; // 5 минут
    private static final long HEARTBEAT_INTERVAL_MS = 1 * 60 * 1000;      // 1 минута

    // Объекты для работы с данными и сетью
    private SharedPreferences sharedPrefs; // Для сохранения deviceId и IP
    private OkHttpClient httpClient;        // HTTP-клиент для запросов к серверу
    private Gson gson;                      // Для парсинга JSON
    private Handler handler;                // Для планирования задач (периодические запросы)

    // Данные устройства и плейлиста
    private String deviceId;
    private List<ContentItem> currentPlaylist = new ArrayList<>();
    private int currentPlaylistItemIndex = 0; // Текущий элемент в плейлисте

    // UI элементы из activity_main.xml
    private ImageView imageView;           // Для отображения изображений
    private PlayerView videoView;          // Для ExoPlayer (видео)

    // UI элементы для ввода IP-адреса
    private LinearLayout ipAddressLayout;
    private EditText ipAddressInput;
    private Button saveIpButton;
    private Button changeIpButton; // НОВАЯ КНОПКА: Изменить IP

    // UI элементы (для регистрации)
    private LinearLayout registrationLayout; // Контейнер для формы регистрации
    private EditText registrationKeyInput;  // Поле ввода ключа
    private Button registerButton;          // Кнопка регистрации

    // Общие UI элементы
    private ProgressBar progressBar;       // Индикатор загрузки
    private TextView statusTextView;        // Текстовое поле для статуса

    private ExoPlayer player; // Объект ExoPlayer

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Убедиться, что экран всегда активен (не выключается)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Инициализация UI элементов по их ID
        imageView = findViewById(R.id.imageView);
        videoView = findViewById(R.id.videoView);

        // ИНИЦИАЛИЗАЦИИ ДЛЯ UI-ЭЛЕМЕНТОВ IP
        ipAddressLayout = findViewById(R.id.ipAddressLayout);
        ipAddressInput = findViewById(R.id.ipAddressInput);
        saveIpButton = findViewById(R.id.saveIpButton);
        changeIpButton = findViewById(R.id.changeIpButton); // Инициализация НОВОЙ КНОПКИ

        // ИНИЦИАЛИЗАЦИИ (для регистрации)
        registrationLayout = findViewById(R.id.registrationLayout);
        registrationKeyInput = findViewById(R.id.registrationKeyInput);
        registerButton = findViewById(R.id.registerButton);

        // Общие UI
        progressBar = findViewById(R.id.progressBar);
        statusTextView = findViewById(R.id.statusTextView);


        // Инициализация объектов
        sharedPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS) // Таймауты для сетевых операций
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        gson = new Gson();
        handler = new Handler(Looper.getMainLooper()); // Создаем Handler в основном потоке

        // Попытка получить deviceId и IP-адрес из локального хранилища
        deviceId = sharedPrefs.getString(KEY_DEVICE_ID, null);
        currentBaseUrl = sharedPrefs.getString(KEY_SERVER_IP, "");

        // --- ЛОГИКА ОТОБРАЖЕНИЯ ФОРМЫ ВВОДА IP-АДРЕСА ИЛИ ПРОДОЛЖЕНИЯ РАБОТЫ ---
        if (currentBaseUrl.isEmpty()) {
            // Если IP не сохранен, показываем форму для его ввода
            showIpAddressForm(true);
            // Устанавливаем дефолтный IP для удобства
            ipAddressInput.setText("http://37.157.217.201:8000"); // Или ваш локальный
            updateStatus("Enter the IP address of the server.");
        } else {
            // Если IP сохранен, скрываем форму IP и проверяем регистрацию устройства
            showIpAddressForm(false);
            checkDeviceRegistration();
        }

        // Настройка обработчика нажатия кнопки "Сохранить IP"
        saveIpButton.setOnClickListener(v -> {
            String inputIp = ipAddressInput.getText().toString().trim();
            if (inputIp.isEmpty()) {
                showToast("Please enter the IP address");
                return;
            }

            // Простая валидация и форматирование URL
            if (!inputIp.startsWith("http://") && !inputIp.startsWith("https://")) {
                inputIp = "http://" + inputIp; // Добавляем http:// по умолчанию
            }
            if (!inputIp.endsWith("/")) {
                inputIp += "/"; // Добавляем слеш в конце, если его нет
            }

            currentBaseUrl = inputIp;
            // Сохраняем новый IP-адрес
            sharedPrefs.edit().putString(KEY_SERVER_IP, currentBaseUrl).apply();
            showToast("Server IP saved: " + currentBaseUrl);
            Log.d(TAG, "Server IP saved: " + currentBaseUrl);

            showIpAddressForm(false); // ИСПРАВЛЕНО: Скрываем форму ввода IP после сохранения
            checkDeviceRegistration(); // Переходим к проверке регистрации
        });

        // Настройка обработчика нажатия НОВОЙ КНОПКИ "Изменить IP"
        changeIpButton.setOnClickListener(v -> {
            // При нажатии кнопки "Изменить IP"
            // Скрываем все остальные View (плейлист, регистрацию, прогресс-бар, статус)
            // И показываем форму ввода IP-адреса
            Log.d(TAG, "Change IP button clicked. Showing IP form.");
            // Устанавливаем текущий IP в поле ввода для удобства редактирования
            ipAddressInput.setText(currentBaseUrl);
            showIpAddressForm(true);
            updateStatus("Change the IP address of the server.");
            // Останавливаем все фоновые процессы (heartbeat, плейлист)
            handler.removeCallbacks(heartbeatRunnable);
            handler.removeCallbacks(playlistRunnable);
            if (player != null) {
                player.stop();
                player.clearMediaItems();
            }
        });


        // Настройка обработчика нажатия кнопки регистрации (СУЩЕСТВУЮЩИЙ КОД)
        registerButton.setOnClickListener(v -> {
            String registrationKey = registrationKeyInput.getText().toString().trim();
            if (registrationKey.isEmpty()) {
                showToast("Enter the registration key");
                return;
            }
            if (currentBaseUrl == null || currentBaseUrl.isEmpty()) {
                showToast("Error: IP address of the server is not established!");
                showIpAddressForm(true); // Просим ввести IP, если его нет
                return;
            }
            showProgressBar(true); // Показываем прогресс-бар
            registerDevice(registrationKey); // Запускаем процесс регистрации
        });

        // Инициализация ExoPlayer для воспроизведения видео
        player = new ExoPlayer.Builder(this).build();
        videoView.setPlayer(player); // Привязываем плеер к VideoView
        player.addListener(new Player.Listener() { // Добавляем слушателя событий плеера
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                // Если видео закончилось, переходим к следующему элементу плейлиста
                if (playbackState == Player.STATE_ENDED) {
                    Log.d(TAG, "Video playback ended. Moving to next item.");
                    playNextContent();
                }
            }
        });
    }

    // Проверяет, зарегистрировано ли устройство
    private void checkDeviceRegistration() {
        if (currentBaseUrl == null || currentBaseUrl.isEmpty()) {
            // Если нет IP, то не можем проверить регистрацию
            showIpAddressForm(true);
            updateStatus("Enter the IP address of the server to verify the registration.");
            // Скрываем кнопку "Изменить IP", когда форма IP уже открыта
            changeIpButton.setVisibility(View.GONE);
            return;
        }

        if (deviceId == null) {
            // Устройство не зарегистрировано, показываем UI для регистрации
            showRegistrationForm(true);
            updateStatus("The device is not registered. Enter the key.");
            // Показываем кнопку "Изменить IP", если ее нет
            changeIpButton.setVisibility(View.VISIBLE);
        } else {
            // Устройство уже зарегистрировано, скрываем форму и запускаем основной функционал
            showRegistrationForm(false);
            updateStatus("The device is registered. Sending Heartbeat...");
            sendHeartbeatAndStartScheduler(); // Отправляем Heartbeat и запускаем планировщик
            // Показываем кнопку "Изменить IP"
            changeIpButton.setVisibility(View.VISIBLE);
        }
    }

    // Отправляет запрос на регистрацию устройства на сервер
    private void registerDevice(String registrationKey) {
        // ИСПОЛЬЗУЕМ currentBaseUrl
        String url = currentBaseUrl + "api/v1/device/register/";
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("registration_key", registrationKey);

        // Если deviceId уже существует (например, при повторной попытке после сбоя), используем его.
        // Иначе генерируем новый UUID.
        if (deviceId != null) {
            jsonBody.addProperty("device_id", deviceId);
        } else {
            deviceId = UUID.randomUUID().toString(); // Генерируем новый уникальный ID
            sharedPrefs.edit().putString(KEY_DEVICE_ID, deviceId).apply(); // Сохраняем его
            jsonBody.addProperty("device_id", deviceId);
        }

        // Добавляем дополнительную информацию о ТВ для сервера
        jsonBody.addProperty("name", Build.MODEL + " (" + Build.MANUFACTURER + ")");
        jsonBody.addProperty("ip_address", getIpAddress());
        jsonBody.addProperty("mac_address", getMacAddress());
        jsonBody.addProperty("app_version", getAppVersion());
        jsonBody.addProperty("serial_number", getSerialNumber());

        // Создаем тело запроса в формате JSON
        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(url)
                .post(body) // Метод POST
                .build();

        // Асинхронный вызов HTTP-запроса
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Обработка ошибок сети или сервера
                Log.e(TAG, "Device registration failed: " + e.getMessage(), e);
                runOnUiThread(() -> { // Выполняем в UI-потоке
                    showProgressBar(false);
                    showToast("Registration error: " + e.getMessage());
                    updateStatus("Registration error. Repeat after 10 seconds.");
                    // Повторная попытка регистрации через 10 секунд
                    handler.postDelayed(() -> registerDevice(registrationKey), 10000);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                // Обработка ответа от сервера
                final String responseBody = response.body() != null ? response.body().string() : "No response body";
                if (response.isSuccessful()) {
                    Log.d(TAG, "Device registered successfully: " + responseBody);
                    runOnUiThread(() -> { // Выполняем в UI-потоке
                        showProgressBar(false);
                        showRegistrationForm(false);
                        showToast("The device is successfully registered!");
                        updateStatus("The device is registered.");
                        sendHeartbeatAndStartScheduler(); // После успешной регистрации, начинаем отправлять heartbeat
                    });
                } else {
                    Log.e(TAG, "Device registration failed: " + response.code() + " - " + responseBody);
                    runOnUiThread(() -> { // Выполняем в UI-потоке
                        showProgressBar(false);
                        showToast("Registration error: (" + response.code() + "): " + responseBody);
                        updateStatus("Registration error. Enter the key again.");
                        // Если ключ неверный, сбрасываем deviceId, чтобы пользователь мог ввести новый ключ
                        if (response.code() == 400 && responseBody.contains("Invalid or expired registration key")) {
                            sharedPrefs.edit().remove(KEY_DEVICE_ID).apply();
                            deviceId = null; // Также сбрасываем deviceId в памяти приложения
                        }
                    });
                }
            }
        });
    }

    // Запускает периодическую отправку heartbeat и запрос плейлиста
    private void sendHeartbeatAndStartScheduler() {
        handler.removeCallbacks(heartbeatRunnable); // Удаляем любые предыдущие запланированные задачи
        handler.removeCallbacks(playlistRunnable);
        handler.post(heartbeatRunnable); // Запускаем heartbeat немедленно
        handler.post(playlistRunnable);  // Запускаем запрос плейлиста немедленно
    }

    // Runnable для периодической отправки heartbeat
    private final Runnable heartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            sendHeartbeat();
            handler.postDelayed(this, HEARTBEAT_INTERVAL_MS); // Планируем следующий heartbeat
        }
    };

    // Runnable для периодического запроса плейлиста
    private final Runnable playlistRunnable = new Runnable() {
        @Override
        public void run() {
            fetchPlaylist();
            handler.postDelayed(this, PLAYLIST_UPDATE_INTERVAL_MS); // Планируем следующий запрос плейлиста
        }
    };

    // Отправляет запрос "Heartbeat" на сервер для обновления статуса устройства
    private void sendHeartbeat() {
        if (deviceId == null) {
            Log.e(TAG, "Cannot send heartbeat: deviceId is null. Reregistering...");
            runOnUiThread(this::checkDeviceRegistration); // Если deviceId потерян, пытаемся перерегистрироваться
            return;
        }
        if (currentBaseUrl == null || currentBaseUrl.isEmpty()) {
            Log.e(TAG, "Cannot send heartbeat: Server IP not set.");
            runOnUiThread(() -> {
                showToast("Server IP is not installed. Please enter it.");
                showIpAddressForm(true); // Показать форму для ввода IP
            });
            return;
        }

        // ИСПОЛЬЗУЕМ currentBaseUrl
        String url = currentBaseUrl + "api/v1/device/status/";
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("device_id", deviceId);
        jsonBody.addProperty("ip_address", getIpAddress());
        jsonBody.addProperty("mac_address", getMacAddress());
        jsonBody.addProperty("app_version", getAppVersion());
        jsonBody.addProperty("serial_number", getSerialNumber());

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Heartbeat failed: " + e.getMessage());
                runOnUiThread(() -> updateStatus("Heartbeat failed: " + e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseBody = response.body() != null ? response.body().string() : "No response body";
                if (response.isSuccessful()) {
                    Log.d(TAG, "Heartbeat sent successfully: " + responseBody);
                    runOnUiThread(() -> updateStatus("Heartbeat OK."));
                } else {
                    Log.e(TAG, "Heartbeat failed: " + response.code() + " - " + responseBody);
                    // Если устройство не найдено на сервере (404), возможно, его удалили из админки.
                    // Сбрасываем deviceId и начинаем процесс регистрации заново.
                    if (response.code() == 404) {
                        runOnUiThread(() -> {
                            showToast("The structure is not found on the server. Re-registration...");
                            sharedPrefs.edit().remove(KEY_DEVICE_ID).apply();
                            deviceId = null;
                            checkDeviceRegistration();
                        });
                    }
                    runOnUiThread(() -> updateStatus("Heartbeat error: " + response.code()));
                }
            }
        });
    }

    // Запрашивает текущий плейлист для устройства с сервера
    private void fetchPlaylist() {
        if (deviceId == null) {
            Log.e(TAG, "Cannot fetch playlist: deviceId is null.");
            return;
        }
        if (currentBaseUrl == null || currentBaseUrl.isEmpty()) {
            Log.e(TAG, "Cannot fetch playlist: Server IP not set.");
            runOnUiThread(() -> {
                showToast("Server IP is not installed. Please enter it.");
                showIpAddressForm(true); // Показать форму для ввода IP
            });
            return;
        }

        // ИСПОЛЬЗУЕМ currentBaseUrl
        String url = currentBaseUrl + "api/v1/playlist/" + deviceId + "/";
        Request request = new Request.Builder()
                .url(url)
                .get() // Метод GET
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Failed to fetch playlist: " + e.getMessage());
                runOnUiThread(() -> updateStatus("Error loading playlist. " + e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseBody = response.body() != null ? response.body().string() : "No response body";
                if (response.isSuccessful()) {
                    Log.d(TAG, "Playlist fetched successfully: " + responseBody);
                    try {
                        // Парсим JSON-ответ в список ContentItem'ов
                        JsonObject playlistJson = gson.fromJson(responseBody, JsonObject.class);
                        JsonArray itemsJson = playlistJson.getAsJsonArray("items");
                        List<ContentItem> fetchedPlaylist = new ArrayList<>();

                        for (JsonElement itemElement : itemsJson) {
                            JsonObject itemObj = itemElement.getAsJsonObject();
                            JsonObject contentObj = itemObj.getAsJsonObject("content"); // Извлекаем вложенный объект content

                            int id = contentObj.get("id").getAsInt();
                            String name = contentObj.get("name").getAsString();
                            String contentType = contentObj.get("content_type").getAsString();
                            String fileUrl = contentObj.get("file_url").getAsString();
                            // Проверяем наличие и nullность duration_seconds
                            Integer duration = contentObj.has("duration_seconds") && !contentObj.get("duration_seconds").isJsonNull()
                                    ? contentObj.get("duration_seconds").getAsInt() : null;

                            fetchedPlaylist.add(new ContentItem(id, name, contentType, fileUrl, duration));
                        }

                        runOnUiThread(() -> {
                            currentPlaylist = fetchedPlaylist;
                            currentPlaylistItemIndex = 0; // Начинаем воспроизведение сначала
                            if (currentPlaylist.isEmpty()) {
                                updateStatus("The playlist is empty.");
                                hideContentViews();
                                return;
                            }
                            updateStatus("Playlist loaded. We are starting playback...");
                            playNextContent(); // Начинаем воспроизведение первого элемента плейлиста
                        });

                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing playlist JSON: " + e.getMessage(), e);
                        runOnUiThread(() -> updateStatus("Playlist parsing error."));
                    }
                } else {
                    Log.e(TAG, "Failed to fetch playlist: " + response.code() + " - " + responseBody);
                    runOnUiThread(() -> {
                        updateStatus("Error loading playlist. " + response.code());
                        // Если плейлист не найден для устройства, скрываем весь контент
                        if (response.code() == 404) {
                            hideContentViews();
                            updateStatus("Playlist not found for device.");
                        }
                    });
                }
            }
        });
    }

    // Воспроизводит следующий элемент из плейлиста
    private void playNextContent() {
        if (currentPlaylist.isEmpty()) {
            Log.d(TAG, "Playlist is empty. Waiting for next update.");
            hideContentViews();
            return;
        }

        // Если достигли конца плейлиста, начинаем сначала
        if (currentPlaylistItemIndex >= currentPlaylist.size()) {
            currentPlaylistItemIndex = 0;
            Log.d(TAG, "Playlist finished. Starting from beginning.");
        }

        ContentItem currentItem = currentPlaylist.get(currentPlaylistItemIndex);
        Log.d(TAG, "Playing: " + currentItem.name + " (" + currentItem.contentType + ") from " + currentItem.fileUrl);
        updateStatus("Playing: " + currentItem.name);

        // Скрываем все виды контента перед отображением нового
        hideContentViews();

        // Воспроизведение в зависимости от типа контента
        if ("image".equals(currentItem.contentType)) {
            imageView.setVisibility(View.VISIBLE);
            if (player != null && player.isPlaying()) { // Если видео играло, останавливаем его
                player.stop();
            }
            // Загружаем изображение с помощью Glide
            Glide.with(this)
                    .load(currentItem.fileUrl)
                    .into(imageView);
            // Планируем переход к следующему элементу через заданную длительность
            if (currentItem.durationSeconds != null && currentItem.durationSeconds > 0) {
                handler.postDelayed(this::playNextContent, currentItem.durationSeconds * 1000L);
            } else {
                Log.w(TAG, "Image duration not set or is 0. Using default 5 seconds.");
                handler.postDelayed(this::playNextContent, 5000L); // Если длительность не указана, по умолчанию 5 секунд
            }

        } else if ("video".equals(currentItem.contentType)) {
            videoView.setVisibility(View.VISIBLE);
            if (player != null) {
                MediaItem mediaItem = MediaItem.fromUri(currentItem.fileUrl);
                player.setMediaItem(mediaItem);
                player.prepare(); // Подготавливаем видео
                player.play();   // Начинаем воспроизведение
            } else {
                Log.e(TAG, "ExoPlayer is null, cannot play video.");
                // Если плеер не готов, пропускаем текущий элемент и переходим к следующему
                currentPlaylistItemIndex++;
                playNextContent();
            }
        } else {
            Log.e(TAG, "Unknown content type: " + currentItem.contentType);
            currentPlaylistItemIndex++; // Пропускаем неизвестный тип
            playNextContent();
        }

        currentPlaylistItemIndex++; // Увеличиваем индекс для следующего элемента
    }

    // Скрывает все элементы отображения контента (изображения, видео)
    private void hideContentViews() {
        imageView.setVisibility(View.GONE);
        videoView.setVisibility(View.GONE);
        if (player != null) {
            player.stop(); // Останавливаем видео, если оно играет
            player.clearMediaItems(); // Очищаем плейлист ExoPlayer
        }
    }

    // Показывает/скрывает форму ввода IP-адреса
    private void showIpAddressForm(boolean show) {
        ipAddressLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        // При показе/скрытии формы IP, скрываем другие основные элементы
        registrationLayout.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        statusTextView.setVisibility(View.GONE); // Скроем статус, пока IP не введен
        hideContentViews();
        // Управляем видимостью кнопки "Изменить IP"
        changeIpButton.setVisibility(show ? View.GONE : View.VISIBLE);
        // Если форма IP показана, скрываем кнопку "Изменить IP",
        // иначе, показываем ее (если она должна быть видимой в текущем состоянии приложения)
    }


    // Показывает/скрывает форму регистрации
    private void showRegistrationForm(boolean show) {
        registrationLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        progressBar.setVisibility(View.GONE); // Прогресс-бар скрыт при форме
        statusTextView.setVisibility(show ? View.VISIBLE : View.GONE);
        hideContentViews(); // Скрываем контент при отображении формы
        if (show) {
            // Если показываем форму регистрации, убедимся, что форма IP скрыта
            ipAddressLayout.setVisibility(View.GONE);
            // Если форма регистрации показана, кнопка "Изменить IP" должна быть видима (чтобы можно было поменять IP)
            changeIpButton.setVisibility(View.VISIBLE);
        } else {
            // Если форма регистрации скрыта, кнопка "Изменить IP" также должна быть видима (если не показана форма IP)
            if (ipAddressLayout.getVisibility() == View.GONE) {
                changeIpButton.setVisibility(View.VISIBLE);
            }
        }
    }

    // Показывает/скрывает прогресс-бар
    private void showProgressBar(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        statusTextView.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            hideContentViews();
            showRegistrationForm(false);
            ipAddressLayout.setVisibility(View.GONE); // Скрываем и форму IP
            changeIpButton.setVisibility(View.GONE); // Скрываем кнопку "Изменить IP" при показе прогресс-бара
        } else {
            // Когда прогресс-бар скрыт, нужно решить, что показывать дальше
            // Это уже зависит от общей логики вашего приложения
            // Например, после загрузки показать плейлист или форму регистрации
            // Или просто показать кнопку "Изменить IP", если нет других активных форм
            if (ipAddressLayout.getVisibility() == View.GONE && registrationLayout.getVisibility() == View.GONE) {
                changeIpButton.setVisibility(View.VISIBLE);
            }
        }
    }

    // Обновляет текстовое поле статуса в UI
    private void updateStatus(String message) {
        Log.d(TAG, "Status: " + message);
        runOnUiThread(() -> { // Убедимся, что обновление происходит в UI-потоке
            statusTextView.setText("Status: " + message);
            statusTextView.setVisibility(View.VISIBLE);
        });
    }

    // Показывает короткое всплывающее сообщение (Toast)
    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }

    // --- Вспомогательные методы для получения информации об устройстве ---

    // Возвращает IP-адрес устройства
    private String getIpAddress() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                int ipAddress = wifiInfo.getIpAddress();
                // Formatter.formatIpAddress устарел, но часто используется для старых API
                // Для новых: NetworkUtils.intToInetAddress(ipAddress).getHostAddress();
                return Formatter.formatIpAddress(ipAddress);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting IP address: " + e.getMessage());
        }
        return null; // Возвращаем null, если не удалось получить
    }

    // Возвращает MAC-адрес устройства (может быть "02:00:00:00:00:00" на новых Android)
    private String getMacAddress() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                String mac = wifiInfo.getMacAddress();
                if (mac != null && !mac.equals("02:00:00:00:00:00")) {
                    return mac;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting MAC address: " + e.getMessage());
        }
        return "UNKNOWN_MAC"; // Заглушка
    }

    // Возвращает версию приложения
    private String getAppVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting app version: " + e.getMessage());
            return "UNKNOWN_VERSION";
        }
    }

    // Возвращает серийный номер устройства или ANDROID_ID как запасной вариант
    private String getSerialNumber() {
        String serial = Build.SERIAL;
        if ("unknown".equalsIgnoreCase(serial) || "android".equalsIgnoreCase(serial)) {
            serial = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        return serial != null ? serial : "UNKNOWN_SERIAL";
    }

    // Методы жизненного цикла Activity
    @Override
    protected void onResume() {
        super.onResume();
        // Возобновить воспроизведение, если было приостановлено и плейлист не пуст
        if (player != null && currentPlaylist.size() > 0) {
            player.setPlayWhenReady(true);
            player.getPlaybackState();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Приостановить воспроизведение, когда приложение неактивно
        if (player != null) {
            player.setPlayWhenReady(false);
            player.getPlaybackState();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Освободить ресурсы ExoPlayer и удалить все запланированные задачи Handler
        if (player != null) {
            player.release();
            player = null;
        }
        handler.removeCallbacksAndMessages(null);
    }

    // Вспомогательный класс для хранения данных элемента контента из плейлиста
    private static class ContentItem {
        int id;
        String name;
        String contentType;
        String fileUrl;
        Integer durationSeconds;

        ContentItem(int id, String name, String contentType, String fileUrl, Integer durationSeconds) {
            this.id = id;
            this.name = name;
            this.contentType = contentType;
            this.fileUrl = fileUrl;
            this.durationSeconds = durationSeconds;
        }
    }
}