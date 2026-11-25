package com.example.campusmarket.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 商品图片缩略图：列表页只取 320px 小图，减少首屏图片体积。
 * 首次访问时生成并缓存到 upload/.thumbs，之后直接命中缓存。
 */
@RestController
@RequestMapping("/thumb")
public class ThumbnailController {

    private static final Logger log = LoggerFactory.getLogger(ThumbnailController.class);
    private static final int MAX_SIZE = 320;
    private static final Set<String> ALLOWED_EXT = new HashSet<>(
            Arrays.asList("jpg", "jpeg", "png", "gif", "webp"));

    @Value("${upload.path}")
    private String uploadPath;

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> thumb(@PathVariable String filename) {
        // 防目录穿越：只取文件名部分
        String name = Paths.get(filename).getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot == name.length() - 1) {
            return ResponseEntity.notFound().build();
        }
        String suffix = name.substring(dot + 1).toLowerCase();
        if (!ALLOWED_EXT.contains(suffix)) {
            return ResponseEntity.notFound().build();
        }

        Path baseDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path source = baseDir.resolve(name).normalize();
        if (!source.startsWith(baseDir) || !Files.isRegularFile(source)) {
            return ResponseEntity.notFound().build();
        }

        // ImageIO 不支持 webp，直接回退原图（浏览器照常显示）
        if ("webp".equals(suffix)) {
            return ResponseEntity.status(302).location(URI.create("/upload/" + name)).build();
        }

        try {
            // 缩略图缓存放在系统临时目录（上传目录在部分机器上仅可读，不能新建文件）
            Path cacheDir = Paths.get(System.getProperty("java.io.tmpdir"), "campus-market-thumbs");
            Files.createDirectories(cacheDir);
            Path cache = cacheDir.resolve(name + ".jpg");
            boolean stale = !Files.exists(cache)
                    || Files.getLastModifiedTime(cache).compareTo(Files.getLastModifiedTime(source)) < 0;
            if (stale) {
                BufferedImage image = ImageIO.read(source.toFile());
                if (image == null) {
                    return ResponseEntity.status(302).location(URI.create("/upload/" + name)).build();
                }
                BufferedImage scaled = scale(image);
                // 先写临时文件再原子替换，避免并发读到半张图
                Path tmp = cacheDir.resolve(name + "." + UUID.randomUUID().toString().replace("-", "") + ".tmp");
                writeJpeg(scaled, tmp);
                try {
                    Files.move(tmp, cache, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (IOException moveError) {
                    Files.move(tmp, cache, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                    .body(new FileSystemResource(cache));
        } catch (Exception e) {
            log.warn("生成缩略图失败: {}", name, e);
            // 缩略图失败不影响商品展示，回退原图
            return ResponseEntity.status(302).location(URI.create("/upload/" + name)).build();
        }
    }

    /**
     * 等比缩放到最长边 MAX_SIZE，透明背景填充白色（JPEG 不支持透明）
     */
    private BufferedImage scale(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int targetWidth = width;
        int targetHeight = height;
        if (width > MAX_SIZE || height > MAX_SIZE) {
            double ratio = Math.min((double) MAX_SIZE / width, (double) MAX_SIZE / height);
            targetWidth = Math.max(1, (int) Math.round(width * ratio));
            targetHeight = Math.max(1, (int) Math.round(height * ratio));
        }
        BufferedImage target = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, targetWidth, targetHeight);
            g.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g.dispose();
        }
        return target;
    }

    private void writeJpeg(BufferedImage image, Path target) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            ImageIO.write(image, "jpg", target.toFile());
            return;
        }
        ImageWriter writer = writers.next();
        try (ImageOutputStream out = new FileImageOutputStream(target.toFile())) {
            writer.setOutput(out);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.82f);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }
}
