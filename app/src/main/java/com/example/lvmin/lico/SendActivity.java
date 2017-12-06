package com.example.lvmin.lico;

import android.media.MediaPlayer;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;

public class SendActivity extends AppCompatActivity {
    public static  int a = 500;
    public static  int b = 20;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);

        Button sendButton = (Button) findViewById(R.id.sendButton);
        final EditText editText = (EditText) findViewById(R.id.stringContent_send);
        final TextView textView = (TextView) findViewById(R.id.textView);
        sendButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Called when a view has been clicked.
             *
             * @param v The view that was clicked.
             */
            @Override
            public void onClick(View v)
            {
                String str = "";
                ByteBuffer stringByteBuffer = Charset.forName("UTF-8").encode(editText.getText().toString());
                byte[] extendedStringBytes = (byte[])getExtendedStringByte(stringByteBuffer).get("data");                 //得到扩展的字符串比特流
                int lengthOfextendedStringBytes = (int)getExtendedStringByte(stringByteBuffer).get("length");
                byte[] waveByte = getWavByte(extendedStringBytes,lengthOfextendedStringBytes);

                //for (int i = 0; i <lengthOfextendedStringBytes; i++)
                //{
                //    str = str + (0xFF &extendedStringBytes[i]) + ' ';
                //}

                str = str + '\n' + Charset.forName("UTF-8").decode(stringByteBuffer);
                textView.setText(str);
                PrintStreamDemo(waveByte,lengthOfextendedStringBytes + 44);
            }

            public boolean[] getBooleanArray(byte b)                //得到byte的每一位的值
            {
                boolean[] array = new boolean[8];
                for (int i = 7; i >= 0; i--)   //对于byte的每bit进行判定
                {
                    array[i] = (b & 1) == 1;   //判定byte的最后一位是否为1，若为1，则是true；否则是false
                    b = (byte) (b >> 1);       //将byte右移一位
                }
                return array;
            }
            public HashMap getExtendedStringByte(ByteBuffer stringByteBuffer)         //扩展比特数组
            {
                HashMap hashMap = addHeaderTail(stringByteBuffer.array(),stringByteBuffer.limit());
                byte[] stringBytes = (byte[]) hashMap.get("data");        //字符串比特流表示
                int length = (int) hashMap.get("length");

                byte[] extendedStringBytes = new byte[length * 16 * a];          //扩展的字符串比特流表示
                int count = 0;                                        //扩展字符串比特流长度计数
                for (int i = 0; i < length; i++)
                {
                    boolean[] array = new boolean[8];    //获得该byte每一位的值
                    array = getBooleanArray(stringBytes[i]);
                    for (int j = 0; j < 8; j++)
                    {
                        if (array[j])                      //如果该位是1，则替换为FF7F
                        {
                            for (int k = 0; k < a; k++) {
                                int x = (k % b) * 32768 / 20;
                                extendedStringBytes[count++] = (byte)(x & 0xFF);
                                extendedStringBytes[count++] = (byte)((x & 0xFF00) >> 8);
                            }
                        }
                        else                              //如果该位是0，则替换为0000
                        {
                            for (int k = 0; k < a; k++) {
                                extendedStringBytes[count++] = 0;
                                extendedStringBytes[count++] = 0;
                            }
                        }
                    }
                }
                HashMap newHashMap = new HashMap();
                newHashMap.put("length", length * 16 * a);
                newHashMap.put("data", extendedStringBytes);

                return newHashMap;
            }
            public byte[] getWavByte(byte[] extendedStringByte, int length)
            {
                byte[] wavByte = new byte[44 + length];

                /*
                    RIFF WAVE Chunk
                    ==================================
                    |       |所占字节数|  具体内容   |
                    ==================================
                    | ID    |  4 Bytes |   'RIFF'    |
                    ----------------------------------
                    | Size  |  4 Bytes |             |
                    ----------------------------------
                    | Type  |  4 Bytes |   'WAVE'    |
                    ----------------------------------
                 */

                wavByte[0] = 'R';
                wavByte[1] = 'I';
                wavByte[2] = 'F';
                wavByte[3] = 'F';

                int size = 36 + length;
                wavByte[7] = (byte)((size>>>24) & 0xff);
                wavByte[6] = (byte)((size>>>16) & 0xff);
                wavByte[5] = (byte)((size>>>8) & 0xff);
                wavByte[4] = (byte)((size) & 0xff);

                wavByte[8] = 'W';
                wavByte[9] = 'A';
                wavByte[10] = 'V';
                wavByte[11] = 'E';

                /*
                    Format Chunk
                    ====================================================================
                    |               |   字节数  |              具体内容                |
                    ====================================================================
                    | ID            |  4 Bytes  |   'fmt '                             |
                    --------------------------------------------------------------------
                    | Size          |  4 Bytes  | 数值为16或18，18则最后又附加信息     |
                    --------------------------------------------------------------------  ----
                    | FormatTag     |  2 Bytes  | 编码方式，一般为0x0001               |     |
                    --------------------------------------------------------------------     |
                    | Channels      |  2 Bytes  | 声道数目，1--单声道；2--双声道       |     |
                    --------------------------------------------------------------------     |
                    | SamplesPerSec |  4 Bytes  | 采样频率                             |     |
                    --------------------------------------------------------------------     |
                    | AvgBytesPerSec|  4 Bytes  | 每秒所需字节数                       |     |===> WAVE_FORMAT
                    --------------------------------------------------------------------     |
                    | BlockAlign    |  2 Bytes  | 数据块对齐单位(每个采样需要的字节数) |     |
                    --------------------------------------------------------------------     |
                    | BitsPerSample |  2 Bytes  | 每个采样需要的bit数                  |     |
                    --------------------------------------------------------------------     |
                    |               |  2 Bytes  | 附加信息（可选，通过Size来判断有无） |     |
                    --------------------------------------------------------------------  ----
                 */

                wavByte[12] = 'f';                                // fmt
                wavByte[13] = 'm';
                wavByte[14] = 't';
                wavByte[15] = ' ';

                size = 16;
                wavByte[19] = (byte)((size>>>24) & 0xff);        //size
                wavByte[18] = (byte)((size>>>16) & 0xff);
                wavByte[17] = (byte)((size>>>8) & 0xff);
                wavByte[16] = (byte)((size) & 0xff);

                wavByte[20] = 0x01;                             //编码方式
                wavByte[21] = 0x00;

                wavByte[22] = 0x01;                             //左声道
                wavByte[23] = 0x00;

                wavByte[24] = 0x44;                             //采样频率
                wavByte[25] = (byte)0xAC;
                wavByte[26] = 0x00;
                wavByte[27] = 0x00;

                wavByte[28] = 0x08;                             //每秒字节数
                wavByte[29] = (byte)0x88;
                wavByte[30] = 0x15;
                wavByte[31] = 0x00;

                wavByte[32] = 0x02;                             //字节数
                wavByte[33] = 0x00;

                wavByte[34] = 0x10;                             //比特数
                wavByte[35] = 0x00;

                /*
                    Data Chunk
                    ==================================
                    |       |所占字节数|  具体内容   |
                    ==================================
                    | ID    |  4 Bytes |   'data'    |
                    ----------------------------------
                    | Size  |  4 Bytes |             |
                    ----------------------------------
                    | data  |          |             |
                    ----------------------------------
                 */

                wavByte[36] = 'd';                        //data
                wavByte[37] = 'a';
                wavByte[38] = 't';
                wavByte[39] = 'a';

                size = length;
                wavByte[43] = (byte)((size>>>24) & 0xff);        //size
                wavByte[42] = (byte)((size>>>16) & 0xff);
                wavByte[41] = (byte)((size>>>8) & 0xff);
                wavByte[40] = (byte)((size) & 0xff);

                for (int i = 0; i < length; i++)
                {
                    wavByte[44+i] = extendedStringByte[i];
                }
                return wavByte;
            }
            public void PrintStreamDemo(byte[] waveByte, int length){
                try {
                    //FileOutputStream out= openFileOutput("test.wav", MODE_PRIVATE);
                    File file = new File(Environment.getExternalStorageDirectory(), "1.wav");
                    file.delete();
                    file.createNewFile();
                    FileOutputStream out = new FileOutputStream(file);
                    out.write(waveByte);
                    out.flush();
                    out.close();

                    MediaPlayer mediaPlayer = new MediaPlayer();
                    FileInputStream fis = new FileInputStream(file);
                    mediaPlayer.setDataSource(fis.getFD());
                    mediaPlayer.prepare();
                    mediaPlayer.setLooping(false);
                    mediaPlayer.start();
                }
                catch (FileNotFoundException e)
                {
                    e.printStackTrace();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            @SuppressWarnings({ "rawtypes", "unchecked" })
            private HashMap addHeaderTail(byte data[], int length){
                byte[] newData;
                byte header = 1;
                byte tail = 1;
                byte esc = 2;
                HashMap hashmap = new HashMap();
                int newLength = length ,now, header_repeat = 10;
                for (int i=0;i<length;++i)  // calc length
                    if (data[i]==header || data[i] == tail || data[i] == esc) newLength++;
                newLength = newLength + 1 + header_repeat;  // add header and tail length
                newData = new byte[newLength];
                for (int i=0;i < header_repeat;++i) newData[i] = header;  // add header
                now = header_repeat;
                for (int i=0;i<length;++i)
                    if (data[i]==header || data[i] == tail || data[i] == esc){
                        newData[now] = esc;  // add ESC
                        now++;
                        newData[now] = (byte)(esc + data[i]);
                        now++;
                    }else{
                        newData[now] = data[i];
                        now++;
                    }
                newData[now] = tail;
                //hashmap.put("length", newLength);
                //hashmap.put("data", newData);
                hashmap.put("length",length);
                hashmap.put("data",data);
                return hashmap;
            }
        });

        Button backButton = (Button) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }


}
