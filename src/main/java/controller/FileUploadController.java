package controller;

import model.OutputRow;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import service.FileService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Controller
public class FileUploadController {

    public static final String UPLOAD_DIRECTORY = System.getProperty("user.dir") + "/uploads";

    private FileService fileService;

    @RequestMapping("/")
    public String uploadPage() {
        return "uploadview";
    }

    @RequestMapping("/upload")
    public String upload(Model model, @RequestParam("files") MultipartFile[] files) {
        StringBuilder fileNames = new StringBuilder();
        for (MultipartFile file : files) {
            Path fileNameAndPath = Paths.get(UPLOAD_DIRECTORY, file.getOriginalFilename());
            fileNames.append(file.getOriginalFilename()).append(" ");
            List<OutputRow> orderedResults = fileService.sortFile(fileNameAndPath);
            model.addAttribute("results", orderedResults);
        }
        model.addAttribute("msg", "File: " + fileNames);
        return "uploadstatusview";
    }
}
