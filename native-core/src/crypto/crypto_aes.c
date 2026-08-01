#include <jni.h>
#include <openssl/evp.h>
#include <stdbool.h>

JNIEXPORT jlong JNICALL
Java_dev_shiro8613_fixio_nativeapi_crypto_NativeCipher_initContext(
    JNIEnv *env, jclass clazz, jboolean isEncrypt, jbyteArray secretArray) {

    jsize secretLen = (*env)->GetArrayLength(env, secretArray);
    if (secretLen != 16) {
        return 0; 
    }

    
    jbyte *secretBytes = (*env)->GetByteArrayElements(env, secretArray, NULL);
    if (secretBytes == NULL) {
        return 0;
    }

    EVP_CIPHER_CTX *ctx = EVP_CIPHER_CTX_new();
    if (ctx == NULL) {
        (*env)->ReleaseByteArrayElements(env, secretArray, secretBytes, JNI_ABORT);
        return 0;
    }

    const EVP_CIPHER *cipher = EVP_aes_128_cfb8();
    int res = 0;

    
    const unsigned char *keyAndIv = (const unsigned char *)secretBytes;

    if (isEncrypt) {
        res = EVP_EncryptInit_ex(ctx, cipher, NULL, keyAndIv, keyAndIv);
    } else {
        res = EVP_DecryptInit_ex(ctx, cipher, NULL, keyAndIv, keyAndIv);
    }

    (*env)->ReleaseByteArrayElements(env, secretArray, secretBytes, JNI_ABORT);

    if (res != 1) {
        EVP_CIPHER_CTX_free(ctx);
        return 0;
    }

    
    return (jlong)(uintptr_t)ctx;
}

JNIEXPORT jint JNICALL
Java_dev_shiro8613_fixio_nativeapi_crypto_NativeCipher_processDirect(
    JNIEnv *env, jclass clazz, jlong ctxPtr, jlong srcAddr, jlong dstAddr, jint len, jboolean isEncrypt) {

    if (ctxPtr == 0 || srcAddr == 0 || dstAddr == 0 || len <= 0) {
        return -1;
    }

    EVP_CIPHER_CTX *ctx = (EVP_CIPHER_CTX *)(uintptr_t)ctxPtr;
    const unsigned char *src = (const unsigned char *)(uintptr_t)srcAddr;
    unsigned char *dst = (unsigned char *)(uintptr_t)dstAddr;

    int outLen = 0;
    int res = 0;

    
    if (isEncrypt) {
        res = EVP_EncryptUpdate(ctx, dst, &outLen, src, len);
    } else {
        res = EVP_DecryptUpdate(ctx, dst, &outLen, src, len);
    }

    if (res != 1) {
        return -1;
    }

    return outLen; 
}

JNIEXPORT jint JNICALL
Java_dev_shiro8613_fixio_nativeapi_crypto_NativeCipher_processByteBuffer(
    JNIEnv *env, jclass clazz, jlong ctxPtr, jobject srcBuf, jobject dstBuf, jint len, jboolean isEncrypt) {

    if (ctxPtr == 0 || srcBuf == NULL || dstBuf == NULL || len <= 0) {
        return -1;
    }

    void *srcAddr = (*env)->GetDirectBufferAddress(env, srcBuf);
    void *dstAddr = (*env)->GetDirectBufferAddress(env, dstBuf);

    if (srcAddr == NULL || dstAddr == NULL) {
        return -2; 
    }

    EVP_CIPHER_CTX *ctx = (EVP_CIPHER_CTX *)(uintptr_t)ctxPtr;
    int outLen = 0;
    int res = 0;

    if (isEncrypt) {
        res = EVP_EncryptUpdate(ctx, (unsigned char *)dstAddr, &outLen, (const unsigned char *)srcAddr, len);
    } else {
        res = EVP_DecryptUpdate(ctx, (unsigned char *)dstAddr, &outLen, (const unsigned char *)srcAddr, len);
    }

    if (res != 1) {
        return -1;
    }

    return outLen;
}

JNIEXPORT void JNICALL
Java_dev_shiro8613_fixio_nativeapi_crypto_NativeCipher_freeContext(
    JNIEnv *env, jclass clazz, jlong ctxPtr) {

    if (ctxPtr != 0) {
        EVP_CIPHER_CTX *ctx = (EVP_CIPHER_CTX *)(uintptr_t)ctxPtr;
        EVP_CIPHER_CTX_free(ctx);
    }
}