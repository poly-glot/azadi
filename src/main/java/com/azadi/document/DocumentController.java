package com.azadi.document;

import com.azadi.auth.AuthorizationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class DocumentController {

    private final DocumentService documentService;
    private final AuthorizationService authorizationService;

    public DocumentController(DocumentService documentService,
                              AuthorizationService authorizationService) {
        this.documentService = documentService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/my-documents")
    public String documentsPage(Model model) {
        var customerId = authorizationService.getCurrentCustomerId();
        var documents = documentService.getDocumentsForCustomer(customerId);
        model.addAttribute("documents", documents);
        return "my-documents";
    }

    @GetMapping("/documents/{id}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id) {
        var customerId = authorizationService.getCurrentCustomerId();
        var document = documentService.getDocument(customerId, id);

        // In production, this would stream from GCS
        // For now, return a placeholder response
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + document.getFileName() + "\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(new byte[0]);
    }
}
