package com.shike.controller;

import com.shike.common.ResultDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

    @GetMapping("/ping")
    public ResultDTO<String> ping() {
        return ResultDTO.success("pong");
    }
}
