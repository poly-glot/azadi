package com.azadi.bank;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

@Component
public class BankDetailsEncryptor {

    private final TextEncryptor textEncryptor;

    public BankDetailsEncryptor(@Value("${azadi.encryption-key}") String encryptionKey) {
        this.textEncryptor = Encryptors.delux(encryptionKey, "a1b2c3d4e5f6a7b8");
    }

    public String encrypt(String plaintext) {
        return textEncryptor.encrypt(plaintext);
    }

    public String decrypt(String ciphertext) {
        return textEncryptor.decrypt(ciphertext);
    }
}
