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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

/**
 * 模拟上传控制器，MVP 阶段将文件保存到本地 uploads/ 目录并通过 /mock-uploads/ 提供访问。
 */
@Slf4j
@RestController
@RequestMapping("/api/upload")
@Tag(name = "模拟上传", description = "模拟图片上传接口")
public class MockUploadController {

    @PostMapping("/mock")
    @Operation(summary = "模拟上传", description = "接收图片文件，保存到本地 uploads 目录，返回可访问的 URL")
    public Result<Map<String, String>> mockUpload(@RequestParam("file") MultipartFile file) {
        try {
            String projectRoot = System.getProperty("user.dir");
            Path uploadPath = Paths.get(projectRoot, "uploads");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = ".jpg";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID() + extension;
            Path targetPath = uploadPath.resolve(filename);
            file.transferTo(targetPath.toFile());

            String url = "/mock-uploads/" + filename;
            log.info("Mock upload: {} -> {}, size: {} bytes", originalFilename, url, file.getSize());
            return Result.success(Map.of("url", url));
        } catch (IOException e) {
            log.error("Mock upload failed", e);
            return Result.error(500, "上传失败: " + e.getMessage());
        }
    }
}
