package com.centraldoalfabeto.game.service;

import java.util.Arrays;

/** Leitura mínima de WAV PCM 16-bit mono 16 kHz, o formato aceito pelo reconhecedor. */
public final class WavAudio {
    public static final int SAMPLE_RATE = 16000;

    private final byte[] pcm;

    private WavAudio(byte[] pcm) {
        this.pcm = pcm;
    }

    public byte[] pcm() {
        return pcm;
    }

    /** Valida o cabeçalho e devolve apenas os dados PCM. Lança IllegalArgumentException se o formato não for o esperado. */
    public static WavAudio parse(byte[] data) {
        if (data == null || data.length < 12
            || !tag(data, 0, "RIFF") || !tag(data, 8, "WAVE")) {
            throw new IllegalArgumentException("O arquivo enviado não é um WAV válido.");
        }

        boolean fmtOk = false;
        int pos = 12;
        while (pos + 8 <= data.length) {
            long size = le32(data, pos + 4);
            int body = pos + 8;
            if (tag(data, pos, "fmt ")) {
                if (size < 16 || body + 16 > data.length) {
                    throw new IllegalArgumentException("Cabeçalho WAV inválido.");
                }
                int format = le16(data, body);
                int channels = le16(data, body + 2);
                long rate = le32(data, body + 4);
                int bits = le16(data, body + 14);
                if (format != 1 || channels != 1 || rate != SAMPLE_RATE || bits != 16) {
                    throw new IllegalArgumentException("Formato de áudio não suportado: use WAV PCM 16-bit mono a 16 kHz.");
                }
                fmtOk = true;
            } else if (tag(data, pos, "data")) {
                if (!fmtOk) {
                    throw new IllegalArgumentException("Cabeçalho WAV inválido.");
                }
                long end = Math.min((long) data.length, body + size);
                if (end <= body) {
                    throw new IllegalArgumentException("O áudio está vazio.");
                }
                return new WavAudio(Arrays.copyOfRange(data, body, (int) end));
            }
            pos = (int) Math.min((long) Integer.MAX_VALUE, body + size + (size % 2));
        }
        throw new IllegalArgumentException("O arquivo WAV não contém dados de áudio.");
    }

    private static boolean tag(byte[] d, int off, String t) {
        for (int i = 0; i < 4; i++) {
            if (off + i >= d.length || d[off + i] != (byte) t.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static int le16(byte[] d, int o) {
        return (d[o] & 0xff) | ((d[o + 1] & 0xff) << 8);
    }

    private static long le32(byte[] d, int o) {
        return (d[o] & 0xffL) | ((d[o + 1] & 0xffL) << 8) | ((d[o + 2] & 0xffL) << 16) | ((d[o + 3] & 0xffL) << 24);
    }
}
