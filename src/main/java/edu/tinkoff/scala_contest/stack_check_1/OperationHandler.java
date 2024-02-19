package edu.tinkoff.scala_contest.stack_check_1;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class OperationHandler implements Handler {

    public static final long OPERATION_TIMEOUT = 15;
    private static final long START_TIME = System.currentTimeMillis();
    public final ExecutorService service = Executors.newFixedThreadPool(2);
    private final Client client;
    private long lastRequestTime;
    private int retriesCount = 0;

    public OperationHandler(Client client) {
        this.client = client;
    }

    @Override
    public ApplicationStatusResponse performOperation(String id) {
        List<Callable<Response>> callableTasks = new ArrayList<>();
        callableTasks.add(() -> client.getApplicationStatus1(id));
        callableTasks.add(() -> client.getApplicationStatus2(id));

        List<Future<Response>> futures;

        try {
            futures = service.invokeAll(callableTasks);
        } catch (InterruptedException e) {
            lastRequestTime = System.currentTimeMillis() - START_TIME;
            retriesCount++;
            return new ApplicationStatusResponse.Failure(Duration.ofMillis(lastRequestTime), retriesCount);
        } finally {
            service.shutdown();
        }

        return getResponse(futures);
    }

    private ApplicationStatusResponse getResponse(List<Future<Response>> responseFutures) {
        Response response1 = getResultFromFuture(responseFutures.get(0));
        Response response2 = getResultFromFuture(responseFutures.get(1));

        Response processedResponse = null;
        if (response1 != null) {
            processedResponse = response1;
        } else if (response2 != null) {
            processedResponse = response2;
        }

        if (processedResponse instanceof Response.Success) {
            return new ApplicationStatusResponse.Success(
                    ((Response.Success) processedResponse).applicationId(),
                    ((Response.Success) processedResponse).applicationStatus()
            );
        } else if (processedResponse instanceof Response.RetryAfter) {
            return new ApplicationStatusResponse.Failure(
                    Duration.ofMillis(System.currentTimeMillis() - START_TIME),
                    ++retriesCount
            );
        } else if (processedResponse instanceof Response.Failure) {
            return new ApplicationStatusResponse.Failure(
                    Duration.ofMillis(System.currentTimeMillis() - START_TIME),
                    ++retriesCount
            );
        }

        return new ApplicationStatusResponse.Failure(
                Duration.ofMillis(lastRequestTime),
                retriesCount
        );
    }

    private Response getResultFromFuture(Future<Response> responseFuture) {
        Response response = null;
        try {
            response = responseFuture.get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            lastRequestTime = System.currentTimeMillis() - START_TIME;
            retriesCount++;
        }
        return response;
    }
}
