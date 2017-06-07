package event_based_server;

import java.io.*;
import java.nio.channels.SelectionKey;
import java.util.concurrent.*;
import java.nio.channels.CompletionHandler;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;


public class FileIOThread {

    private ExecutorService executorService;

    public FileIOThread(){
        executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
        );
    }

    private CompletionHandler<byte[],SelectionKey> callback = new CompletionHandler<byte[], SelectionKey>() {
        @Override
        public void completed(byte[] result, SelectionKey clientKey) {
            clientKey.attach(result); //NOTE: send result to event queue
        }

        @Override
        public void failed(Throwable exc,SelectionKey clientKey) {
//            FIXME : null => something to notify error situation while reading file
            clientKey.attach(null); //NOTE: send error message to event queue
        }
    };

    private Runnable loadFile(String filePath,SelectionKey clientKey){
        Runnable task = new Runnable() {
            @Override
            public void run() {

                Path path = Paths.get("src/event_based_server/sample.txt");
                try {
                    byte[] data= Files.readAllBytes(path);
                    callback.completed(data,clientKey);
//                    FIXME : When the file is bigger than the buffer, Handle it in some way with while statement
//                    while (bufferedInputStream.read(readBuffer, 0, readBuffer.length) != -1) {}

                } catch (IOException e) {
                    callback.failed(e,clientKey);
                }
            }
        };
        return task;
    }

    private Runnable newFile(ByteBuffer bodyContent,String filePath, SelectionKey clientKey){
        Runnable task = new Runnable() {
            @Override
            public void run() {
                FileOutputStream outputStream = null;
                BufferedOutputStream bufferedOutputStream = null;
                byte[] readBuffer = new byte[1024];

                try
                {
                    outputStream = new FileOutputStream(filePath);
                    bufferedOutputStream = new BufferedOutputStream(outputStream);

                    //TODO: following body info from http post request, store it as a file
                    //STEP1 : bodyContent -> readBuffer
                    //STEP2 : readBuffer -> bodyContent
                    //FIXME: When the file is bigger than the buffer, Handle it in some way with while statement
                    bufferedOutputStream.write(readBuffer);

                    callback.completed(readBuffer,clientKey); //FIXME: change result from readBuffer to void or something

                }
                catch (Exception e)
                {
                    callback.failed(e,clientKey);
                }
                finally
                {
                    try
                    {
                        bufferedOutputStream.close();
                    }
                    catch (Exception e)
                    {
                        callback.failed(e,clientKey);
                    }
                }
            }
        };
        return task;
    }

    public void handle(SelectionKey clientKey, HttpParser httpParser){
          //TODO : Following http request info, execute different task with thread pool
            String filePath = httpParser.getRequestURL().substring(1); //FIXME: Parse Directory info and store new file into proper directory
//            FIXME: true => GET // false => POST
            if(true){
                Runnable task = loadFile(filePath,clientKey);
                executorService.submit(task);
            }
            else{
                //FIXME : HttpParser return bytebuffer which contains bodycontent
                ByteBuffer bodyContent = ByteBuffer.allocate(1024);
                Runnable task = newFile(bodyContent,filePath,clientKey);
                executorService.submit(task);
            }

    }

}