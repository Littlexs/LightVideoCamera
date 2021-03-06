package com.example.bdxk.lightvideorecord.utils;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Uri转换工具类
 *
 */
public class UriUtils 
{
    /**
	 * 根据Uri获取文件的绝对路径，解决Android4.4以上版本Uri转换
	 *
	 * @param fileUri
	 */
	@TargetApi(19)
	public static String getFileAbsolutePath(Context context, Uri fileUri) 
	{
		if (context == null || fileUri == null)
			return null;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, fileUri))
		{
			if (isExternalStorageDocument(fileUri))
			{
				String docId = DocumentsContract.getDocumentId(fileUri);
				String[] split = docId.split(":");
				String type = split[0];
				if ("primary".equalsIgnoreCase(type)) {
					return Environment.getExternalStorageDirectory() + "/" + split[1];
				}
			}
			else if (isDownloadsDocument(fileUri))
			{
				String id = DocumentsContract.getDocumentId(fileUri);
				Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
				return getDataColumn(context, contentUri, null, null);
			}
			else if (isMediaDocument(fileUri))
			{
				String docId = DocumentsContract.getDocumentId(fileUri);
				String[] split = docId.split(":");
				String type = split[0];
				Uri contentUri = null;
				if ("image".equals(type))
				{
					contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
				}
				else if ("video".equals(type))
				{
					contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
				}
				else if ("audio".equals(type))
				{
					contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
				}
				String selection = MediaStore.Images.Media._ID + "=?";
				String[] selectionArgs = new String[] { split[1] };
				return getDataColumn(context, contentUri, selection, selectionArgs);
			}
		} 
		else if ("content".equalsIgnoreCase(fileUri.getScheme()))
		{
			if (isGooglePhotosUri(fileUri))
				return fileUri.getLastPathSegment();
			return getDataColumn(context, fileUri, null, null);
		}
		else if ("file".equalsIgnoreCase(fileUri.getScheme()))
		{
			return fileUri.getPath();
		}
		return null;
	}

	public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs)
	{
		Cursor cursor = null;
		String[] projection = { MediaStore.Images.Media.DATA };
		try {
			cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
			if (cursor != null && cursor.moveToFirst()) {
				int index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
				return cursor.getString(index);
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return null;
	}

	/**
	 * @param uri
	 *            The Uri to check.
	 * @return Whether the Uri authority is ExternalStorageProvider.
	 */
	public static boolean isExternalStorageDocument(Uri uri)
	{
		return "com.android.externalstorage.documents".equals(uri.getAuthority());
	}

	/**
	 * @param uri
	 *            The Uri to check.
	 * @return Whether the Uri authority is DownloadsProvider.
	 */
	public static boolean isDownloadsDocument(Uri uri)
	{
		return "com.android.providers.downloads.documents".equals(uri.getAuthority());
	}

	/**
	 * @param uri
	 *            The Uri to check.
	 * @return Whether the Uri authority is MediaProvider.
	 */
	public static boolean isMediaDocument(Uri uri)
	{
		return "com.android.providers.media.documents".equals(uri.getAuthority());
	}

	/**
	 * @param uri
	 *            The Uri to check.
	 * @return Whether the Uri authority is Google Photos.
	 */
	public static boolean isGooglePhotosUri(Uri uri)
	{
		return "com.google.android.apps.photos.content".equals(uri.getAuthority());
	}

	/**
	 * 设置输出文件夹
	 *
	 * @return
	 */
	public static String getOutputMediaFile() {
		File sdDir = null;
		boolean sdCardExist = Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
		if (sdCardExist) {
			sdDir = Environment.getExternalStorageDirectory();
		} else if (!sdCardExist) {
			Log.i("UriUtils","没有内存卡");
		}
		File eis = new File(sdDir.toString() + "/BDXK/");
		try {
			if (!eis.exists()) {
				eis.mkdir();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		return eis.getPath() + File.separator +"VID_" + timeStamp + ".mp4";
	}
}
