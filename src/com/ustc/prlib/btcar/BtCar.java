package com.ustc.prlib.btcar;


import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.Toast;

public class BtCar extends Activity {

	private static final String TAG = "Robot";
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
    // Array adapter for the conversation thread
    //private ArrayAdapter<String> mConversationArrayAdapter;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the services
    private BluetoothService mBTService = null;
   
    //Touch Point
    private PointF touchPoint = new PointF(0, 0);
   
    private controlThread controlThread = null;
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_h);
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
       
        //¿ØÖÆ°å
        ImageView imageView = (ImageView) findViewById(R.id.imageView1);
        imageView.setOnTouchListener(controlTouchListener);
    }
    
    //¼àÌý¿ØÖÆ°å
    private OnTouchListener controlTouchListener =  new OnTouchListener() {
		
		public boolean onTouch(View v, MotionEvent event) {
			// TODO Auto-generated method stub
	    	//»ñµÃ´¥ÃþµÄ×ø±ê
	    	float x = event.getX();
	    	float y = event.getY(); 
	    	
	    	switch (event.getAction()) 
	    	{
	    	//´¥ÃþÆÁÄ»Ê±¿Ì
	    	case MotionEvent.ACTION_DOWN:
	    		//Log.d(TAG, " x:" + x + " y:" + y);
	    		synchronized (this) {
		    		touchPoint.x = x;
		    		touchPoint.y = y;
				}
	    		break;
	    	//´¥Ãþ²¢ÒÆ¶¯Ê±¿Ì
	    	case MotionEvent.ACTION_MOVE:
	    		//Log.d(TAG, " x:" + x + " y:" + y);
	    		synchronized (this) {
		    		touchPoint.x = x;
		    		touchPoint.y = y;
				}
	    		break;
	    	//ÖÕÖ¹´¥ÃþÊ±¿Ì
	    	case MotionEvent.ACTION_UP:
	    		//Log.d(TAG, " x:" + x + " y:" + y);
	    		synchronized (this) {
		    		touchPoint.x = 0;
		    		touchPoint.y = 0;
				}
	    		break;
	    	}
	    	return true;
		}
	};
	
	@Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setup() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the session
        } else {
            if (mBTService == null) 
            {
                // Initialize the BluetoothService to perform bluetooth connections
                mBTService = new BluetoothService(this, mHandler);
            }
        }
    }
    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
        // Stop the Bluetooth services
        if (mBTService != null) mBTService.stop();
        
    }

    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    
    private void sendControlSignal(byte[] command)
    {
        // Check that we're actually connected before trying anything
        if (mBTService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        mBTService.write(command);
    }
    
	private void setup() {
        Log.d(TAG, "setup()");

        // Initialize the BluetoothService to perform bluetooth connections
        mBTService = new BluetoothService(this, mHandler);

    }
	
	private class controlThread extends Thread{
	    //Sent to the robot command
		//high 4bit represent speed, low 4bit represent direction
	    private byte[] commandBuffer = new byte[3];
	    float x, y, alpha;
	    int carSpeed = 0; // -100~100 
	    int carDirection = 0; // -60~60
	    
	    private boolean flag = true;
	    
		public controlThread()
		{
			commandBuffer[0] = 111;
			commandBuffer[1] = 0;
			commandBuffer[2] = 0;
		}
		
		public void run()
		{
			while(mBTService.getState() == BluetoothService.STATE_CONNECTED){
				//read the touch point coordinate
				synchronized (this) {
					x = touchPoint.x;
					y = touchPoint.y;
				}
				
				if (x==0 && y==0) {
					commandBuffer[1] = 0;
					commandBuffer[2] = 0;
				}else {
					//calculate the command i.e speed and direction
					//scale and transform coordinate
					x =  (x - 275)/2;
					y = -(y - 525)/2;
					
					//Log.d(TAG, "x:"+x+" y:"+y);
					carSpeed = (int) Math.sqrt(x * x + y * y);
					if (carSpeed <= 25) 
					{
						commandBuffer[1] = 0;
						commandBuffer[2] = 0;
					}
					else 
					{
						if(carSpeed >= 125)
						{
							carSpeed = 125;
						}
						//carSpeed = (int) (carSpeed * 1.25);
						alpha = (float) Math.atan2(y, x);
						//Log.d(TAG, "alpha"+alpha);
						//forward or backword for a straight line
						if (alpha > Math.PI*17/36 && alpha < Math.PI*19/36 
								|| (alpha > -Math.PI*19/36 && alpha < -Math.PI*17/36)) {
							carDirection = 0;
						}
						//backleft
						else if (alpha > Math.PI*5/6 || alpha < -Math.PI*5/6 
								|| (alpha > -Math.PI/6 && alpha < Math.PI/6)) {
							carDirection = 60;
						}
						else if (alpha > -Math.PI*5/6 && alpha < -Math.PI/2) {
							alpha = (float) Math.abs(alpha + Math.PI / 2);
							carDirection = (int) (alpha*180/Math.PI);
						}
						//backright
						else if (alpha > -Math.PI/2 && alpha < -Math.PI/6) {
							alpha = (float) Math.abs(alpha + Math.PI / 2);
							carDirection = (int) (alpha*180/Math.PI);
						}
						//frontright
						else if (alpha > Math.PI/6 && alpha < Math.PI/2) {
							alpha = (float) Math.abs(Math.PI / 2 - alpha);
							carDirection = (int) (alpha*180/Math.PI);
						}
						//frontleft
						else if (alpha > Math.PI/2 && alpha < Math.PI * 5 / 6) {
							alpha = (float) Math.abs(alpha - Math.PI / 2);
							carDirection = (int) (alpha*180/Math.PI);
						}
						else {
							carDirection = 0;
						}
						
						commandBuffer[1] = (byte) (carSpeed - 25);
						commandBuffer[2] = (byte) carDirection;
						//dirction: turn right
						if (x > 0) 
						{
							commandBuffer[2] = (byte) -commandBuffer[2];
						}
						//speed: backWard
						if (y < 0)
						{
							commandBuffer[1] = (byte) -commandBuffer[1];
						}

					}
				}
				
				Log.d(TAG, "speed:"+commandBuffer[1]+" direction:"+commandBuffer[2]);
				
				//send the command through bluetooth
				sendControlSignal(commandBuffer);
				
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Log.e(TAG, "controlThread::sleep()");
				}
			}
		}
	}
	
	@Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mBTService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mBTService.getState() == BluetoothService.STATE_NONE) {
              // Start the Bluetooth services
            	mBTService.start();
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
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothService.STATE_CONNECTED:
                    setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                    //mConversationArrayAdapter.clear();
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
                //byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                //String writeMessage = new String(writeBuf);
                //mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
                //byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                //String readMessage = new String(readBuf, 0, msg.arg1);
                //mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mBTService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled
                // Initialize the BluetoothService to perform bluetooth connections
                mBTService = new BluetoothService(this, mHandler);
            } else {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
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
