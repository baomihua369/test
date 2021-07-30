package com.example.Camera_CS;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    //组件
    private TextureView mPreviewView;
    private LinearLayout linearLayout;
    private LinearLayout timelinearLatout;
    private ImageView image_flip;
    private ImageView image_turn;
    private ImageView image_take;
    private ImageView Photo_album;
    private Button btn_takePic;
    private Button btn_takeVideo;
    private Button btn_OneToOne;
    private Button btn_FourToThree;
    private Button btn_SixteenToNine;
    private Button btn_Full;
    private Chronometer chronometer;

    //线程
    private HandlerThread handlerThread;
    private Handler mCameraHandler;

    //全局变量
    private boolean judgePoV = true;
    private boolean judgeSoS = true;
    private String mCameraId = "0";
    private CameraManager manager;
    private CameraDevice device;
    private StreamConfigurationMap map;
    private Size mPreviewSize;//预览Size
    private List<Size> ratioFourToThreeList = new ArrayList<>();//按照不同比例将Size放到不同集合
    private List<Size> ratioSixteenToNineList = new ArrayList<>();
    private List<Size> ratioOneToOneList = new ArrayList<>();
    private List<Size> ratioFullList = new ArrayList<>();
    private double windowManagerWidth = 0;
    private double windowManagerHeight = 0;
    private Size defaultSize;
    private Size mCaptureSize;//合适的拍照尺寸
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CameraCaptureSession mCameraCaptureSession;
    private CaptureRequest mCaptureRequest;
    private ImageReader mImageReader;
    private OrientationEventListener orientationEventListener;
    int rotation;
    int mOrientation;
    private MediaRecorder mMediaRecorder;
    private String mNextVideoAbsolutePath;
    String path;
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mPreviewSession;
    private String fileName;
    private String pictureFileName;
    private String videoFileName;
    private String timeStamp;
    private List<Size> ratioList = ratioFullList;
    private Size onetoOneSize;
    private Size fullSize;
    //后置摄像头旋转角
    private static final SparseArray ORIEBTATION = new SparseArray();
    private static final SparseArray ORIEBTATION_FACING = new SparseArray();

    static {
        ORIEBTATION.append(Surface.ROTATION_0, 90);
        ORIEBTATION.append(Surface.ROTATION_90, 0);
        ORIEBTATION.append(Surface.ROTATION_180, 270);
        ORIEBTATION.append(Surface.ROTATION_270, 180);

        ORIEBTATION_FACING.append(Surface.ROTATION_0, 270);
        ORIEBTATION_FACING.append(Surface.ROTATION_90, 0);
        ORIEBTATION_FACING.append(Surface.ROTATION_180, 90);
        ORIEBTATION_FACING.append(Surface.ROTATION_270, 180);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //去掉Activity上面的状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);


        //传感器监听
        orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                mOrientation = orientation;
            }
        };
        orientationEventListener.enable();
        initView();//绑定按键
    }

    private void initView() {
        mPreviewView = (TextureView) findViewById(R.id.textureView);
        linearLayout = (LinearLayout) findViewById(R.id.linearlayout);
        timelinearLatout = (LinearLayout) findViewById(R.id.L_Time);
        image_flip = (ImageView) findViewById(R.id.image_flip);
        image_take = (ImageView) findViewById(R.id.image_take);
        Photo_album = (ImageView) findViewById(R.id.Photo_album);
        image_turn = (ImageView) findViewById(R.id.img_turn);
        btn_takePic = (Button) findViewById(R.id.btn_takepicture);
        btn_takeVideo = (Button) findViewById(R.id.btn_video);
        btn_OneToOne = (Button) findViewById(R.id.btn_OneToOne);
        btn_FourToThree = (Button) findViewById(R.id.btn_FourToThree);
        btn_SixteenToNine = (Button) findViewById(R.id.btn_SixteenToNine);
        btn_Full = (Button) findViewById(R.id.btn_Full);
        chronometer = (Chronometer) findViewById(R.id.chronometer);
//        chronometer.setFormat("%s");
        image_flip.setOnClickListener(this);
        image_take.setOnClickListener(this);
        btn_takePic.setOnClickListener(this);
        btn_takeVideo.setOnClickListener(this);
        btn_OneToOne.setOnClickListener(this);
        btn_FourToThree.setOnClickListener(this);
        btn_SixteenToNine.setOnClickListener(this);
        btn_Full.setOnClickListener(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onResume() {
        super.onResume();
        setFileName();
        setFolderPath(path);
        startThread();
        if (!mPreviewView.isAvailable()) {
            mPreviewView.setSurfaceTextureListener(mSurfaceTextureListener);
        } else {
            openCamera();
        }
    }

    private void setFileName() {
        path = Environment.getExternalStorageDirectory() + "/DCIM/CameraV2/";
        timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    }

    public void setFolderPath(String path) {
        File mFolder = new File(path);
        if (!mFolder.exists()) {
            mFolder.mkdirs();
            Log.d("sunsun", "文件夹不存在去创建");
        } else {
            Log.d("sunsun", "文件夹已创建");
        }
    }

    private void startThread() {
        handlerThread = new HandlerThread("cameraThread");
        handlerThread.start();
        mCameraHandler = new Handler(handlerThread.getLooper());
    }

    TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

        }
    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void openCamera() {
        setupCamera();
        String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO};
        int i = 0;
        for (String permission : permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                this.requestPermissions(permissions, i++);
                return;
            }
        }
        try {
            manager.openCamera(mCameraId, mStateCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /***
     *
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setupCamera() {
        WindowManager windowManager = (WindowManager) this
                .getSystemService(Context.WINDOW_SERVICE);
        windowManagerWidth = windowManager.getDefaultDisplay().getWidth();
        windowManagerHeight = windowManager.getDefaultDisplay().getHeight();
        onetoOneSize = new Size((int) windowManagerWidth, (int) windowManagerWidth);
        fullSize = new Size((int) windowManagerWidth, (int) windowManagerHeight);
//        ratioOneToOneList.add(onetoOneSize);
        manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        CameraCharacteristics characteristics = null;
        try {
            characteristics = manager.getCameraCharacteristics(mCameraId);
            map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        mPreviewSize = getOptimalSize(map.getOutputSizes(SurfaceTexture.class));
        mCaptureSize = Collections.max(ratioList, new Comparator<Size>() {
            @Override
            public int compare(Size o1, Size o2) {
                return Long.signum(o1.getWidth() * o1.getHeight() - o2.getWidth() * o2.getHeight());
            }
        });
        changeSurfaceWidthAndHeight(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        setupImageReader();
    }

    /***
     *
     * @param sizeMap
     * @return
     */
    private Size getOptimalSize(Size[] sizeMap) {
        for (Size size : sizeMap) {
            if ((double) size.getWidth() / size.getHeight() == Constants.Size.RATIO_4_3) {
                ratioFourToThreeList.add(size);
            } else if ((double) size.getWidth() / size.getHeight() == Constants.Size.RATIO_16_9) {
                ratioSixteenToNineList.add(size);
            } else if ((double) size.getWidth() / size.getHeight() == Constants.Size.RATIO_1_1) {
                ratioOneToOneList.add(size);
            }
        }
        ratioFullList.add(FullSizeChoose(sizeMap,fullSize.getWidth(),fullSize.getHeight()));

        if (ratioFourToThreeList.size()==0){
            btn_FourToThree.setVisibility(View.INVISIBLE);
        }else{
            btn_FourToThree.setVisibility(View.VISIBLE);
        }

        if (ratioSixteenToNineList.size()==0){
            btn_SixteenToNine.setVisibility(View.INVISIBLE);
        }else{
            btn_SixteenToNine.setVisibility(View.VISIBLE);
        }

        if (ratioOneToOneList.size()==0){
            btn_OneToOne.setVisibility(View.INVISIBLE);
        }else{
            btn_OneToOne.setVisibility(View.VISIBLE);
        }

        if (ratioFourToThreeList.size()==0){
            btn_FourToThree.setVisibility(View.INVISIBLE);
        }else{
            btn_FourToThree.setVisibility(View.VISIBLE);
        }
        Log.d("sunsun(List)", "" + Arrays.toString(sizeMap));
        Log.d("sunsun(FourToThree)", ratioFourToThreeList.toString());
        Log.d("sunsun(SixteenToNine)", ratioSixteenToNineList.toString());
        Log.d("sunsun(oneToOne)", ratioOneToOneList.toString());
        for (Size size : ratioList) {
            if (size.getHeight() == windowManagerWidth) {
                Log.d("sunsun(oneToOne)", size.getHeight() + "   " + windowManagerWidth);

                defaultSize = size;
                Log.d("sunsun(defaultSize)", defaultSize.toString());
            }
        }
        return defaultSize;
    }


    private Size FullSizeChoose(Size[] sizeMap,int width,int height){
        List<Size> sizeList = new ArrayList<>();
        for (Size option : sizeMap) {

            //如果宽大于高,手机是横屏
            if (width > height) {
                if (option.getWidth() >= width && option.getHeight() >= height) {
                    sizeList.add(option);
                }
            } else {//竖屏
                if (option.getWidth() >= height && option.getHeight() >= width) {
                    sizeList.add(option);
                }
            }
        }

        if (sizeList.size() > 1) {
            return Collections.min(sizeList, new Comparator<Size>() {
                @Override
                public int compare(Size o1, Size o2) {
                    return Long.signum(o1.getWidth() * o1.getHeight() - o2.getWidth() * o2.getHeight());
                }
            });
        }
        return sizeList.get(0);
    }

    //改变预览尺寸,重新打开预览
    private void restartPreview() {
        for (Size size : ratioList) {
            if (size.getHeight() == windowManagerWidth) {
                mPreviewSize = size;
            }
        }

        if (mPreviewSize == null && ratioList != null && ratioList.size() != 0) {
            mPreviewSize = ratioList.get(0);
        }

        mCaptureSize = Collections.max(ratioList, new Comparator<Size>() {
            @Override
            public int compare(Size o1, Size o2) {
                return Long.signum(o1.getWidth() * o1.getHeight() - o2.getWidth() * o2.getHeight());
            }
        });

        if (ratioList == ratioOneToOneList) {
            changeSurfaceWidthAndHeight(onetoOneSize.getWidth(), onetoOneSize.getHeight());
        } else {
            changeSurfaceWidthAndHeight(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        }

//        changeSurfaceWidthAndHeight(mPreviewSize.getWidth(),mPreviewSize.getHeight());
        Log.v("sunsunrestart1", mPreviewSize.toString() + "  " + linearLayout.toString());
        startPreview();
    }

    /***
     *
     * @param height
     * @param width
     */
    private void changeSurfaceWidthAndHeight(int height, int width) {

        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) linearLayout.getLayoutParams();
        params.height = height;
        params.width = width;
        Log.d("sunsun(params)", params.height + "  " + params.width);
        linearLayout.setLayoutParams(params);
        Log.d("sunsun(linearLayout)", linearLayout.getWidth() + "  "
                + linearLayout.getHeight() + "  "
                + mPreviewSize.getWidth() + "  "
                + mPreviewSize.getHeight());

    }

    CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            device = camera;
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            device.close();
            device = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            device.close();
            device = null;
        }
    };

    /***
     *
     */
    private void startPreview() {
        SurfaceTexture mSurfaceTexture = mPreviewView.getSurfaceTexture();
        mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previewSurface = new Surface(mSurfaceTexture);

        try {
            mCaptureRequestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);
            device.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        mCameraCaptureSession = session;
                        mCaptureRequest = mCaptureRequestBuilder.build();
                        mCameraCaptureSession.setRepeatingRequest(mCaptureRequest, null, mCameraHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    /***
     * takepicture
     */
    private void takePicture() {
        Capture();
    }


    private void Capture() {
        //设置一个拍照用的bulider
        CaptureRequest.Builder mTakePicRequestBuilder = null;
        try {
            mTakePicRequestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        mTakePicRequestBuilder.addTarget(mImageReader.getSurface());
        //获取摄像头方向
        if (mOrientation >= 45 && mOrientation < 135) {
            rotation = 3;
        } else if (mOrientation >= 135 && mOrientation < 225) {
            rotation = 2;
        } else if (mOrientation >= 225 && mOrientation < 315) {
            rotation = 1;
        } else {
            rotation = 0;
        }
        if ("0".equals(mCameraId)) {
            //设置拍照方向
            mTakePicRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, (Integer) ORIEBTATION.get(rotation));
        } else if ("1".equals(mCameraId)) {
            mTakePicRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, (Integer) ORIEBTATION_FACING.get(rotation));
        }
        CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                Toast.makeText(getApplicationContext(), "拍照结束,照片已保存", Toast.LENGTH_LONG).show();
                unLockFocus();
                super.onCaptureCompleted(session, request, result);
            }
        };
        try {
            mCameraCaptureSession.capture(mTakePicRequestBuilder.build(), mCaptureCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void setupImageReader() {
        mImageReader = ImageReader.newInstance(mCaptureSize.getWidth(), mCaptureSize.getHeight(), ImageFormat.JPEG, 2);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                mCameraHandler.post(new ImageSaver(reader.acquireNextImage()));
            }
        }, mCameraHandler);
    }

    /***
     * 缩略图
     */
    @SuppressLint("HandlerLeak")
    private Handler mhandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (null != msg) {
                switch (msg.what) {
                    case 1:
                        Matrix matrix = new Matrix();
                        if (mCameraId == "1") {
                            if (mOrientation > 315 || mOrientation < 45) {
                                matrix.setRotate(270);
                            } else {
                                matrix.setRotate(90);
                            }
                        } else {
                            matrix.setRotate(90);
                        }
                        // 配置压缩的参数
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true; //获取当前图片的边界大小，而不是将整张图片载入在内存中，避免内存溢出
                        BitmapFactory.decodeFile(fileName, options);
                        options.inJustDecodeBounds = false;
                        ////inSampleSize的作用就是可以把图片的长短缩小inSampleSize倍，所占内存缩小inSampleSize的平方
                        options.inSampleSize = caculateSampleSize(options, 600, 600);

                        Bitmap bm = BitmapFactory.decodeFile(fileName, options); // 解码文件

                        bm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
                        Bitmap Fbm = toRoundBitmap(bm);
                        Photo_album.setVisibility(View.VISIBLE);
                        Photo_album.setImageBitmap(Fbm);
                        break;
                }
            }
        }
    };

    public static Bitmap toRoundBitmap(Bitmap bitmap) {
        // 前面同上，绘制图像分别需要bitmap，canvas，paint对象
        bitmap = Bitmap.createScaledBitmap(bitmap, 500, 500, true);
        Bitmap bm = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // 这里需要先画出一个圆
        canvas.drawCircle(250, 250, 250, paint);
        // 圆画好之后将画笔重置一下
        paint.reset();
        // 设置图像合成模式，该模式为只在源图像和目标图像相交的地方绘制源图像
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return bm;
    }

    private void unLockFocus() {
        try {
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            mCameraCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /***
     * 缩略图尺寸
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private int caculateSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int sampleSize = 1;
        int picWidth = options.outWidth;
        int picHeight = options.outHeight;
        if (picWidth > reqWidth || picHeight > reqHeight) {
            int halfPicWidth = picWidth / 2;
            int halfPicHeight = picHeight / 2;
            while (halfPicWidth / sampleSize > reqWidth || halfPicHeight / sampleSize > reqHeight) {
                sampleSize *= 2;
            }
        }
        return sampleSize;
    }


    /***
     * 照片储存
     */
    private class ImageSaver implements Runnable {
        Image mImage;

        public ImageSaver(Image image) {
            this.mImage = image;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            File mImageFile = new File(path);
            if (!mImageFile.exists()) {
                mImageFile.mkdir();
            }
            pictureFileName = path + "IMG_" + timeStamp + ".JPEG";
            fileName = pictureFileName;
            try {
                FileOutputStream fos = new FileOutputStream(pictureFileName);
                fos.write(data, 0, data.length);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
            } finally {
                mImage.close();
            }
            mhandler.sendEmptyMessage(1);
        }
    }


    /***
     * video
     */
    private void takeVideo() {
        startRecordingVideo();
    }

    private void startRecordingVideo() {
        if (null == device || !mPreviewView.isAvailable() || null == mPreviewSize) {
            return;
        }

        try {
            closePreviewSession();
            setUpMediaRecorder();
            SurfaceTexture texture = mPreviewView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<>();
            //为相机预览设置曲面
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mPreviewBuilder.addTarget(previewSurface);
            //设置MediaRecorder的表面
            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            mPreviewBuilder.addTarget(recorderSurface);
            chronometer.setBase(SystemClock.elapsedRealtime());
            chronometer.start();// 开始计时
            // 启动捕获会话
            // 一旦会话开始，我们就可以更新UI并开始录制
            device.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mPreviewSession = session;
                    updatePreview();
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMediaRecorder.start();
                        }
                    });
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                }
            }, mCameraHandler);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    /***
     *setUpMediaRecorder
     */
    private void setUpMediaRecorder() throws IOException {
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC); //设置用于录制的音源
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);//开始捕捉和编码数据到setOutputFile（指定的文件）
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); //设置在录制过程中产生的输出文件的格式
        if (mNextVideoAbsolutePath == null || mNextVideoAbsolutePath.isEmpty()) {
            mNextVideoAbsolutePath = getVideoFilePath();
            fileName = mNextVideoAbsolutePath;
        }
        mMediaRecorder.setOutputFile(mNextVideoAbsolutePath);//设置输出文件的路径
        mMediaRecorder.setVideoEncodingBitRate(10000000);//设置录制的视频编码比特率
        mMediaRecorder.setVideoFrameRate(25);//设置要捕获的视频帧速率
        mMediaRecorder.setVideoSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());//设置要捕获的视频的宽度和高度
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);//设置视频编码器，用于录制
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);//设置audio的编码格式
        int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
        Log.d("sunsun(Recorder)", "setUpMediaRecorder: " + rotation);

        //获取摄像头方向
        if (mOrientation >= 45 && mOrientation < 135) {
            rotation = 3;
        } else if (mOrientation >= 135 && mOrientation < 225) {
            rotation = 2;
        } else if (mOrientation >= 225 && mOrientation < 315) {
            rotation = 1;
        } else {
            rotation = 0;
        }

        if ("0".equals(mCameraId)) {
            //设置拍照方向
            mMediaRecorder.setOrientationHint((Integer) ORIEBTATION.get(rotation));
        } else if ("1".equals(mCameraId)) {
            mMediaRecorder.setOrientationHint((Integer) ORIEBTATION_FACING.get(rotation));
        }
        mMediaRecorder.prepare();
    }

    private String getVideoFilePath() {
        videoFileName = path + "VIDEO_" + timeStamp + ".MP4";
        return videoFileName;
    }

    private void updatePreview() {
        if (null == device) {
            return;
        }
        try {
            mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void stopVideo() {
        stopRecordingVideo();
    }

    private void stopRecordingVideo() {
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        mNextVideoAbsolutePath = null;
        bitmapVideo();
        startPreview();
    }

    private void bitmapVideo() {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            //根据url获取缩略图
            retriever.setDataSource(fileName);
            //获得第一帧图片
            bitmap = retriever.getFrameAtTime(1);
            Photo_album.setImageBitmap(toRoundBitmap(bitmap));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } finally {
            retriever.release();
        }
    }

    private void closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession.close();
            mPreviewSession = null;
        }
    }

    private void setVisibility() {
        if (judgeSoS) {
            image_flip.setVisibility(View.INVISIBLE);
            image_turn.setVisibility(View.INVISIBLE);
            btn_takePic.setVisibility(View.INVISIBLE);
            btn_takeVideo.setVisibility(View.INVISIBLE);
            Photo_album.setVisibility(View.INVISIBLE);
            timelinearLatout.setVisibility(View.VISIBLE);
        } else {
            image_flip.setVisibility(View.VISIBLE);
            image_turn.setVisibility(View.VISIBLE);
            btn_takePic.setVisibility(View.VISIBLE);
            btn_takeVideo.setVisibility(View.VISIBLE);
            Photo_album.setVisibility(View.VISIBLE);
            timelinearLatout.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
        if (device != null) {
            device.close();
            device = null;
        }
    }

    /***
     *
     * @param v
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.image_flip:
                if (mCameraId == "0") {
                    mCameraId = "1";
                } else {
                    mCameraId = "0";
                }
                if (mCameraCaptureSession != null) {
                    mCameraCaptureSession.close();
                    mCameraCaptureSession = null;
                }
                if (device != null) {
                    device.close();
                    device = null;
                }
                ratioOneToOneList.clear();
                ratioFourToThreeList.clear();
                ratioSixteenToNineList.clear();
                openCamera();
                break;
            case R.id.image_take:
                if (judgePoV) {
                    takePicture();
                } else {
                    if (judgeSoS) {
                        Toast.makeText(this, "录像开始", Toast.LENGTH_SHORT).show();
                        setVisibility();
                        judgeSoS = false;
                        //开始摄像

                        takeVideo();

                    } else {
                        Toast.makeText(this, "录像结束", Toast.LENGTH_SHORT).show();
                        setVisibility();
                        judgeSoS = true;
                        //停止摄像
                        chronometer.setBase(SystemClock.elapsedRealtime());// 结束计时

                        stopVideo();

                        fileName = videoFileName;
                    }
                }
                break;
            case R.id.btn_takepicture:
                btn_Full.setVisibility(View.VISIBLE);
                btn_SixteenToNine.setVisibility(View.VISIBLE);
                btn_FourToThree.setVisibility(View.VISIBLE);
                btn_OneToOne.setVisibility(View.VISIBLE);
                judgePoV = true;
                image_take.setBackgroundDrawable(getResources().getDrawable(R.drawable.yuanhuan));
                break;
            case R.id.btn_video:
                btn_Full.setVisibility(View.INVISIBLE);
                btn_SixteenToNine.setVisibility(View.INVISIBLE);
                btn_FourToThree.setVisibility(View.INVISIBLE);
                btn_OneToOne.setVisibility(View.INVISIBLE);
                judgePoV = false;
                image_take.setBackgroundDrawable(getResources().getDrawable(R.drawable.redyuanhuan));
                break;

            case R.id.btn_OneToOne:
                mPreviewSize = null;
                if (ratioList == ratioOneToOneList) {
                    return;
                }
                    ratioList = ratioOneToOneList;
                Log.v("sunsunrestart", ratioList.toString() + "  " + ratioOneToOneList.toString());
                restartPreview();
                break;
            case R.id.btn_FourToThree:
                mPreviewSize = null;
                if (ratioList == ratioFourToThreeList) {
                    return;
                }
                ratioList = ratioFourToThreeList;
                restartPreview();
                break;
            case R.id.btn_SixteenToNine:
                mPreviewSize = null;

                if (ratioList == ratioSixteenToNineList) {
                    return;
                }
                ratioList = ratioSixteenToNineList;
                restartPreview();
                break;
            case R.id.btn_Full:
                mPreviewSize = null;
                if (ratioList == ratioFullList) {
                    return;
                }
                ratioList = ratioFullList;
                restartPreview();
                break;
        }
    }
}
