package com.example.lvmin.lico;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Time;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import org.w3c.dom.Text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;

import static java.lang.Math.abs;

public class RecevieActivity extends AppCompatActivity {

    private final static int FLAG_WAV = 0;
    private final static int FLAG_AMR = 1;
    private int mState = -1;    //-1:没再录制，0：录制wav，1：录制amr
    private Button btn_record_wav;
    private Button btn_record_amr;
    private Button btn_stop;
    private TextView txt;
    private UIHandler uiHandler;
    private UIThread uiThread;
    private AudioRecord audioRecord;
    private int bufferSizeInBytes = 1024;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recevie);
        findViewByIds();
        setListeners();
        init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_recevie, menu);
        return true;
    }
    private void findViewByIds(){
        btn_record_wav = (Button)this.findViewById(R.id.btn_record_wav);
        btn_record_amr = (Button)this.findViewById(R.id.btn_record_amr);
        btn_stop = (Button)this.findViewById(R.id.btn_stop);
        txt = (TextView)this.findViewById(R.id.text);
    }
    private void setListeners(){
        btn_record_wav.setOnClickListener(btn_record_wav_clickListener);
        btn_record_amr.setOnClickListener(btn_record_amr_clickListener);
        btn_stop.setOnClickListener(btn_stop_clickListener);
    }
    private void init(){
        uiHandler = new UIHandler();
    }
    private Button.OnClickListener btn_record_wav_clickListener = new Button.OnClickListener(){
        public void onClick(View v) {
            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
            }
            audioRecord = new AudioRecord(AudioFileFunc.AUDIO_INPUT, AudioFileFunc.AUDIO_SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 4096);
            audioRecord.startRecording();
            int state = audioRecord.getRecordingState();
            byte[] buffer = new byte[441000];
            int index = Integer.MAX_VALUE;
            boolean foundHead = false;
            int readByteCount = 0;
            TextView textView = (TextView) findViewById(R.id.textView2);
            textView.setText("已经开始录制");
            String str = "结果：";
            audioRecord.startRecording();
            readByteCount = audioRecord.read(buffer, 0, buffer.length);
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;

            if (!foundHead) {
                for (int i = 0; i + SendActivity.a * 13 / 2 + SendActivity.b < readByteCount / 2; i++) {
                    int sum = CalSum(buffer, i, SendActivity.b);
                    int max = CalMax(buffer, i, SendActivity.b);
                    int min = CalMin(buffer, i, SendActivity.b);
                    int th1 = 10000;
                    int thMax = 3000;
                    int thMin = -3000;
                    if (max > thMax && min < thMin) {
                        // Rising edge detected
                        int sampleh;
                        int samplel;
                        int sample1Max;
                        int sample1Min;
                        int sample2Max;
                        int sample2Min;
                        if (!((sample1Max = CalMax(buffer, i + SendActivity.a / 2, SendActivity.b)) > thMax
                                && (sample1Min = CalMin(buffer, i + SendActivity.a / 2, SendActivity.b)) < thMin))
                            continue;
                        if ((sample2Max = CalMax(buffer, i + SendActivity.a * 3 / 2, SendActivity.b)) > thMax
                                && (sample2Min = CalMin(buffer, i + SendActivity.a * 3 / 2, SendActivity.b)) < thMin)
                            continue;
                        if ((CalMax(buffer, i + SendActivity.a * 5 / 2, SendActivity.b)) > thMax
                                && (CalMin(buffer, i + SendActivity.a * 5 / 2, SendActivity.b)) < thMin)
                            continue;
                        if ((CalMax(buffer, i + SendActivity.a * 7 / 2, SendActivity.b)) > thMax
                                && (CalMin(buffer, i + SendActivity.a * 7 / 2, SendActivity.b)) < thMin)
                            continue;
                        if ((CalMax(buffer, i + SendActivity.a * 9 / 2, SendActivity.b)) > thMax
                                && (CalMin(buffer, i + SendActivity.a * 9 / 2, SendActivity.b)) < thMin)
                            continue;
                        if ((CalMax(buffer, i + SendActivity.a * 11 / 2, SendActivity.b)) > thMax
                                && (CalMin(buffer, i + SendActivity.a * 11 / 2, SendActivity.b)) < thMin)
                            continue;
                        if ((CalMax(buffer, i + SendActivity.a * 13 / 2, SendActivity.b)) > thMax
                                && (CalMin(buffer, i + SendActivity.a * 13 / 2, SendActivity.b)) < thMin)
                            continue;

                        index = (int) (i + 7.5 * SendActivity.a);
                        foundHead = true;
                        break;
                    }
                }
            }
            int thresholdMax = 3000;
            int thresholdMin = -3000;

            byte[] resultBytes = new byte[buffer.length];
            int resultLength = 0;
            if (foundHead) {
                byte b = 0;
                int bIndex = 0;
                while (index + SendActivity.b < readByteCount / 2) {
                    b = (byte) (b << 1);
                    int sample = CalSum(buffer, index, SendActivity.b);
                    int sampleMax = CalMax(buffer, index, SendActivity.b);
                    int sampleMin = CalMin(buffer, index, SendActivity.b);
                    if (sampleMax > thresholdMax && sampleMin < thresholdMin) {
                        b = (byte) (b | 0x01);
                    }
                    bIndex++;
                    if (bIndex == 8) {
                        bIndex = 0;
                        if (b == 0b00110000)
                            break;
                        resultBytes[resultLength] = b;
                        ++resultLength;
                    }
                    index += SendActivity.a;
                }
                ByteBuffer resultByteBuffer = ByteBuffer.wrap(resultBytes, 0, resultLength);
                CharBuffer resultCharBuffer = Charset.forName("UTF-8").decode(resultByteBuffer);
                str = str + resultCharBuffer.toString() ;
            }

            textView.setText(str);
        }
    };
    private Button.OnClickListener btn_record_amr_clickListener = new Button.OnClickListener(){
        public void onClick(View v){
            record(FLAG_AMR);
        }
    };
    private Button.OnClickListener btn_stop_clickListener = new Button.OnClickListener(){
        public void onClick(View v){
            stop();
        }
    };
    /**
     * 开始录音
     * @param mFlag，0：录制wav格式，1：录音amr格式
     */
    private void record(int mFlag){
        if(mState != -1){
            Message msg = new Message();
            Bundle b = new Bundle();// 存放数据
            b.putInt("cmd",CMD_RECORDFAIL);
            b.putInt("msg", ErrorCode.E_STATE_RECODING);
            msg.setData(b);

            uiHandler.sendMessage(msg); // 向Handler发送消息,更新UI
            return;
        }
        int mResult = -1;
        switch(mFlag){
            case FLAG_WAV:
                AudioRecordFunc mRecord_1 = AudioRecordFunc.getInstance();
                mResult = mRecord_1.startRecordAndFile();
                break;
            case FLAG_AMR:
                MediaRecordFunc mRecord_2 = MediaRecordFunc.getInstance();
                mResult = mRecord_2.startRecordAndFile();
                break;
        }
        if(mResult == ErrorCode.SUCCESS){
            uiThread = new UIThread();
            new Thread(uiThread).start();
            mState = mFlag;
        }else{
            Message msg = new Message();
            Bundle b = new Bundle();// 存放数据
            b.putInt("cmd",CMD_RECORDFAIL);
            b.putInt("msg", mResult);
            msg.setData(b);

            uiHandler.sendMessage(msg); // 向Handler发送消息,更新UI
        }
    }
    /**
     * 停止录音
     */
    private void stop(){
        if(mState != -1){
            switch(mState){
                case FLAG_WAV:
                    AudioRecordFunc mRecord_1 = AudioRecordFunc.getInstance();
                    mRecord_1.stopRecordAndFile();

                    break;
                case FLAG_AMR:
                    MediaRecordFunc mRecord_2 = MediaRecordFunc.getInstance();
                    mRecord_2.stopRecordAndFile();
                    break;
            }
            if(uiThread != null){
                uiThread.stopThread();
            }
            if(uiHandler != null)
                uiHandler.removeCallbacks(uiThread);
            Message msg = new Message();
            Bundle b = new Bundle();// 存放数据
            b.putInt("cmd",CMD_STOP);
            b.putInt("msg", mState);
            msg.setData(b);
            uiHandler.sendMessageDelayed(msg,1000); // 向Handler发送消息,更新UI
            mState = -1;
        }
    }

    private int GetInt(byte[] source, int index) {
        return source[index * 2] + (source[index * 2 + 1] << 8);
    }

    private int CalSum(byte[] source, int start, int length)
    {
        int sum =  abs(GetInt(source, start));
        for (int i = start + 1; i < start + length; i++){
            int newInt = GetInt(source, i);
            sum += abs(newInt);
        }
        return sum;
    }

    private int CalMax(byte[] source, int start, int length){
        int max = -10000;
        for (int i = start; i < start + length; i++){
            int newInt = GetInt(source,i);
            if (max <= newInt)
                max = newInt;
        }
        return max;
    }

    private  int CalMin(byte[] source, int start, int length){
        int min = 10000;
        for (int i = start; i < start + length; i++){
            int newInt = GetInt(source, i);
            if (min >= newInt)
                min = newInt;
        }
        return  min;
    }

    /**
     * 去头去尾
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private HashMap removeHeaderTail(byte data[], int length){
        byte[] newData = new byte[length-1];
        byte header = 0;
        byte tail = 1;
        byte esc = 2;
        HashMap hashmap = new HashMap();
        int newLength = 0, i = 0;
        while (data[i]==header && i < length) i++;
        for (; i < length - 1; ++i){
            if (data[i]==esc){
                i++;  // escape ESC
                newData[newLength] = (byte)(data[i]-esc);
                newLength++;
            }
            else{
                newData[newLength] = data[i];
                newLength++;
            }
        }
        hashmap.put("length", newLength);
        hashmap.put("data", newData);
        return hashmap;
    }

    /**
     * 反扩展
     */
    private HashMap getNotExtendedStringBytes(byte data[], int length){
        HashMap hashMap = new HashMap();
        byte[] notExtendedStringBytes = new byte[length / 16];

        for (int i = 0; i < length; i+=16) {
            int number = 0;
            for (int j = 0; j < 8; j++) {
                if (data[i + 2 * j] == 0xff && data[i + 2 * j + 1] == 0x7f)
                    number += 2 ^ (7 - j);
            }

            notExtendedStringBytes[i / 16] = (byte)number;
        }
        hashMap.put("length",length/16);
        hashMap.put("data",data);
        return hashMap;
    }

    /**
     * 过滤器
     */
    private byte[] filter(byte data[], int length){
        byte[] newData = new byte[length];
        for (int i = 0;i < length; i+=2){
            if ((data[i] | ((int)data[i+1] <<  8)) < 0x01ff) {
                newData[i] = 0x00;
                newData[i + 1] = 0x00;
            }
            else {
                newData[i] = (byte) 0xff;
                newData[i + 1] = 0x7f;
            }
        }

        return newData;
    }

    private final static int CMD_RECORDING_TIME = 2000;
    private final static int CMD_RECORDFAIL = 2001;
    private final static int CMD_STOP = 2002;
    class UIHandler extends Handler{
        public UIHandler() {
        }
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            Log.d("MyHandler", "handleMessage......");
            super.handleMessage(msg);
            Bundle b = msg.getData();
            int vCmd = b.getInt("cmd");
            switch(vCmd)
            {
                case CMD_RECORDING_TIME:
                    int vTime = b.getInt("msg");
                    RecevieActivity.this.txt.setText("正在录音中，已录制："+vTime+" s");
                    break;
                case CMD_RECORDFAIL:
                    int vErrorCode = b.getInt("msg");
                    String vMsg = ErrorCode.getErrorInfo(RecevieActivity.this, vErrorCode);
                    RecevieActivity.this.txt.setText("录音失败："+vMsg);
                    break;
                case CMD_STOP:
                    int vFileType = b.getInt("msg");
                    switch(vFileType){
                        case FLAG_WAV:
                            AudioRecordFunc mRecord_1 = AudioRecordFunc.getInstance();
                            long mSize = mRecord_1.getRecordFileSize();
                            RecevieActivity.this.txt.setText("录音已停止.录音文件:"+AudioFileFunc.getWavFilePath()+"\n文件大小："+mSize);
                            break;
                        case FLAG_AMR:
                            MediaRecordFunc mRecord_2 = MediaRecordFunc.getInstance();
                            mSize = mRecord_2.getRecordFileSize();
                            RecevieActivity.this.txt.setText("录音已停止.录音文件:"+AudioFileFunc.getAMRFilePath()+"\n文件大小："+mSize);
                            break;
                    }
                    break;
                default:
                    break;
            }
        }
    };
    class UIThread implements Runnable {
        int mTimeMill = 0;
        boolean vRun = true;
        public void stopThread(){
            vRun = false;
        }
        public void run() {
            while(vRun){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                mTimeMill ++;
                Log.d("thread", "mThread........"+mTimeMill);
                Message msg = new Message();
                Bundle b = new Bundle();// 存放数据
                b.putInt("cmd",CMD_RECORDING_TIME);
                b.putInt("msg", mTimeMill);
                msg.setData(b);

                RecevieActivity.this.uiHandler.sendMessage(msg); // 向Handler发送消息,更新UI
            }

        }
    }
}


