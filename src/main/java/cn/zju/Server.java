package cn.zju;

import cn.zju.util.ByteBufferUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Iterator;

import static cn.zju.util.ByteBufferUtil.debugAll;

@Slf4j
public class Server {
    public static void main(String[] args) throws IOException {
        //创建Selector
        Selector selector = Selector.open();
        //创建socket
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.bind(new InetSocketAddress(8080));
        ssc.configureBlocking(false); //非阻塞
        //将channel注册到selector中
        SelectionKey sscKey = ssc.register(selector, 0, null);
        sscKey.interestOps(SelectionKey.OP_ACCEPT); //处理Accept请求
        log.debug("SelectionKey:{}",sscKey);

        while(true) {
            selector.select(); // choose a set of keys
            Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
            //遍历所有准备好的key
            while(iter.hasNext()) {
                SelectionKey key = iter.next();
                log.debug("key:{}",key);
                iter.remove(); //处理完成后需要删除否则会空指针
                if(key.isAcceptable()) {
                    //处理连接请求
                    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel(); //根据key找到server channel
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    //为每个socketChannel创建对应的缓冲区
                    ByteBuffer buffer = ByteBuffer.allocate(16);
                    //将accept生成的socket channel 加入selector管理
                    socketChannel.configureBlocking(false);
                    SelectionKey scKey = socketChannel.register(selector, 0, buffer);
                    scKey.interestOps(SelectionKey.OP_READ);
                    log.debug("socketChannel:{}", socketChannel);
                    log.debug("scKey:{}",scKey);
                }else if(key.isReadable()) {
                    try {
                        SocketChannel socketChannel = (SocketChannel) key.channel();
                        ByteBuffer buffer = (ByteBuffer) key.attachment();
                        int len = socketChannel.read(buffer);
                        if(len == -1) {
                            key.cancel();//EOF
                        }else {
                            split(buffer);
                            if(buffer.position() == buffer.limit()) {
                                ByteBuffer newBuffer = ByteBuffer.allocate(buffer.capacity()*2); //扩容
                                buffer.flip(); //读之前都切换模式
                                newBuffer.put(buffer); //拷贝原内容
                                key.attach(newBuffer);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        key.cancel();
                    }
                }
            }
        }



    }

    private static void split(ByteBuffer source) {
        source.flip(); //转为读模式
        for(int i = 0; i < source.limit(); i++) {
            if(source.get(i) == '\n') {
                //不会导致position指针后移
                int len = i - source.position() + 1;
                ByteBuffer target = ByteBuffer.allocate(len);
                for(int j = 0; j < len; j++) {
                    target.put(source.get()); //get()导致position指针后移
                }
                debugAll(target);
            }
        }
        source.compact();
    }
}
