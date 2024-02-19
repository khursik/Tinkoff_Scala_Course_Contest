package edu.tinkoff.scala_contest.stack_check_2;

public interface Client {
    //блокирующий метод для чтения данных
    Event readData();
    //блокирующий метод отправки данных
    Result sendData(Address dest, Payload payload);
}