package cn.zju;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import cn.zju.util.ByteBufferUtil;

@Slf4j
public class Server {
    public static void main(String[] args) throws IOException {
        //创建socket
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.bind(new InetSocketAddress(8080));
        ssc.configureBlocking(false); //非阻塞
        //创建缓冲区
        ByteBuffer buffer = ByteBuffer.allocate(16);
        List<SocketChannel> socketChannelList = new ArrayList<>();
        while(true) {
//            log.debug("listening");
            SocketChannel socketChannel = ssc.accept();
            if(socketChannel != null) {
                log.debug("connect:{}",socketChannel);
                socketChannel.configureBlocking(false);
                socketChannelList.add(socketChannel);
            }
            for(SocketChannel sc : socketChannelList) {
                int len = sc.read(buffer);
                if(len != 0) {
                    //转为读模式用于输出
                    buffer.flip();
                    ByteBufferUtil.debugRead(buffer);
                    buffer.clear();
                }

            }

        }
    }
}
