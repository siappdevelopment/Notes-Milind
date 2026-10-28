package com.notebook.notes.checklist.Advertisement;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class ManegeParameter {




    static int jas = 256;
    static int sjdh = 128;
    static String sdjbf = "AES/CBC/PKCS7Padding";
    static String ushdd = "U2F";

    public static String cipherSignatureSpace(String password, String cipherText) throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        byte[] ctBytes = com.sun.jersey.core.util.Base64.decode(cipherText.getBytes(hsjdsb));
        byte[] saltBytes = Arrays.copyOfRange(ctBytes, 8, 16);
        byte[] ciphertextBytes = Arrays.copyOfRange(ctBytes, 16, ctBytes.length);
        byte[] key = new byte[jas /8];
        byte[] iv = new byte[sjdh /8];

        EvpKDF(password.getBytes(hsjdsb), jas, sjdh, saltBytes, key, iv);

        Cipher cipher = Cipher.getInstance(sdjbf);
        SecretKey keyS = new SecretKeySpec(key, aesKey);

        cipher.init(Cipher.DECRYPT_MODE, keyS, new IvParameterSpec(iv));
        byte[] plainText = cipher.doFinal(ciphertextBytes);
        return new String(plainText);
    }
    private static byte[] EvpKDF(byte[] password, int keySize, int ivSize, byte[] salt, byte[] resultKey, byte[] resultIv) throws NoSuchAlgorithmException {
        return EvpKDF(password, keySize, ivSize, salt, 1, ahhuhas, resultKey, resultIv);
    }
    public static String skndn = "sdG";
    public static String asadpass;
    public static String sdasdada;
    private static byte[] EvpKDF(byte[] password, int keySize, int ivSize, byte[] salt, int iterations, String hashAlgorithm, byte[] resultKey, byte[] resultIv) throws NoSuchAlgorithmException {
        keySize = keySize / 32;
        ivSize = ivSize / 32;
        int targetKeySize = keySize + ivSize;
        byte[] derivedBytes = new byte[targetKeySize * 4];
        int numberOfDerivedWords = 0;
        byte[] block = null;
        MessageDigest hasher = MessageDigest.getInstance(hashAlgorithm);
        while (numberOfDerivedWords < targetKeySize) {
            if (block != null) {
                hasher.update(block);
            }
            hasher.update(password);
            block = hasher.digest(salt);
            hasher.reset();

            for (int i = 1; i < iterations; i++) {
                block = hasher.digest(block);
                hasher.reset();
            }

            System.arraycopy(block, 0, derivedBytes, numberOfDerivedWords * 4,
                    Math.min(block.length, (targetKeySize - numberOfDerivedWords) * 4));

            numberOfDerivedWords += block.length/4;
        }

        System.arraycopy(derivedBytes, 0, resultKey, 0, keySize * 4);
        System.arraycopy(derivedBytes, keySize * 4, resultIv, 0, ivSize * 4);

        return derivedBytes;
    }

    static String aesKey = "AES";
    static String hsjdsb = "UTF-8";
    public static String sdfsvdfvs = "L1T7J9N5";
    static String ahhuhas = "MD5";
    public static String ksgqwl = ushdd + skndn;


}
