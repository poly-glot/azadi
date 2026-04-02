package com.azadi.contact;

import com.azadi.auth.AuthorizationService;
import com.azadi.contact.dto.UpdateContactDetailsRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ContactController {

    private final ContactService contactService;
    private final AuthorizationService authorizationService;

    public ContactController(ContactService contactService,
                             AuthorizationService authorizationService) {
        this.contactService = contactService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/my-contact-details")
    public String contactDetailsPage(Model model) {
        var customerId = authorizationService.getCurrentCustomerId();
        var customer = contactService.getCustomer(customerId);
        model.addAttribute("customer", customer);
        return "my-contact-details";
    }

    @PostMapping("/my-contact-details")
    public String updateContactDetails(@Valid @ModelAttribute UpdateContactDetailsRequest request,
                                       HttpServletRequest httpRequest,
                                       RedirectAttributes redirectAttributes) {
        var customerId = authorizationService.getCurrentCustomerId();
        contactService.updateContactDetails(customerId, request.phone(), request.email(),
            request.addressLine1(), request.addressLine2(), request.city(),
            request.postcode(), httpRequest.getRemoteAddr());
        redirectAttributes.addFlashAttribute("success", "Contact details updated successfully.");
        return "redirect:/my-contact-details";
    }
}
