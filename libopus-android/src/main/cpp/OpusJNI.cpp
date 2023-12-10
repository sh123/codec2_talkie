#include <jni.h>
#include <cstdlib>
#include "opus.h"

namespace Java_com_radio_opus_Opus {

    struct Context {
        OpusEncoder* encoder;
        OpusDecoder* decoder;
    };

    static Context *getContext(jlong jp) {
        auto p = (unsigned long) jp;
        Context *con;
        con = (Context *) p;
        return con;
    }

    static jlong create(JNIEnv *env, jclass clazz, jint sampleRate, jint numChannels, jint application, jint bitrate, jint complexity) {
        struct Context *con;
        con = (struct Context *) malloc(sizeof(struct Context));

        int encoderError;
        OpusEncoder* encoder = opus_encoder_create(sampleRate, numChannels, application, &encoderError);
        if (!encoderError) {
            encoderError = opus_encoder_init(encoder, sampleRate, numChannels, application);

            opus_encoder_ctl(encoder, OPUS_SET_BITRATE(bitrate));
            opus_encoder_ctl(encoder, OPUS_SET_COMPLEXITY(complexity));
            opus_encoder_ctl(encoder, OPUS_SET_SIGNAL(OPUS_SIGNAL_VOICE));
        }

        int decoderError;
        OpusDecoder* decoder = opus_decoder_create(sampleRate, numChannels, &decoderError);
        if (!decoderError) {
            decoderError = opus_decoder_init(decoder, sampleRate, numChannels);
        }

        if (encoderError || decoderError) {
            free(encoder);
            free(decoder);
            free(con);
            return 0;
        }

        con->decoder = decoder;
        con->encoder = encoder;

        auto pv = (unsigned long) con;
        return pv;
    }

    static jint destroy(JNIEnv *env, jclass clazz, jlong n) {
        Context *con = getContext(n);
        free(con->encoder);
        free(con->decoder);
        free(con);
        return 0;
    }

    static jint decode(JNIEnv *env, jclass clazz, jlong n, jbyteArray in, jshortArray out, jint frames)
    {
        Context *con = getContext(n);
        OpusDecoder *decoder = con->decoder;

        jint inputArraySize = env->GetArrayLength(in);

        jbyte* encodedData = env->GetByteArrayElements(in, 0);
        jshort* decodedData = env->GetShortArrayElements(out, 0);

        int samples = opus_decode(decoder, (const unsigned char *)encodedData, inputArraySize, decodedData, frames, 0);

        env->ReleaseByteArrayElements(in, encodedData, JNI_ABORT);
        env->ReleaseShortArrayElements(out, decodedData, 0);

        return samples;
    }

    static jint encode(JNIEnv *env, jclass clazz, jlong n, jshortArray in, jint frames, jbyteArray out)
    {
        Context *con = getContext(n);
        OpusEncoder *encoder = con->encoder;

        jint outputArraySize = env->GetArrayLength(out);

        jshort* audioSignal = env->GetShortArrayElements(in, 0);
        jbyte* encodedSignal = env->GetByteArrayElements(out, 0);

        int dataArraySize = opus_encode(encoder, audioSignal, frames, (unsigned char *)encodedSignal, outputArraySize);

        env->ReleaseShortArrayElements(in, audioSignal, JNI_ABORT);
        env->ReleaseByteArrayElements(out, encodedSignal, 0);

        return dataArraySize;
    }

    static JNINativeMethod method_table[] = {
        {"create", "(IIIII)J",                  (void *) create },
        {"destroy", "(J)I",                     (void *) destroy },
        {"decode", "(J[B[SI)I",                 (void *) decode },
        {"encode", "(J[SI[B)I",                 (void *) encode }
    };
}

using namespace Java_com_radio_opus_Opus;

extern "C" jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    } else {
        jclass clazz = env->FindClass("com/radio/opus/Opus");
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
