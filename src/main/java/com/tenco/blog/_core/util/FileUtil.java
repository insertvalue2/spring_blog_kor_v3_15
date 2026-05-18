package com.tenco.blog._core.util;

import com.tenco.blog._core.errors.Exception400;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

// IoC 안함 (파일 기능 처리에만 동작할 수 있도록 static 메서드로 구현할 예정)
public class FileUtil {

    // 업로드될 파일 경로를 미지 상수로 지정
    // System.getProperty("user.home")을 사용해서 OS 상관없이
    // 사용자 홈 경로를 동적으로 설정해서 가져 옴
    // 예) window : C:\Users\사용명\blog_uploads
    // 예) Mac : /Users/사용자명/blog_uploads
    public static final String IMAGES_DIR = Paths.get(System
            .getProperty("user.home"), "blog_uploads").toString();

    // 1. 파일 저장 하는 기능
    public static String saveFile(MultipartFile file, String uploadDir) throws IOException {
        // 1단계 : 파일 유효성 검사 - 파일이 없거나 크기가 0이면 오류
        if (file == null || file.isEmpty()) {
            return  null; // 프로필 이미지 업로드는 선택 사항 임.
        }

        // 2단계 : 파일 업로드 경로 생성 (존재 여부 확인)
        // Path : 파일 시스템 경로를 나타내는 객체
        // Path.get() : 문자열 경로를 Path 객체로 변환해주는 객체
        Path uploadPath = Paths.get(IMAGES_DIR);
        // Files.exists() : 파일/디텍토리 존재 여부 확인
        if(Files.exists(uploadPath) == false) {
            // 현재 서버 컴퓨터에 images/* 없는 상태
            Files.createDirectories(uploadPath); // 상위 폴더까지 자동 생성 해 줌
        }

        // 3단계 : 원본 파일 이름 가져오기
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new Exception400("파일명이 없습니다");
        }

        // 4단계 : UUID를 사용한 고유 파일명 생성
        String uuid = UUID.randomUUID().toString(); // 난수 발생
        String savedFileName = uuid + "_" + originalFilename;
        // 예) "12334123-123e-123_a.png 파일명으로 재 생성 됨.

        // 5단계 : 메모리상에 존재하는 파일 데이터를 로컬 컴퓨터(디스크)에 저장
        // 5.1 - 파일폴더경로 + 재생성한파일이름 ---> 정확한 위치에 파일이 생성 됨
        //  예 : images/123-2322-123_a.png
        Path filePath = uploadPath.resolve(savedFileName);

        Files.copy(file.getInputStream(), filePath);
        return savedFileName;
    }

    // 2. 파일 삭제 하는 기능
    public static void deleteFile(String fileName, String uploadDir) throws IOException {
        if (fileName == null || fileName.isEmpty()) {
            return;
        }
        // Path  -> C://upload/xxx_a.png
        Path filePath = Paths.get(uploadDir, fileName);
        if(Files.exists(filePath)) {
            // 정확한 폴더 경로 존재 확인, 파일명 기준으로 파일이 존재 한다면
            Files.delete(filePath); // 실제 폴더에서 파일 삭제 됨.
        }

    }


    // 3. 편의 기능 만들 예정 (이미지 파일이 맞는지 확인)
    public static boolean isImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        // pdf, hwp <-- 막아 줘야 한다.
        String contentType = file.getContentType(); // image/png, image/jpg, application/pdf
        boolean isImage = contentType.startsWith("image/");
        return isImage;
    }

}
