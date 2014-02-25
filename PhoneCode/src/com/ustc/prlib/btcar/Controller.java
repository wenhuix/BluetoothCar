package com.ustc.prlib.btcar;

import android.widget.ImageView;


//计算速度和方向
public class Controller {
	
	private static final int MAX_VELOCITY = 125;
	private static final int MAX_ROTATION = 60;
	
	/**
	 * 
	 * @param v:控制板视图
	 * @param x:触摸点的x坐标
	 * @param y:触摸点的y坐标
	 * @return byte[2]: byte[0] = 速度, byte[1] = 方向
	 */
	public int[] CalcuteVAndR(ImageView v, float x, float y){
		
		int[] result = new int[2];
		float centralX = (float)(v.getWidth() / 2);
		float centralY = (float)(v.getHeight() / 2);
		int range = (int)Math.min(centralX, centralY);
		int velocity = 0, rotation = 0;
		float alpha = 0;
		
		if (x==0 && y==0) {
			result[0] = result[1] = 0;
		}else {

			x = x - centralX;
			y = -(y - centralY);
			// Log.d(TAG, "x:"+x+" y:"+y);
			velocity = (int) Math.sqrt(x * x + y * y);
			
			if (velocity <= 0) {
				velocity = 0;
				rotation = 0;
			} else {
				velocity = Math.min(velocity, range);
				// carSpeed = (int) (carSpeed * 1.25);
				alpha = (float) Math.atan2(y, x);
				// Log.d(TAG, "alpha"+alpha);
				// forward or backward for a straight line
				if (alpha > Math.PI * 17 / 36
						&& alpha < Math.PI * 19 / 36
						|| (alpha > -Math.PI * 19 / 36 && alpha < -Math.PI * 17 / 36)) {
					rotation = 0;
				}
				// backleft
				else if (alpha > Math.PI * 5 / 6
						|| alpha < -Math.PI * 5 / 6
						|| (alpha > -Math.PI / 6 && alpha < Math.PI / 6)) {
					rotation = 60;
				} else if (alpha > -Math.PI * 5 / 6
						&& alpha < -Math.PI / 2) {
					alpha = (float) Math.abs(alpha + Math.PI / 2);
					rotation = (int) (alpha * 180 / Math.PI);
				}
				// backright
				else if (alpha > -Math.PI / 2 && alpha < -Math.PI / 6) {
					alpha = (float) Math.abs(alpha + Math.PI / 2);
					rotation = (int) (alpha * 180 / Math.PI);
				}
				// frontright
				else if (alpha > Math.PI / 6 && alpha < Math.PI / 2) {
					alpha = (float) Math.abs(Math.PI / 2 - alpha);
					rotation = (int) (alpha * 180 / Math.PI);
				}
				// frontleft
				else if (alpha > Math.PI / 2 && alpha < Math.PI * 5 / 6) {
					alpha = (float) Math.abs(alpha - Math.PI / 2);
					rotation = (int) (alpha * 180 / Math.PI);
				} else {
					rotation = 0;
				}

				// rotation: turn right
				if (x > 0) {
					rotation = -rotation;
				}
				// velocity: backWard
				if (y < 0) {
					velocity = -velocity;
				}

			}
		}
		//Scale the velocity to 0~125
		velocity = (velocity * MAX_VELOCITY / range);
		//if |velocity| < 40, the car will not move, so rotation = 0;
		if (Math.abs(velocity) < MAX_VELOCITY / 5){
			rotation = 0;
		}
		result[0] = velocity;
		result[1] = rotation;

		return result;	
	}
	
	public int calculateV(ImageView v, float x, float y) {

		float centralX = (float) (v.getWidth() / 2);
		float centralY = (float) (v.getHeight() / 2);
		int range = (int) Math.min(centralX, centralY);
		int velocity = 0;

		if(Math.abs(x - centralX)<range){
			velocity = Math.min((int) Math.abs(y - centralY), range);
		}
		if (y>centralY) {
			velocity = -velocity;
		}
	
		// Scale the velocity to 0~125
		return velocity * MAX_VELOCITY / range;

	}
	
	public int calculateR(ImageView v, float x, float y)
	{
		float centralX = (float) (v.getWidth() / 2);
		float centralY = (float) (v.getHeight() / 2);
		int range = (int) (Math.min(centralX, centralY) * 0.75);
		int rotation = 0;

		if(Math.abs(y - centralY)<range){
			rotation = (int) Math.min(Math.abs(x - centralX),range) - MAX_ROTATION / 5; 
		}
		// rotation: turn right
		if (x > centralX) {
			rotation = -rotation;
		}
		return rotation * MAX_ROTATION / (range - MAX_ROTATION / 5);
	}

}
