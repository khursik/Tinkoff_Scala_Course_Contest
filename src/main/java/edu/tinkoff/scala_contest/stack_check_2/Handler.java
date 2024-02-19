package edu.tinkoff.scala_contest.stack_check_2;

import java.time.Duration;

public interface Handler {
    Duration timeout();
    void performOperation();
}