package com.radio.codec2talkie.tools;

public class Dsp {
    // Filter coefficients for high-pass and low-pass
    private float hpfA0, hpfA1, hpfA2, hpfB1, hpfB2; // High-pass coefficients
    private float lpfA0, lpfA1, lpfA2, lpfB1, lpfB2; // Low-pass coefficients

    // Intermediate filter states
    private float hpfX1, hpfX2, hpfY1, hpfY2; // High-pass states
    private float lpfX1, lpfX2, lpfY1, lpfY2; // Low-pass states

    private final int _sampleRate;

    public Dsp(int sampleRate, float lowCutoffFrequency, float highCutoffFrequency) {
        _sampleRate = sampleRate;
        configureHighPassFilter(lowCutoffFrequency, sampleRate);
        configureLowPassFilter(highCutoffFrequency, sampleRate);
    }

    private void configureHighPassFilter(float cutoffFreq, float sampleRate) {
        double omega = 2 * Math.PI * cutoffFreq / sampleRate;
        double sn = Math.sin(omega);
        double cs = Math.cos(omega);
        double alpha = sn / Math.sqrt(2); // Q = sqrt(2)/2 for Butterworth

        double b0 = (1 + cs) / 2;
        double b1 = -(1 + cs);
        double b2 = (1 + cs) / 2;
        double a0 = 1 + alpha;
        double a1 = -2 * cs;
        double a2 = 1 - alpha;

        hpfA0 = (float)(b0 / a0);
        hpfA1 = (float)(b1 / a0);
        hpfA2 = (float)(b2 / a0);
        hpfB1 = (float)(a1 / a0);
        hpfB2 = (float)(a2 / a0);
    }

    public void updateFilterSettings(float lowCutoffFrequency, float highCutoffFrequency) {
        configureHighPassFilter(lowCutoffFrequency, _sampleRate);
        configureLowPassFilter(highCutoffFrequency, _sampleRate);
    }

    private void configureLowPassFilter(float cutoffFreq, float sampleRate) {
        // Example: Calculate coefficients for low-pass filter (Butterworth, 2nd order)
        double omega = 2 * Math.PI * cutoffFreq / sampleRate;
        double sn = Math.sin(omega);
        double cs = Math.cos(omega);
        double alpha = sn / Math.sqrt(2); // Q = sqrt(2)/2 for Butterworth

        double b0 = (1 - cs) / 2;
        double b1 = 1 - cs;
        double b2 = (1 - cs) / 2;
        double a0 = 1 + alpha;
        double a1 = -2 * cs;
        double a2 = 1 - alpha;

        lpfA0 = (float)(b0 / a0);
        lpfA1 = (float)(b1 / a0);
        lpfA2 = (float)(b2 / a0);
        lpfB1 = (float)(a1 / a0);
        lpfB2 = (float)(a2 / a0);
    }

    public void audioFilterBandpass(short[] pcmBuffer, int pcmBufferSize) {
        for (int i = 0; i < pcmBufferSize; i++) {
            // Apply high-pass filter
            float x0 = pcmBuffer[i];
            float y0 = hpfA0 * x0 + hpfA1 * hpfX1 + hpfA2 * hpfX2 - hpfB1 * hpfY1 - hpfB2 * hpfY2;

            hpfX2 = hpfX1;
            hpfX1 = x0;
            hpfY2 = hpfY1;
            hpfY1 = y0;

            // Apply low-pass filter on the output of high-pass filter
            float z0 = lpfA0 * y0 + lpfA1 * lpfX1 + lpfA2 * lpfX2 - lpfB1 * lpfY1 - lpfB2 * lpfY2;

            lpfX2 = lpfX1;
            //noinspection SuspiciousNameCombination
            lpfX1 = y0;
            lpfY2 = lpfY1;
            lpfY1 = z0;

            // Store the filtered sample back into the buffer
            pcmBuffer[i] = (short)z0;
        }
    }

    public void downSamplePcm(short[] inputSamples, short[] outputSamples, int originalRate, int targetRate) {
        int sampleRatio = originalRate / targetRate;

        int outputIndex = 0;

        for (int i = 0; i < inputSamples.length; i += sampleRatio) {
            long sum = 0;
            int count = 0;

            for (int j = 0; j < sampleRatio; j++) {
                if (i + j < inputSamples.length) {
                    sum += inputSamples[i + j];
                    count++;
                }
            }

            outputSamples[outputIndex++] = (short) (sum / count); // Average value
        }
    }

    public  void adjustPcmGain(short[] samples, float gainFactor) {
        for (int i = 0; i < samples.length; i++) {
            // Adjust the sample with the gain factor and avoid clipping
            int adjustedValue = Math.round(samples[i] * gainFactor);

            // Clamp the value to avoid overflow/underflow
            if (adjustedValue > Short.MAX_VALUE) {
                samples[i] = Short.MAX_VALUE;
            } else if (adjustedValue < Short.MIN_VALUE) {
                samples[i] = Short.MIN_VALUE;
            } else {
                samples[i] = (short) adjustedValue;
            }
        }
    }

}