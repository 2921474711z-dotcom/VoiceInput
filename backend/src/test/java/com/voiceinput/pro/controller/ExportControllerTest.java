package com.voiceinput.pro.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.voiceinput.pro.service.ExportService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ExportControllerTest {

    @Mock
    private ExportService exportService;

    @InjectMocks
    private ExportController exportController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(exportController).build();
    }

    @Test
    void downloadShouldExposeUtf8AttachmentHeaders() throws Exception {
        byte[] bytes = "导出内容".getBytes(StandardCharsets.UTF_8);
        when(exportService.download("export-1"))
            .thenReturn(new ExportService.ExportDownload("会议纪要.txt", "text/plain; charset=utf-8", bytes));

        mockMvc.perform(get("/api/exports/export-1/download"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment;")))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("filename*=UTF-8''")))
            .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, String.valueOf(bytes.length)))
            .andExpect(content().contentType("text/plain; charset=utf-8"))
            .andExpect(content().bytes(bytes));
    }
}
