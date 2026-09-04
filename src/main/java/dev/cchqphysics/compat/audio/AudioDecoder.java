package dev.cchqphysics.compat.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

final class AudioDecoder {
    private static final int MAX_DECODED_BYTES = 128 * 1024 * 1024;

    private AudioDecoder() {}

    static boolean canDecode(String format, byte[] data) {
        if (format.endsWith("_STREAM")) return false;
        try (BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(data))) {
            AudioSystem.getAudioFileFormat(in);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    static DecodedAudio decode(String format, byte[] data) throws Exception {
        try (BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(data));
             AudioInputStream source = AudioSystem.getAudioInputStream(in)) {
            AudioFormat src = source.getFormat();
            float sampleRate = src.getSampleRate() > 0F ? src.getSampleRate() : 48000F;
            int channels = Math.max(1, src.getChannels());
            AudioFormat pcm = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, 16,
                    channels, channels * 2, sampleRate, false);
            try (AudioInputStream converted = AudioSystem.getAudioInputStream(pcm, source)) {
                return downmix(converted, channels, Math.round(sampleRate));
            }
        }
    }

    private static DecodedAudio downmix(AudioInputStream in, int channels, int sampleRate) throws IOException {
        final int frameSize = channels * 2;
        final byte[] input = new byte[Math.max(8192, frameSize * 2048)];
        final byte[] remainder = new byte[frameSize];
        final byte[] monoChunk = new byte[Math.max(4096, (input.length / frameSize) * 2 + 2)];
        final long frameLength = in.getFrameLength();
        final int initialCapacity;
        if (frameLength > 0 && frameLength <= (MAX_DECODED_BYTES / 2L)) {
            initialCapacity = (int)Math.min(MAX_DECODED_BYTES, Math.max(8192L, frameLength * 2L));
        } else {
            initialCapacity = 8192;
        }
        final ByteArrayOutputStream out = new ByteArrayOutputStream(initialCapacity);

        int carry = 0;
        while (true) {
            int prefix = carry;
            if (carry > 0) {
                System.arraycopy(remainder, 0, input, 0, carry);
            }

            int read = in.read(input, prefix, input.length - prefix);
            if (read < 0 && carry == 0) break;

            int total = carry + Math.max(read, 0);
            int completeBytes = total - total % frameSize;

            if (channels == 1) {
                if (out.size() + completeBytes > MAX_DECODED_BYTES) {
                    throw new IOException("decoded audio exceeds 128 MiB safety limit");
                }
                out.write(input, 0, completeBytes);
            } else {
                int monoBytes = 0;
                for (int frame = 0; frame < completeBytes; frame += frameSize) {
                    int sum = 0;
                    for (int channel = 0; channel < channels; channel++) {
                        int idx = frame + channel * 2;
                        short sample = (short)((input[idx] & 0xFF) | (input[idx + 1] << 8));
                        sum += sample;
                    }
                    int mono = Math.max(-32768, Math.min(32767, sum / channels));
                    monoChunk[monoBytes++] = (byte)(mono & 0xFF);
                    monoChunk[monoBytes++] = (byte)((mono >>> 8) & 0xFF);
                }
                if (out.size() + monoBytes > MAX_DECODED_BYTES) {
                    throw new IOException("decoded audio exceeds 128 MiB safety limit");
                }
                out.write(monoChunk, 0, monoBytes);
            }

            carry = total - completeBytes;
            if (carry > 0) {
                System.arraycopy(input, completeBytes, remainder, 0, carry);
            }
            if (read < 0) break;
        }
        return new DecodedAudio(out.toByteArray(), sampleRate);
    }
}
