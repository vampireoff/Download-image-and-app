package com.example.loadfile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener{
	
	private static final String LOG_TAG="Download_Progressbar";
	String imgHttp1="http://pic19.nipic.com/20120320/2814373_114247218000_2.jpg";
	String fileurl="http://download.alicdn.com/wireless/taobao4android/latest/702757.apk?spm=0.0.0.0.l9eiHo&file=702757.apk";
	private TextView txt;
	private Button downImg, downFile, tonew;
	private ImageView imgView;
	private ProgressBar progressBar;
	private Bitmap bitmap;
	private static final int LOADING=1;
	private static final int END=2;
	int maxSize=0;
	int nowSize=0;
	
	Handler handler;
	
	private String fileName="xxxx.png";
	
	@SuppressLint("HandlerLeak")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		txt=(TextView)findViewById(R.id.txt);
		downImg=(Button)findViewById(R.id.downImg);
		downFile=(Button)findViewById(R.id.downFile);
		tonew=(Button)findViewById(R.id.tonew);
		downImg.setOnClickListener(this);
		downFile.setOnClickListener(this);
		imgView=(ImageView)findViewById(R.id.imgView);
		progressBar=(ProgressBar)findViewById(R.id.progressBar);
		tonew.setOnClickListener(this);
		//progressBar.setVisibility(View.GONE);
		handler=new Handler(){
			@Override
			public void handleMessage(Message msg) {
//super.handleMessage(msg);
				if(null!=progressBar){
//���ý��������ֵ
					progressBar.setMax(maxSize);
//��ǰ�Ѿ����ص�ֵ
					nowSize+=msg.getData().getInt("loadingSize");
//���ý������ĵ�ǰ����ֵ
					progressBar.setProgress(nowSize);
					if(msg.what==LOADING){
//��ʾ�Ѿ����ص�ֵ
						txt.setText("�����أ�"+(nowSize*100)/maxSize+"%");
						Log.e("Download_Progressbar", "�������أ�"+nowSize);
					}
					if(msg.what==END){
//������ɺ����ؽ�����
						progressBar.setVisibility(View.INVISIBLE);
//��ʾͼƬ
						imgView.setImageBitmap(bitmap);
//��ͼƬ���浽sdcard��
						saveImg(Environment.getExternalStorageDirectory(),bitmap);
//������ǰ�߳�
						Thread.currentThread().interrupt();
					}
				}
			}
		};
	}
	@Override
	public void onClick(View paramView) {
		switch (paramView.getId()) {
		case R.id.tonew:
			XListViewActivity.launch(MainActivity.this);
			break;
		case R.id.downImg:
//�������ͼƬ�ͽ�����
			if(null!=bitmap){
				imgView.setImageBitmap(null);
				nowSize=0;
				progressBar.setProgress(0);
				txt.setText("��������......");
				
			}
			//1����ʾ������
			progressBar.setVisibility(View.VISIBLE);
//2����ʼ����
			new MyThread(imgHttp1).start();
			break;
		case R.id.downFile:
			new AppFileDownUtils(MainActivity.this, fileurl, "taobao.apk").start();
			break;

		default:
			break;
		}
	}
	
//����ͼƬ����
	public void saveImg(File fullPath,Bitmap bitmap){
		File file=new File(fullPath, fileName);
		if(file.exists()){
			file.delete();
		}
		try {
			FileOutputStream fos=new FileOutputStream(file);
			boolean isSaved=bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
			if(isSaved){
				fos.flush();
				fos.close();
			}
			Log.e(LOG_TAG, "�ļ�����ɹ�");
		} catch (FileNotFoundException e) {
			Log.e(LOG_TAG, "����ʧ��");
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(LOG_TAG, "����ʧ��");
			e.printStackTrace();
		}
	}
	class MyThread extends Thread{
		String httpImg;
		public MyThread(String httpImg){
			this.httpImg=httpImg;
		}
		@Override
		public void run() {
//super.run();
			ByteArrayOutputStream bos=new ByteArrayOutputStream();
			try {
				URL url=new URL(httpImg);
				HttpURLConnection con=(HttpURLConnection)url.openConnection();
				con.setDoInput(true);
				con.connect();
				InputStream is=con.getInputStream();
//��ȡ�ļ��Ĵ�С
				maxSize=con.getContentLength();
				byte []buffer=new byte[1024];
				int len=-1;
				while((len=is.read(buffer))!=-1){
					bos.write(buffer,0,len);
//������Ϣ
					Message msg=new Message();
					msg.what=LOADING;
					Bundle bundle=new Bundle();
					bundle.putInt("loadingSize", len);
					msg.setData(bundle);
//					Thread.sleep(100);
					handler.sendMessage(msg);
				}
				
//�ر�������
				is.close();
//�ر�����
				con.disconnect();
				byte []imgBytes=bos.toByteArray();
				bitmap=BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
				bos.flush();
				bos.close();
//���ؽ���������Ϣ
				Message msg=new Message();
				msg.what=END;
				handler.sendMessage(msg);
			} catch (MalformedURLException e) {
				Log.e("Download_Progressbar", "MalformedURLException");
				e.printStackTrace();
			} catch (IOException e) {
				Log.e("Download_Progressbar", "IOException");
				e.printStackTrace();
			}
		}
	}
	
	public class AppFileDownUtils extends Thread { 
		   
	    private Context mContext; 
	    private String mDownloadUrl; // �ļ�����url�������ǿռ�� 
	    private String mFileName; 
	    private Message msg; 
	   
	    private final String APP_FOLDER = "DownDemo"; // sd��Ӧ��Ŀ¼ 
	   
	    public static final int MSG_UNDOWN = 0; //δ��ʼ���� 
	    public static final int MSG_DOWNING = 1; // ������ 
	    public static final int MSG_FINISH = 1; // ������� 
	    public static final int MSG_FAILURE = 2;// ����ʧ�� 
	   
	    private NotificationManager mNotifManager; 
	    private Notification mDownNotification; 
	    private RemoteViews mContentView; // ���ؽ���View 
	    private PendingIntent mDownPendingIntent; 
	   
	    public AppFileDownUtils(Context context, 
	            String downloadUrl, String fileName) { 
	        mContext = context; 
	        mDownloadUrl = downloadUrl; 
	        mFileName = fileName; 
	        mNotifManager = (NotificationManager) mContext 
	                .getSystemService(Context.NOTIFICATION_SERVICE); 
	        msg = new Message(); 
	    } 
	   
		@SuppressLint("NewApi")
		@Override 
	    public void run() { 
	        try { 
	            if (Environment.getExternalStorageState().equals( 
	                    Environment.MEDIA_MOUNTED)) { 
	                // SD��׼���� 
	                File sdcardDir = Environment.getExternalStorageDirectory(); 
	                // �ļ����·���� sdcard/DownDemo/apkFile 
	                File folder = new File(sdcardDir + "/" + APP_FOLDER); 
	                if (!folder.exists()) {
						folder.mkdir();
					}
	                File saveFilePath = new File(folder, mFileName); 
	                System.out.println(saveFilePath); 
	                if (Build.VERSION.SDK_INT < 16) {
	                	mDownNotification = new Notification.Builder(MainActivity.this)
	                	.setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle("׼������...").getNotification();
					}else {
						mDownNotification = new Notification.Builder(MainActivity.this)
						.setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle("׼������...").build();
					}
	                mDownNotification.flags = Notification.FLAG_ONGOING_EVENT; 
	                mDownNotification.flags = Notification.FLAG_NO_CLEAR; 
	                mContentView = new RemoteViews(mContext.getPackageName(), 
	                        R.layout.notif_view); 
	                mContentView.setImageViewResource(R.id.downLoadIcon, 
	                        android.R.drawable.stat_sys_download); 
	                mDownPendingIntent = PendingIntent.getActivity(mContext, 0, new Intent(), 0); 
	                mNotifManager.notify(R.drawable.appicon, mDownNotification); 
	                boolean downSuc = downloadFile(mDownloadUrl, saveFilePath); 
	                if (downSuc) { 
	                    msg.what = MSG_FINISH; 
	                    Intent intent = new Intent(Intent.ACTION_VIEW); 
	                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
	                    intent.setDataAndType(Uri.fromFile(saveFilePath), 
	                            "application/vnd.android.package-archive"); 
	                    PendingIntent contentIntent = PendingIntent.getActivity( 
	                            mContext, 0, intent, 0); 
	                    
	                    Notification notification;
	                    
	                    if (Build.VERSION.SDK_INT < 16) {
	                    	notification = new Notification.Builder(MainActivity.this)
	                    	.setSmallIcon(R.drawable.appicon).setContentTitle("���سɹ�")
	                    	.setContentText("�����װ").setContentIntent(contentIntent).getNotification();
						}else {
							notification = new Notification.Builder(MainActivity.this)
							.setSmallIcon(R.drawable.appicon).setContentTitle("���سɹ�")
							.setContentText("�����װ").setContentIntent(contentIntent).build();
						}
	                    notification.flags = Notification.FLAG_ONGOING_EVENT; 
	                    notification.flags = Notification.FLAG_AUTO_CANCEL; 
	                    mNotifManager.notify(R.drawable.appicon, notification); 
	                } else { 
	                    msg.what = MSG_FAILURE; 
	                    PendingIntent contentIntent = PendingIntent.getActivity( 
	                            mContext, 0, new Intent(), 0); 
	                    Notification notification;
	                    if (Build.VERSION.SDK_INT < 16) {
	                    	notification = new Notification.Builder(MainActivity.this)
	                    	.setSmallIcon(R.drawable.appicon).setContentTitle("����ʧ��")
	                    	.setContentIntent(contentIntent).getNotification();
	                    }else {
	                    	notification = new Notification.Builder(MainActivity.this)
	                    	.setSmallIcon(R.drawable.appicon).setContentTitle("����ʧ��")
	                    	.setContentIntent(contentIntent).build();
						}
	                    notification.flags = Notification.FLAG_AUTO_CANCEL; 
	                    mNotifManager.notify(R.drawable.appicon, notification); 
	                } 
	   
	            } else { 
	                Toast.makeText(mContext, Environment.getExternalStorageState(), 
	                        Toast.LENGTH_SHORT).show(); 
	                msg.what = MSG_FAILURE; 
	            } 
	        } catch (Exception e) { 
	            msg.what = MSG_FAILURE; 
	        } finally { 
	        } 
	    } 
	   
	    /**
	     * 
	     * Desc:�ļ�����
	     * 
	     * @param downloadUrl
	     *            ����URL
	     * @param saveFilePath
	     *            �����ļ�·��
	     * @return ture:���سɹ� false:����ʧ��
	     */ 
	    public boolean downloadFile(String downloadUrl, File saveFilePath) { 
	        int fileSize = -1; 
	        int downFileSize = 0; 
	        boolean result = false; 
	        int progress = 0; 
	        try { 
	            URL url = new URL(downloadUrl); 
	            HttpURLConnection conn = (HttpURLConnection) url.openConnection(); 
	            if (null == conn) { 
	                return false; 
	            } 
	            // ��ȡ��ʱʱ�� ���뼶 
	            conn.setReadTimeout(10000); 
	            conn.setRequestMethod("GET"); 
	            conn.setDoInput(true); 
	            conn.connect(); 
	            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) { 
	                fileSize = conn.getContentLength(); 
	                InputStream is = conn.getInputStream(); 
	                if (!saveFilePath.exists()) {
						saveFilePath.createNewFile();
					}
	                FileOutputStream fos = new FileOutputStream(saveFilePath); 
	                byte[] buffer = new byte[1024]; 
	                int i = 0; 
	                int tempProgress = -1; 
	                while ((i = is.read(buffer)) != -1) {
	                    downFileSize = downFileSize + i; 
	                    // ���ؽ��� 
	                    progress = (int) (downFileSize * 100.0 / fileSize); 
	                    fos.write(buffer, 0, i); 
	   
	                    synchronized (this) { 
	                        if (downFileSize == fileSize) { 
	                            // ������� 
	                            mNotifManager.cancel(R.id.downLoadIcon); 
	                        } else if (tempProgress != progress) { 
	                            // ���ؽ��ȷ����ı䣬����Message 
	                            mContentView.setTextViewText(R.id.progressPercent, 
	                                    progress + "%"); 
	                            mContentView.setProgressBar(R.id.downLoadProgress, 
	                                    100, progress, false); 
	                            mDownNotification.contentView = mContentView; 
	                            mDownNotification.contentIntent = mDownPendingIntent; 
	                            mNotifManager.notify(R.drawable.appicon, 
	                                    mDownNotification); 
	                            tempProgress = progress; 
	                        } 
	                    } 
	                } 
	                fos.flush(); 
	                fos.close(); 
	                is.close(); 
	                result = true; 
	            } else { 
	                result = false; 
	            } 
	        } catch (Exception e) { 
	            result = false; 
	        } 
	        return result; 
	    } 
	}
}
