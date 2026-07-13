package com.xiyue.upload.controller;

import com.xiyue.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

/**
 * 模拟上传控制器，MVP 阶段不实际存储文件，仅返回 mock URL。
 */
@Slf4j
@RestController
@RequestMapping("/api/upload")
@Tag(name = "模拟上传", description = "模拟图片上传接口")
public class MockUploadController {

    @PostMapping("/mock")
    @Operation(summary = "模拟上传", description = "模拟文件上传，接收 MultipartFile 并返回 mock URL")
    public Result<Map<String, String>> mockUpload(@RequestParam("file") MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = ".jpg";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String mockUrl = "/mock-uploads/" + UUID.randomUUID() + extension;
        log.info("Mock upload: {} -> {}, size: {} bytes", originalFilename, mockUrl, file.getSize());
        return Result.success(Map.of("url", mockUrl));
    }
}
