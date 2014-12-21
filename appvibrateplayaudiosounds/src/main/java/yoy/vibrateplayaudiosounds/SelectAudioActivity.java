package yoy.vibrateplayaudiosounds;

import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.*;


public class SelectAudioActivity extends ActionBarActivity {

    boolean isCancelled = false;
    Vibrator vibrator = null;
    AsyncTask asyncTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_audio);

        vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.select_audio, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private long getCriticalByte(byte[] array){
        long result = -1;

        for (byte b : array){
            if (Math.abs(b) > result){
                result = Math.abs(b);
            }
        }

        return (long)(result * 0.5);
    }

    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Audio.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        startManagingCursor(cursor);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){

        if(requestCode == 1){

            if(resultCode == RESULT_OK){
                final Uri uri = data.getData();

                InputStream is = null;
                try {
                    is = getContentResolver().openInputStream(uri);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                if(is != null)         {
                    try{
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        BufferedInputStream in = new BufferedInputStream(is);

                        int read;
                        byte[] buff = new byte[1024];
                        while ((read = in.read(buff)) > 0)
                        {
                            out.write(buff, 0, read);
                        }
                        out.flush();
                        final byte[] audioBytes = out.toByteArray();
                        final int vibroCount = (audioBytes.length * 2) + 1;
                        final long criticalByte = getCriticalByte(audioBytes);

                        isCancelled = false;
                        asyncTask = new AsyncTask() {
                            @Override
                            protected Object doInBackground(Object[] params) {
                                MediaPlayer mediaPlayer = null;
                                try{
                                    mediaPlayer = new MediaPlayer();
                                    mediaPlayer.setDataSource(SelectAudioActivity.this, uri);
                                    mediaPlayer.prepare();
                                    mediaPlayer.start();
                                }
                                catch (Exception ex){
                                    Toast.makeText(SelectAudioActivity.this, "Media player failed with exception: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                                }

                                int audioBytesIterator = 0;
                                for (int i = 1; i < vibroCount; i++){
                                    if (isCancelled){
                                        mediaPlayer.stop();
                                        break;
                                    }
                                    long audiotByteAbs = Math.abs(audioBytes[audioBytesIterator]);
                                    long vibroTime = audiotByteAbs < criticalByte ? 0 : audiotByteAbs * 3;
                                    long delayTime = vibroTime == 0 ? 500 : 150;
                                    i++;
                                    vibrator.vibrate(new long[] { vibroTime, delayTime }, -1);
                                    audioBytesIterator++;
                                }
                                return null;
                            }
                        };
                        asyncTask.execute();
                    }
                    catch (Exception ex){
                        Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    finally {
                        if (is != null){
                            try{
                                is.close();
                            }
                            catch (Exception ex){

                            }
                        }
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void selectAudioFile(){
        Intent intent_upload = new Intent();
        intent_upload.setType("audio/*");
        intent_upload.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent_upload,1);
    }

    public void btn_onClick(View v){
        selectAudioFile();
    }

    public void btnStop_onClick(View v){
        isCancelled = true;
    }
}
