package edu.tinkoff.scala_contest.stack_check_2;

import java.util.List;

public record Event(List<Address> recipients, Payload payload) {}