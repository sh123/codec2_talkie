#include <jni.h>
#include <cstdlib>
#include "codec2/codec2_fdmdv.h"
#include "codec2/codec2.h"
#include "codec2/fsk.h"

namespace Java_com_ustadmobile_codec2_Codec2 {

    struct Context {
        struct CODEC2 *c2;
        short *buf; //raw audio data
        unsigned char *bits; //codec2 data
        int nsam; //nsam: number of samples per frame - e.g. raw (uncompressed) size = samples per frame
        int nbit;
        int nbyte;//size of one frame of codec2 data
    };

    struct ContextFsk {
        struct FSK *fsk;
        float *modBuf;
        unsigned char *modBits;
        short *demodBuf;
        COMP *demodCBuf;
        unsigned char *demodBits;
        int Nbits;
        int N;
        int Ts;
    };

    static Context *getContext(jlong jp) {
        auto p = (unsigned long) jp;
        Context *con;
        con = (Context *) p;
        return con;
    }

    static ContextFsk *getContextFsk(jlong jp) {
        auto p = (unsigned long) jp;
        ContextFsk *conFsk;
        conFsk = (ContextFsk *) p;
        return conFsk;
    }

    static jlong create(JNIEnv *env, jclass clazz, int mode) {
        struct Context *con;
        con = (struct Context *) malloc(sizeof(struct Context));
        struct CODEC2 *c;
        c = codec2_create(mode);
        con->c2 = c;
        con->nsam = codec2_samples_per_frame(c);
        con->nbit = codec2_bits_per_frame(con->c2);
        con->buf = (short*)malloc(con->nsam*sizeof(short));
        con->nbyte = (con->nbit +  7) / 8;
        con->bits = (unsigned char*)malloc(con->nbyte*sizeof(char));
        auto pv = (unsigned long) con;
        return pv;
    }

    static jlong fskCreate(JNIEnv *env, jclass clazz, int sampleFrequency, int symbolRate, int toneFreq, int toneSpacing) {
        struct ContextFsk *conFsk;
        conFsk = (struct ContextFsk *) malloc(sizeof(struct ContextFsk));
        struct FSK *fsk;
        fsk = fsk_create_hbr(sampleFrequency, symbolRate, MODE_2FSK, sampleFrequency/symbolRate, FSK_DEFAULT_NSYM, toneFreq, toneSpacing);
        conFsk->fsk = fsk;

        conFsk->Nbits = fsk->Nbits;
        conFsk->N = fsk->N;
        conFsk->Ts = fsk->Ts;

        conFsk->modBuf = (float*)malloc(sizeof(float) * conFsk->N);
        conFsk->modBits = (uint8_t*)malloc(sizeof(uint8_t) * conFsk->Nbits);

        conFsk->demodCBuf = (COMP*)malloc(sizeof(COMP) * (fsk->N + 2 * fsk->Ts));
        conFsk->demodBits = (uint8_t*)malloc(sizeof(uint8_t) * fsk->Nbits);
        conFsk->demodBuf = (int16_t*)malloc(sizeof(short) * (fsk->N + 2 * fsk->Ts));

        //fsk_set_freq_est_limits(fsk, 500, 2700);
        fsk_set_freq_est_limits(fsk, 0, sampleFrequency / 2);
        fsk_set_freq_est_alg(fsk, 0);

        auto pv = (unsigned long) conFsk;
        return pv;
    }

    static jint c2spf(JNIEnv *env, jclass clazz, jlong n) {
        Context *con = getContext(n);
        return con->nsam;
    }

    static jint c2bits(JNIEnv *env, jclass clazz, jlong n) {
        Context *con = getContext(n);
        return con->nbyte;
    }

    static jint fskDemodSamplesBufSize(JNIEnv * env, jclass clazz, jlong n) {
        ContextFsk *conFsk = getContextFsk(n);
        return sizeof(short) * (conFsk->N + 2 * conFsk->Ts);
    }

    static jint fskDemodBitsBufSize(JNIEnv * env, jclass clazz, jlong n) {
        ContextFsk *conFsk = getContextFsk(n);
        return sizeof(uint8_t) * conFsk->Nbits;
    }

    static jint fskModSamplesBufSize(JNIEnv * env, jclass clazz, jlong n) {
        ContextFsk *conFsk = getContextFsk(n);
        return conFsk->N;
    }

    static jint fskModBitsBufSize(JNIEnv * env, jclass clazz, jlong n) {
        ContextFsk *conFsk = getContextFsk(n);
        return conFsk->Nbits;
    }

    static jint fskSamplesPerSymbol(JNIEnv * env, jclass clazz, jlong n) {
        ContextFsk *conFsk = getContextFsk(n);
        return conFsk->Ts;
    }

    static jint fskNin(JNIEnv * env, jclass clazz, jlong n) {
        ContextFsk *conFsk = getContextFsk(n);
        return fsk_nin(conFsk->fsk);
    }

    static jint destroy(JNIEnv *env, jclass clazz, jlong n) {
        Context *con = getContext(n);
        codec2_destroy(con->c2);
        free(con->bits);
        free(con->buf);
        free(con);
        return 0;
    }

    static jint fskDestroy(JNIEnv *env, jclass clazz, jlong n) {
        ContextFsk *conFsk = getContextFsk(n);
        fsk_destroy(conFsk->fsk);
        free(conFsk->demodBuf);
        free(conFsk->demodBits);
        free(conFsk->demodCBuf);
        free(conFsk->modBits);
        free(conFsk->modBuf);
        free(conFsk);
        return 0;
    }

    static jlong encode(JNIEnv *env, jclass clazz, jlong n, jshortArray inputBuffer, jcharArray outputBits) {
        Context *con = getContext(n);
        jshort *jbuf = env->GetShortArrayElements(inputBuffer, nullptr);
        for (int i = 0; i < con->nsam; i++) {
            auto v = (short) jbuf[i];
            con->buf[i] = v;
        }
        env->ReleaseShortArrayElements(inputBuffer, jbuf, 0);

        codec2_encode(con->c2, con->bits, con->buf);

        jchar *jbits = env->GetCharArrayElements(outputBits, nullptr);
        for (int i = 0; i < con->nbyte; i++) {
            jbits[i] = con->bits[i];
        }
        env->ReleaseCharArrayElements(outputBits, jbits, 0);
        return 0;
    }

    static jlong fskModulate(JNIEnv *env, jclass clazz, jlong n, jshortArray outputSamples, jbyteArray inputBits) {
        ContextFsk *conFsk = getContextFsk(n);
        int inputBitsSize = env->GetArrayLength(inputBits);
        env->GetByteArrayRegion(inputBits, 0, inputBitsSize, reinterpret_cast<jbyte*>(conFsk->modBits));
        fsk_mod(conFsk->fsk, conFsk->modBuf, conFsk->modBits, inputBitsSize);
        jshort *jOutBuf = env->GetShortArrayElements(outputSamples, nullptr);
        for (int i = 0; i < conFsk->N; i++) {
            jOutBuf[i] = (int16_t)(conFsk->modBuf[i] * FDMDV_SCALE);
        }
        env->ReleaseShortArrayElements(outputSamples, jOutBuf, 0);
        return 0;
    }

    static jlong decode(JNIEnv *env, jclass clazz, jlong n, jshortArray outputSamples, jbyteArray inputBits) {
        Context *con = getContext(n);
        env->GetByteArrayRegion(inputBits, 0, con->nbyte, reinterpret_cast<jbyte*>(con->bits));
        codec2_decode_ber(con->c2, con->buf, con->bits, 0.0);
        env->SetShortArrayRegion(outputSamples, 0, con->nsam, con->buf);
        return 0;
    }

    static jlong fskDemodulate(JNIEnv * env, jclass clazz, jlong n, jshortArray inputSamples, jbyteArray outputBits) {
        ContextFsk *conFsk = getContextFsk(n);
        env->GetShortArrayRegion(inputSamples, 0, conFsk->N, reinterpret_cast<jshort*>(conFsk->demodBuf));
        for(int i = 0; i < fsk_nin(conFsk->fsk); i++){
            conFsk->demodCBuf[i].real = ((float)conFsk->demodBuf[i]) / FDMDV_SCALE;
            conFsk->demodCBuf[i].imag = 0.0;
        }
        fsk_demod(conFsk->fsk, conFsk->demodBits, conFsk->demodCBuf);
        env->SetByteArrayRegion(outputBits, 0, conFsk->Nbits, reinterpret_cast<const jbyte *>(conFsk->demodBits));
        return 0;
    }

    static JNINativeMethod method_table[] = {
        {"create",             "(I)J",     (void *) create},
        {"getSamplesPerFrame", "(J)I",     (void *) c2spf},
        {"getBitsSize",        "(J)I",     (void *) c2bits},
        {"destroy",            "(J)I",     (void *) destroy},
        {"encode",             "(J[S[C)J", (void *) encode},
        {"decode",             "(J[S[B)J", (void *) decode},
        {"fskCreate",          "(IIII)J",  (void *) fskCreate},
        {"fskDestroy",         "(J)I",     (void *) fskDestroy},
        {"fskModulate",        "(J[S[B)J", (void *) fskModulate},
        {"fskDemodulate",      "(J[S[B)J", (void *) fskDemodulate},
        {"fskDemodBitsBufSize","(J)I",     (void *) fskDemodBitsBufSize},
        {"fskModSamplesBufSize","(J)I",    (void *) fskModSamplesBufSize},
        {"fskDemodSamplesBufSize","(J)I",  (void *) fskDemodSamplesBufSize},
        {"fskModBitsBufSize",  "(J)I",     (void *) fskModBitsBufSize},
        {"fskSamplesPerSymbol","(J)I",     (void *) fskSamplesPerSymbol},
        {"fskNin",             "(J)I",     (void *) fskNin}
    };
}

using namespace Java_com_ustadmobile_codec2_Codec2;

extern "C" jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    } else {
        jclass clazz = env->FindClass("com/ustadmobile/codec2/Codec2");
        if (clazz) {
            jint ret = env->RegisterNatives(clazz, method_table,
                                            sizeof(method_table) / sizeof(method_table[0]));
            env->DeleteLocalRef(clazz);
            return ret == 0 ? JNI_VERSION_1_6 : JNI_ERR;
        } else {
            return JNI_ERR;
        }
    }
}