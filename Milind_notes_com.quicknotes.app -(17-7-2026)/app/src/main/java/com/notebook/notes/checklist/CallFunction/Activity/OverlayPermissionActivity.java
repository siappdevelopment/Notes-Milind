package com.notebook.notes.checklist.CallFunction.Activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.transition.Explode;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.notebook.notes.checklist.R;

import org.jetbrains.annotations.Nullable;

public class OverlayPermissionActivity extends AppCompatActivity {

    private Window window;
    private WindowManager.LayoutParams layoutParams;
    private ConstraintLayout overlay_root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_overlay_permission);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        }
        overlay_root = findViewById(R.id.overlay_root);
        window = getWindow();
        window.addFlags(7078560);
        window.setSoftInputMode(2);
        setFinishOnTouchOutside(true);
        int animEnter = R.anim.optin_slide_up;
        overridePendingTransition(animEnter, 0);
        window.setEnterTransition(new Explode());
        window.setExitTransition(new Explode());
        overridePendingTransition(animEnter, 0);
        new Handler().postDelayed(() -> {
            try {
                OverlayPermissionActivity.this.finish();
            } catch (Exception ignored) {
            }
        }, 3000);

        layoutParams = window.getAttributes();
        layoutParams.gravity = Gravity.BOTTOM;
        layoutParams.x = 1;
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.dimAmount = 0.2f;
        window.setAttributes(layoutParams);

        overlay_root.setOnClickListener(v -> OverlayPermissionActivity.this.finish());
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        finish();
        return super.dispatchTouchEvent(motionEvent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 132) {
            FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(this);
            Bundle bundle = new Bundle();
            if (Settings.canDrawOverlays(this)) {
                bundle.putString("PermissionStatus", "Allowed");
            } else {
                bundle.putString("PermissionStatus", "Denied");
            }
            firebaseAnalytics.logEvent("OverlayPermissionReport", bundle);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        overridePendingTransition(0, R.anim.optin_slide_down);
    }
}
