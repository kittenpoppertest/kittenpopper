package layout;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.kitten.kittenpopper.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class KittenPoppingService extends IntentService {

    private static final int BLOCK_SCREEN_DELAY = 16000;
    private static final int POPUP_DURATION = 600;

    WindowManager wm;
    Button clickmeButton;
    Handler handler;
    Map<String, Object> popupImageSettings;
    Map<String, Object> additionalImageSettings;

    public KittenPoppingService() {
        super("KittenPoppingService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        handler = new Handler();
        popupImageSettings = null;
        additionalImageSettings = null;

        dispatchUponSettings();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (popupImageSettings != null) {
            if (popupImageSettings.get("file") != null) {
                ((File)popupImageSettings.get("file")).delete();
            }
        }

        if (additionalImageSettings != null) {
            if (additionalImageSettings.get("file") != null) {
                ((File)additionalImageSettings.get("file")).delete();
            }
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
    }

    private void dispatchUponSettings() {
        try {
            JSONObject settingsJson = getSettingsJson();

            JSONArray popupImageArray = settingsJson.getJSONArray(getString(R.string.popup_image_name));
            Map<String, Object> popupImageMap = parseImageArray(popupImageArray);
            handlePopupImage(popupImageMap);

            if (settingsJson.has(getString(R.string.additional_image_name))) {
                JSONArray additionalImageArray = settingsJson.getJSONArray(getString(R.string.additional_image_name));
                Map<String, Object> additionalImageMap = parseImageArray(additionalImageArray);
                handleAdditionalImage(additionalImageMap);
            }
        } catch (JSONException e) {
            Toast.makeText(this, "Error parsing settings json", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayPopupUserInterface(final Map<String, Object> imageSettings) {
        clickmeButton = new Button(this);
        clickmeButton.setText(R.string.popup_button_text);
        clickmeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawPopupImageRandomly(imageSettings);
            }
        });

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;
        params.x = 0;
        params.y = 0;

        wm.addView(clickmeButton, params);
    }

    private void drawPopupImageRandomly(Map<String, Object> imageSettings) {
        Point position = getRandomPosition();

        final LinearLayout linearLayout = new LinearLayout(getBaseContext());
        linearLayout.setBackground((Drawable)imageSettings.get("drawable"));
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                (int)imageSettings.get("width"),
                (int)imageSettings.get("height"),
                (int)imageSettings.get("type"),
                (int)imageSettings.get("flags"),
                (int)imageSettings.get("format"));
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = position.x;
        params.y = position.y;

        wm.addView(linearLayout, params);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                wm.removeView(linearLayout);
            }
        }, POPUP_DURATION);
    }

    private JSONObject getSettingsJson() throws JSONException {
        SharedPreferences preferences = getSharedPreferences(getString(R.string.preference_file), MODE_PRIVATE);
        String settingsString = preferences.getString(getString(R.string.received_settings), null);
        return new JSONObject(settingsString);
    }

    private void handlePopupImage(Map<String, Object> imageSettings) {
        displayPopupUserInterface(imageSettings);
    }

    private void handleAdditionalImage(Map<String, Object> imageSettings) {
        scheduleBlockScreen(imageSettings);
    }

    private void scheduleBlockScreen(final Map<String, Object> blockingImageSettings) {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                blockScreen(blockingImageSettings);
            }
        }, BLOCK_SCREEN_DELAY);
    }

    private void blockScreen(Map<String, Object> blockingImageSettings) {
        LinearLayout linearLayout = new LinearLayout(getBaseContext());
        linearLayout.setBackground((Drawable)blockingImageSettings.get("drawable"));
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                (int)blockingImageSettings.get("width"),
                (int)blockingImageSettings.get("height"),
                (int)blockingImageSettings.get("type"),
                (int)blockingImageSettings.get("flags"),
                (int)blockingImageSettings.get("format"));
        wm.removeView(clickmeButton);
        wm.addView(linearLayout, params);
    }

    private Map<String, Object> parseImageArray(JSONArray imageJsonArray) throws JSONException {
        Map<String, Object> imageMap = new HashMap<>();
        imageMap.put("name", imageJsonArray.getString(0));
        imageMap.put("path", imageJsonArray.getString(1));
        imageMap.put("data", imageJsonArray.getString(2));
        imageMap.put("width", imageJsonArray.getInt(3));
        imageMap.put("height", imageJsonArray.getInt(4));
        imageMap.put("type", imageJsonArray.getInt(5));
        imageMap.put("flags", imageJsonArray.getInt(6));
        imageMap.put("format", imageJsonArray.getInt(7));

        imageMap.put("file", new File(getFilesDir(), (String)imageMap.get("path")));
        saveBase64ToFile((File)imageMap.get("file"), (String)imageMap.get("data"));

        String imageFilePath = ((File)imageMap.get("file")).getPath();
        Drawable imageDrawable = Drawable.createFromPath(imageFilePath);
        imageMap.put("drawable", imageDrawable);

        return imageMap;
    }

    private Point getRandomPosition() {
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x;
        int screenHeight = size.y;

        Random random = new Random();
        int random_width = random.nextInt(screenWidth);
        int random_height = random.nextInt(screenHeight);

        return new Point(random_width, random_height);
    }

    private void saveBase64ToFile(File file, String data) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(Base64.decode(data, Base64.DEFAULT));
            fileOutputStream.close();
        } catch (Exception e) {
            Log.d("Yoni", "error saving file " + file.getName() + ": " + e.toString());
        }
    }
}

