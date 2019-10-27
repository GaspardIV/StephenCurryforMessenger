package com.gaspard.otto.stephencurryformessenger;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.util.ArrayDeque;
import java.util.Deque;

public class StephenCurryService extends AccessibilityService {
    FrameLayout mLayout;
    boolean continueShooting = false;
    int mWidth;
    int mHeight;
    private WindowManager mWindowManager;
    private View mChatHeadView;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

    }

    @Override
    public void onInterrupt() {

    }

    private void configureShotButton() {
        final Button powerButton = (Button) mLayout.findViewById(R.id.shot);
        powerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (continueShooting) {
                    powerButton.setText("STOP");
                    continueShooting = false;
                } else {
                    continueShooting = true;
                    startShoting();
                    powerButton.setText("START");

                }

            }
        });
    }

    private boolean shot() {
        Rect screen = new Rect();
        getRootInActiveWindow().getBoundsInScreen(screen);
        Rect ball = findBall();
        Log.d("aaaa", "shot: " + ball);
        Path swipePath = new Path();
        if (ball != null) {
            int ballMiddleX = ball.left + 216 / 2;
            int ballMiddleY = ball.top + 216 / 2;
            int screenMiddleX = screen.right / 2;
            int move = (ballMiddleX - screenMiddleX) / 2;
            swipePath.moveTo(ballMiddleX, ballMiddleY);
            swipePath.lineTo(screenMiddleX + move, 0);
            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath, 10, 30));
            dispatchGesture(gestureBuilder.build(), null, null);
        }
        return ball != null;
    }

    private void startShoting() {
//        final Button powerButton = (Button) mLayout.findViewById(R.id.shot);
        try {

            for (int i = 0; i < 10 && continueShooting; i++) {
                Thread.sleep(1500);
                continueShooting &= shot();
            }

            while (continueShooting) {
                Thread.sleep(3700);
                continueShooting &= shot();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Rect findBall() {
        Deque<AccessibilityNodeInfo> deque = new ArrayDeque<>();
        deque.add(getRootInActiveWindow());
        while (!deque.isEmpty()) {
            AccessibilityNodeInfo node = deque.removeFirst();
            if (node.getPackageName().toString().equals("com.facebook.orca") && node.getClassName().toString().equals("android.widget.ImageView")) {
                Rect r = new Rect();
                node.getBoundsInScreen(r);
                return r;
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                deque.addLast(node.getChild(i));
            }
        }
        return null;
    }

    private AccessibilityNodeInfo findScrollableNode(AccessibilityNodeInfo root) {
        Deque<AccessibilityNodeInfo> deque = new ArrayDeque<>();
        deque.add(root);
        while (!deque.isEmpty()) {
            AccessibilityNodeInfo node = deque.removeFirst();
            if (node.getActionList().contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)) {
                return node;
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                deque.addLast(node.getChild(i));
            }
        }
        return null;
    }

    @Override
    protected void onServiceConnected() {
        createOverlayLayout();
//        configureShotButton();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void createOverlayLayout() {
        mChatHeadView = LayoutInflater.from(this).inflate(R.layout.layout_chat_head, null);

        //Add the view to the window.
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 100;
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mChatHeadView, params);

        //Set the close button.
        ImageView closeButton = (ImageView) mChatHeadView.findViewById(R.id.close_btn);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disableSelf();
            }
        });


        //Drag and move chat head using user's touch action.
        final ImageView chatHeadImage = (ImageView) mChatHeadView.findViewById(R.id.chat_head_profile_iv);


        Display display = mWindowManager.getDefaultDisplay();
        final Point size = new Point();
        display.getSize(size);
        final RelativeLayout layout = mChatHeadView.findViewById(R.id.chat_head_root);
        ViewTreeObserver vto = layout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                layout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int width = layout.getMeasuredWidth();
                int height = layout.getMeasuredHeight();
                mWidth = size.x - width;
                mHeight = size.y - height;
            }
        });

        final GestureDetector flingGestureDetector = new GestureDetector(this, new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return false;
            }

            @Override
            public void onShowPress(MotionEvent e) {

            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {

            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                Log.d("AA", "onFling: " + velocityX + " " + velocityY);
                float xWall = velocityX >= 0 ? mWidth : 0;
                float yWall = velocityY >= 0 ? mHeight : 0;

                if (velocityX != 0) {
                    ValueAnimator xValAnimator = ValueAnimator.ofFloat(params.x, xWall);
                    int distanceX = (int) Math.abs(params.x - xWall);
                    xValAnimator.setDuration((long) (4000 * distanceX / Math.abs(velocityX)));
                    xValAnimator.setInterpolator(new OvershootInterpolator());
                    xValAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        public void onAnimationUpdate(ValueAnimator animation) {
                            params.x = (int) (float) animation.getAnimatedValue();
                            mWindowManager.updateViewLayout(mChatHeadView, params);
                        }
                    });
                    //                xValAnimator.start();
                }

                if (velocityY != 0) {
                    ValueAnimator yValAnimator = ValueAnimator.ofFloat(params.y, yWall);
                    float distanceY = params.y - xWall;
                    yValAnimator.setDuration((long) Math.abs(4000 * distanceY / velocityY));
                    yValAnimator.setInterpolator(new OvershootInterpolator());
                    yValAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        public void onAnimationUpdate(ValueAnimator animation) {
                            if (0 < params.x && params.x < mWidth) {
                                params.y = /*params.y +*/ (int) (float) animation.getAnimatedValue();
                                mWindowManager.updateViewLayout(mChatHeadView, params);
                            } else {
                                animation.cancel();
                            }
                        }
                    });
                    yValAnimator.start();
                }
                return false;
            }
        });

        chatHeadImage.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!flingGestureDetector.onTouchEvent(event)) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            return true;
                        case MotionEvent.ACTION_UP:
                            int middle = mWidth / 2;
                            float nearestXWall = params.x >= middle ? mWidth : 0;
                            ValueAnimator va = ValueAnimator.ofFloat(params.x, nearestXWall);
                            int mDuration = 400; //in millis
                            va.setDuration(mDuration);
//                        va.setInterpolator(new LinearInterpolator());
//                        va.setInterpolator(new AccelerateInterpolator());
//                        va.setInterpolator(new BounceInterpolator());
                            va.setInterpolator(new OvershootInterpolator());
//                        va.setInterpolator(new AnticipateOvershootInterpolator());
                            va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                public void onAnimationUpdate(ValueAnimator animation) {
                                    params.x = (int) (float) animation.getAnimatedValue();
                                    mWindowManager.updateViewLayout(mChatHeadView, params);
                                }
                            });
                            va.start();
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            params.x = initialX + (int) (event.getRawX() - initialTouchX);
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            mWindowManager.updateViewLayout(mChatHeadView, params);
                            return true;
                    }
                }
                return false;
            }
        });
    }
}
