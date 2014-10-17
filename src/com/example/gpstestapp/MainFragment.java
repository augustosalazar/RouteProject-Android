package com.example.gpstestapp;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainFragment extends Fragment implements LocationListener{

	private int serverResponseCode = 0;
	protected Button mButtonGps;
	protected Button mButtonCargar;
	protected TextView textViewAltitude;
	protected TextView textViewLongitude;
	protected TextView textViewSpeed;
	protected TextView textViewLatitude;
	protected boolean mStarted;
	private LocationManager mLocationManager;
	private MainFragment listener = this;
	private String TAG = MainFragment.class.getSimpleName();

	private static final String FILE_HEADER = "TIMESTAMP,PREDIO,LAT,LONG,ALT";
	public static final String FILE_PATH_ROOT = Environment
			.getExternalStorageDirectory().getPath();
	public static final String APP_FOLDER_NAME = "/Routes/";
	public static final String APP_FOLDER_PATH = FILE_PATH_ROOT
			+ APP_FOLDER_NAME;

	private File mFile;
	private BufferedWriter mWriter;
	private StringBuilder mString;
	
	private Context mContext;

	public MainFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_main, container,
				false);

		mStarted = false;

		mButtonGps = (Button) rootView.findViewById(R.id.button1);
		mButtonCargar = (Button) rootView.findViewById(R.id.button2);
		textViewAltitude = (TextView) rootView
				.findViewById(R.id.textViewAltitude);
		textViewSpeed = (TextView) rootView
				.findViewById(R.id.textViewSpeed);
		textViewLongitude = (TextView) rootView
				.findViewById(R.id.textViewLongitude);
		textViewLatitude = (TextView) rootView
				.findViewById(R.id.textViewLatitude);

		mButtonGps.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mStarted == false) {
					starCapturing();
				} else {
					Toast.makeText(getActivity(), "Stoped",
							Toast.LENGTH_LONG).show();
					stopCapturing();
					mStarted = !mStarted;
					mButtonGps.setText("Comenzar");
				}

			}
		});
		
		mButtonCargar.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.d(TAG, "uploadButton click");
				uploadData();
			}
		});

		return rootView;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
		
		mContext = getActivity().getApplicationContext();

		if (mString == null)
			mString = new StringBuilder();
	}
	
	/* Checks if external storage is available for read and write */
	public boolean isExternalStorageWritable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	        return true;
	    }
	    return false;
	}
	
	/* Checks if external storage is available to at least read */
	public boolean isExternalStorageReadable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state) ||
	        Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
	        return true;
	    }
	    return false;
	}

	public void starCapturing() {
		boolean gps_enabled = true, network_enabled = true;
		mLocationManager = (LocationManager) getActivity()
				.getSystemService(Context.LOCATION_SERVICE);

		try {
			gps_enabled = mLocationManager
					.isProviderEnabled(LocationManager.GPS_PROVIDER);
		} catch (Exception ex) {

		}
		try {
			network_enabled = mLocationManager
					.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		} catch (Exception ex) {

		}
		Log.d(TAG, "gps_enabled " + gps_enabled + " network_enabled "
				+ network_enabled);
		if (!gps_enabled || !network_enabled) {
			AlertDialog.Builder dialog = new AlertDialog.Builder(
					getActivity());
			dialog.setMessage("GPS no esta habilitado!").setPositiveButton(
					"OK", new DialogInterface.OnClickListener() {

						@Override
						public void onClick(
								DialogInterface paramDialogInterface,
								int paramInt) {
							// TODO Auto-generated method stub
							// Intent myIntent = new Intent(
							// Settings.ACTION_LOCATION_SOURCE_SETTINGS);
							// getActivity().startActivity(myIntent);
							// // get gps
						}
					});
			dialog.show();

		} else {

			if (isExternalStorageWritable() == false){
				Log.d(TAG, "Ext not writable ");
				Toast.makeText(getActivity(), "Ext not writable", Toast.LENGTH_LONG)
				.show();
			}
			File directory = new File(APP_FOLDER_PATH);
			directory.mkdirs();
			String fileName = APP_FOLDER_PATH + "DATA.csv";
			mFile = new File(fileName);
			
			if (!mFile.exists()) {
				try {
					mFile.createNewFile();
					mWriter = new BufferedWriter(new FileWriter(mFile));
					mWriter.write(FILE_HEADER);
					Log.d(TAG, "New SENSORS file created succesfully!");
				} catch (IOException e) {
					Log.d(TAG, "Error at creating " + fileName);
					Toast.makeText(getActivity(), "Error creando archivo", Toast.LENGTH_LONG)
					.show();
					e.printStackTrace();
				}
			} else {
				try {
					mWriter = new BufferedWriter(
							new FileWriter(mFile, true));
				} catch (IOException e) {
					Log.d(TAG, "Error at creating " + fileName);
					e.printStackTrace();
				}

				Log.d(TAG, "SENSORS file opened succesfully!");
			}

			mLocationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, 0, 0, listener);
			Toast.makeText(getActivity(), "Started", Toast.LENGTH_LONG)
					.show();
			mStarted = !mStarted;
			mButtonGps.setText("Detener");
		}
	}

	public void stopCapturing() {
		if (mLocationManager != null) {
			mLocationManager.removeUpdates(this);
		}

		if (mWriter != null) {
			try {
				mWriter.close();
				mWriter = null;
				Log.d(TAG, "SENSORS BufferedWriter closed");
			} catch (IOException e) {
				Log.d(TAG, "Error at closing SENSORS BufferedWriter");
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		double mLat = location.getLatitude();
		double mLong = location.getLongitude();
		double mAlt = location.getAltitude();
		double mSpeed = location.getSpeed();

		mString.setLength(0);
		mString.append(String.valueOf(System.currentTimeMillis()));
		mString.append("," + "Predio");
		mString.append("," + String.valueOf(mLat));
		mString.append("," + String.valueOf(mLong));
		mString.append("," + String.valueOf(mAlt));

		try {
			if (mWriter != null) {
				mWriter.newLine();
				mWriter.write(mString.toString());
			}
		} catch (IOException e) {
			Log.d(TAG, "Error at writing in SENSORS: " + mString.toString());
			e.printStackTrace();
		}

		textViewLatitude.setText(String.valueOf(mLat));
		textViewLongitude.setText(String.valueOf(mLong));
		textViewAltitude.setText(String.valueOf(mAlt));
		textViewSpeed.setText(String.valueOf(mSpeed));
	}

	@Override
	public void onStop() {
		super.onStop();
		Log.d(TAG, "onStop");
		stopCapturing();
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
		if (mStarted) {
			starCapturing();
		} else {

		}
	}

	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub

	}
	

	public void uploadData() {
		//final String serverUrl = "http://augustodesarrollador.com/ruta/";
		final String serverUrl = "http://192.168.0.12:8888/RouteProject-Web/";

		new Thread(new Runnable() {
			@Override
			public void run() {
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Log.d(TAG, "Upload starting");
					}
				});

				uploadFile(serverUrl + "upload.php", APP_FOLDER_PATH
						+ "DATA.csv", true);
			}
		}).start();
	}
	

	public int uploadFile(String upLoadServerUri, final String sourceFileUri,
			final boolean debugMode) {

		String fileName = sourceFileUri;

		HttpURLConnection conn = null;
		DataOutputStream dos = null;
		String lineEnd = "\r\n";
		String twoHyphens = "--";
		String boundary = "*****";
		int bytesRead, bytesAvailable, bufferSize;
		byte[] buffer;
		int maxBufferSize = 1 * 1024 * 1024;
		File sourceFile = new File(sourceFileUri);
		
		Log.d(TAG, "Upload File" + sourceFileUri);

		if (!sourceFile.isFile()) {

			Log.e(TAG, "Source File not exist :" + sourceFileUri);
			
			return 0;

		} else {
			try {
				
				Log.d(TAG, "Upload File found :" + sourceFileUri);

				// open a URL connection to the Servlet
				FileInputStream fileInputStream = new FileInputStream(
						sourceFile);
				URL url = new URL(upLoadServerUri);

				// Open a HTTP connection to the URL
				conn = (HttpURLConnection) url.openConnection();
				conn.setDoInput(true); // Allow Inputs
				conn.setDoOutput(true); // Allow Outputs
				conn.setUseCaches(false); // Don't use a Cached Copy
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Connection", "Keep-Alive");
				conn.setRequestProperty("ENCTYPE", "multipart/form-data");
				conn.setRequestProperty("Content-Type",
						"multipart/form-data;boundary=" + boundary);
				conn.setRequestProperty("uploaded_file", fileName);

				dos = new DataOutputStream(conn.getOutputStream());

				dos.writeBytes(twoHyphens + boundary + lineEnd);
				dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
						+ fileName + "\"" + lineEnd);

				dos.writeBytes(lineEnd);

				// create a buffer of maximum size
				bytesAvailable = fileInputStream.available();

				bufferSize = Math.min(bytesAvailable, maxBufferSize);
				buffer = new byte[bufferSize];

				// read file and write it into form...
				bytesRead = fileInputStream.read(buffer, 0, bufferSize);

				while (bytesRead > 0) {

					dos.write(buffer, 0, bufferSize);
					bytesAvailable = fileInputStream.available();
					bufferSize = Math.min(bytesAvailable, maxBufferSize);
					bytesRead = fileInputStream.read(buffer, 0, bufferSize);

				}

				// send multipart form data necesssary after file data...
				dos.writeBytes(lineEnd);
				dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

				// Responses from the server (code and message)
				serverResponseCode = conn.getResponseCode();
				String serverResponseMessage = conn.getResponseMessage();

				Log.i(TAG, "HTTP Response is : "
						+ serverResponseMessage + ": " + serverResponseCode);

				if (serverResponseCode == 200) {

					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(mContext,
									"File Upload Complete.", Toast.LENGTH_SHORT)
									.show();

							if (debugMode == false) {
								File file = new File(sourceFileUri);
								file.delete();
							}
						}
					});
				}

				// close the streams //
				fileInputStream.close();
				dos.flush();
				dos.close();

			} catch (MalformedURLException ex) {

				ex.printStackTrace();

				Log.e(TAG, "error: " + ex.getMessage(), ex);
			} catch (Exception e) {

				e.printStackTrace();

				Log.e(TAG,
						"Exception : " + e.getMessage(), e);
			}
			return serverResponseCode;

		} // End else block
	}
}
