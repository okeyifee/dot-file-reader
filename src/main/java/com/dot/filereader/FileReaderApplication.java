package com.dot.filereader;

import com.dot.filereader.service.FileReaderService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class FileReaderApplication implements CommandLineRunner {
	Logger logger = LoggerFactory.getLogger(FileReaderApplication.class);

	FileReaderService fileReaderService;

	public FileReaderApplication(FileReaderService fileReaderService) {
		this.fileReaderService = fileReaderService;
	}

	public static void main(String[] args) {
		SpringApplication.run(FileReaderApplication.class, args);
	}

	@Override
	public void run(String... args) {
		logger.info("Started DotFileReaderApplication .....");

		if(args.length != 4){
			logger.error("Invalid number of arguments passed. Required 4 arguments but found {}", args.length);
			System.exit(1);
		}

		fileReaderService.ipLimitChecker(args[0], args[1], args[2], args[3]);
		logger.info("Finished running DotFileReaderApplication.");
		System.exit(0);
	}
}
