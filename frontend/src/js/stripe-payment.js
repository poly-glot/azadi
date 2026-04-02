/**
 * Stripe Payment Module
 * Initializes Stripe Elements for card payments on the make-a-payment page.
 */

export function initStripePayment() {
  const cardContainer = document.getElementById('card-element');
  const paymentForm = document.getElementById('payment-form');

  if (!cardContainer || !paymentForm) return;

  // Read the publishable key from a data attribute on the form
  const publishableKey = paymentForm.dataset.stripeKey;
  if (!publishableKey) {
    console.warn('Stripe publishable key not found on #payment-form[data-stripe-key]');
    return;
  }

  // Stripe.js should already be loaded from the CDN in the template
  if (typeof Stripe === 'undefined') {
    console.warn('Stripe.js not loaded. Ensure the script is included in the page.');
    return;
  }

  const stripe = Stripe(publishableKey); // eslint-disable-line no-undef
  const elements = stripe.elements();

  const style = {
    base: {
      color: '#282828',
      fontFamily: "'Poppins', Arial, sans-serif",
      fontSize: '14px',
      '::placeholder': {
        color: '#afb4b7',
      },
    },
    invalid: {
      color: '#e4042b',
      iconColor: '#e4042b',
    },
  };

  const cardElement = elements.create('card', { style });
  cardElement.mount('#card-element');

  // Display validation errors
  const errorDisplay = document.getElementById('card-errors');
  cardElement.on('change', (event) => {
    if (errorDisplay) {
      errorDisplay.textContent = event.error ? event.error.message : '';
    }
  });

  // Handle form submission
  paymentForm.addEventListener('submit', async (event) => {
    event.preventDefault();

    const submitButton = paymentForm.querySelector('[type="submit"]');
    if (submitButton) {
      submitButton.disabled = true;
      submitButton.textContent = 'Processing...';
    }

    try {
      const { paymentMethod, error } = await stripe.createPaymentMethod({
        type: 'card',
        card: cardElement,
      });

      if (error) {
        if (errorDisplay) {
          errorDisplay.textContent = error.message;
        }
        if (submitButton) {
          submitButton.disabled = false;
          submitButton.textContent = 'Pay';
        }
        return;
      }

      // Post the payment method ID to the server
      const amountInput = paymentForm.querySelector('[name="amount"]');
      const csrfInput = paymentForm.querySelector('[name="_csrf"]');

      const formData = new FormData();
      formData.append('paymentMethodId', paymentMethod.id);
      if (amountInput) formData.append('amount', amountInput.value);
      if (csrfInput) formData.append('_csrf', csrfInput.value);

      const response = await fetch(paymentForm.action, {
        method: 'POST',
        body: formData,
      });

      const result = await response.json();

      if (result.success) {
        window.location.href = result.redirectUrl || '/finance/make-a-payment?success=true';
      } else {
        if (errorDisplay) {
          errorDisplay.textContent = result.message || 'Payment failed. Please try again.';
        }
        if (submitButton) {
          submitButton.disabled = false;
          submitButton.textContent = 'Pay';
        }
      }
    } catch (err) {
      if (errorDisplay) {
        errorDisplay.textContent = 'An unexpected error occurred. Please try again.';
      }
      if (submitButton) {
        submitButton.disabled = false;
        submitButton.textContent = 'Pay';
      }
    }
  });
}
