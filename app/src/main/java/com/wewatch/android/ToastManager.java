package com.wewatch.android;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ToastManager {
    private static final int MAX_TOASTS = 3;
    private static final long DUPLICATE_WINDOW_MS = 1500;

    private final Context context;
    private final ViewGroup inAppContainer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final WindowManager windowManager;

    private LinearLayout overlayContainer;
    private boolean overlayAttached = false;
    private String lastMessage = "";
    private String lastKind = "";
    private long lastShownAt = 0;

    public ToastManager(Context context, ViewGroup container) {
        this.context = context.getApplicationContext();
        this.inAppContainer = container;
        this.windowManager = (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);
    }

    public void showToast(String message, String kind) {
        showToast(message, kind, durationForKind(kind));
    }

    public void showToast(String message, String kind, int durationMs) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }

        handler.post(() -> {
            String cleanMessage = message.trim();
            String cleanKind = normalizeKind(kind);
            long now = System.currentTimeMillis();
            if (cleanMessage.equals(lastMessage) && cleanKind.equals(lastKind) && now - lastShownAt < DUPLICATE_WINDOW_MS) {
                return;
            }

            lastMessage = cleanMessage;
            lastKind = cleanKind;
            lastShownAt = now;

            View toast = createToast(cleanMessage, cleanKind);
            int duration = Math.max(1600, Math.min(8000, durationMs));
            if (!showSystemToast(toast, duration)) {
                addToast(inAppContainer, toast, duration);
            }
        });
    }

    public void clear() {
        handler.removeCallbacksAndMessages(null);
        inAppContainer.removeAllViews();
        removeOverlayContainer();
    }

    private View createToast(String message, String kind) {
        View toast = LayoutInflater.from(context).inflate(R.layout.toast_item, null, false);

        TextView timeView = toast.findViewById(R.id.toastTime);
        TextView messageView = toast.findViewById(R.id.toastMessage);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        timeView.setText(sdf.format(new Date()));
        timeView.setTextColor(accentForKind(kind));

        messageView.setText(message);
        toast.setBackground(createBackground(kind));
        toast.setElevation(dp(12));

        return toast;
    }

    private boolean showSystemToast(View toast, int durationMs) {
        if (!canDrawSystemOverlay()) {
            return false;
        }

        try {
            ensureOverlayContainer();
            addToast(overlayContainer, toast, durationMs);
            return true;
        } catch (Exception error) {
            removeOverlayContainer();
            return false;
        }
    }

    private void ensureOverlayContainer() {
        if (overlayAttached && overlayContainer != null) {
            return;
        }

        overlayContainer = new LinearLayout(context);
        overlayContainer.setOrientation(LinearLayout.VERTICAL);
        overlayContainer.setGravity(Gravity.TOP | Gravity.END);
        overlayContainer.setClipChildren(false);
        overlayContainer.setClipToPadding(false);
        overlayContainer.setPadding(dp(18), dp(28), dp(18), 0);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.END;
        windowManager.addView(overlayContainer, params);
        overlayAttached = true;
    }

    private int overlayType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        return WindowManager.LayoutParams.TYPE_PHONE;
    }

    private boolean canDrawSystemOverlay() {
        return windowManager != null
                && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context));
    }

    private void addToast(ViewGroup target, View toast, int durationMs) {
        toast.setAlpha(0f);
        toast.setTranslationX(dp(22));
        toast.setTranslationY(-dp(4));
        toast.setLayoutParams(toastLayoutParams(target));
        target.addView(toast, 0);

        while (target.getChildCount() > MAX_TOASTS) {
            View last = target.getChildAt(target.getChildCount() - 1);
            removeToast(last);
        }

        AnimatorSet slideIn = new AnimatorSet();
        slideIn.playTogether(
                ObjectAnimator.ofFloat(toast, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(toast, View.TRANSLATION_X, dp(22), 0f),
                ObjectAnimator.ofFloat(toast, View.TRANSLATION_Y, -dp(4), 0f)
        );
        slideIn.setDuration(180);
        slideIn.start();

        handler.postDelayed(() -> removeToast(toast), durationMs);
    }

    private ViewGroup.LayoutParams toastLayoutParams(ViewGroup target) {
        int width = Math.min(getScreenWidth() - dp(36), dp(430));
        if (target instanceof LinearLayout) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.END;
            params.setMargins(0, 0, 0, dp(8));
            return params;
        }

        WindowManager.LayoutParams windowParams = null;
        ViewGroup.LayoutParams existing = target.getLayoutParams();
        if (existing instanceof WindowManager.LayoutParams) {
            windowParams = (WindowManager.LayoutParams) existing;
        }
        if (windowParams != null) {
            windowParams.width = width;
            return windowParams;
        }

        return new ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void removeToast(View toast) {
        ViewParent parent = toast.getParent();
        if (!(parent instanceof ViewGroup)) {
            return;
        }

        ViewGroup group = (ViewGroup) parent;
        AnimatorSet slideOut = new AnimatorSet();
        slideOut.playTogether(
                ObjectAnimator.ofFloat(toast, View.ALPHA, toast.getAlpha(), 0f),
                ObjectAnimator.ofFloat(toast, View.TRANSLATION_X, toast.getTranslationX(), dp(20))
        );
        slideOut.setDuration(240);
        slideOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (toast.getParent() == group) {
                    group.removeView(toast);
                }
                if (group == overlayContainer && group.getChildCount() == 0) {
                    removeOverlayContainer();
                }
            }
        });
        slideOut.start();
    }

    private void removeOverlayContainer() {
        if (!overlayAttached || overlayContainer == null || windowManager == null) {
            overlayAttached = false;
            overlayContainer = null;
            return;
        }

        try {
            windowManager.removeView(overlayContainer);
        } catch (Exception ignored) {
        }
        overlayAttached = false;
        overlayContainer = null;
    }

    private GradientDrawable createBackground(String kind) {
        int surface = Color.argb(214, 8, 11, 16);
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{withAlpha(accentForKind(kind), 58), surface, surface}
        );
        drawable.setCornerRadius(dp(8));
        drawable.setStroke(dp(1), Color.argb(32, 255, 255, 255));
        return drawable;
    }

    private int accentForKind(String kind) {
        switch (normalizeKind(kind)) {
            case "file":
                return Color.rgb(143, 125, 255);
            case "play":
            case "pause":
            case "seek":
            case "sync":
                return Color.rgb(255, 198, 109);
            case "left":
                return Color.rgb(255, 107, 122);
            default:
                return Color.rgb(110, 231, 216);
        }
    }

    private int durationForKind(String kind) {
        switch (normalizeKind(kind)) {
            case "file":
                return 5200;
            case "left":
                return 4400;
            case "play":
            case "pause":
            case "seek":
            case "sync":
                return 2800;
            default:
                return 3600;
        }
    }

    private String normalizeKind(String kind) {
        return kind == null || kind.trim().isEmpty() ? "event" : kind.trim().toLowerCase(Locale.US);
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private int getScreenWidth() {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
