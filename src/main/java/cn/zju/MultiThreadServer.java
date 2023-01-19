package cn.zju;

import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.zju.util.ByteBufferUtil.debugAll;

@Slf4j
public class MultiThreadServer {
    public static void main(String[] args) throws IOException {
        Thread.currentThread().setName("Listen");
        //创建Selector
        Selector listenSelector = Selector.open();
        //创建socket
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.bind(new InetSocketAddress(8080));
        ssc.configureBlocking(false); //非阻塞
        //将channel注册到selector中
        SelectionKey sscKey = ssc.register(listenSelector, 0, null);
        sscKey.interestOps(SelectionKey.OP_ACCEPT); //处理Accept请求
        log.debug("SelectionKey:{}",sscKey);

        Reader[] readers = new Reader[Runtime.getRuntime().availableProcessors()];
        for(int i = 0; i < readers.length; i++){
            readers[i] = new Reader("reader-"+i);
        }
        AtomicInteger idx = new AtomicInteger();
        while(true) {
            listenSelector.select(); // choose a set of keys
            Iterator<SelectionKey> iter = listenSelector.selectedKeys().iterator();
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
                    readers[idx.getAndIncrement() % readers.length].initAndStart(socketChannel,buffer);
                }
            }
        }

    }

    /**
     * 读取数据的线程
     */
    static class Reader implements Runnable{
        private Selector readSelector; //用于读数据的selector
        private String name; //线程名
        private volatile boolean start = false; //一个reader只能开一个线程
        private Thread thread;
        private ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>(); //用于调整多线程的运行顺序

        Reader(String name){
            this.name = name;
        }

        /**
         *
         * @param socketChannel server的socketChannel
         * @throws IOException
         */
        public void initAndStart(SocketChannel socketChannel,ByteBuffer buffer) throws IOException {
            if(!start){
                readSelector = Selector.open();
                thread = new Thread(this, name);
                thread.start();
                start = true;
            }
            queue.add(()->{
                try {
                    socketChannel.register(readSelector,SelectionKey.OP_READ,buffer); //属于Listen线程，需要在readSelector.select()前执行，否则被阻塞
                } catch (ClosedChannelException e) {
                    throw new RuntimeException(e);
                }
            });
            readSelector.wakeup(); //使得select()不阻塞
        }

        @Override
        public void run() {
            while(true){
                try {
                    readSelector.select(); //Selects a set of keys whose corresponding channels are ready for I/O operations
                    Runnable task = queue.poll();
                    if(task != null){
                        task.run();
                    }
                    Iterator<SelectionKey> iter = readSelector.selectedKeys().iterator();
                    while(iter.hasNext()){
                        SelectionKey key = iter.next();
                        iter.remove();
                        if(key.isReadable()){
                            try {
                                SocketChannel socketChannel = (SocketChannel) key.channel();
                                ByteBuffer buffer = (ByteBuffer) key.attachment();
                                log.debug("read : {}",socketChannel.getRemoteAddress());
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
                } catch (IOException e) {
                    e.printStackTrace();
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