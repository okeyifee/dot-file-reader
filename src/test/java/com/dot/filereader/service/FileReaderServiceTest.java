package com.dot.filereader.service;

import com.dot.filereader.entity.BlockedIp;
import com.dot.filereader.entity.UserAccessLog;
import com.dot.filereader.repository.BlockedIpTableRepository;
import com.dot.filereader.repository.UserAccessLogRepository;
import com.tngtech.java.junit.dataprovider.DataProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.test.context.ContextConfiguration;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {FileReaderService.class})
@ExtendWith(MockitoExtension.class)
class FileReaderServiceTest {

    @Mock
    private BlockedIpTableRepository blockedIpTableRepository;

    @Mock
    private UserAccessLogRepository userAccessLogRepository;

    @Mock
    private Logger logger;

    @InjectMocks
    private FileReaderService fileReaderService;

    @BeforeEach
    public void init() {
        fileReaderService.logger = logger;
    }

    @DataProvider
    public static Object[][] accessLogBatchProvider() {
        final UserAccessLog userAccessLog = UserAccessLog.builder()
                .userAgent("agent")
                .status("201")
                .ipAddress("8080:8080")
                .request("request")
                .date(Timestamp.valueOf(LocalDateTime.now()))
                .build();

        return new Object[][]{
                {null, 0},
                {new ArrayList<UserAccessLog>(), 0},
                {new ArrayList<>(Collections.singletonList(userAccessLog)), 1}
        };
    }

    @DataProvider
    public static Object[][] blockedIpsBatchProvider() {
        final BlockedIp blockedIp = BlockedIp.builder()
                .ip("8080:8080")
                .requestNumber(10)
                .comment("exceeded request limit")
                .build();

        return new Object[][]{
                {null, 0},
                {new ArrayList<UserAccessLog>(), 0},
                {new ArrayList<>(Collections.singletonList(blockedIp)), 1}
        };
    }

    @DataProvider
    public static Object[][] invalidDateStringProvider() {
        return new Object[][]{
                {null, null, null, "Invalid date String passed, {}", null},
                {null, "yyyy-MM-dd.HH:mm:ss", null, "Invalid date String passed, {}", null},
                {"", "yyyy-MM-dd.HH:mm:ss", null, "Invalid date String passed, {}", ""},
                {" ", "yyyy-MM-dd.HH:mm:ss", null, "Invalid date String passed, {}", " "},
                {"2022-01-01 23:59:57", null, null, "Invalid dateTimeFormat passed, {}", null},
                {"2022-01-01 23:59:57", "", null, "Invalid dateTimeFormat passed, {}", ""},
                {"2022-01-01 23:59:57", " ", null, "Invalid dateTimeFormat passed, {}", " "},
                {"2022-01-01 23:59:49.990", "yyyy-MM-dd HH:mm:ss.SSS", Timestamp.valueOf("2022-01-01 23:59:49.99"), null, null},
                {"2022-01-01.23:59:49", "yyyy-MM-dd.HH:mm:ss", Timestamp.valueOf("2022-01-01 23:59:49"), null, null}
        };
    }

    /**
     * Method under test: {@link FileReaderService#toTimeStamp(String, String)}
     */
    @Test
    void testToTimeStampShouldThrowExceptionWhenDateStringCouldNotBeParsed() {
        fileReaderService.logger = logger;
        fileReaderService.toTimeStamp("2022-01-01 23:59:49", "yyyy-MM-dd.HH:mm:ss");

        verify(logger).error("Error parsing date string: {}. Exception is : {}", "2022-01-01 23:59:49", "Text '2022-01-01 23:59:49' could not be parsed at index 10");
    }

    /**
     * Method under test: {@link FileReaderService#toTimeStamp(String, String)}
     */
    @ParameterizedTest
    @MethodSource("invalidDateStringProvider")
    void testToTimeStamp(String dateString, String dateTimeFormat, Timestamp expectedTimeStamp, String expectedLogMessage, String logObject) {
        final Timestamp actualTimestamp = fileReaderService.toTimeStamp(dateString, dateTimeFormat);

        assertEquals(expectedTimeStamp, actualTimestamp);

        if (expectedLogMessage != null) {
            verify(logger).error(expectedLogMessage, logObject);
        }
    }

    /**
     * Method under test: {@link FileReaderService#saveAccessLogBatch(ArrayList)}
     */
    @ParameterizedTest
    @MethodSource("accessLogBatchProvider")
    void testSaveAccessLogBatch(ArrayList<UserAccessLog> userAccessLogs, int expectedNumberOfInvocations) {
        // when
        fileReaderService.saveAccessLogBatch(userAccessLogs);

        // then
        verify(userAccessLogRepository, times(expectedNumberOfInvocations)).saveAllAndFlush(userAccessLogs);
    }

    /**
     * Method under test: {@link FileReaderService#saveBlockedIpsBatch(ArrayList)}
     */
    @ParameterizedTest
    @MethodSource("blockedIpsBatchProvider")
    void testSaveBlockedIpsBatch(ArrayList<BlockedIp> blockedIps, int expectedNumberOfInvocations) {
        // when
        fileReaderService.saveBlockedIpsBatch(blockedIps);

        // then
        verify(blockedIpTableRepository, times(expectedNumberOfInvocations)).saveAllAndFlush(blockedIps);
    }

    @Test
    void testReadFile() {
        fileReaderService.readFile("access.log");
        verify(userAccessLogRepository, times(1)).saveAllAndFlush(any(ArrayList.class));
    }

    @Test
    void testReadFileWithInvalidPath() {
        fileReaderService.readFile("access.log");
        verify(userAccessLogRepository, times(1)).saveAllAndFlush(any(ArrayList.class));
    }

    @Test
    void testReadFileWith() {
        fileReaderService.readFile("access.log");
        verify(userAccessLogRepository, times(1)).saveAllAndFlush(any(ArrayList.class));
    }
}