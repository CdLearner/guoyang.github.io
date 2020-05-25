package com.hiido.service;

public interface TransferService {
        void processJob(String... type)throws Exception;
        void test() throws Exception;
}
