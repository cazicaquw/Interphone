#include <jni.h>

#include <string.h>
#include <unistd.h>

#include <speex/speex.h>

//used for echo cancellation
#include <speex/speex_echo.h>
SpeexEchoState *echostate;
//used for preprocessor
#include <speex/speex_preprocess.h>
SpeexPreprocessState *preprocess_state;

static int eco_frame_size;
static int eco_open;
static int codec_open = 0;

static int dec_frame_size;
static int enc_frame_size;


static SpeexBits ebits, dbits;
void *enc_state;
void *dec_state;

static JavaVM *gJavaVM;

extern "C"{
JNIEXPORT jint JNICALL Java_jay_codec_Speex_open
  (JNIEnv *env, jobject obj, jint compression) {
	int tmp;

	if (codec_open++ != 0)
		return (jint)0;

	speex_bits_init(&ebits);
	speex_bits_init(&dbits);

	enc_state = speex_encoder_init(&speex_nb_mode);
	dec_state = speex_decoder_init(&speex_nb_mode);
	tmp = compression;
	speex_encoder_ctl(enc_state, SPEEX_SET_QUALITY, &tmp);
	speex_encoder_ctl(enc_state, SPEEX_GET_FRAME_SIZE, &enc_frame_size);
	speex_decoder_ctl(dec_state, SPEEX_GET_FRAME_SIZE, &dec_frame_size);

	return (jint)0;
}

//extern "C"
JNIEXPORT jint Java_jay_codec_Speex_encode
    (JNIEnv *env, jobject obj, jshortArray lin, jint offset, jbyteArray encoded, jint size) {

        jshort buffer[enc_frame_size];
        jbyte output_buffer[enc_frame_size];
	int nsamples = (size-1)/enc_frame_size + 1;
	int i, tot_bytes = 0;

	if (!codec_open)
		return 0;

	speex_bits_reset(&ebits);

	for (i = 0; i < nsamples; i++) {
		env->GetShortArrayRegion(lin, offset + i*enc_frame_size, enc_frame_size, buffer);
		speex_encode_int(enc_state, buffer, &ebits);
	}
	//env->GetShortArrayRegion(lin, offset, enc_frame_size, buffer);
	//speex_encode_int(enc_state, buffer, &ebits);

	tot_bytes = speex_bits_write(&ebits, (char *)output_buffer,
				     enc_frame_size);
	env->SetByteArrayRegion(encoded, 0, tot_bytes,
				output_buffer);

        return (jint)tot_bytes;
}

//extern "C"
JNIEXPORT jint JNICALL Java_jay_codec_Speex_decode
    (JNIEnv *env, jobject obj, jbyteArray encoded, jshortArray lin, jint size) {

        jbyte buffer[dec_frame_size];
        jshort output_buffer[dec_frame_size];
        jsize encoded_length = size;

	if (!codec_open)
		return 0;

	env->GetByteArrayRegion(encoded, 0, encoded_length, buffer);
	speex_bits_read_from(&dbits, (char *)buffer, encoded_length);
	speex_decode_int(dec_state, &dbits, output_buffer);
	env->SetShortArrayRegion(lin, 0, dec_frame_size,
				 output_buffer);

	return (jint)dec_frame_size;
}

//extern "C"
JNIEXPORT jint JNICALL Java_jay_codec_Speex_getFrameSize
    (JNIEnv *env, jobject obj) {

	if (!codec_open)
		return 0;
	return (jint)enc_frame_size;

}

//extern "C"
JNIEXPORT void JNICALL Java_jay_codec_Speex_close
    (JNIEnv *env, jobject obj) {

	if (--codec_open != 0)
		return;

	speex_bits_destroy(&ebits);
	speex_bits_destroy(&dbits);
	speex_decoder_destroy(dec_state);
	speex_encoder_destroy(enc_state);
}

JNIEXPORT jint JNICALL Java_jay_codec_Speex_echoinit
    (JNIEnv *env, jobject obj,jint framesize,jint filterlength) {
	if (eco_open++ != 0)
			return (jint)0;

	echostate = speex_echo_state_init(framesize,filterlength);
	preprocess_state = speex_preprocess_state_init(framesize, framesize*50);
	speex_preprocess_ctl(preprocess_state, SPEEX_PREPROCESS_SET_ECHO_STATE, echostate);
	jint denoise = 1;
	jint noiseSuppress = -25;
	speex_preprocess_ctl(preprocess_state, SPEEX_PREPROCESS_SET_DENOISE, &denoise); //denoise
	speex_preprocess_ctl(preprocess_state, SPEEX_PREPROCESS_SET_NOISE_SUPPRESS, &noiseSuppress); //noise dB
	jint vad = 1;
	jint vadProbStart = 80;
	jint vadProbContinue = 65;
	speex_preprocess_ctl(preprocess_state, SPEEX_PREPROCESS_SET_VAD, &vad); //silence detect
	speex_preprocess_ctl(preprocess_state, SPEEX_PREPROCESS_SET_PROB_START , &vadProbStart); //Set probability required for the VAD to go from silence to voice
	speex_preprocess_ctl(preprocess_state, SPEEX_PREPROCESS_SET_PROB_CONTINUE, &vadProbContinue); //Set probability required for the VAD to stay in the voice state (integer percent)


	eco_frame_size = framesize;
	if(echostate == 0)
		return (jint)0;
	else
		return (jint)1;
	//speex_preprocess_ctl(preprocess_state, SPEEX_PREPROCESS_SET_ECHO_STATE,echo_state);
}

JNIEXPORT jint JNICALL Java_jay_codec_Speex_echoplayback
    (JNIEnv *env, jobject obj, jshortArray play) {
	if (!eco_open)
			return 0;
	jshort temp_buffer[eco_frame_size];
	env->GetShortArrayRegion(play, 0, eco_frame_size, temp_buffer);
	speex_echo_playback(echostate,temp_buffer);
	return (jint)eco_frame_size;
}

JNIEXPORT jint JNICALL Java_jay_codec_Speex_echocapture
    (JNIEnv *env, jobject obj, jshortArray rec, jshortArray out) {
	if (!eco_open)
				return 0;
	jshort temp_buffer[eco_frame_size];
	jshort temp_out[eco_frame_size];
	env->GetShortArrayRegion(rec, 0, eco_frame_size, temp_buffer);
	jint speech = speex_preprocess_run(preprocess_state, temp_buffer);
	speex_echo_capture(echostate,temp_buffer,temp_out);
	env->SetShortArrayRegion(out,0,eco_frame_size,temp_out);
	return (jint)speech;
}

JNIEXPORT void JNICALL Java_jay_codec_Speex_echoclose
    (JNIEnv *env, jobject obj) {
	if (--eco_open != 0)
		return;
	 speex_echo_state_destroy(echostate);
	 speex_preprocess_state_destroy(preprocess_state);
}


}

