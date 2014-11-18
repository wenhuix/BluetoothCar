package com.ustc.prlib.btcar;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.ustc.prlib.util.bluetooth.BluetoothService;
import com.ustc.prlib.util.bluetooth.DeviceListActivity;

public class BtCarActivity extends Activity{

	private static final String TAG = "BTCar";
	private static final boolean D = true;

	// Message types sent from the BluetoothService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;

	// Key names received from the BluetoothService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Name of the connected device
	private String mConnectedDeviceName = null;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the services
	private BluetoothService mBTService = null;
	

	// Touch Point
	private PointF touchPoint1 = new PointF(0, 0);
	private PointF touchPoint2 = new PointF(0, 0);

	private Thread controlThread = null;

	private ImageView imageView1 = null; // control panel 1
	private ImageView imageView2 = null; // control panel 2

	private Context context = this;

	private Controller controller = new Controller();
	
	protected PowerManager.WakeLock mWakeLock;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {

			// landscape
			setContentView(R.layout.activity_main_l);
			// 控制板1
			imageView1 = (ImageView) findViewById(R.id.imageView1);
			imageView1.setOnTouchListener(controlTouchListener1);
			// 控制板2
			imageView2 = (ImageView) findViewById(R.id.imageView2);
			imageView2.setOnTouchListener(controlTouchListener2);

		} else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			// portrait
			setContentView(R.layout.activity_main_p);
			// 当屏幕竖着时，只是用控制板1控制小车
			// 控制板1
			imageView1 = (ImageView) findViewById(R.id.imageView1);
			imageView1.setOnTouchListener(controlTouchListener1);
		}

		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(context, "该设备不支持蓝牙，强制退出", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
        
		// Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

	}

	@Override
	public void onStart() {
		super.onStart();
		if (D)
			Log.e(TAG, "++ ON START ++");

		// If BT is not on, request that it be enabled.
		// setup() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			// Otherwise, setup the session
		} else {
			if (mBTService == null) {
				// Initialize the BluetoothService to perform bluetooth
				// connections
				mBTService = new BluetoothService(this, mHandler);
			}
		}
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		if (D)
			Log.e(TAG, "+ ON RESUME +");

		// Performing this check in onResume() covers the case in which BT was
		// not enabled during onStart(), so we were paused to enable it...
		// onResume() will be called when ACTION_REQUEST_ENABLE activity
		// returns.
		if (mBTService != null) {
			// Only if the state is STATE_NONE, do we know that we haven't
			// started already
			if (mBTService.getState() == BluetoothService.STATE_NONE) {
				// Start the Bluetooth services
				mBTService.start();
			}
		}

	}

	@Override
	public synchronized void onPause() {
		super.onPause();
		if (D)
			Log.e(TAG, "- ON PAUSE -");
	}

	@Override
	public void onStop() {
		super.onStop();
		if (D)
			Log.e(TAG, "-- ON STOP --");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (D)
			Log.e(TAG, "--- ON DESTROY ---");
		// Stop the Bluetooth services
		if (mBTService != null)
			mBTService.stop();

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		// Checks the orientation of the screen
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {

			// Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
			setContentView(R.layout.activity_main_l);
			// 控制板1
			imageView1 = (ImageView) findViewById(R.id.imageView1);
			imageView1.setOnTouchListener(controlTouchListener1);
			// 控制板2
			imageView2 = (ImageView) findViewById(R.id.imageView2);
			imageView2.setOnTouchListener(controlTouchListener2);
		} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {

			// Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
			setContentView(R.layout.activity_main_p);
			// 当屏幕竖着时，只是用控制板1控制小车
			// 控制板1
			imageView1 = (ImageView) findViewById(R.id.imageView1);
			imageView1.setOnTouchListener(controlTouchListener1);
		}
	}

	// 监听控制板1
	private OnTouchListener controlTouchListener1 = new OnTouchListener() {

		public boolean onTouch(View v, MotionEvent event) {

			// 获得触摸的坐标
			float x = event.getX();
			float y = event.getY();

			switch (event.getAction()) {
			// 触摸屏幕时刻
			case MotionEvent.ACTION_DOWN:
				synchronized (this) {
					touchPoint1.x = x;
					touchPoint1.y = y;
				}
				break;
			// 触摸并移动时刻
			case MotionEvent.ACTION_MOVE:
				synchronized (this) {
					touchPoint1.x = x;
					touchPoint1.y = y;
				}
				break;
			// 终止触摸时刻
			case MotionEvent.ACTION_UP:
				synchronized (this) {
					touchPoint1.x = 0;
					touchPoint1.y = 0;
				}
				break;
			}
			return true;
		}
	};

	// 监听控制板2
	private OnTouchListener controlTouchListener2 = new OnTouchListener() {

		public boolean onTouch(View v, MotionEvent event) {

			// 获得触摸的坐标
			float x = event.getX();
			float y = event.getY();

			switch (event.getAction()) {
			// 触摸屏幕时刻
			case MotionEvent.ACTION_DOWN:
				synchronized (this) {
					touchPoint2.x = x;
					touchPoint2.y = y;
				}
				break;
			// 触摸并移动时刻
			case MotionEvent.ACTION_MOVE:
				synchronized (this) {
					touchPoint2.x = x;
					touchPoint2.y = y;
				}
				break;
			// 终止触摸时刻
			case MotionEvent.ACTION_UP:
				synchronized (this) {
					touchPoint2.x = 0;
					touchPoint2.y = 0;
				}
				break;
			}
			return true;
		}
	};

	private void sendControlSignal(byte[] command) {
		// Check that we're actually connected before trying anything
		if (mBTService.getState() != BluetoothService.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
					.show();
			return;
		}

		mBTService.write(command);
	}

	private class controlThread extends Thread {
		// Sent control command
		private byte[] buffer  = new byte[3];
		private byte speed=0, rotation=0;
		float x1, y1, x2, y2;

		public controlThread() {
			buffer[0] = 111;  //Start of the package
			buffer[1] = 0;
			buffer[2] = 0;
		}

		public void run() {
			while (mBTService.getState() == BluetoothService.STATE_CONNECTED) {
				// read the touch point coordinate
				synchronized (this) {
					x1 = touchPoint1.x;
					y1 = touchPoint1.y;
					x2 = touchPoint2.x;
					y2 = touchPoint2.y;
				}

				if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {

					if (imageView1 != null && imageView2 != null) {
						speed    = (byte) controller.calculateV(
								imageView1.getHeight(), imageView1.getWidth(),
								x1, y1);
						rotation = (byte) controller.calculateR(
								imageView2.getHeight(), imageView2.getWidth(),
								x2, y2);
					}

				} else if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
					if (imageView1 != null) {
						int[] result = controller.CalcuteVAndR(
								imageView1.getHeight(), imageView1.getWidth(),
								x1, y1);
						speed    = (byte) result[0];
						rotation = (byte) result[1];
					}

				}

				// send the command through bluetooth
				if (speed != buffer[1] || rotation != buffer[2]) {
					buffer[1] = speed;
					buffer[2] = rotation;
					sendControlSignal(buffer);
				}
				
				Log.d(TAG, "speed:" + speed + " direction:"
				 + rotation);
				
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
					Log.e(TAG, "controlThread::sleep()");
				}
			}
		}
	}

	private final void setStatus(int resId) {
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(resId);
	}

	private final void setStatus(CharSequence subTitle) {
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(subTitle);
	}

	// The Handler that gets information back from the BluetoothService
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				if (D)
					Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BluetoothService.STATE_CONNECTED:
					setStatus(getString(R.string.title_connected_to,
							mConnectedDeviceName));
					// mConversationArrayAdapter.clear();
					if (controlThread == null) {
						Log.d(TAG, "new controlThread");
						controlThread = new controlThread();

					}
					Log.d(TAG, "controlThread.start");
					controlThread.start();
					break;
				case BluetoothService.STATE_CONNECTING:
					setStatus(R.string.title_connecting);
					break;
				case BluetoothService.STATE_LISTEN:
				case BluetoothService.STATE_NONE:

					setStatus(R.string.title_not_connected);
					break;
				}
				break;
			case MESSAGE_WRITE:
				// byte[] writeBuf = (byte[]) msg.obj;
				// construct a string from the buffer
				// String writeMessage = new String(writeBuf);
				// mConversationArrayAdapter.add("Me:  " + writeMessage);
				break;
			case MESSAGE_READ:
				// byte[] readBuf = (byte[]) msg.obj;
				// construct a string from the valid bytes in the buffer
				// String readMessage = new String(readBuf, 0, msg.arg1);
				// mConversationArrayAdapter.add(mConnectedDeviceName+":  " +
				// readMessage);
				break;
			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				/*Toast.makeText(getApplicationContext(),
						"连接到" + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();*/
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D)
			Log.d(TAG, "onActivityResult " + resultCode);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				// Get the device MAC address
				String address = data.getExtras().getString(
						DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// Get the BLuetoothDevice object
				BluetoothDevice device = mBluetoothAdapter
						.getRemoteDevice(address);
				// Attempt to connect to the device
				mBTService.connect(device);
			}
			break;
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled
				// Initialize the BluetoothService to perform bluetooth
				// connections
				mBTService = new BluetoothService(this, mHandler);
			} else {
				// User did not enable Bluetooth or an error occured
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this, "蓝牙没有打开，程序退出", Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent serverIntent = null;
		switch (item.getItemId()) {

		case R.id.connect_scan:
			// Launch the DeviceListActivity to see devices and do scan
			serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			return true;
			
		}
		return false;
	}
	

}
