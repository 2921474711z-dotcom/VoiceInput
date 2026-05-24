package com.voiceinput.pro.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.voiceinput.pro.service.ProofreadService;
import com.voiceinput.pro.service.TaskService;
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
class TaskControllerTest {

    @Mock
    private TaskService taskService;

    @Mock
    private ProofreadService proofreadService;

    @InjectMocks
    private TaskController taskController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(taskController).build();
    }

    @Test
    void exportMarkdownShouldExposeUtf8AttachmentHeaders() throws Exception {
        byte[] bytes = "# 会议纪要\n\n内容".getBytes(StandardCharsets.UTF_8);
        when(taskService.exportMarkdown("task-1"))
            .thenReturn(new TaskService.MarkdownExportResult("会议纪要.md", bytes));

        mockMvc.perform(get("/api/history/task-1/export-markdown"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment;")))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("filename*=UTF-8''")))
            .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, String.valueOf(bytes.length)))
            .andExpect(content().contentType("text/markdown; charset=utf-8"))
            .andExpect(content().bytes(bytes));
    }
}
