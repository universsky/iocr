package isword.iocr;

import java.io.File;
import java.io.FileNotFoundException;

import com.googlecode.tesseract.android.TessBaseAPI;

import isword.iocr.R;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

/**
 * 
 * @author �����¹⽣
 * 2013��12��12��  ����10:15:14
 */

public class MainActivity extends Activity {

	private static final int PHOTO_CAPTURE = 0x11;// ����
	private static final int PHOTO_RESULT = 0x12;// ���

	private static String LANGUAGE = "eng";
	private static String IMG_PATH = getSDPath() + java.io.File.separator
			+ "ocrtest";

	private static TextView tvResult;
	private static EditText etResult;
	private static ImageView ivSelected;
	private static ImageView ivTreated;
	private static Button btnCamera;
	private static Button btnSelect;
	private static CheckBox chPreTreat;
	private static RadioGroup radioGroup;
	private static String textResult;
	private static Bitmap bitmapSelected;
	private static Bitmap bitmapTreated;
	private static final int SHOWRESULT = 0x101;
	private static final int SHOWTREATEDIMG = 0x102;

	// ��handler���ڴ����޸Ľ��������
	public static Handler myHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case SHOWRESULT:
				if (etResult.equals(""))
					tvResult.setText("ʶ��ʧ��!");
				else{
					//tvResult.setText(textResult);
					tvResult.setText("ʶ����:");
					etResult.setText(textResult);
				}		
				
				break;
			case SHOWTREATEDIMG:
				//etResult.setText("ʶ����......");
				tvResult.setText("ʶ����......");
				showPicture(ivTreated, bitmapTreated);
				break;
			}
			super.handleMessage(msg);
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// ���ļ��в����� ���ȴ����ļ���
		File path = new File(IMG_PATH);
		if (!path.exists()) {
			path.mkdirs();
		}

		tvResult = (TextView) findViewById(R.id.tv_result);
		etResult = (EditText) findViewById(R.id.et_result);
		ivSelected = (ImageView) findViewById(R.id.iv_selected);
		ivTreated = (ImageView) findViewById(R.id.iv_treated);
		btnCamera = (Button) findViewById(R.id.btn_camera);
		btnSelect = (Button) findViewById(R.id.btn_select);
		chPreTreat = (CheckBox) findViewById(R.id.ch_pretreat);
		radioGroup = (RadioGroup) findViewById(R.id.radiogroup);

		btnCamera.setOnClickListener(new cameraButtonListener());
		btnSelect.setOnClickListener(new selectButtonListener());
		//���ã�Ĭ��Ϊ��ֵ������
		chPreTreat.setChecked(true);
		// �������ý������ԣ� ֮ǰ init ������ѵ���ַ�����·��
		radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				switch (checkedId) {
				case R.id.rb_en:
					LANGUAGE = "eng";
					break;
				case R.id.rb_ch:
					LANGUAGE = "chi_sim";
					break;
				}
			}

		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode == Activity.RESULT_CANCELED)
			return;

		if (requestCode == PHOTO_CAPTURE) {
			tvResult.setText("abc");
			startPhotoCrop(Uri.fromFile(new File(IMG_PATH, "temp.jpg")));
		}

		// ������
		if (requestCode == PHOTO_RESULT) {
			bitmapSelected = decodeUriAsBitmap(Uri.fromFile(new File(IMG_PATH,
					"temp_cropped.jpg")));
			if (chPreTreat.isChecked())
				tvResult.setText("Ԥ������......");
			else
				tvResult.setText("ʶ����......");
			// ��ʾѡ���ͼƬ
			showPicture(ivSelected, bitmapSelected);
			
			// ���߳�������ʶ��
			new Thread(new Runnable() {
				@Override
				public void run() {
					if (chPreTreat.isChecked()) {
						bitmapTreated = IOpticalCharacterRecognize
								.doPretreatment(bitmapSelected);
						Message msg = new Message();
						msg.what = SHOWTREATEDIMG;
						myHandler.sendMessage(msg);
						textResult = doOcr(bitmapTreated, LANGUAGE);
					} else {
						bitmapTreated = IOpticalCharacterRecognize
								.converyToGrayImg(bitmapSelected);
						Message msg = new Message();
						msg.what = SHOWTREATEDIMG;
						myHandler.sendMessage(msg);
						textResult = doOcr(bitmapTreated, LANGUAGE);
					}
					Message msg2 = new Message();
					msg2.what = SHOWRESULT;
					myHandler.sendMessage(msg2);
				}

			}).start();

		}

		super.onActivityResult(requestCode, resultCode, data);
	}
	
	// ����ʶ��
	class cameraButtonListener implements OnClickListener {

		@Override
		public void onClick(View arg0) {
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			intent.putExtra(MediaStore.EXTRA_OUTPUT,
					Uri.fromFile(new File(IMG_PATH, "temp.jpg")));
			startActivityForResult(intent, PHOTO_CAPTURE);
		}
	};

	// �����ѡȡ��Ƭ���ü�
	class selectButtonListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			intent.setType("image/*");
			intent.putExtra("crop", "true");
			intent.putExtra("scale", true);
			intent.putExtra("return-data", false);
			intent.putExtra(MediaStore.EXTRA_OUTPUT,
					Uri.fromFile(new File(IMG_PATH, "temp_cropped.jpg")));
			intent.putExtra("outputFormat",
					Bitmap.CompressFormat.JPEG.toString());
			intent.putExtra("noFaceDetection", true); // no face detection
			startActivityForResult(intent, PHOTO_RESULT);
		}

	}
	
	// ��ͼƬ��ʾ��view��
	public static void showPicture(ImageView iv, Bitmap bmp){
		iv.setImageBitmap(bmp);
	}
	
	/**
	 * ����ͼƬʶ��
	 * 
	 * @param bitmap
	 *            ��ʶ��ͼƬ
	 * @param language
	 *            ʶ������
	 * @return ʶ�����ַ���
	 */
 	public String doOcr(Bitmap bitmap, String language) {
		TessBaseAPI baseApi = new TessBaseAPI();
		//��ʼ��OCR���ַ���data·��:getSDPath()="/mnt/sdcard"
		baseApi.init(getSDPath(), language);
		//baseApi.init(".", language);
		// ����Ӵ��У�tess-twoҪ��BMP����Ϊ������
		bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
		baseApi.setImage(bitmap);
		String text = baseApi.getUTF8Text();
		baseApi.clear();
		baseApi.end();
		return text;
	}
 	
 	/**
 	 *
 	 public boolean init(String datapath, String language) {
        if (datapath == null)
            throw new IllegalArgumentException("Data path must not be null!");
        
        if (!datapath.endsWith(File.separator))
            datapath += File.separator;

        File tessdata = new File(datapath + "tessdata");
        if (!tessdata.exists() || !tessdata.isDirectory())
            throw new IllegalArgumentException("Data path must contain subfolder tessdata!");

        return nativeInit(datapath, language);
    }
 	 **/
 	

	/**
	 * ��ȡsd����·��
	 * @return ·�����ַ���
	 */
	public static String getSDPath() {
		File sdDir = null;
		boolean sdCardExist = Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED); // �ж�sd���Ƿ����
		if (sdCardExist) {
			sdDir = Environment.getExternalStorageDirectory();// ��ȡ���Ŀ¼
		}
		return sdDir.toString();
	}

	/**
	 * ����ϵͳͼƬ�༭���вü�
	 */
	public void startPhotoCrop(Uri uri) {
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(uri, "image/*");
		intent.putExtra("crop", "true");
		intent.putExtra("scale", true);
		intent.putExtra(MediaStore.EXTRA_OUTPUT,
				Uri.fromFile(new File(IMG_PATH, "temp_cropped.jpg")));
		intent.putExtra("return-data", false);
		intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
		intent.putExtra("noFaceDetection", true); // no face detection
		startActivityForResult(intent, PHOTO_RESULT);
	}

	/**
	 * ����URI��ȡλͼ
	 * @param uri
	 * @return ��Ӧ��λͼ
	 */
	private Bitmap decodeUriAsBitmap(Uri uri) {
		Bitmap bitmap = null;
		try {
			bitmap = BitmapFactory.decodeStream(getContentResolver()
					.openInputStream(uri));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		return bitmap;
	}
}
