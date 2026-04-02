package com.azadi.document;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public List<Document> getDocumentsForCustomer(String customerId) {
        return documentRepository.findByCustomerId(customerId);
    }

    public Document getDocument(String customerId, Long documentId) {
        var document = documentRepository.findById(documentId)
            .orElseThrow(() -> new NoSuchElementException("Document not found: " + documentId));

        if (!customerId.equals(document.getCustomerId())) {
            throw new AccessDeniedException("You do not have access to this document.");
        }

        return document;
    }
}
