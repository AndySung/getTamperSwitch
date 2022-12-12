package com.garen.gettamperswitch.ota;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

public class TcpClient implements Runnable{private static DataInputStream din;
    private static DataOutputStream dout;
    private static Socket clientSocket;
    private static String TAG = "TcpClient";
    private boolean isDownloadDone = false;
    private OtaHr40 mOtaHr40 = null;
    private String IpAddress = "192.168.1.100";
    private int port = 6969;

    //private static String HR40_OTA_PACKET_NAME = "0101-111222.pdf";    //  f65212d4f43046af22cdcdedee3739dc *0101-111222.pdf
    private static String HR40_OTA_PACKET_NAME = "HR40-OTA-v9.20221201_to_v9.20221202_no_block.zip";    //  4fbdb63d6df21dd711a1f113c1a433de *OTA-HR40.EV.v7.20220511.1109.zip   //"OTA-HR40.EV.v7.20220511.1109.zip"

    private static String HR40_OTA_PACKET_DIR = "/data/media/0/Download/";


    public TcpClient(OtaHr40 ota, String ip_address, int i_port){
        mOtaHr40 = ota;
        IpAddress = ip_address;
        port = i_port;
    }

    private String getOtaFileName(){
        return HR40_OTA_PACKET_DIR + HR40_OTA_PACKET_NAME;
    }

    @Override
    public void run() {
        try {
            clientSocket = new Socket(IpAddress, port);
            din = new DataInputStream(clientSocket.getInputStream());
            dout = new DataOutputStream(clientSocket.getOutputStream());
            String str = "";
            int i = 0;
            str = din.readUTF();
            Log.i(TAG,"Server: " + str);    // Server: Enter your name
            dout.writeUTF("HR40 APP");
            str = din.readUTF();
            Log.i(TAG,"Server: " + str);    // Server: Welcome, HR40 APP
            while(!(i == 3)) {
                /*
                    Server: Choose an option
                    1. Download File
                    2. List Files
                    3. Exit
                    */
                str = din.readUTF();
                Log.i(TAG,"\nServer: " + str);


                // Client select: List Files
                dout.writeInt(2);

                // 读取 TCP Server 发送的信息: 其将列出 文件列表
                /*
                    Select an Option
                    1. Filter Files by Department
                    2. Enter Serial No. to Download
                    3. Enter FileName to Download
                    4. Go To Main Menu
                    */
                str = din.readUTF();
                Log.i(TAG,"\nServer: " + str);

                // Client select: Enter FileName to Download
                dout.writeInt(3);


                // Server: Enter the FileName which you want to Download
                str = din.readUTF();
                Log.i(TAG,"\nServer: " + str);

                // Client input: OTA File name
                dout.writeUTF(HR40_OTA_PACKET_NAME);
                receiveFile();

                /*
                    Server: Select an Option
                    1. Filter Files by Department
                    2. Enter Serial No. to Download
                    3. Enter FileName to Download
                    4. Go To Main Menu*/
                str = din.readUTF();
                Log.i(TAG,"\nServer: " + str);

                // Client select: Go To Main Menu
                dout.writeInt(4);

                /*
                    Server: Choose an option
                    1. Download File
                    2. List Files
                    3. Exit*/
                str = din.readUTF();
                Log.i(TAG,"\nServer: " + str);

                // Client select: Exit
                dout.writeInt(3);
                break;
            }

            clientSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        // 升级 HR40 BSP 固件.
        mOtaHr40.otaUpdate(getOtaFileName());
    }


    private void receiveFile(){
        int bytesRead = 0, current = 0;
        int len;

        try {
            String fileName = din.readUTF();
            int fileLength = din.readInt();

            byte[] byteArray = new byte[1024];

            BufferedInputStream bis = new BufferedInputStream(din);

            File file = new File(HR40_OTA_PACKET_DIR + fileName);
            Log.i(TAG,"Will downloading file :" + file.getAbsolutePath() + "(" + fileLength + ")");

            // fileFoundFlag is a Flag which denotes the file is present or absent from the Server directory, is present int 0 is sent, else 1
            int fileFoundFlag = din.readInt();
            if(fileFoundFlag == 1)
                return;

            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));

            // 注意: TCP read 是阻塞的.
//            while ( true){
//                bytesRead = bis.read(byteArray);
//                Log.i(TAG,"bytesRead " + bytesRead );
//                bos.write(byteArray, 0, bytesRead);
//
//                if(bis.available() != 0){
//                    break;
//                }
//            }

            do{
                // 注意: TCP read 是阻塞的.
                bytesRead = bis.read(byteArray);
                bos.write(byteArray, 0, bytesRead);
                current += bytesRead;
                //Log.i(TAG,"current Szie :" + current + "; now bytesRead : " + bytesRead  );
                // 当实际接收到字节大小 和 预期一样的时候，停止接收数据.
            }while (current != fileLength);
            isDownloadDone = true;
            bos.close();
            Log.i(TAG,"        File " + fileName + " Successfully Downloaded!" );
            //writeInt is used to reset if any bytes are present in the buffer after the file transfer
            dout.writeInt(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
