package com.dot.filereader.service;

import com.dot.filereader.entity.BlockedIp;
import com.dot.filereader.entity.UserAccessLog;
import com.dot.filereader.repository.BlockedIpTableRepository;
import com.dot.filereader.repository.UserAccessLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class FileReaderService {
    private static final int BATCH_SIZE = 1000;
    Logger logger = LoggerFactory.getLogger(FileReaderService.class);

    private final UserAccessLogRepository userAccessLogRepository;
    private final BlockedIpTableRepository blockedIpTableRepository;

    public FileReaderService(UserAccessLogRepository userAccessLogRepository, BlockedIpTableRepository blockedIpTableRepository) {
        this.userAccessLogRepository = userAccessLogRepository;
        this.blockedIpTableRepository = blockedIpTableRepository;
    }

    /**
     * This method reads the contents of given file and save to the repository in batches.
     *
     * @param path the file path supplied by user.
     *
     * Throws Exception if file is not found.
     */
    public void readFile(final String path){
        if (path  == null || path.isEmpty()) {
            logger.error("Supplied path is blank, {}" , path);
            exit();
        }

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path);
        if (inputStream == null) {
            logger.error("File not found, {}" , path);
            exit();
        }

        ArrayList<UserAccessLog> batch = new ArrayList<>();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            int count = 0;

            while ((line = bufferedReader.readLine()) != null || !batch.isEmpty()) {
                if (line != null) {
                    final String[] parts = line.split("\\|"); // Split using pipe delimiter
                    final Timestamp timestamp = toTimeStamp(parts[0], "yyyy-MM-dd HH:mm:ss.SSS");

                    if (timestamp == null) {
                        continue;
                    }

                    final UserAccessLog userAccessLog = UserAccessLog.builder()
                            .ipAddress(parts[1])
                            .date(timestamp)
                            .request(parts[2])
                            .status(parts[3])
                            .userAgent(parts[4])
                            .build();
                    batch.add(userAccessLog);
                    count++;
                }

                if (batch.size() == BATCH_SIZE || (line == null)) { // check if batch is full or no more content to read
                    saveAccessLogBatch(batch);
                    batch.clear();
                }
            }
            logger.info("Finished reading {} lines from file to database.", count);
        } catch (IOException ex) {
            logger.error("Exception occurred : {}", ex.getMessage());
        }
    }

    public void saveAccessLogBatch(ArrayList<UserAccessLog> entries) {
        if (entries != null && !entries.isEmpty()) {
            userAccessLogRepository.saveAllAndFlush(entries);
            logger.info("Successfully saved batch");
        }
    }

    public void saveBlockedIpsBatch(ArrayList<BlockedIp> entries) {
        if (entries != null && !entries.isEmpty()) {
            blockedIpTableRepository.saveAllAndFlush(entries);
            logger.info("Successfully saved batch");
        }
    }

    /**
     * This method calls read file to read the file contents and then saveBlockedIpsBatch to
     *  retrieve blocked Ips and save to database
     *
     * @param path the file path passed by user.
     * @param startTime the startTime supplied by user.
     * @param duration the duration supplied by user.
     * @param limit the limit supplied by user.
     */
    public void ipLimitChecker(final String path, final String startTime, final String duration, final String limit){
        if (!StringUtils.isNumeric(limit)){
            logger.error("Invalid command passed for limit");
            exit();
        }

        readFile(path);
        saveBlockedIpsBatch(retrieveBlockedIps(startTime, duration, limit));
    }

    /**
     * This method retrieves all Ips with request counts greater than limit.
     *
     * @param startTime the file path supplied by user.
     * @param duration the duration supplied by user.
     * @param limit the limit supplied by user.
     *
     * @return A list containing BlockedIps.
     */
    public ArrayList<BlockedIp> retrieveBlockedIps(final String startTime, final String duration, final String limit) {
        final Timestamp startDate = toTimeStamp(startTime, "yyyy-MM-dd.HH:mm:ss");

        List<Object[]> logsWithinInterval = userAccessLogRepository.findByDateBetweenAndCountGreaterThan(startDate, getEndDate(startDate, duration), Integer.parseInt(limit));

        ArrayList<BlockedIp> blockedIps = new ArrayList<>();
        for(Object[] entry : logsWithinInterval){
            String ipAddress = (String) entry[0];
            Long count = (Long) entry[1];

            BlockedIp blockedIp = BlockedIp.builder()
                    .ip(ipAddress)
                    .requestNumber(Integer.parseInt(String.valueOf(count)))
                    .comment("Ip log exceeded user supplied limit of " + limit).build();

            logger.info("Blocked Ip : {}. Request count : {}", ipAddress, count);
            blockedIps.add(blockedIp);
        }
        return blockedIps;
    }

    /**
     * This method generates the end date for the search query using the supplied start date and duration.
     *
     * @param startDateTime the startDateTime supplied by user.
     * @param duration the duration supplied by user.
     *
     * @return Timestamp of end date time or null if invalid duration is given.
     */
    public Timestamp getEndDate(final Timestamp startDateTime, final String duration) {
        if (startDateTime == null) {
            logger.error("null value supplied for parameter start date");
            exit();
        }

        Timestamp endDate = null;
        switch (duration) {
            case "daily" -> endDate = Timestamp.from(startDateTime.toInstant().plus(1, ChronoUnit.DAYS));
            case "hourly" -> endDate = Timestamp.from(startDateTime.toInstant().plus(1, ChronoUnit.HOURS));
            default -> {
                logger.error("Invalid argument for parameter duration : {}", duration);
                exit();
            }
        }
        return endDate;
    }

    /**
     * This method generates Timestamp for a given dateString value
     *
     * @param dateString the dateString supplied by user.
     * @param dateTimeFormat supplied date time formatter.
     *
     * @return Timestamp.
     */
    public Timestamp toTimeStamp(final String dateString, final String dateTimeFormat){
        Timestamp timestamp = null;

        if(dateString == null || dateString.isBlank()){
            logger.error("Invalid date String passed, {}", dateString);
            return null;
        }

        if (dateTimeFormat == null || dateTimeFormat.isBlank()) {
            logger.error("Invalid dateTimeFormat passed, {}", dateTimeFormat);
            return null;
        }

        try {
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateTimeFormat);
            final LocalDateTime dateTime = LocalDateTime.parse(dateString.trim(), formatter);
            timestamp = Timestamp.valueOf(dateTime);
        } catch (DateTimeParseException ex) {
            logger.error("Error parsing date string: {}. Exception is : {}", dateString, ex.getMessage());
        }
        return timestamp;
    }

    private void exit() {
        System.exit(1);
    }
}
