package com.azadi.web;

import com.azadi.auth.AuthorizationService;
import com.azadi.document.Document;
import com.azadi.document.DocumentController;
import com.azadi.document.DocumentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private DocumentService documentService;
    @MockitoBean private AuthorizationService authorizationService;

    @Test
    @WithMockUser
    @DisplayName("Empty document list renders without error")
    void emptyDocuments_renders200() throws Exception {
        when(authorizationService.getCurrentCustomerId()).thenReturn("CUST-1");
        when(documentService.getDocumentsForCustomer("CUST-1")).thenReturn(List.of());

        mockMvc.perform(get("/my-documents"))
            .andExpect(status().isOk())
            .andExpect(view().name("my-documents"))
            .andExpect(model().attributeExists("documents"))
            .andExpect(content().string(containsString("no documents available")));
    }

    @Test
    @WithMockUser
    @DisplayName("Document list renders file names and correct download links")
    void documentList_rendersFileNamesAndLinks() throws Exception {
        var doc = new Document();
        doc.setId(42L);
        doc.setFileName("Finance_Agreement.pdf");

        when(authorizationService.getCurrentCustomerId()).thenReturn("CUST-1");
        when(documentService.getDocumentsForCustomer("CUST-1")).thenReturn(List.of(doc));

        mockMvc.perform(get("/my-documents"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Finance_Agreement.pdf")))
            .andExpect(content().string(containsString("/documents/42/download")));
    }

    @Test
    @WithMockUser
    @DisplayName("Document.getFileType() derives extension correctly for template icon path")
    void documentFileType_appearsInRenderedHtml() throws Exception {
        var doc = new Document();
        doc.setId(1L);
        doc.setFileName("Report.doc");

        when(authorizationService.getCurrentCustomerId()).thenReturn("CUST-1");
        when(documentService.getDocumentsForCustomer("CUST-1")).thenReturn(List.of(doc));

        mockMvc.perform(get("/my-documents"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("/assets/img/file-icons/doc.png")));
    }
}
