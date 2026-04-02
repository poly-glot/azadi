package com.azadi.bank;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

@Component
public class BankDetailsEncryptor {

    private final TextEncryptor textEncryptor;

    public BankDetailsEncryptor(@Value("${azadi.encryption-key}") String encryptionKey,
                                @Value("${azadi.encryption-salt}") String encryptionSalt) {
        this.textEncryptor = Encryptors.delux(encryptionKey, encryptionSalt);
    }

    public String encrypt(String plaintext) {
        return textEncryptor.encrypt(plaintext);
    }

    public String decrypt(String ciphertext) {
        return textEncryptor.decrypt(ciphertext);
    }
}
