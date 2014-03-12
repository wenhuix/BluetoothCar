package com.ustc.prlib.btcar;


//计算速度和方向
public class Controller {

	private static final int MAX_VELOCITY = 100;
	private static final int MAX_ROTATION = 60;

	/**
	 * 
	 * @param height
	 *            : 触摸板高度
	 * @param width
	 *            ：触摸板宽度
	 * @param x
	 *            :触摸点的x坐标
	 * @param y
	 *            :触摸点的y坐标
	 * @return byte[2]: byte[0] = 速度, byte[1] = 方向
	 */
	public int[] CalcuteVAndR(int height, int width, float x, float y) {

		int[] result = new int[2];
		float centralX = (float) (width / 2);
		float centralY = (float) (height / 2);
		int velocity = 0, rotation = 0;
		float alpha = 0;

		int range = (int) Math.min(centralX, centralY);

		if (x == 0 && y == 0 || range == 0) {
			result[0] = result[1] = 0;
		} else {

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
				else if (alpha > Math.PI * 5 / 6 || alpha < -Math.PI * 5 / 6
						|| (alpha > -Math.PI / 6 && alpha < Math.PI / 6)) {
					rotation = 60;
				} else if (alpha > -Math.PI * 5 / 6 && alpha < -Math.PI / 2) {
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

			// Scale the velocity
			velocity = (velocity * MAX_VELOCITY / range);
			// if |velocity| is little, the rotation = 0;
			if (Math.abs(velocity) < MAX_VELOCITY / 5) {
				rotation = 0;
			}
			result[0] = velocity;
			result[1] = rotation;
		}

		return result;
	}

	public int calculateV(int height, int width, float x, float y) {

		int velocity = 0;
		float centralX = (float) (width / 2);
		float centralY = (float) (height / 2);
		int range = (int) Math.min(centralX, centralY);

		if (range == 0) {
			velocity = 0;
		} else {
			if (Math.abs(x - centralX) < range) {
				velocity = Math.min((int) Math.abs(y - centralY), range);
			}
			if (y > centralY) {
				velocity = -velocity;
			}
			// Scale the velocity to 0~MAX_VELOCITY
			velocity = velocity * MAX_VELOCITY / range;
		}

		return velocity;

	}

	public int calculateR(int height, int width, float x, float y) {
		int rotation = 0;
		float centralX = (float) (width / 2);
		float centralY = (float) (height / 2);
		int range = (int) (Math.min(centralX, centralY) * 0.75);
		if (range == 0) {
			rotation = 0;
		} else {
			if (Math.abs(y - centralY) < range) {
				rotation = (int) Math.min(Math.abs(x - centralX), range)
						- MAX_ROTATION / 5;
			}
			// rotation: turn right
			if (x > centralX) {
				rotation = -rotation;
			}
			rotation = rotation * MAX_ROTATION / (range - MAX_ROTATION / 5);
		}

		return rotation;
	}

}
