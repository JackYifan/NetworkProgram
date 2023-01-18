package cn.zju;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import cn.zju.util.ByteBufferUtil;

@Slf4j
public class Server {
    public static void main(String[] args) throws IOException {
        //创建socket
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.bind(new InetSocketAddress(8080));
        //创建缓冲区
        ByteBuffer buffer = ByteBuffer.allocate(16);
        while(true) {
            log.debug("listening");
            SocketChannel sc = ssc.accept();
            sc.read(buffer);
            //转为读模式用于输出
            buffer.flip();
            ByteBufferUtil.debugRead(buffer);
            buffer.clear();
        }
    }
}
