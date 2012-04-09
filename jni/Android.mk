LOCAL_PATH := $(call my-dir)
SPEEX	:= speex-1.2rc1

include $(CLEAR_VARS)
 
LOCAL_MODULE    := speex_jni
LOCAL_CFLAGS = -DFIXED_POINT -DUSE_KISS_FFT -DEXPORT="" -UHAVE_CONFIG_H 
LOCAL_C_INCLUDES := $(LOCAL_PATH)/$(SPEEX)/include
 
LOCAL_SRC_FILES := speex_jni.cpp \
$(SPEEX)/libspeex/bits.c \
$(SPEEX)/libspeex/buffer.c \
$(SPEEX)/libspeex/cb_search.c \
$(SPEEX)/libspeex/exc_10_16_table.c \
$(SPEEX)/libspeex/exc_10_32_table.c \
$(SPEEX)/libspeex/exc_20_32_table.c \
$(SPEEX)/libspeex/exc_5_256_table.c \
$(SPEEX)/libspeex/exc_5_64_table.c \
$(SPEEX)/libspeex/exc_8_128_table.c \
$(SPEEX)/libspeex/fftwrap.c \
$(SPEEX)/libspeex/filterbank.c \
$(SPEEX)/libspeex/filters.c \
$(SPEEX)/libspeex/gain_table.c \
$(SPEEX)/libspeex/gain_table_lbr.c \
$(SPEEX)/libspeex/hexc_10_32_table.c \
$(SPEEX)/libspeex/hexc_table.c \
$(SPEEX)/libspeex/high_lsp_tables.c \
$(SPEEX)/libspeex/jitter.c \
$(SPEEX)/libspeex/kiss_fft.c \
$(SPEEX)/libspeex/kiss_fftr.c \
$(SPEEX)/libspeex/lpc.c \
$(SPEEX)/libspeex/lsp.c \
$(SPEEX)/libspeex/lsp_tables_nb.c \
$(SPEEX)/libspeex/ltp.c \
$(SPEEX)/libspeex/mdf.c \
$(SPEEX)/libspeex/modes.c \
$(SPEEX)/libspeex/modes_wb.c \
$(SPEEX)/libspeex/nb_celp.c \
$(SPEEX)/libspeex/preprocess.c \
$(SPEEX)/libspeex/quant_lsp.c \
$(SPEEX)/libspeex/resample.c \
$(SPEEX)/libspeex/sb_celp.c \
$(SPEEX)/libspeex/scal.c \
$(SPEEX)/libspeex/smallft.c \
$(SPEEX)/libspeex/speex.c \
$(SPEEX)/libspeex/speex_callbacks.c \
$(SPEEX)/libspeex/speex_header.c \
$(SPEEX)/libspeex/stereo.c \
$(SPEEX)/libspeex/vbr.c \
$(SPEEX)/libspeex/vq.c \
$(SPEEX)/libspeex/window.c \


include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
SPANDSP     := spandsp
LOCAL_MODULE    := g722_jni
LOCAL_SRC_FILES := g722_jni.cpp \
	$(SPANDSP)/g722.c \
	$(SPANDSP)/vector_int.c
LOCAL_ARM_MODE := arm
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog
LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(SPANDSP)/spandsp $(LOCAL_PATH)/$(SPANDSP)
LOCAL_CFLAGS = -O3 
include $(BUILD_SHARED_LIBRARY)