package edu.tinkoff.scala_contest.stack_check_2;

import java.time.Duration;
import java.util.concurrent.*;

public class OperationHandler implements Handler {

    private final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 2);
    private final LinkedBlockingQueue<Event> queue = new LinkedBlockingQueue<>();
    private final Client client;

    public OperationHandler(Client client) {
        this.client = client;
    }

    @Override
    public Duration timeout() {
        return Duration.ofMillis(1000L);
    }

    @Override
    public void performOperation() {
        Thread producer = new Thread(() -> {
            while (true) {
                Event event = client.readData();

                while (true) {
                    while (queue.size() > 1000) {
                        doWait();
                    }
                    queue.add(event);
                    queue.notify();
                    break;
                }
            }
        });
        Thread consumer = new Thread(() -> {
            while (true) {
                Event event = getEvent();
                for (Address recipient : event.recipients()) {
                    service.submit(() -> {
                        Result result = Result.REJECTED;
                        while (result == Result.REJECTED) {
                            try {
                                result = client.sendData(recipient, event.payload());
                                if (result == Result.REJECTED) {
                                    Thread.sleep(timeout());
                                }
                            } catch (InterruptedException ignore) {
                            }
                        }
                    });
                }
            }
        });
        producer.start();
        consumer.start();
    }

    private Event getEvent() {
        while (true) {
            while (queue.isEmpty()) {
                doWait();
            }
            Event event = queue.poll();
            queue.notify();
            return event;
        }
    }

    private void doWait() {
        try {
            queue.wait();
        } catch (InterruptedException ignore) {}
    }
}
