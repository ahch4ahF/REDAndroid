package ch.redacted.util;

import android.support.v4.widget.SwipeRefreshLayout;
import android.widget.ImageView;

import java.lang.reflect.Field;

import ch.redacted.app.R;

/**
 * Created by sxo on 26/05/17.
 */

public class ImageHelper {

	public static String replaceImageLinks(String body) {
		String temp = body;

		temp = temp.replaceAll("onclick=\"[^\"]*\"", "");
		temp = temp.replaceAll("class=\"[^\"]*\"", "");

		return temp;
	}

	public static String stripImages(String body) {
		return body.replaceAll("<img[^>]+>", "");
	}

    public static ImageView getRippy(SwipeRefreshLayout swipeRefreshContainer) {
		ImageView img = null;
		Field f;
		try {
			f = swipeRefreshContainer.getClass().getDeclaredField("mCircleView");
			f.setAccessible(true);
			img = (ImageView)f.get(swipeRefreshContainer);
			img.setImageResource(R.drawable.ic_rippy);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		}
		return img;
	}

	public static String getFirstImageLink(String body) {
		try {
			return body.substring(body.indexOf("alt=") + 5, body.indexOf("src=") - 2);
		} catch (IndexOutOfBoundsException e) {
			return "";
		}
	}
}
