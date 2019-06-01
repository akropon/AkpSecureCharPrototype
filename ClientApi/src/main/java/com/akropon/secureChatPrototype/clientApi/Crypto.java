package com.akropon.secureChatPrototype.clientApi;

import javafx.util.Pair;

import java.util.Random;

public class Crypto {

    public static final int KEY_LENGTH = 4;

    public static String decodeText(byte[] encodedText, byte[] key) {
        byte[] textAsBytes = new byte[encodedText.length];
        for (int i = 0; i < encodedText.length; i++) {
            textAsBytes[i] = (byte) (encodedText[i] ^ key[i % key.length]);
        }
        return new String(textAsBytes);
    }

    public static byte[] encodeText(String text, byte[] key) {
        byte[] textAsBytes = text.getBytes();
        byte[] encodedText = new byte[textAsBytes.length];
        for (int i = 0; i < encodedText.length; i++) {
            encodedText[i] = (byte) (textAsBytes[i] ^ key[i % key.length]);
        }
        return encodedText;
    }

    static Pair<byte[], byte[]> generateLocalPartsOfKey() {
        Random random = new Random();
        byte[] secretPart = new byte[KEY_LENGTH];
        random.nextBytes(secretPart);
        byte[] publicPart = new byte[KEY_LENGTH];

        for (int i = 0; i < publicPart.length; i++) {
            publicPart[i] = (byte) (random.nextInt() + secretPart[i % secretPart.length]);
        }

        return new Pair<>(secretPart, publicPart);
    }

    static byte[] getSecretKey(byte[] mySecretPartOfKey, byte[] hisPublicPartOfKey) {
        return magicSum(mySecretPartOfKey, hisPublicPartOfKey);
    }

    static byte[] genSecretPartOfKey() {
        return genSequence();
    }

    static byte[] genBaseSequence() {
        return genSequence();
    }

    static byte[] getPublicPartOfKey(byte[] baseSequence, byte[] mySecretPartOfKey) {
        return magicSum(baseSequence, mySecretPartOfKey);
    }

    // TODO: 2019-05-19 implement normally!
    private static byte[] genSequence() {
        byte[] sequence = new byte[KEY_LENGTH];
        new Random().nextBytes(sequence);
        return sequence;
    }

    // TODO: 2019-05-19 implement normally!
    private static byte[] magicSum(byte[] seq1, byte[] seq2) {
        byte[] sum = new byte[KEY_LENGTH];
        for (int i = 0; i < KEY_LENGTH; i++) {
            sum[i] = (byte) (seq1[i] + seq2[i]);
        }
        return sum;
    }
}
