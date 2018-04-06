package servlets;

import JobExecutors.ResponseWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@WebServlet(name = "randRes", urlPatterns = "/random", asyncSupported = true)
public class RandomResponseServlet extends HttpServlet {
    private int requestHandleCapacity = 150;
    private List<AsyncContext> jobs = Collections.synchronizedList(new ArrayList<>(requestHandleCapacity));
    private Logger logger = LogManager.getLogger();
    private int numberOfThreads = 2;
    private Thread[] workers = new Thread[numberOfThreads];

    @Override
    public void init() throws ServletException {

        for (int i = 0; i < numberOfThreads; i++) {
            workers[i] = new Thread(new ResponseWriter(jobs));
            workers[i].setName("Response-Writer-Thread" + i);
            workers[i].start();
        }
    }

    @Override
    public void destroy() {
        logger.info("Hit Random Write Servlet Destroy");
        for (Thread thread : workers) {
            thread.interrupt();
            try {
                thread.join(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(600000);
        jobs.add(asyncContext);
    }
}
