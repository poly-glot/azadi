package com.azadi.contact;

import com.azadi.auth.AuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public String updateContactDetails(@RequestParam String phone,
                                       @RequestParam String email,
                                       @RequestParam String addressLine1,
                                       @RequestParam(required = false) String addressLine2,
                                       @RequestParam String city,
                                       @RequestParam String postcode,
                                       HttpServletRequest request,
                                       RedirectAttributes redirectAttributes) {
        var customerId = authorizationService.getCurrentCustomerId();
        contactService.updateContactDetails(customerId, phone, email, addressLine1,
            addressLine2, city, postcode, request.getRemoteAddr());
        redirectAttributes.addFlashAttribute("success", "Contact details updated successfully.");
        return "redirect:/my-contact-details";
    }
}
