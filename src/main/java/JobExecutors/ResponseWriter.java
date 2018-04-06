package JobExecutors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.AsyncContext;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.BufferOverflowException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ResponseWriter implements Runnable {
    private List<AsyncContext> jobs;
    private Logger logger = LogManager.getLogger();


    public ResponseWriter(List<AsyncContext> jobs) {
        this.jobs = jobs;
    }

    private void writeToResponse(AsyncContext asyncContext) throws IOException {
        int i;
        int dataSize = 10485760;
        try {
            int written = asyncContext.getRequest().getAttribute("written") == null ? 0 : (int) asyncContext.getRequest()
                    .getAttribute("written");
            int limit = 102400;
            PrintWriter out = asyncContext.getResponse().getWriter();
            for (i = written + 1; i < dataSize; i++) {
                if (i > 0 && i % limit == 0) {
                    asyncContext.getRequest().setAttribute("written", i);
                    break;
                }
                out.print((char) (ThreadLocalRandom.current().nextInt(90) + 32));
            }
            if (i >= dataSize) {
                jobs.remove(asyncContext);
                asyncContext.complete();
            } else {
                asyncContext.getRequest().setAttribute("writable", null);
            }

        } catch (Exception e) {
            e.printStackTrace();
            jobs.remove(asyncContext);
        }
    }

    private static synchronized boolean isResponseWritable(AsyncContext asyncContext) throws IllegalStateException{
        if(asyncContext != null && asyncContext.getRequest().getAttribute("writable") == null) {
            asyncContext.getRequest().setAttribute("writable",false );
            return true;
        }
        return false;
    }


    @Override
    public void run() {
        AsyncContext asyncContext = null;
        while (true) {
            if(Thread.interrupted()) {
                logger.info("Hit thread exit");
                break;
            }
            try {
                int index = ThreadLocalRandom.current().nextInt(jobs.size());
                asyncContext = jobs.get(index);
                if (isResponseWritable(asyncContext)) {
                    writeToResponse(asyncContext);
                }
            }
            catch (IndexOutOfBoundsException  | IllegalArgumentException e) {
            }
            catch (IOException  | IllegalStateException e) {
                jobs.remove(asyncContext);
                e.printStackTrace();
            }
        }
    }
}
