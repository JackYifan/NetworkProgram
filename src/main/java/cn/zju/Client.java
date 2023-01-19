package cn.zju;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


public class Client {
    public static void main(String[] args) throws IOException {
        SocketChannel sc = SocketChannel.open();
        sc.connect(new InetSocketAddress(8080));
        sc.write(Charset.defaultCharset().encode("Hello\n"));
        System.in.read();
        // 客户端接收数据
        // int count = 0;
        // while(true){
        //     ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);
        //     int len = sc.read(buffer);
        //     count += len;
        //     System.out.println("received: " + count);
        //     buffer.clear();
        // }
    }
}
