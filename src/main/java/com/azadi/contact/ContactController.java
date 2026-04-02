package com.azadi.contact;

import com.azadi.auth.AuthorizationService;
import com.azadi.contact.dto.UpdateContactCommand;
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
        model.addAttribute("customer", contactService.getCustomer(customerId));
        return "my-contact-details";
    }

    @PostMapping("/my-contact-details")
    public String updateContactDetails(@RequestParam(required = false) String homePhone,
                                       @RequestParam(required = false) String mobilePhone,
                                       @RequestParam(required = false) String email,
                                       @RequestParam(required = false) String houseName,
                                       @RequestParam(required = false) String postcode,
                                       HttpServletRequest httpRequest,
                                       RedirectAttributes redirectAttributes) {
        var customerId = authorizationService.getCurrentCustomerId();
        var customer = contactService.getCustomer(customerId);
        var command = UpdateContactCommand.mergeWith(customer, homePhone, mobilePhone, email, houseName, postcode);

        contactService.updateContactDetails(customerId, command, httpRequest.getRemoteAddr());

        redirectAttributes.addFlashAttribute("success", "Contact details updated successfully.");
        return "redirect:/my-contact-details";
    }
}
