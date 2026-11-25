package com.example.campusmarket.controller;

import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.common.UploadRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/upload")
public class FileUploadController {

    private static final Logger log = LoggerFactory.getLogger(FileUploadController.class);

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 单张图片最大 5MB
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 单个文件最大 20MB
    private static final Set<String> ALLOWED_EXT = new HashSet<>(
            Arrays.asList("jpg", "jpeg", "png", "webp", "gif"));
    // 聊天可传输的文档类后缀白名单
    private static final Set<String> ALLOWED_FILE_EXT = new HashSet<>(
            Arrays.asList("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
                    "txt", "csv", "zip", "rar", "7z", "md"));

    @Value("${upload.path}")
    private String uploadPath;

    @Autowired
    private UploadRateLimiter uploadRateLimiter;

    // 上传图片
    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        if (!uploadRateLimiter.tryAcquire(uid)) {
            return Result.error("上传过于频繁，请稍后再试");
        }
        if (file == null || file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            return Result.error("图片大小不能超过 5MB");
        }

        // 1. 校验文件名与后缀（文件名无点或点在首尾会导致越界/异常）
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            return Result.error("文件名非法");
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot <= 0 || dot == originalFilename.length() - 1) {
            return Result.error("图片格式不支持");
        }
        String suffix = originalFilename.substring(dot + 1).toLowerCase();
        if (!ALLOWED_EXT.contains(suffix)) {
            return Result.error("仅支持 jpg/jpeg/png/webp/gif 格式");
        }

        // 2. 魔数校验，防止篡改后缀上传非图片内容（如 svg/html 脚本）
        if (!checkMagicNumber(file, suffix)) {
            return Result.error("文件内容与后缀不符，上传失败");
        }

        // 3. 生成新文件名（UUID，去掉原始文件名避免目录穿越与重名）
        String newFilename = UUID.randomUUID().toString().replace("-", "") + "." + suffix;

        try {
            Path baseDir = Paths.get(uploadPath).toAbsolutePath().normalize();
            Files.createDirectories(baseDir);
            Path dest = baseDir.resolve(newFilename).normalize();
            // 防目录穿越：确保目标路径仍在上传根目录内
            if (!dest.startsWith(baseDir)) {
                return Result.error("非法的文件路径");
            }
            file.transferTo(dest.toFile());

            // 返回相对访问路径，前端展示时再拼接 baseURL，便于换域名不失效
            return Result.success("/upload/" + newFilename);
        } catch (IOException e) {
            log.error("文件上传失败: {}", newFilename, e);
            return Result.error("上传失败，请稍后重试");
        }
    }

    // 上传文件（聊天用，文档类，最大 20MB）
    @PostMapping("/file")
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        if (!uploadRateLimiter.tryAcquire(uid)) {
            return Result.error("上传过于频繁，请稍后再试");
        }
        if (file == null || file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            return Result.error("文件大小不能超过 20MB");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            return Result.error("文件名非法");
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot <= 0 || dot == originalFilename.length() - 1) {
            return Result.error("文件格式不支持");
        }
        String suffix = originalFilename.substring(dot + 1).toLowerCase();
        if (!ALLOWED_FILE_EXT.contains(suffix)) {
            return Result.error("不支持的文件类型：" + suffix);
        }
        String newFilename = UUID.randomUUID().toString().replace("-", "") + "." + suffix;
        try {
            Path baseDir = Paths.get(uploadPath).toAbsolutePath().normalize();
            Files.createDirectories(baseDir);
            Path dest = baseDir.resolve(newFilename).normalize();
            if (!dest.startsWith(baseDir)) {
                return Result.error("非法的文件路径");
            }
            file.transferTo(dest.toFile());
            return Result.success("/upload/" + newFilename);
        } catch (IOException e) {
            log.error("文件上传失败: {}", newFilename, e);
            return Result.error("上传失败，请稍后重试");
        }
    }

    /**
     * 校验文件头魔数是否与声明后缀一致
     */
    private boolean checkMagicNumber(MultipartFile file, String suffix) {
        try (InputStream in = file.getInputStream()) {
            byte[] head = new byte[12];
            int read = in.read(head);
            if (read < 4) {
                return false;
            }
            int b0 = head[0] & 0xFF, b1 = head[1] & 0xFF, b2 = head[2] & 0xFF, b3 = head[3] & 0xFF;
            switch (suffix) {
                case "jpg":
                case "jpeg":
                    return b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF;
                case "png":
                    return b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47;
                case "gif":
                    return b0 == 0x47 && b1 == 0x49 && b2 == 0x46;
                case "webp":
                    // RIFF....WEBP
                    return b0 == 0x52 && b1 == 0x49 && b2 == 0x46 && b3 == 0x46
                            && read >= 12 && (char) head[8] == 'W' && (char) head[9] == 'E'
                            && (char) head[10] == 'B' && (char) head[11] == 'P';
                default:
                    return false;
            }
        } catch (IOException e) {
            log.warn("读取文件头失败", e);
            return false;
        }
    }
}
