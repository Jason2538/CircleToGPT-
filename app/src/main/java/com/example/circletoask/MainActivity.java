package com.example.circletoask;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.database.*;
import android.graphics.*;
import android.hardware.display.*;
import android.media.*;
import android.media.projection.*;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Circle to Ask: user-triggered screenshot selection and Android image sharing.
 * Requires user consent for every screen capture; no network or screen-reading permissions.
 */
public final class MainActivity extends Activity {
    static final int BG = 0xff0b0d15, PANEL = 0xff202335, ACCENT = 0xff9c74ff;
    static final int WHITE = 0xfff7f5ff, MUTED = 0xffafb2cc;
    static int dp(Context c, float n) { return (int)(c.getResources().getDisplayMetrics().density*n+0.5f); }
    static android.graphics.drawable.GradientDrawable shape(int color, Context c) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(c,16)); return d;
    }
    static TextView label(Context c, String s, float size, int color) {
        TextView v = new TextView(c); v.setText(s); v.setTextColor(color); v.setTextSize(size); return v;
    }
    static TextView button(Context c, String s, int color) {
        TextView v = label(c,s,16,WHITE); v.setGravity(Gravity.CENTER);
        v.setPadding(dp(c,14),dp(c,15),dp(c,14),dp(c,15));
        v.setBackground(shape(color,c)); return v;
    }
    static void gap(LinearLayout l, int h) { l.addView(new View(l.getContext()),new LinearLayout.LayoutParams(1,dp(l.getContext(),h))); }
    static final String PREFS = "circletoask_settings";
    static final String ACTION_SETTINGS = "com.example.circletoask.OPEN_SETTINGS";
    static final String ACTION_CAPTURE = "com.example.circletoask.OPEN_CAPTURE";
    static final String KEY_QUICK_START = "double_press_quick_start";
    TextView status, toggle, quickToggle;

    static void createLauncherShortcuts(Context c) {
        if (Build.VERSION.SDK_INT < 25) return;
        try {
            android.content.pm.ShortcutManager sm=c.getSystemService(android.content.pm.ShortcutManager.class);
            if (sm==null || !sm.isRequestPinShortcutSupported() && sm.getMaxShortcutCountPerActivity()==0) return;
            Intent capture=new Intent(c,MainActivity.class).setAction(ACTION_CAPTURE);
            Intent settings=new Intent(c,MainActivity.class).setAction(ACTION_SETTINGS);
            android.content.pm.ShortcutInfo quick=new android.content.pm.ShortcutInfo.Builder(c,"quick-capture")
                .setShortLabel("바로 검색").setLongLabel("화면 선택 바로 시작")
                .setIcon(android.graphics.drawable.Icon.createWithResource(c,android.R.drawable.ic_menu_search))
                .setIntent(capture).build();
            android.content.pm.ShortcutInfo options=new android.content.pm.ShortcutInfo.Builder(c,"open-options")
                .setShortLabel("앱 설정").setLongLabel("Circle to Ask 설정")
                .setIcon(android.graphics.drawable.Icon.createWithResource(c,android.R.drawable.ic_menu_preferences))
                .setIntent(settings).build();
            sm.setDynamicShortcuts(java.util.Arrays.asList(quick,options));
        } catch (Exception ignored) { /* OEM launcher shortcuts are optional */ }
    }
    boolean shouldQuickStart(Intent i) {
        if (i==null) return false;
        if (ACTION_CAPTURE.equals(i.getAction())) return true;
        if (ACTION_SETTINGS.equals(i.getAction())) return false;
        return Intent.ACTION_MAIN.equals(i.getAction()) &&
                getSharedPreferences(PREFS,MODE_PRIVATE).getBoolean(KEY_QUICK_START,false);
    }
    void openCapture() {
        try {
            Intent i=new Intent(this,Capture.class);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this,"화면 선택을 시작하지 못했습니다.",Toast.LENGTH_LONG).show();
        }
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        createLauncherShortcuts(this);
        if (shouldQuickStart(getIntent())) {
            openCapture();
            finish();
            return;
        }
        LinearLayout root = new LinearLayout(this);
        root.setPadding(dp(this,24),dp(this,40),dp(this,24),dp(this,24));
        root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        ScrollView sc = new ScrollView(this); sc.addView(root); setContentView(sc);
        root.addView(label(this,"◎",64,ACCENT)); gap(root,10);
        root.addView(label(this,"Circle to Ask",29,WHITE)); gap(root,10);
        root.addView(label(this,"다른 앱에서 궁금한 부분을 S펜이나 손가락으로 동그라미 그려 ChatGPT에 보내세요.",16,MUTED));
        gap(root,25);
        TextView steps = label(this,"① 화면 위 표시 허용\n\n② 플로팅 버튼 켜기\n\n③ 다른 앱에서 버튼 누르고 화면 캡처 허용\n\n④ 영역을 둘러 그려 ChatGPT에 보내기",16,WHITE);
        steps.setPadding(dp(this,20),dp(this,20),dp(this,20),dp(this,20));
        steps.setBackground(shape(PANEL,this)); root.addView(steps); gap(root,22);
        status=label(this,"",14,MUTED); root.addView(status); gap(root,14);
        TextView permission=button(this,"다른 앱 위에 표시 권한 설정",PANEL); root.addView(permission);
        permission.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName()))));
        gap(root,10); toggle=button(this,"플로팅 버튼 켜기",ACCENT); root.addView(toggle);
        toggle.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) { Toast.makeText(this,"다른 앱 위에 표시를 허용하세요.",Toast.LENGTH_LONG).show(); return; }
            if (Bubble.running) startService(new Intent(this,Bubble.class).setAction("STOP"));
            else {
                if (Build.VERSION.SDK_INT>=33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)
                    requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},11);
                startForegroundService(new Intent(this,Bubble.class));
            }
            new Handler(getMainLooper()).postDelayed(this::refresh,350);
        });
        gap(root,18);
        TextView heading=label(this,"빠른 실행 및 측면 버튼",20,WHITE);
        root.addView(heading); gap(root,10);
        quickToggle=button(this,"",ACCENT);root.addView(quickToggle);
        quickToggle.setOnClickListener(v->{
            boolean current=getSharedPreferences(PREFS,MODE_PRIVATE).getBoolean(KEY_QUICK_START,false);
            getSharedPreferences(PREFS,MODE_PRIVATE).edit().putBoolean(KEY_QUICK_START,!current).apply();
            refresh();
        });
        gap(root,10);
        TextView guide=label(this,
          "갤럭시 설정 → 유용한 기능 → 측면 버튼 → 두 번 누르기 → 앱 열기 → Circle to Ask를 선택하세요.\n\n" +
          "위 빠른 실행이 켜져 있으면 측면 버튼 두 번 누르기 또는 홈 화면 앱 아이콘에서 화면 선택 동의 창을 바로 엽니다. 앱 아이콘을 길게 눌러 '앱 설정'을 선택하면 이 화면으로 돌아올 수 있습니다.",
          14,MUTED);
        guide.setPadding(dp(this,14),dp(this,15),dp(this,14),dp(this,15));
        guide.setBackground(shape(PANEL,this));root.addView(guide);gap(root,10);
        TextView searchNow=button(this,"지금 화면 선택 시작",ACCENT);root.addView(searchNow);
        searchNow.setOnClickListener(v->openCapture());
        gap(root,22);
        root.addView(label(this,"접근성 버튼 바로가기 (선택)",20,WHITE));gap(root,10);
        root.addView(label(this,"설정 → 접근성 → 설치된 앱에서 Circle to Ask 바로가기를 켜고 접근성 버튼에 할당할 수 있습니다. 이 기능은 화면 내용이나 입력 정보를 읽지 않습니다.",14,MUTED));
        gap(root,10);
        TextView accessibility=button(this,"접근성 설정 열기",PANEL);root.addView(accessibility);
        accessibility.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        gap(root,18);
        root.addView(label(this,"동작 중 화면은 Android 캡처 동의 후 한 장만 가져옵니다. 잠금 화면·보호된 앱은 캡처할 수 없습니다. Google의 시스템 서클 투 서치가 아닌 독립형 도우미 앱입니다.",13,MUTED));
    }
    void refresh() {
        if (status==null) return;
        status.setText((Settings.canDrawOverlays(this)?"● 화면 위 표시 허용됨":"○ 화면 위 표시 권한 필요")+
           (Bubble.running?"   ·   플로팅 버튼 켜짐":"   ·   버튼 꺼짐"));
        toggle.setText(Bubble.running?"플로팅 버튼 끄기":"플로팅 버튼 켜기");
        if (quickToggle!=null) {
            boolean enabled=getSharedPreferences(PREFS,MODE_PRIVATE).getBoolean(KEY_QUICK_START,false);
            quickToggle.setText(enabled?"측면 버튼/앱 아이콘 바로 실행: 켜짐":"측면 버튼/앱 아이콘 바로 실행: 꺼짐");
        }
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (shouldQuickStart(intent)) openCapture();
    }
    @Override protected void onResume() { super.onResume(); refresh(); }

    /**
     * Optional Android Accessibility Button integration. It cannot intercept
     * the Samsung side key or observe any windows: that shortcut is assigned
     * by the OEM Side Button settings instead.
     */
    public static final class AccessShortcut extends android.accessibilityservice.AccessibilityService {
        android.accessibilityservice.AccessibilityButtonController.AccessibilityButtonCallback callback;
        @Override public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent event) { }
        @Override public void onInterrupt() { }
        void trigger() {
            try {
                Intent i=new Intent(this,Capture.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            } catch (Exception e) {
                Toast.makeText(this,"화면 선택을 시작하지 못했습니다.",Toast.LENGTH_LONG).show();
            }
        }
        @Override protected void onServiceConnected() {
            super.onServiceConnected();
            try {
                android.accessibilityservice.AccessibilityServiceInfo info=getServiceInfo();
                if(info!=null){
                    info.flags |= android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON;
                    setServiceInfo(info);
                }
                callback=new android.accessibilityservice.AccessibilityButtonController.AccessibilityButtonCallback() {
                    @Override public void onClicked(android.accessibilityservice.AccessibilityButtonController controller) {
                        trigger();
                    }
                };
                getAccessibilityButtonController().registerAccessibilityButtonCallback(callback,new Handler(getMainLooper()));
            } catch(Exception ignored) { }
        }
        @Override public void onDestroy() {
            if(callback!=null)try {
                getAccessibilityButtonController().unregisterAccessibilityButtonCallback(callback);
            } catch(Exception ignored) { }
            super.onDestroy();
        }
    }

    public static final class Bubble extends Service {
        static volatile boolean running=false;
        WindowManager wm; View bubble; WindowManager.LayoutParams params;
        @Override public void onCreate() {
            super.onCreate();
            NotificationManager nm=getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel("bubble","플로팅 버튼",NotificationManager.IMPORTANCE_LOW));
            Notification n=new Notification.Builder(this,"bubble")
              .setSmallIcon(android.R.drawable.ic_menu_search)
              .setContentTitle("Circle to Ask").setContentText("플로팅 버튼 활성화")
              .setOngoing(true).build();
            if (Build.VERSION.SDK_INT>=34) startForeground(1,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            else startForeground(1,n);
            running=true;
            if (!Settings.canDrawOverlays(this)) { stopSelf(); return; }
            wm=getSystemService(WindowManager.class);
            TextView v=label(this,"◎",34,WHITE); v.setGravity(Gravity.CENTER);
            v.setBackground(shape(ACCENT,this)); v.setElevation(dp(this,8));
            bubble=v;
            int s=dp(this,54);
            params=new WindowManager.LayoutParams(s,s,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
              WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
            params.gravity=Gravity.TOP|Gravity.START;
            params.x=getResources().getDisplayMetrics().widthPixels-dp(this,78);
            params.y=getResources().getDisplayMetrics().heightPixels/3;
            try { wm.addView(bubble,params); } catch(Exception e) { stopSelf();return; }
            bubble.setOnTouchListener(new View.OnTouchListener() {
                float x,y; int ox,oy; boolean moved;
                @Override public boolean onTouch(View view,MotionEvent e) {
                    switch(e.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            x=e.getRawX(); y=e.getRawY(); ox=params.x; oy=params.y; moved=false; return true;
                        case MotionEvent.ACTION_MOVE:
                            int dx=(int)(e.getRawX()-x),dy=(int)(e.getRawY()-y);
                            if(Math.abs(dx)+Math.abs(dy)>dp(Bubble.this,10)) moved=true;
                            if(moved) {
                                params.x=Math.max(0,ox+dx); params.y=Math.max(0,oy+dy);
                                try { wm.updateViewLayout(bubble,params); }catch(Exception ignored){}
                            }
                            return true;
                        case MotionEvent.ACTION_UP:
                            if(!moved) {
                                bubble.setVisibility(View.GONE);
                                Intent launch=new Intent(Bubble.this,Capture.class);
                                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                try { startActivity(launch); }catch(Exception ex){ bubble.setVisibility(View.VISIBLE); }
                            }
                            return true;
                    }
                    return true;
                }
            });
        }
        @Override public int onStartCommand(Intent i,int flags,int id) {
            if(i!=null && "STOP".equals(i.getAction())){stopSelf();return START_NOT_STICKY;}
            if(bubble!=null && i!=null && "SHOW".equals(i.getAction())) bubble.setVisibility(View.VISIBLE);
            return START_STICKY;
        }
        @Override public void onDestroy(){
            running=false;
            if(wm!=null&&bubble!=null) try{wm.removeView(bubble);}catch(Exception ignored){}
            super.onDestroy();
        }
        @Override public IBinder onBind(Intent i){return null;}
    }

    public static final class Capture extends Activity {
        static final String ACTION="com.example.circletoask.RESULT";
        static final String PATH="path";
        BroadcastReceiver receiver;
        boolean selected=false;
        @Override public void onCreate(Bundle b) {
            super.onCreate(b);
            receiver=new BroadcastReceiver(){
                @Override public void onReceive(Context c,Intent i){
                    String path=i.getStringExtra(PATH);
                    if(path!=null){
                        selected=true;
                        startActivity(new Intent(Capture.this,Select.class).putExtra(PATH,path));
                    } else {
                        Toast.makeText(Capture.this,i.getStringExtra("error"),Toast.LENGTH_LONG).show();
                    }
                    finish();
                }
            };
            if(Build.VERSION.SDK_INT>=33) registerReceiver(receiver,new IntentFilter(ACTION),Context.RECEIVER_NOT_EXPORTED);
            else registerReceiver(receiver,new IntentFilter(ACTION));
            try{
                MediaProjectionManager m=getSystemService(MediaProjectionManager.class);
                Intent req=Build.VERSION.SDK_INT>=34
                  ?m.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
                  :m.createScreenCaptureIntent();
                startActivityForResult(req,91);
            }catch(Exception e){finish();}
        }
        @Override protected void onActivityResult(int request,int result,Intent data){
            super.onActivityResult(request,result,data);
            if(request!=91) return;
            if(result!=RESULT_OK || data==null){finish();return;}
            Intent service=new Intent(this,Projection.class);
            service.putExtra("code",result); service.putExtra("token",data);
            try{startForegroundService(service);}catch(Exception e){finish();}
        }
        @Override protected void onDestroy(){
            if(receiver!=null) unregisterReceiver(receiver);
            if(!selected){
                if(Bubble.running)startService(new Intent(this,Bubble.class).setAction("SHOW"));
                stopService(new Intent(this,Projection.class));
            }
            super.onDestroy();
        }
    }

    public static final class Projection extends Service {
        final AtomicBoolean finished=new AtomicBoolean();
        HandlerThread thread; Handler h; MediaProjection projection;
        VirtualDisplay display; ImageReader reader;
        @Override public int onStartCommand(Intent intent,int flags,int startId){
            NotificationManager nm=getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel("capture","화면 캡처",NotificationManager.IMPORTANCE_LOW));
            Notification n=new Notification.Builder(this,"capture")
              .setSmallIcon(android.R.drawable.ic_menu_camera).setContentTitle("Circle to Ask")
              .setContentText("한 장의 화면을 캡처 중").build();
            startForeground(2,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
            if(intent==null){done(null,"캡처 요청이 없습니다.");return START_NOT_STICKY;}
            Intent token=Build.VERSION.SDK_INT>=33?intent.getParcelableExtra("token",Intent.class):intent.getParcelableExtra("token");
            if(token==null){done(null,"캡처 권한 정보가 없습니다.");return START_NOT_STICKY;}
            thread=new HandlerThread("screen-capture");thread.start();h=new Handler(thread.getLooper());
            h.postDelayed(()->take(token),450);
            h.postDelayed(()->done(null,"화면 캡처 시간이 초과되었습니다."),10000);
            return START_NOT_STICKY;
        }
        void take(Intent token){
            if(finished.get())return;
            try{
                MediaProjectionManager mgr=getSystemService(MediaProjectionManager.class);
                projection=mgr.getMediaProjection(Activity.RESULT_OK,token);
                if(projection==null){done(null,"캡처 권한을 얻지 못했습니다.");return;}
                projection.registerCallback(new MediaProjection.Callback(){
                    @Override public void onStop(){done(null,"화면 공유가 중단되었습니다.");}
                },h);
                android.util.DisplayMetrics dm=new android.util.DisplayMetrics();
                getSystemService(WindowManager.class).getDefaultDisplay().getRealMetrics(dm);
                final int w=dm.widthPixels, ht=dm.heightPixels;
                reader=ImageReader.newInstance(w,ht,PixelFormat.RGBA_8888,2);
                reader.setOnImageAvailableListener(r->{
                    if(finished.get())return;
                    Image img=null;Bitmap raw=null,cropped=null;
                    try{
                        img=r.acquireLatestImage();if(img==null)return;
                        Image.Plane plane=img.getPlanes()[0];
                        ByteBuffer buf=plane.getBuffer();
                        int stride=plane.getRowStride(),px=plane.getPixelStride();
                        int padded=w+(stride-px*w)/px;
                        raw=Bitmap.createBitmap(padded,ht,Bitmap.Config.ARGB_8888);
                        raw.copyPixelsFromBuffer(buf);
                        cropped=Bitmap.createBitmap(raw,0,0,w,ht);
                        File out=new File(getCacheDir(),"screen.png");
                        try(FileOutputStream os=new FileOutputStream(out)){
                            cropped.compress(Bitmap.CompressFormat.PNG,100,os);
                        }
                        done(out.getAbsolutePath(),null);
                    }catch(Exception ex){done(null,"이미지를 저장하지 못했습니다.");}
                    finally{
                        if(img!=null)img.close();
                        if(cropped!=null)cropped.recycle();
                        if(raw!=null)raw.recycle();
                    }
                },h);
                display=projection.createVirtualDisplay("CircleToAsk",w,ht,dm.densityDpi,
                  DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader.getSurface(),null,h);
            }catch(Exception e){done(null,"화면 캡처에 실패했습니다: "+e.getClass().getSimpleName());}
        }
        void done(String path,String error){
            if(!finished.compareAndSet(false,true))return;
            Intent result=new Intent(Capture.ACTION).setPackage(getPackageName());
            if(path!=null)result.putExtra(Capture.PATH,path);
            if(error!=null)result.putExtra("error",error);
            sendBroadcast(result);stopSelf();
        }
        @Override public void onDestroy(){
            if(display!=null)display.release();
            if(reader!=null)reader.close();
            if(projection!=null)projection.stop();
            if(thread!=null)thread.quitSafely();
            super.onDestroy();
        }
        @Override public IBinder onBind(Intent i){return null;}
    }

    static final class Region extends View {
        final Bitmap image; final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        final Paint outline=new Paint(Paint.ANTI_ALIAS_FLAG),tint=new Paint(Paint.ANTI_ALIAS_FLAG);
        final Path shape=new Path(); final RectF bounds=new RectF();
        boolean drawing=false, selected=false;
        Region(Context c,Bitmap b){
            super(c);image=b;setBackgroundColor(BG);
            outline.setColor(ACCENT);outline.setStrokeWidth(dp(c,3));outline.setStyle(Paint.Style.STROKE);
            outline.setStrokeCap(Paint.Cap.ROUND);outline.setStrokeJoin(Paint.Join.ROUND);
            tint.setColor(0x339c74ff);setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        }
        @Override protected void onDraw(Canvas c){
            float scale=Math.min(getWidth()/(float)image.getWidth(),getHeight()/(float)image.getHeight());
            float w=image.getWidth()*scale,ht=image.getHeight()*scale;
            bounds.set((getWidth()-w)/2,(getHeight()-ht)/2,(getWidth()+w)/2,(getHeight()+ht)/2);
            c.drawBitmap(image,null,bounds,paint);
            if(selected||drawing){if(selected)c.drawPath(shape,tint);c.drawPath(shape,outline);}
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            float x=e.getX(),y=e.getY();
            int action=e.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN){
                if(!bounds.contains(x,y))return false;
                shape.reset();shape.moveTo(x,y);drawing=true;selected=false;invalidate();return true;
            }
            if(!drawing)return false;
            x=Math.max(bounds.left,Math.min(bounds.right,x));
            y=Math.max(bounds.top,Math.min(bounds.bottom,y));
            if(action==MotionEvent.ACTION_MOVE){
                for(int i=0;i<e.getHistorySize();i++)shape.lineTo(e.getHistoricalX(i),e.getHistoricalY(i));
                shape.lineTo(x,y);invalidate();return true;
            }
            if(action==MotionEvent.ACTION_UP){
                shape.lineTo(x,y);shape.close();drawing=false;
                RectF b=new RectF();shape.computeBounds(b,true);
                selected=b.width()>dp(getContext(),16)&&b.height()>dp(getContext(),16);
                invalidate();return true;
            }
            if(action==MotionEvent.ACTION_CANCEL){clear();return true;}
            return true;
        }
        void clear(){shape.reset();selected=false;drawing=false;invalidate();}
        Bitmap cut(boolean all){
            if(all)return image.copy(Bitmap.Config.ARGB_8888,false);
            if(!selected)return null;
            Matrix m=new Matrix();
            m.setRectToRect(bounds,new RectF(0,0,image.getWidth(),image.getHeight()),Matrix.ScaleToFit.FILL);
            Path p=new Path(shape);p.transform(m);
            RectF b=new RectF();p.computeBounds(b,true);
            int left=Math.max(0,(int)Math.floor(b.left)-8),top=Math.max(0,(int)Math.floor(b.top)-8);
            int right=Math.min(image.getWidth(),(int)Math.ceil(b.right)+8);
            int bottom=Math.min(image.getHeight(),(int)Math.ceil(b.bottom)+8);
            if(left>=right||top>=bottom)return null;
            Bitmap out=Bitmap.createBitmap(right-left,bottom-top,Bitmap.Config.ARGB_8888);
            Canvas c=new Canvas(out);c.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR);
            c.translate(-left,-top);c.clipPath(p);c.drawBitmap(image,0,0,paint);
            return out;
        }
    }

    public static final class Select extends Activity {
        Region region;EditText question;
        @Override public void onCreate(Bundle state){
            super.onCreate(state);
            String path=getIntent().getStringExtra(Capture.PATH);
            Bitmap bmp=path==null?null:BitmapFactory.decodeFile(path);
            if(bmp==null){finish();return;}
            LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
            root.setBackgroundColor(BG);setContentView(root);
            TextView title=label(this,"◎  S펜으로 원하는 부분을 둘러 그리세요",17,WHITE);
            title.setPadding(dp(this,16),dp(this,17),0,dp(this,12));root.addView(title);
            region=new Region(this,bmp);root.addView(region,new LinearLayout.LayoutParams(-1,0,1f));
            LinearLayout footer=new LinearLayout(this);footer.setOrientation(LinearLayout.VERTICAL);
            footer.setPadding(dp(this,16),dp(this,14),dp(this,16),dp(this,18));
            footer.setBackground(shape(PANEL,this));root.addView(footer);
            question=new EditText(this);
            question.setSingleLine(false);question.setMaxLines(2);question.setTextSize(15);
            question.setTextColor(WHITE);question.setHintTextColor(MUTED);
            question.setHint("질문 입력 (예: 이 과학 개념을 설명해줘)");
            footer.addView(question);gap(footer,10);
            LinearLayout row=new LinearLayout(this);footer.addView(row);
            TextView clear=button(this,"다시 그리기",BG);
            row.addView(clear,new LinearLayout.LayoutParams(0,-2,1f));
            clear.setOnClickListener(v->region.clear());
            TextView send=button(this,"ChatGPT에 보내기",ACCENT);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,2f);p.leftMargin=dp(this,8);
            row.addView(send,p);send.setOnClickListener(v->share(false));
            gap(footer,8);
            TextView all=label(this,"전체 화면 공유하기",14,MUTED);
            all.setGravity(Gravity.CENTER);all.setPadding(0,dp(this,6),0,dp(this,6));footer.addView(all);
            all.setOnClickListener(v->share(true));
        }
        void share(boolean all){
            if(!all&&!region.selected){
                Toast.makeText(this,"먼저 영역을 동그라미로 선택하세요.",Toast.LENGTH_SHORT).show();return;
            }
            Bitmap crop=null;
            try{
                crop=region.cut(all);if(crop==null)return;
                File dir=new File(getCacheDir(),"captures");if(!dir.exists())dir.mkdirs();
                File image=new File(dir,"selected_"+System.currentTimeMillis()+".png");
                try(FileOutputStream os=new FileOutputStream(image)){crop.compress(Bitmap.CompressFormat.PNG,100,os);}
                Uri uri=new Uri.Builder().scheme("content").authority(getPackageName()+".images")
                    .appendPath(image.getName()).build();
                String prompt=question.getText().toString().trim();
                if(prompt.isEmpty())prompt="이미지의 선택 영역을 쉽게 설명해줘.";
                Intent send=new Intent(Intent.ACTION_SEND);
                send.setType("image/png");send.putExtra(Intent.EXTRA_STREAM,uri);
                send.putExtra(Intent.EXTRA_TEXT,prompt);
                send.setClipData(ClipData.newRawUri("선택 이미지",uri));
                send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Intent target=new Intent(send).setPackage("com.openai.chatgpt");
                try{
                    grantUriPermission("com.openai.chatgpt",uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(target);
                }catch(android.content.ActivityNotFoundException missing){
                    startActivity(Intent.createChooser(send,"이미지 공유할 앱 선택"));
                }
                finish();
            }catch(Exception e){
                Toast.makeText(this,"이미지 공유에 실패했습니다.",Toast.LENGTH_LONG).show();
            }finally{if(crop!=null)crop.recycle();}
        }
        @Override protected void onDestroy(){
            if(Bubble.running) startService(new Intent(this,Bubble.class).setAction("SHOW"));
            new File(getCacheDir(),"screen.png").delete();
            super.onDestroy();
        }
    }

    public static final class ImageProvider extends ContentProvider {
        File resolve(Uri u)throws FileNotFoundException{
            String n=u.getLastPathSegment();
            if(n==null || !n.matches("selected_[0-9]+\\.png"))throw new FileNotFoundException();
            File f=new File(new File(getContext().getCacheDir(),"captures"),n);
            if(!f.isFile())throw new FileNotFoundException();
            return f;
        }
        @Override public boolean onCreate(){return true;}
        @Override public String getType(Uri u){return "image/png";}
        @Override public android.os.ParcelFileDescriptor openFile(Uri u,String mode)throws FileNotFoundException{
            if(!"r".equals(mode))throw new FileNotFoundException();
            return android.os.ParcelFileDescriptor.open(resolve(u),android.os.ParcelFileDescriptor.MODE_READ_ONLY);
        }
        @Override public Cursor query(Uri u,String[] projection,String sel,String[] args,String sort){
            try{
                File f=resolve(u);
                String[] cols=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;
                MatrixCursor c=new MatrixCursor(cols);Object[] values=new Object[cols.length];
                for(int i=0;i<cols.length;i++){
                    if(OpenableColumns.DISPLAY_NAME.equals(cols[i]))values[i]=f.getName();
                    if(OpenableColumns.SIZE.equals(cols[i]))values[i]=f.length();
                }
                c.addRow(values);return c;
            }catch(Exception e){return null;}
        }
        @Override public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
        @Override public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
        @Override public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
    }
}
