package yoy.vibrateplayaudiosounds;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import java.io.*;

public class SelectAudioActivity extends ActionBarActivity {

    boolean isCancelled = false;
    Vibrator vibrator = null;
    AsyncTask asyncTask;

    public void btn_onClick(View v){
        selectAudioFile();
    }

    public void btnStop_onClick(View v){
        isCancelled = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_audio);

        vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){

        if(requestCode == 1){

            if(resultCode == RESULT_OK){
                isCancelled = true;
                final Uri uri = data.getData();

                InputStream is = null;
                try {
                    is = getContentResolver().openInputStream(uri);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                if(is != null) {
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
                                isCancelled = false;
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

    @Override
    public void onBackPressed(){
        isCancelled = true;
        finish();
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

    private void selectAudioFile(){
        Intent intent_upload = new Intent();
        intent_upload.setType("audio/*");
        intent_upload.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent_upload,1);
    }
}
